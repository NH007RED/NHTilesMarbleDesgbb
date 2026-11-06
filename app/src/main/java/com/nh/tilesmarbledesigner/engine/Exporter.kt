package com.nh.tilesmarbledesigner.engine

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.nh.tilesmarbledesigner.data.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Exporter {
    internal fun dir(ctx: Context) = File(ctx.cacheDir, "exports").apply { mkdirs() }
    internal fun safe(n: String) = n.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifEmpty { "design" }

    fun savePng(ctx: Context, b: Bitmap, name: String): File {
        val f = File(dir(ctx), safe(name) + ".png")
        f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    fun share(ctx: Context, f: File, mime: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Saves a PNG into the phone gallery (Pictures/NH Tiles & Marble). Returns the folder, or null on failure. */
    fun saveToGallery(ctx: Context, b: Bitmap, name: String): String? {
        val fname = safe(name) + "_" + System.currentTimeMillis() + ".png"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val folder = "Pictures/NH Tiles & Marble"
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fname)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, folder)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv) ?: return null
                ctx.contentResolver.openOutputStream(uri)?.use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                cv.clear(); cv.put(MediaStore.Images.Media.IS_PENDING, 0)
                ctx.contentResolver.update(uri, cv, null, null)
                folder
            } else {
                val dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "NH Tiles & Marble").apply { mkdirs() }
                File(dir, fname).outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                dir.absolutePath
            }
        } catch (e: Throwable) { null }
    }

    /** Sends a file straight to WhatsApp (or WhatsApp Business). Returns false if neither is installed. */
    fun sendWhatsApp(ctx: Context, f: File, mime: String, text: String = ""): Boolean {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = mime; putExtra(Intent.EXTRA_STREAM, uri)
                if (text.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, text)
                setPackage(pkg); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { ctx.startActivity(i); return true } catch (e: ActivityNotFoundException) { /* try next package */ }
        }
        return false
    }

    fun copyTo(ctx: Context, f: File, uri: Uri) {
        ctx.contentResolver.openOutputStream(uri)?.use { o -> f.inputStream().use { it.copyTo(o) } }
    }

    private class Doc {
        val pdf = PdfDocument()
        var page: PdfDocument.Page? = null
        lateinit var c: Canvas
        var y = 0f
        var n = 0
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.BLACK }
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF0B1530.toInt() }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF0B1530.toInt() }

        fun newPage() {
            page?.let { footer(); pdf.finishPage(it) }
            n++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, n).create())
            c = page!!.canvas; y = 50f
        }

        private fun footer() {
            c.drawText("NH Developer - NH Tiles & Marble Room Designer", 40f, 820f, body.apply { textSize = 9f; color = Color.GRAY })
            body.textSize = 11f; body.color = Color.BLACK
        }

        fun need(h: Float) { if (page == null || y + h > 800f) newPage() }

        fun line(s: String, p: Paint = body, gap: Float = 16f) { need(gap); c.drawText(s, 40f, y, p); y += gap }

        fun wrap(s: String) {
            var cur = ""
            for (word in s.split(" ")) {
                if (body.measureText("$cur $word") > 500f) { line(cur); cur = word } else cur = if (cur.isEmpty()) word else "$cur $word"
            }
            if (cur.isNotEmpty()) line(cur)
        }

        fun finish(f: File) {
            page?.let { footer(); pdf.finishPage(it) }
            f.outputStream().use { pdf.writeTo(it) }
            pdf.close()
        }
    }

    fun createPdf(ctx: Context, p: ProjectData, r: Calc.Report, preview: Bitmap?): File {
        val u = p.unit
        fun d(m: Double) = String.format(Locale.US, "%.2f", u.fromM(m)) + " " + u.label
        fun a(m2: Double) = String.format(Locale.US, "%.2f", u.areaFromM2(m2)) + " " + u.areaLabel
        val doc = Doc(); doc.newPage()
        doc.line("NH Tiles & Marble Room Designer", doc.title, 26f)
        doc.line("Calculation report: ${p.name}", doc.bold)
        doc.line("Generated " + SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date()), gap = 22f)
        if (preview != null) {
            val w = 515f; val h = w * preview.height / preview.width
            doc.need(h + 10)
            doc.c.drawBitmap(preview, null, RectF(40f, doc.y, 40f + w, doc.y + h), Paint(Paint.FILTER_BITMAP_FLAG))
            doc.y += h + 18f
        }
        doc.line("Room", doc.bold)
        p.sections.forEachIndexed { i, s -> doc.line("Floor section ${i + 1}: ${d(s.length)} x ${d(s.width)}  =  ${a(s.length * s.width)}") }
        doc.line("Wall height: ${d(p.height)}    Total floor area: ${a(r.floorArea)}", gap = 22f)
        doc.line("Floor tiles", doc.bold)
        val ft = p.floorTile; val f = r.floor
        doc.line("Tile size: ${d(ft.l)} x ${d(ft.w)}    Tiles per box: ${ft.perBox}    Wastage: ${ft.wastagePct}%    Spare: ${ft.spare}")
        doc.line("Tiles for exact area: ${f.baseTiles}    Tiles required (incl. wastage/spare): ${f.requiredTiles}")
        doc.line("Boxes required: ${f.boxes}    Tiles purchased: ${f.purchased}    Extra tiles: ${f.extra}", gap = 22f)
        doc.line("Walls", doc.bold)
        r.walls.forEach { w -> doc.line("Wall ${w.index}: ${d(w.length)} x ${d(w.height)} = ${a(w.gross)}, openings -${a(w.openings)}, net ${a(w.net)}") }
        doc.line("Net wall area: ${a(r.wallNet)}  (gross ${a(r.wallGross)}, openings ${a(r.openingsArea)})")
        val wt = p.wallTile; val w = r.wall
        doc.line("Wall tile: ${d(wt.l)} x ${d(wt.w)}    Tiles per box: ${wt.perBox}    Wastage: ${wt.wastagePct}%    Spare: ${wt.spare}")
        doc.line("Tiles required: ${w.requiredTiles}    Boxes: ${w.boxes}    Purchased: ${w.purchased}    Extra: ${w.extra}", gap = 22f)
        doc.line("Marble / granite slabs (${d(p.slabL)} x ${d(p.slabW)})", doc.bold)
        doc.line("Floor slabs: ${r.floorSlabs}    Wall slabs: ${r.wallSlabs}", gap = 22f)
        doc.line("Total estimate", doc.bold)
        doc.line("Boxes (floor + wall): ${r.totalBoxes}    Tiles purchased: ${r.totalTiles}    Purchased area: ${a(r.totalPurchasedArea)}", gap = 22f)
        doc.line("Assumptions", doc.bold)
        r.assumptions.forEach { doc.wrap("- $it") }
        val out = File(dir(ctx), safe(p.name) + "_report.pdf")
        doc.finish(out)
        return out
    }
}

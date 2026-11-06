package com.nh.tilesmarbledesigner.engine

import android.content.Context
import android.graphics.Bitmap
import com.nh.tilesmarbledesigner.data.ProjectData
import com.nh.tilesmarbledesigner.data.Surf
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exports the room as a Wavefront OBJ + MTL with PNG textures (zipped). Units: metres, Y up.
 * Opens in Blender, Windows 3D Viewer, MeshLab, Sketchfab and most Android/iOS 3D viewers.
 * Doors and windows are painted on the wall textures; furniture is exported as coloured boxes.
 */
object ModelExporter {
    private fun num(x: Float) = String.format(Locale.US, "%.4f", x)

    private fun surfacePoint(s: Surf, u: Float, v: Float, L: Float, W: Float, H: Float): FloatArray = when (s) {
        Surf.FLOOR -> floatArrayOf(-L / 2 + u * L, 0f, W / 2 - v * W)
        Surf.CEILING -> floatArrayOf(-L / 2 + u * L, H, W / 2 - v * W)
        Surf.BACK -> floatArrayOf(-L / 2 + u * L, H - v * H, W / 2)
        Surf.FRONT -> floatArrayOf(L / 2 - u * L, H - v * H, -W / 2)
        Surf.LEFT -> floatArrayOf(-L / 2, H - v * H, W / 2 - u * W)
        Surf.RIGHT -> floatArrayOf(L / 2, H - v * H, -W / 2 + u * W)
    }

    private fun inward(s: Surf): FloatArray = when (s) {
        Surf.FLOOR -> floatArrayOf(0f, 1f, 0f)
        Surf.CEILING -> floatArrayOf(0f, -1f, 0f)
        Surf.BACK -> floatArrayOf(0f, 0f, -1f)
        Surf.FRONT -> floatArrayOf(0f, 0f, 1f)
        Surf.LEFT -> floatArrayOf(1f, 0f, 0f)
        Surf.RIGHT -> floatArrayOf(-1f, 0f, 0f)
    }

    private class ObjWriter {
        val sb = StringBuilder()
        var vi = 1; var ti = 1; var ni = 1

        /** Writes a quad, flipping the vertex order so it is counter-clockwise when seen from the side [n] points to. */
        fun quad(p: List<FloatArray>, uv: List<FloatArray>?, n: FloatArray) {
            val e1 = floatArrayOf(p[1][0] - p[0][0], p[1][1] - p[0][1], p[1][2] - p[0][2])
            val e2 = floatArrayOf(p[2][0] - p[0][0], p[2][1] - p[0][1], p[2][2] - p[0][2])
            val cx = e1[1] * e2[2] - e1[2] * e2[1]
            val cy = e1[2] * e2[0] - e1[0] * e2[2]
            val cz = e1[0] * e2[1] - e1[1] * e2[0]
            val flip = cx * n[0] + cy * n[1] + cz * n[2] < 0f
            val pts = if (flip) p.reversed() else p
            val uvs = if (flip) uv?.reversed() else uv
            pts.forEach { sb.append("v ").append(num(it[0])).append(' ').append(num(it[1])).append(' ').append(num(it[2])).append('\n') }
            uvs?.forEach { sb.append("vt ").append(num(it[0])).append(' ').append(num(it[1])).append('\n') }
            sb.append("vn ").append(num(n[0])).append(' ').append(num(n[1])).append(' ').append(num(n[2])).append('\n')
            sb.append('f')
            for (i in 0 until 4) {
                sb.append(' ').append(vi + i)
                if (uvs != null) sb.append('/').append(ti + i) else sb.append('/')
                sb.append('/').append(ni)
            }
            sb.append('\n')
            vi += 4; if (uvs != null) ti += 4; ni += 1
        }
    }

    fun exportZip(ctx: Context, p: ProjectData): File {
        val L = p.roomL.toFloat(); val W = p.roomW.toFloat(); val H = p.height.toFloat()
        val textures = Scene.surfaceBitmaps(p, p.designs, 1024)
        val w = ObjWriter()
        val mtl = StringBuilder()
        w.sb.append("# NH Tiles & Marble Room Designer - ").append(p.name).append("\n# Units: metres, Y up\nmtllib room.mtl\n")

        val uvCorners = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)
        for (s in Surf.values()) {
            val name = s.name.lowercase()
            mtl.append("newmtl surf_").append(name).append("\nKa 1 1 1\nKd 1 1 1\nKs 0.1 0.1 0.1\nNs 20\nmap_Kd textures/").append(name).append(".png\n\n")
            w.sb.append("o ").append(s.label.replace(' ', '_')).append("\nusemtl surf_").append(name).append('\n')
            val pts = uvCorners.map { (u, v) -> surfacePoint(s, u, v, L, W, H) }
            val uvs = uvCorners.map { (u, v) -> floatArrayOf(u, 1f - v) }
            w.quad(pts, uvs, inward(s))
        }

        val boxes = FurnitureLib.layout(p)
        val colours = LinkedHashSet<Int>()
        boxes.forEachIndexed { i, b ->
            colours += b.color
            w.sb.append("o Furniture_").append(i + 1).append("\nusemtl col_").append(String.format("%06X", b.color and 0xFFFFFF)).append('\n')
            val x0 = b.cx - b.w / 2; val x1 = b.cx + b.w / 2
            val z0 = b.cz - b.d / 2; val z1 = b.cz + b.d / 2
            val y0 = b.y0; val y1 = b.y0 + b.h
            fun q(n: FloatArray, vararg c: FloatArray) = w.quad(c.toList(), null, n)
            q(floatArrayOf(0f, 1f, 0f), floatArrayOf(x0, y1, z0), floatArrayOf(x1, y1, z0), floatArrayOf(x1, y1, z1), floatArrayOf(x0, y1, z1))
            q(floatArrayOf(0f, -1f, 0f), floatArrayOf(x0, y0, z0), floatArrayOf(x1, y0, z0), floatArrayOf(x1, y0, z1), floatArrayOf(x0, y0, z1))
            q(floatArrayOf(0f, 0f, -1f), floatArrayOf(x0, y0, z0), floatArrayOf(x1, y0, z0), floatArrayOf(x1, y1, z0), floatArrayOf(x0, y1, z0))
            q(floatArrayOf(0f, 0f, 1f), floatArrayOf(x0, y0, z1), floatArrayOf(x1, y0, z1), floatArrayOf(x1, y1, z1), floatArrayOf(x0, y1, z1))
            q(floatArrayOf(-1f, 0f, 0f), floatArrayOf(x0, y0, z0), floatArrayOf(x0, y0, z1), floatArrayOf(x0, y1, z1), floatArrayOf(x0, y1, z0))
            q(floatArrayOf(1f, 0f, 0f), floatArrayOf(x1, y0, z0), floatArrayOf(x1, y0, z1), floatArrayOf(x1, y1, z1), floatArrayOf(x1, y1, z0))
        }
        colours.forEach { c ->
            mtl.append("newmtl col_").append(String.format("%06X", c and 0xFFFFFF)).append('\n')
                .append("Kd ").append(num((c shr 16 and 255) / 255f)).append(' ').append(num((c shr 8 and 255) / 255f)).append(' ')
                .append(num((c and 255) / 255f)).append("\nKs 0.05 0.05 0.05\nNs 10\n\n")
        }

        val out = File(Exporter.dir(ctx), Exporter.safe(p.name) + "_3D.zip")
        ZipOutputStream(out.outputStream().buffered()).use { z ->
            fun put(name: String, bytes: ByteArray) { z.putNextEntry(ZipEntry(name)); z.write(bytes); z.closeEntry() }
            put("room.obj", w.sb.toString().toByteArray())
            put("room.mtl", mtl.toString().toByteArray())
            for ((s, bmp) in textures) {
                val bos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
                put("textures/${s.name.lowercase()}.png", bos.toByteArray())
            }
            put("README.txt", ("${p.name}\nRoom ${num(L)} x ${num(W)} x ${num(H)} m (length x width x height).\n" +
                "Import room.obj (keep room.mtl and the textures folder next to it).\n" +
                "Units are metres, Y axis is up. Doors and windows are painted on the wall textures.\n" +
                "Generated by NH Tiles & Marble Room Designer - NH Developer.\n").toByteArray())
        }
        return out
    }
}

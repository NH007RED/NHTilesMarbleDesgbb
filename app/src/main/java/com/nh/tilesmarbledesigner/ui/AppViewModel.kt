package com.nh.tilesmarbledesigner.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nh.tilesmarbledesigner.data.*
import com.nh.tilesmarbledesigner.engine.Materials
import com.nh.tilesmarbledesigner.engine.Scene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val repo = Repository(AppDb.get(ctx))
    private val store = SettingsStore(ctx)

    val settings = store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val projects = repo.projects.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val customMaterials = repo.materials.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The project currently being edited (new or loaded). */
    var draft by mutableStateOf(ProjectData.default())
    var draftId by mutableStateOf<Long?>(null)
    var selectedSurface by mutableStateOf(Surf.FLOOR)
    var toast by mutableStateOf<String?>(null)

    var cropSource by mutableStateOf<Bitmap?>(null)
    var cropResult by mutableStateOf<Bitmap?>(null)

    fun update(f: (ProjectData) -> ProjectData) { draft = f(draft) }

    fun newDraft(name: String, shape: RoomShape, unit: LengthUnit, l: Double, w: Double, h: Double) {
        val s = settings.value
        draft = ProjectData.default(unit, s.wastage, s.groutMm, s.groutColor)
            .copy(name = name.ifBlank { "New Room" }, shape = shape)
            .withRoom(l, if (shape == RoomShape.SQUARE) l else w, h)
        draftId = null
    }

    fun loadTemplate(t: RoomTemplate) {
        draft = t.build().copy(unit = settings.value.unit)
        draftId = null
        selectedSurface = Surf.FLOOR
        toast = "${t.name} template loaded"
    }

    fun saveSettings(s: AppSettings) = viewModelScope.launch { store.save(s) }

    private fun thumbFile(id: Long) = File(File(ctx.filesDir, "thumbs").apply { mkdirs() }, "$id.png")

    fun save(onDone: (Long) -> Unit = {}) {
        val d = draft
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    val json = d.toJson()
                    val existing = draftId?.let { repo.get(it) }
                    val pid = existing?.id ?: repo.insert(ProjectEntity(name = d.name, createdAt = now, updatedAt = now, json = json, thumbPath = null))
                    val f = thumbFile(pid)
                    val b = Scene.render(d, d.designs, 480, 360, 22f, 28f, 1f)
                    f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 90, it) }
                    b.recycle()
                    repo.update(ProjectEntity(pid, d.name, existing?.createdAt ?: now, now, json, f.absolutePath))
                    pid
                }
                draftId = id
                toast = "Project saved"
                onDone(id)
            } catch (e: Throwable) { toast = "Could not save project" }
        }
    }

    fun open(id: Long, then: () -> Unit = {}) {
        viewModelScope.launch {
            val e = withContext(Dispatchers.IO) { repo.get(id) }
            if (e == null) { toast = "Project not found"; return@launch }
            draft = ProjectData.fromJson(e.json); draftId = id; selectedSurface = Surf.FLOOR
            then()
        }
    }

    suspend fun load(id: Long): ProjectEntity? = withContext(Dispatchers.IO) { repo.get(id) }

    fun duplicate(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val e = repo.get(id) ?: return@withContext
                val now = System.currentTimeMillis()
                val nid = repo.insert(e.copy(id = 0, name = e.name + " (copy)", createdAt = now, updatedAt = now, thumbPath = null))
                val tp = e.thumbPath?.let { File(it) }
                if (tp != null && tp.exists()) {
                    val nf = thumbFile(nid); tp.copyTo(nf, true)
                    repo.update(e.copy(id = nid, name = e.name + " (copy)", createdAt = now, updatedAt = now, thumbPath = nf.absolutePath))
                }
            }
            toast = "Project duplicated"
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.get(id)?.thumbPath?.let { File(it).delete() }; repo.delete(id) }
            if (draftId == id) draftId = null
            toast = "Project deleted"
        }
    }

    /** Saves the cropped bitmap as a reusable material; optionally applies it to a surface. */
    fun saveCustomMaterial(name: String, bmp: Bitmap, apply: Surf?, groutMm: Double? = null) {
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) {
                    val f = File(File(ctx.filesDir, "textures").apply { mkdirs() }, "tex_${System.currentTimeMillis()}.png")
                    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    repo.addMaterial(CustomMaterialEntity(name = name.ifBlank { "Custom texture" }, path = f.absolutePath, createdAt = System.currentTimeMillis()))
                    f.absolutePath
                }
                if (apply != null) {
                    val old = draft.design(apply)
                    update { it.copy(designs = it.designs + (apply to old.copy(materialId = "custom:$path", groutMm = groutMm ?: old.groutMm))) }
                    toast = "Texture saved and applied to ${apply.label}"
                } else toast = "Texture saved to library"
            } catch (e: Throwable) { toast = "Could not save texture" }
        }
    }

    fun deleteMaterial(m: CustomMaterialEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { File(m.path).delete(); repo.deleteMaterial(m.id) }
            Materials.evict("custom:${m.path}")
            toast = "Texture deleted"
        }
    }
}

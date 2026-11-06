package com.nh.tilesmarbledesigner.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val json: String,
    val thumbPath: String?
)

@Entity(tableName = "materials")
data class CustomMaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val createdAt: Long
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC") fun all(): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM projects WHERE id = :id") suspend fun get(id: Long): ProjectEntity?
    @Insert suspend fun insert(e: ProjectEntity): Long
    @Update suspend fun update(e: ProjectEntity)
    @Query("DELETE FROM projects WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY createdAt DESC") fun all(): Flow<List<CustomMaterialEntity>>
    @Insert suspend fun insert(e: CustomMaterialEntity): Long
    @Query("DELETE FROM materials WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [ProjectEntity::class, CustomMaterialEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun projects(): ProjectDao
    abstract fun materials(): MaterialDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "nh_tiles.db").build().also { inst = it }
        }
    }
}

class Repository(db: AppDb) {
    private val pd = db.projects()
    private val md = db.materials()
    val projects = pd.all()
    val materials = md.all()
    suspend fun get(id: Long) = pd.get(id)
    suspend fun insert(e: ProjectEntity) = pd.insert(e)
    suspend fun update(e: ProjectEntity) = pd.update(e)
    suspend fun delete(id: Long) = pd.delete(id)
    suspend fun addMaterial(e: CustomMaterialEntity) = md.insert(e)
    suspend fun deleteMaterial(id: Long) = md.delete(id)
}

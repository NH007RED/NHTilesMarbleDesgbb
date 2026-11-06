package com.nh.tilesmarbledesigner.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val unit: LengthUnit = LengthUnit.FT,
    val wastage: Double = 0.0,
    val groutMm: Double = 3.0,
    val groutColor: Int = 0xFFB8B8B8.toInt(),
    val themeId: String = "forest"
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val ctx: Context) {
    private val kUnit = stringPreferencesKey("unit")
    private val kWaste = doublePreferencesKey("wastage")
    private val kGrout = doublePreferencesKey("grout")
    private val kColor = intPreferencesKey("groutColor")
    private val kTheme = stringPreferencesKey("theme")

    val flow: Flow<AppSettings> = ctx.dataStore.data.map {
        AppSettings(LengthUnit.of(it[kUnit]), it[kWaste] ?: 0.0, it[kGrout] ?: 3.0, it[kColor] ?: 0xFFB8B8B8.toInt(), it[kTheme] ?: "forest")
    }

    suspend fun save(s: AppSettings) {
        ctx.dataStore.edit {
            it[kUnit] = s.unit.name; it[kWaste] = s.wastage; it[kGrout] = s.groutMm; it[kColor] = s.groutColor; it[kTheme] = s.themeId
        }
    }
}

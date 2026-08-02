package com.kun.glasssuite.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cloudmusic_settings")

data class Settings(
    val apiBaseUrl: String = AppConfig.DEFAULT_API,
    val accentHex: String = "#C62F2F",
    val darkMode: Boolean = false,
    val themeMode: Int = 0,
    val ghOwner: String = "jiangtengqiao",
    val ghRepo: String = "GlassSuite",
    val betaServerUrl: String = "http://10.0.2.2:3100",
    val betaKey: String = "",
    val lyricFontSize: Int = 18,
    val lyricOffsetMs: Int = 0,
    val lyricMode: Int = 0,
    val diyLyric: String = "",
    val quality: String = "exhigh",
    val userId: Long = 0L,
    val profileJson: String = "",
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val API_BASE = stringPreferencesKey("api_base")
        val ACCENT = stringPreferencesKey("accent")
        val DARK = booleanPreferencesKey("dark")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val GH_OWNER = stringPreferencesKey("gh_owner")
        val GH_REPO = stringPreferencesKey("gh_repo")
        val BETA_URL = stringPreferencesKey("beta_url")
        val BETA_KEY = stringPreferencesKey("beta_key")
        val LYRIC_SIZE = intPreferencesKey("lyric_size")
        val LYRIC_OFFSET = intPreferencesKey("lyric_offset")
        val LYRIC_MODE = intPreferencesKey("lyric_mode")
        val DIY_LYRIC = stringPreferencesKey("diy_lyric")
        val QUALITY = stringPreferencesKey("quality")
        val USER_ID = longPreferencesKey("user_id")
        val PROFILE = stringPreferencesKey("profile_json")
    }

    val data: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            apiBaseUrl = p[Keys.API_BASE] ?: AppConfig.DEFAULT_API,
            accentHex = p[Keys.ACCENT] ?: "#C62F2F",
            darkMode = p[Keys.DARK] ?: false,
            themeMode = p[Keys.THEME_MODE] ?: 0,
            ghOwner = p[Keys.GH_OWNER] ?: "jiangtengqiao",
            ghRepo = p[Keys.GH_REPO] ?: "GlassSuite",
            betaServerUrl = p[Keys.BETA_URL] ?: "http://10.0.2.2:3100",
            betaKey = p[Keys.BETA_KEY] ?: "",
            lyricFontSize = p[Keys.LYRIC_SIZE] ?: 18,
            lyricOffsetMs = p[Keys.LYRIC_OFFSET] ?: 0,
            lyricMode = p[Keys.LYRIC_MODE] ?: 0,
            diyLyric = p[Keys.DIY_LYRIC] ?: "",
            quality = p[Keys.QUALITY] ?: "exhigh",
            userId = p[Keys.USER_ID] ?: 0L,
            profileJson = p[Keys.PROFILE] ?: "",
        )
    }

    suspend fun updateApiBase(url: String) {
        context.dataStore.edit { it[Keys.API_BASE] = url.trim().trimEnd('/') }
    }

    suspend fun setAccent(hex: String) = context.dataStore.edit { it[Keys.ACCENT] = hex }

    suspend fun setDarkMode(v: Boolean) = context.dataStore.edit { it[Keys.DARK] = v }

    suspend fun setThemeMode(v: Int) = context.dataStore.edit { it[Keys.THEME_MODE] = v }

    suspend fun setGhRepo(owner: String, repo: String) = context.dataStore.edit {
        it[Keys.GH_OWNER] = owner.trim()
        it[Keys.GH_REPO] = repo.trim()
    }

    suspend fun setBetaServer(url: String) = context.dataStore.edit { it[Keys.BETA_URL] = url.trim() }

    suspend fun setBetaKey(key: String) = context.dataStore.edit { it[Keys.BETA_KEY] = key }

    suspend fun setLyricFontSize(v: Int) = context.dataStore.edit { it[Keys.LYRIC_SIZE] = v }

    suspend fun setLyricOffset(v: Int) = context.dataStore.edit { it[Keys.LYRIC_OFFSET] = v }

    suspend fun setLyricMode(v: Int) = context.dataStore.edit { it[Keys.LYRIC_MODE] = v }

    suspend fun setDiyLyric(v: String) = context.dataStore.edit { it[Keys.DIY_LYRIC] = v }

    suspend fun setQuality(v: String) = context.dataStore.edit { it[Keys.QUALITY] = v }

    suspend fun setLogin(userId: Long, profileJson: String) {
        context.dataStore.edit {
            it[Keys.USER_ID] = userId
            it[Keys.PROFILE] = profileJson
        }
    }

    suspend fun logout() {
        context.dataStore.edit {
            it[Keys.USER_ID] = 0L
            it[Keys.PROFILE] = ""
        }
    }
}

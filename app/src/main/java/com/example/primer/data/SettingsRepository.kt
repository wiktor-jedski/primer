package com.example.primer.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.primer.data.model.AppSettings
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ISettingsRepository {
    fun loadSettings(): AppSettings
    suspend fun loadPat(): String
    suspend fun savePat(pat: String)
    fun saveGithubRepo(repo: String)
    fun saveGithubBranch(branch: String)
    fun saveMarkers(markers: List<String>)
    fun saveAffirmation(affirmation: String)
    fun saveHabits(habits: List<String>)
    fun saveStrategies(strategies: List<String>)
    fun saveNotificationTime(hour: Int, minute: Int)
}

interface PatStore {
    suspend fun loadPat(): String
    suspend fun savePat(pat: String)
}

class EncryptedPatStore(context: Context) : PatStore {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "primer_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun loadPat(): String = withContext(Dispatchers.IO) {
        prefs.getString("pat", "") ?: ""
    }

    override suspend fun savePat(pat: String): Unit = withContext(Dispatchers.IO) {
        prefs.edit().putString("pat", pat).apply()
    }
}

class SettingsRepository(
    context: Context,
    private val patStore: PatStore = EncryptedPatStore(context)
) : ISettingsRepository {

    private val prefs = context.getSharedPreferences("primer_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun loadSettings(): AppSettings = AppSettings(
        githubRepo = prefs.getString("github_repo", "") ?: "",
        githubBranch = prefs.getString("github_branch", "") ?: "",
        journalMarkers = loadList("journal_markers"),
        affirmation = prefs.getString("affirmation", "") ?: "",
        habits = loadList("habits"),
        strategies = loadList("strategies"),
        notificationHour = prefs.getInt("notification_hour", 8),
        notificationMinute = prefs.getInt("notification_minute", 0)
    )

    override suspend fun loadPat(): String = patStore.loadPat()

    override suspend fun savePat(pat: String) = patStore.savePat(pat)

    override fun saveGithubRepo(repo: String) {
        prefs.edit().putString("github_repo", repo).apply()
    }

    override fun saveGithubBranch(branch: String) {
        prefs.edit().putString("github_branch", branch).apply()
    }

    override fun saveMarkers(markers: List<String>) {
        prefs.edit().putString("journal_markers", gson.toJson(markers)).apply()
    }

    override fun saveAffirmation(affirmation: String) {
        prefs.edit().putString("affirmation", affirmation).apply()
    }

    override fun saveHabits(habits: List<String>) {
        prefs.edit().putString("habits", gson.toJson(habits)).apply()
    }

    override fun saveStrategies(strategies: List<String>) {
        prefs.edit().putString("strategies", gson.toJson(strategies)).apply()
    }

    override fun saveNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt("notification_hour", hour)
            .putInt("notification_minute", minute)
            .apply()
    }

    private fun loadList(key: String): List<String> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return gson.fromJson(json, Array<String>::class.java).toList()
    }
}

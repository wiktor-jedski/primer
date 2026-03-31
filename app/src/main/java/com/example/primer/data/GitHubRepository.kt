package com.example.primer.data

import com.example.primer.data.model.JournalSection
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.util.Base64

interface IGitHubRepository {
    suspend fun fetchJournalSections(markers: List<String>): Result<List<JournalSection>>
}

internal fun parseJournal(text: String, markers: List<String>): List<JournalSection> {
    val lines = text.lines()
    val sectionMap = mutableMapOf<String, MutableList<String>>()
    var currentMarker: String? = null

    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed in markers -> {
                currentMarker = trimmed
                sectionMap.getOrPut(trimmed) { mutableListOf() }
            }
            currentMarker != null -> {
                if (trimmed.isEmpty()) {
                    currentMarker = null
                } else {
                    sectionMap[currentMarker]!!.add(trimmed)
                }
            }
        }
    }

    return markers
        .filter { marker -> sectionMap[marker]?.isNotEmpty() == true }
        .map { marker -> JournalSection(title = marker, items = sectionMap[marker]!!) }
}

class GitHubRepository(
    private val settingsRepository: ISettingsRepository,
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = "https://api.github.com"
) : IGitHubRepository {

    override suspend fun fetchJournalSections(markers: List<String>): Result<List<JournalSection>> {
        val settings = settingsRepository.loadSettings()
        val pat = settingsRepository.loadPat()
        val date = LocalDate.now().minusDays(1).toString()

        val parts = settings.githubRepo.split("/", limit = 2)
        if (parts.size != 2) {
            return Result.failure(IllegalArgumentException("githubRepo must be in 'owner/repo' format"))
        }
        val (owner, repo) = parts

        val url = "$baseUrl/repos/$owner/$repo/contents/$date.txt?ref=${settings.githubBranch}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "token $pat")
            .build()

        return try {
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}"))
            }
            val body = response.body?.string()
                ?: return Result.failure(Exception("Empty response body"))
            val content = JsonParser.parseString(body)
                .asJsonObject
                .get("content")
                .asString
                .replace("\n", "")
            val text = Base64.getDecoder().decode(content).toString(Charsets.UTF_8)
            Result.success(parseJournal(text, markers))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

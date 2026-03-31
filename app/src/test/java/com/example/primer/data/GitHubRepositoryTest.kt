package com.example.primer.data

import com.example.primer.data.model.AppSettings
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Base64
import java.util.concurrent.TimeUnit

class GitHubRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: GitHubRepository
    private val markers = listOf("## Goals", "## Habits")

    private val defaultSettings = AppSettings(
        githubRepo = "owner/testrepo",
        githubBranch = "main",
        journalMarkers = markers,
        affirmation = "",
        habits = emptyList(),
        strategies = emptyList(),
        notificationHour = 8,
        notificationMinute = 0
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString().trimEnd('/')
        repo = GitHubRepository(
            settingsRepository = FakeSettingsRepository(defaultSettings, "test-pat"),
            client = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .build(),
            baseUrl = baseUrl
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `200 response with valid base64 content returns success with parsed sections`() = runTest {
        val journalText = "## Goals\nWrite tests\nShip feature\n\n## Habits\nMeditate\n"
        val encoded = Base64.getEncoder().encodeToString(journalText.toByteArray())
        server.enqueue(MockResponse().setBody("""{"content":"$encoded"}"""))

        val result = repo.fetchJournalSections(markers)

        assertTrue(result.isSuccess)
        val sections = result.getOrThrow()
        assertEquals(2, sections.size)
        assertEquals("## Goals", sections[0].title)
        assertEquals(listOf("Write tests", "Ship feature"), sections[0].items)
        assertEquals("## Habits", sections[1].title)
        assertEquals(listOf("Meditate"), sections[1].items)
    }

    @Test
    fun `404 response returns failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val result = repo.fetchJournalSections(markers)

        assertTrue(result.isFailure)
    }

    @Test
    fun `network IOException returns failure`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))

        val result = repo.fetchJournalSections(markers)

        assertTrue(result.isFailure)
    }

    @Test
    fun `base64 content with embedded newlines is decoded correctly`() = runTest {
        val journalText = "## Goals\nitem1\n"
        val flat = Base64.getEncoder().encodeToString(journalText.toByteArray())
        val withNewlines = flat.chunked(60).joinToString("\\n")
        server.enqueue(MockResponse().setBody("""{"content":"$withNewlines"}"""))

        val result = repo.fetchJournalSections(markers)

        assertTrue(result.isSuccess)
        assertEquals(listOf("item1"), result.getOrThrow()[0].items)
    }

    @Test
    fun `PAT is included in Authorization header`() = runTest {
        val encoded = Base64.getEncoder().encodeToString("## Goals\nitem\n".toByteArray())
        server.enqueue(MockResponse().setBody("""{"content":"$encoded"}"""))

        repo.fetchJournalSections(markers)

        val recordedRequest = server.takeRequest()
        assertEquals("token test-pat", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `correct URL path is constructed from repo branch and date`() = runTest {
        val encoded = Base64.getEncoder().encodeToString("## Goals\nitem\n".toByteArray())
        server.enqueue(MockResponse().setBody("""{"content":"$encoded"}"""))

        repo.fetchJournalSections(markers)

        val recordedRequest = server.takeRequest()
        val expectedDate = LocalDate.now().minusDays(1).toString()
        val expectedPath = "/repos/owner/testrepo/contents/$expectedDate.txt?ref=main"
        assertEquals(expectedPath, recordedRequest.path)
    }
}

private class FakeSettingsRepository(
    private val settings: AppSettings,
    private val pat: String
) : ISettingsRepository {
    override fun loadSettings() = settings
    override suspend fun loadPat() = pat
    override suspend fun savePat(pat: String) = Unit
    override fun saveGithubRepo(repo: String) = Unit
    override fun saveGithubBranch(branch: String) = Unit
    override fun saveMarkers(markers: List<String>) = Unit
    override fun saveAffirmation(affirmation: String) = Unit
    override fun saveHabits(habits: List<String>) = Unit
    override fun saveStrategies(strategies: List<String>) = Unit
    override fun saveNotificationTime(hour: Int, minute: Int) = Unit
}

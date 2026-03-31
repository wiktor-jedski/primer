package com.example.primer.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class SettingsRepositoryTest {

    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repo = SettingsRepository(context, FakePatStore())
    }

    @Test
    fun `loadSettings on fresh prefs returns defaults`() {
        val settings = repo.loadSettings()
        assertEquals("", settings.githubRepo)
        assertEquals("", settings.githubBranch)
        assertEquals(emptyList<String>(), settings.journalMarkers)
        assertEquals("", settings.affirmation)
        assertEquals(emptyList<String>(), settings.habits)
        assertEquals(emptyList<String>(), settings.strategies)
        assertEquals(8, settings.notificationHour)
        assertEquals(0, settings.notificationMinute)
    }

    @Test
    fun `saveGithubRepo round-trip`() {
        repo.saveGithubRepo("owner/repo")
        assertEquals("owner/repo", repo.loadSettings().githubRepo)
    }

    @Test
    fun `saveGithubBranch round-trip`() {
        repo.saveGithubBranch("develop")
        assertEquals("develop", repo.loadSettings().githubBranch)
    }

    @Test
    fun `saveAffirmation round-trip`() {
        repo.saveAffirmation("I am focused")
        assertEquals("I am focused", repo.loadSettings().affirmation)
    }

    @Test
    fun `saveMarkers list round-trip`() {
        repo.saveMarkers(listOf("## A", "## B"))
        assertEquals(listOf("## A", "## B"), repo.loadSettings().journalMarkers)
    }

    @Test
    fun `saveHabits list round-trip`() {
        repo.saveHabits(listOf("Exercise", "Read"))
        assertEquals(listOf("Exercise", "Read"), repo.loadSettings().habits)
    }

    @Test
    fun `saveStrategies list round-trip`() {
        repo.saveStrategies(listOf("Focus blocks"))
        assertEquals(listOf("Focus blocks"), repo.loadSettings().strategies)
    }

    @Test
    fun `empty list saved loads as empty list not null crash`() {
        repo.saveMarkers(emptyList())
        assertEquals(emptyList<String>(), repo.loadSettings().journalMarkers)
    }

    @Test
    fun `saveNotificationTime round-trip`() {
        repo.saveNotificationTime(7, 30)
        val settings = repo.loadSettings()
        assertEquals(7, settings.notificationHour)
        assertEquals(30, settings.notificationMinute)
    }

    @Test
    fun `overwrite save keeps last value`() {
        repo.saveAffirmation("first")
        repo.saveAffirmation("second")
        assertEquals("second", repo.loadSettings().affirmation)
    }

    @Test
    fun `overwrite list save keeps last list`() {
        repo.saveMarkers(listOf("## A"))
        repo.saveMarkers(listOf("## B", "## C"))
        assertEquals(listOf("## B", "## C"), repo.loadSettings().journalMarkers)
    }

    @Test
    fun `loadPat delegates to patStore`() = runTest {
        val fakePatStore = FakePatStore()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repoWithPat = SettingsRepository(context, fakePatStore)
        repoWithPat.savePat("secret-token")
        assertEquals("secret-token", repoWithPat.loadPat())
    }
}

private class FakePatStore : PatStore {
    private var pat = ""
    override suspend fun loadPat() = pat
    override suspend fun savePat(pat: String) { this.pat = pat }
}

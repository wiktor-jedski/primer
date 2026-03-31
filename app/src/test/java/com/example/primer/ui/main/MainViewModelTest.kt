package com.example.primer.ui.main

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.primer.data.IGitHubRepository
import com.example.primer.data.ISettingsRepository
import com.example.primer.data.model.AppSettings
import com.example.primer.data.model.JournalSection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private val defaultSettings = AppSettings(
        githubRepo = "owner/repo",
        githubBranch = "main",
        journalMarkers = listOf("## Goals"),
        affirmation = "I am focused",
        habits = listOf("Exercise", "Read"),
        strategies = listOf("Deep work"),
        notificationHour = 8,
        notificationMinute = 0
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeSettingsRepo(settings: AppSettings = defaultSettings) =
        object : ISettingsRepository {
            override fun loadSettings() = settings
            override suspend fun loadPat() = "token123"
            override suspend fun savePat(pat: String) {}
            override fun saveGithubRepo(repo: String) {}
            override fun saveGithubBranch(branch: String) {}
            override fun saveMarkers(markers: List<String>) {}
            override fun saveAffirmation(affirmation: String) {}
            override fun saveHabits(habits: List<String>) {}
            override fun saveStrategies(strategies: List<String>) {}
            override fun saveNotificationTime(hour: Int, minute: Int) {}
        }

    private fun successRepo(sections: List<JournalSection>) =
        object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.success(sections)
        }

    private fun failureRepo() =
        object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.failure<List<JournalSection>>(Exception("Network error"))
        }

    @Test
    fun `successful fetch emits Ready with journalAvailable true`() = runTest {
        val sections = listOf(JournalSection("## Goals", listOf("Ship it")))
        val vm = MainViewModel(app, fakeSettingsRepo(), successRepo(sections), testDispatcher)

        val state = vm.uiState.value
        assertTrue(state is MainUiState.Ready)
        val ready = state as MainUiState.Ready
        assertTrue(ready.journalAvailable)
        assertEquals(sections, ready.journalSections)
        assertEquals(defaultSettings, ready.settings)
    }

    @Test
    fun `failed fetch emits Ready with journalAvailable false and settings populated`() = runTest {
        val vm = MainViewModel(app, fakeSettingsRepo(), failureRepo(), testDispatcher)

        val state = vm.uiState.value
        assertTrue(state is MainUiState.Ready)
        val ready = state as MainUiState.Ready
        assertFalse(ready.journalAvailable)
        assertEquals(emptyList<JournalSection>(), ready.journalSections)
        assertEquals(defaultSettings, ready.settings)
    }

    @Test
    fun `refresh triggers another fetch and transitions through Loading then Ready`() = runTest {
        var fetchCount = 0
        val gitHubRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>): Result<List<JournalSection>> {
                fetchCount++
                return Result.success(emptyList())
            }
        }
        val vm = MainViewModel(app, fakeSettingsRepo(), gitHubRepo, testDispatcher)
        assertEquals(1, fetchCount)
        assertTrue(vm.uiState.value is MainUiState.Ready)

        vm.refresh()

        assertEquals(2, fetchCount)
        assertTrue(vm.uiState.value is MainUiState.Ready)
    }

    @Test
    fun `refresh while loading cancels previous job and starts new fetch`() = runTest {
        val deferred = CompletableDeferred<List<JournalSection>>()
        var fetchCount = 0
        val blockingRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>): Result<List<JournalSection>> {
                fetchCount++
                return Result.success(deferred.await())
            }
        }
        val vm = MainViewModel(app, fakeSettingsRepo(), blockingRepo, testDispatcher)

        // First fetch started but blocked on deferred
        assertEquals(1, fetchCount)
        assertTrue(vm.uiState.value is MainUiState.Loading)

        // refresh() cancels first job and starts second
        vm.refresh()
        assertEquals(2, fetchCount)
        assertTrue(vm.uiState.value is MainUiState.Loading)

        // Complete the deferred — second job finishes
        deferred.complete(emptyList())
        assertTrue(vm.uiState.value is MainUiState.Ready)
    }

    @Test
    fun `empty markers list still calls fetch and returns empty sections`() = runTest {
        val settingsWithNoMarkers = defaultSettings.copy(journalMarkers = emptyList())
        var fetchCalled = false
        val gitHubRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>): Result<List<JournalSection>> {
                fetchCalled = true
                assertEquals(emptyList<String>(), markers)
                return Result.success(emptyList())
            }
        }
        val vm = MainViewModel(app, fakeSettingsRepo(settingsWithNoMarkers), gitHubRepo, testDispatcher)

        assertTrue(fetchCalled)
        val state = vm.uiState.value as MainUiState.Ready
        assertEquals(emptyList<JournalSection>(), state.journalSections)
    }
}

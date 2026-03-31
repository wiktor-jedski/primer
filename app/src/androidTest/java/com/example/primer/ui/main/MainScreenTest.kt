package com.example.primer.ui.main

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.primer.data.IGitHubRepository
import com.example.primer.data.ISettingsRepository
import com.example.primer.data.model.AppSettings
import com.example.primer.data.model.JournalSection
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val app: Application
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application

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

    private fun fakeSettingsRepo(settings: AppSettings = defaultSettings) =
        object : ISettingsRepository {
            override fun loadSettings() = settings
            override suspend fun loadPat() = ""
            override suspend fun savePat(pat: String) {}
            override fun saveGithubRepo(repo: String) {}
            override fun saveGithubBranch(branch: String) {}
            override fun saveMarkers(markers: List<String>) {}
            override fun saveAffirmation(affirmation: String) {}
            override fun saveHabits(habits: List<String>) {}
            override fun saveStrategies(strategies: List<String>) {}
            override fun saveNotificationTime(hour: Int, minute: Int) {}
        }

    @Test
    fun loadingState_showsProgressIndicator_notAffirmation() {
        val deferred = CompletableDeferred<List<JournalSection>>()
        val blockingRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.success(deferred.await())
        }
        val vm = MainViewModel(app, fakeSettingsRepo(), blockingRepo)

        composeTestRule.setContent {
            MainScreen(viewModel = vm, onNavigateToSettings = {})
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText(defaultSettings.affirmation).assertDoesNotExist()
    }

    @Test
    fun readyState_journalAvailable_showsAllSections() {
        val sections = listOf(JournalSection("## Goals", listOf("Ship it", "Stay focused")))
        val gitHubRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.success(sections)
        }
        val vm = MainViewModel(app, fakeSettingsRepo(), gitHubRepo)

        composeTestRule.setContent {
            MainScreen(viewModel = vm, onNavigateToSettings = {})
        }

        composeTestRule.waitUntil { vm.uiState.value is MainUiState.Ready }

        composeTestRule.onNodeWithText("I am focused").assertIsDisplayed()
        composeTestRule.onNodeWithText("## Goals").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ship it").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stay focused").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deep work").assertIsDisplayed()
    }

    @Test
    fun readyState_journalNotAvailable_hidesJournalButShowsLocalData() {
        val gitHubRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.failure<List<JournalSection>>(Exception("Network error"))
        }
        val vm = MainViewModel(app, fakeSettingsRepo(), gitHubRepo)

        composeTestRule.setContent {
            MainScreen(viewModel = vm, onNavigateToSettings = {})
        }

        composeTestRule.waitUntil { vm.uiState.value is MainUiState.Ready }

        composeTestRule.onNodeWithText("I am focused").assertIsDisplayed()
        composeTestRule.onNodeWithText("## Goals").assertDoesNotExist()
        composeTestRule.onNodeWithText("Exercise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deep work").assertIsDisplayed()
    }

    @Test
    fun overflowMenu_clickSettings_invokesNavigateCallback() {
        val gitHubRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.success(emptyList())
        }
        val vm = MainViewModel(app, fakeSettingsRepo(), gitHubRepo)
        var navigatedToSettings = false

        composeTestRule.setContent {
            MainScreen(viewModel = vm, onNavigateToSettings = { navigatedToSettings = true })
        }

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").performClick()

        assert(navigatedToSettings)
    }

    @Test
    fun affirmationText_matchesSettingsValue() {
        val customSettings = defaultSettings.copy(affirmation = "Unique affirmation text")
        val gitHubRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.success(emptyList())
        }
        val vm = MainViewModel(app, fakeSettingsRepo(customSettings), gitHubRepo)

        composeTestRule.setContent {
            MainScreen(viewModel = vm, onNavigateToSettings = {})
        }

        composeTestRule.waitUntil { vm.uiState.value is MainUiState.Ready }
        composeTestRule.onNodeWithText("Unique affirmation text").assertIsDisplayed()
    }

    @Test
    fun habitItems_eachAppearsInList() {
        val settings = defaultSettings.copy(habits = listOf("Meditate", "Journal", "Cold shower"))
        val gitHubRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.success(emptyList())
        }
        val vm = MainViewModel(app, fakeSettingsRepo(settings), gitHubRepo)

        composeTestRule.setContent {
            MainScreen(viewModel = vm, onNavigateToSettings = {})
        }

        composeTestRule.waitUntil { vm.uiState.value is MainUiState.Ready }
        composeTestRule.onNodeWithText("Meditate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Journal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cold shower").assertIsDisplayed()
    }

    @Test
    fun strategyItems_eachAppearsInList() {
        val settings = defaultSettings.copy(strategies = listOf("Pomodoro", "Eat the frog"))
        val gitHubRepo = object : IGitHubRepository {
            override suspend fun fetchJournalSections(markers: List<String>) =
                Result.success(emptyList())
        }
        val vm = MainViewModel(app, fakeSettingsRepo(settings), gitHubRepo)

        composeTestRule.setContent {
            MainScreen(viewModel = vm, onNavigateToSettings = {})
        }

        composeTestRule.waitUntil { vm.uiState.value is MainUiState.Ready }
        composeTestRule.onNodeWithText("Pomodoro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Eat the frog").assertIsDisplayed()
    }
}

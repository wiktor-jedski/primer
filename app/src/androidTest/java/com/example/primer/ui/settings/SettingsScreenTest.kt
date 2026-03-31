package com.example.primer.ui.settings

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.primer.data.ISettingsRepository
import com.example.primer.data.model.AppSettings
import com.example.primer.notification.IAlarmScheduler
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val app: Application
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

    private val defaultSettings = AppSettings(
        githubRepo = "owner/repo",
        githubBranch = "main",
        journalMarkers = listOf("## Goals"),
        affirmation = "I am focused",
        habits = listOf("Exercise"),
        strategies = listOf("Deep work"),
        notificationHour = 9,
        notificationMinute = 5
    )

    private fun fakeSettingsRepo(
        settings: AppSettings = defaultSettings,
        pat: String = ""
    ) = object : ISettingsRepository {
        override fun loadSettings() = settings
        override suspend fun loadPat() = pat
        override suspend fun savePat(p: String) {}
        override fun saveGithubRepo(r: String) {}
        override fun saveGithubBranch(b: String) {}
        override fun saveMarkers(m: List<String>) {}
        override fun saveAffirmation(a: String) {}
        override fun saveHabits(h: List<String>) {}
        override fun saveStrategies(s: List<String>) {}
        override fun saveNotificationTime(h: Int, m: Int) {}
    }

    private val noOpAlarm = object : IAlarmScheduler { override fun scheduleNext() {} }

    private fun makeViewModel(
        settings: AppSettings = defaultSettings,
        pat: String = ""
    ) = SettingsViewModel(
        application = app,
        settingsRepository = fakeSettingsRepo(settings, pat),
        alarmScheduler = noOpAlarm,
        dispatcher = Dispatchers.Main
    )

    @Test
    fun repoFieldDisplaysValueFromViewModelState() {
        val vm = makeViewModel(settings = defaultSettings.copy(githubRepo = "owner/repo"))
        composeTestRule.setContent { SettingsScreen(viewModel = vm) }
        composeTestRule.onNodeWithTag("github_repo_field").assertTextContains("owner/repo")
    }

    @Test
    fun patFieldShowsMaskedCharacters() {
        val vm = makeViewModel(pat = "secret123")
        composeTestRule.setContent { SettingsScreen(viewModel = vm) }
        composeTestRule.onNodeWithTag("pat_field")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test
    fun typingInAffirmationFieldAndLosingFocusUpdatesViewModel() {
        val vm = makeViewModel(settings = defaultSettings.copy(affirmation = "Old"))
        composeTestRule.setContent { SettingsScreen(viewModel = vm) }

        composeTestRule.onNodeWithTag("affirmation_field")
            .performClick()
            .performTextClearance()
            .performTextInput("New affirmation")
        // Move focus away to trigger onFocusChanged save
        composeTestRule.onNodeWithTag("github_repo_field").performClick()

        assertEquals("New affirmation", vm.affirmation.value)
    }

    @Test
    fun addMarkerRowAppendsMarkerToList() {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = emptyList()))
        composeTestRule.setContent { SettingsScreen(viewModel = vm) }

        composeTestRule.onNodeWithTag("new_marker_input").performTextInput("## Goals")
        composeTestRule.onNodeWithTag("add_marker_confirm").performClick()

        composeTestRule.onNodeWithText("## Goals").assertIsDisplayed()
    }

    @Test
    fun deleteButtonOnFirstMarkerRemovesIt() {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = listOf("## Goals")))
        composeTestRule.setContent { SettingsScreen(viewModel = vm) }

        composeTestRule.onNodeWithTag("marker_delete_0").performClick()

        assertEquals(emptyList<String>(), vm.markers.value)
    }

    @Test
    fun upButtonOnSecondMarkerMovesItAboveFirst() {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = listOf("A", "B")))
        composeTestRule.setContent { SettingsScreen(viewModel = vm) }

        composeTestRule.onNodeWithTag("marker_up_1").performClick()

        assertEquals(listOf("B", "A"), vm.markers.value)
    }

    @Test
    fun notificationRowShowsTimeInHhMmFormat() {
        val vm = makeViewModel(settings = defaultSettings.copy(notificationHour = 9, notificationMinute = 5))
        composeTestRule.setContent { SettingsScreen(viewModel = vm) }

        composeTestRule.onNodeWithTag("notification_time").assertTextContains("09:05")
    }
}

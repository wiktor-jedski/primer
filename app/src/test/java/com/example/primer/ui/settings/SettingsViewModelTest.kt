package com.example.primer.ui.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.primer.data.ISettingsRepository
import com.example.primer.data.model.AppSettings
import com.example.primer.notification.IAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private val defaultSettings = AppSettings(
        githubRepo = "owner/repo",
        githubBranch = "main",
        journalMarkers = listOf("## Goals"),
        affirmation = "I am focused",
        habits = listOf("Exercise"),
        strategies = listOf("Deep work"),
        notificationHour = 8,
        notificationMinute = 0
    )

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun fakeSettingsRepo(
        settings: AppSettings = defaultSettings,
        pat: String = "my_pat"
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

    private class FakeAlarmScheduler : IAlarmScheduler {
        var callCount = 0
        override fun scheduleNext() { callCount++ }
    }

    private fun makeViewModel(
        settings: AppSettings = defaultSettings,
        alarm: FakeAlarmScheduler = FakeAlarmScheduler(),
        pat: String = "my_pat"
    ) = SettingsViewModel(app, fakeSettingsRepo(settings, pat), alarm, testDispatcher)

    @Test
    fun `init loads persisted values from repository into StateFlows`() = runTest {
        val vm = makeViewModel()
        assertEquals("owner/repo", vm.githubRepo.value)
        assertEquals("main", vm.githubBranch.value)
        assertEquals("my_pat", vm.pat.value)
        assertEquals(listOf("## Goals"), vm.markers.value)
        assertEquals("I am focused", vm.affirmation.value)
        assertEquals(listOf("Exercise"), vm.habits.value)
        assertEquals(listOf("Deep work"), vm.strategies.value)
        assertEquals(8, vm.notificationHour.value)
        assertEquals(0, vm.notificationMinute.value)
    }

    // --- Marker tests ---

    @Test
    fun `addMarker appends marker to list`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = emptyList()))
        vm.addMarker("x")
        assertTrue(vm.markers.value.contains("x"))
    }

    @Test
    fun `addMarker with duplicate value keeps both entries`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = emptyList()))
        vm.addMarker("x")
        vm.addMarker("x")
        assertEquals(2, vm.markers.value.size)
    }

    @Test
    fun `removeMarker on single-item list yields empty list`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = listOf("a")))
        vm.removeMarker(0)
        assertEquals(emptyList<String>(), vm.markers.value)
    }

    @Test
    fun `removeMarker(1) on two-item list leaves only first item`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = listOf("a", "b")))
        vm.removeMarker(1)
        assertEquals(listOf("a"), vm.markers.value)
    }

    @Test
    fun `moveMarkerUp(0) is a no-op`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = listOf("a", "b")))
        vm.moveMarkerUp(0)
        assertEquals(listOf("a", "b"), vm.markers.value)
    }

    @Test
    fun `moveMarkerDown on last index is a no-op`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = listOf("a", "b")))
        vm.moveMarkerDown(1)
        assertEquals(listOf("a", "b"), vm.markers.value)
    }

    @Test
    fun `moveMarkerUp(1) on abc swaps first two`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = listOf("a", "b", "c")))
        vm.moveMarkerUp(1)
        assertEquals(listOf("b", "a", "c"), vm.markers.value)
    }

    @Test
    fun `moveMarkerDown(1) on abc swaps last two`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(journalMarkers = listOf("a", "b", "c")))
        vm.moveMarkerDown(1)
        assertEquals(listOf("a", "c", "b"), vm.markers.value)
    }

    // --- Habit tests ---

    @Test
    fun `addHabit appends habit to list`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(habits = emptyList()))
        vm.addHabit("Run")
        assertTrue(vm.habits.value.contains("Run"))
    }

    @Test
    fun `removeHabit on single-item list yields empty list`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(habits = listOf("Run")))
        vm.removeHabit(0)
        assertEquals(emptyList<String>(), vm.habits.value)
    }

    @Test
    fun `removeHabit(1) on two-item list leaves only first item`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(habits = listOf("Run", "Read")))
        vm.removeHabit(1)
        assertEquals(listOf("Run"), vm.habits.value)
    }

    @Test
    fun `moveHabitUp(0) is a no-op`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(habits = listOf("a", "b")))
        vm.moveHabitUp(0)
        assertEquals(listOf("a", "b"), vm.habits.value)
    }

    @Test
    fun `moveHabitDown on last index is a no-op`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(habits = listOf("a", "b")))
        vm.moveHabitDown(1)
        assertEquals(listOf("a", "b"), vm.habits.value)
    }

    @Test
    fun `moveHabitUp(1) on abc swaps first two`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(habits = listOf("a", "b", "c")))
        vm.moveHabitUp(1)
        assertEquals(listOf("b", "a", "c"), vm.habits.value)
    }

    @Test
    fun `moveHabitDown(1) on abc swaps last two`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(habits = listOf("a", "b", "c")))
        vm.moveHabitDown(1)
        assertEquals(listOf("a", "c", "b"), vm.habits.value)
    }

    // --- Strategy tests ---

    @Test
    fun `addStrategy appends strategy to list`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(strategies = emptyList()))
        vm.addStrategy("Focus blocks")
        assertTrue(vm.strategies.value.contains("Focus blocks"))
    }

    @Test
    fun `removeStrategy removes correct entry`() = runTest {
        val vm = makeViewModel(settings = defaultSettings.copy(strategies = listOf("a", "b")))
        vm.removeStrategy(0)
        assertEquals(listOf("b"), vm.strategies.value)
    }

    // --- Notification tests ---

    @Test
    fun `saveNotificationTime updates state and calls alarmScheduler`() = runTest {
        val alarm = FakeAlarmScheduler()
        val vm = makeViewModel(alarm = alarm)
        vm.saveNotificationTime(8, 30)
        assertEquals(8, vm.notificationHour.value)
        assertEquals(30, vm.notificationMinute.value)
        assertTrue(alarm.callCount >= 1)
    }
}

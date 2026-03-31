package com.example.primer.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.primer.data.ISettingsRepository
import com.example.primer.data.SettingsRepository
import com.example.primer.notification.DefaultAlarmScheduler
import com.example.primer.notification.IAlarmScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val settingsRepository: ISettingsRepository,
    private val alarmScheduler: IAlarmScheduler,
    private val dispatcher: CoroutineDispatcher
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        SettingsRepository(application),
        DefaultAlarmScheduler(application),
        Dispatchers.IO
    )

    private val _githubRepo = MutableStateFlow("")
    val githubRepo: StateFlow<String> = _githubRepo

    private val _githubBranch = MutableStateFlow("")
    val githubBranch: StateFlow<String> = _githubBranch

    private val _pat = MutableStateFlow("")
    val pat: StateFlow<String> = _pat

    private val _markers = MutableStateFlow<List<String>>(emptyList())
    val markers: StateFlow<List<String>> = _markers

    private val _affirmation = MutableStateFlow("")
    val affirmation: StateFlow<String> = _affirmation

    private val _habits = MutableStateFlow<List<String>>(emptyList())
    val habits: StateFlow<List<String>> = _habits

    private val _strategies = MutableStateFlow<List<String>>(emptyList())
    val strategies: StateFlow<List<String>> = _strategies

    private val _notificationHour = MutableStateFlow(8)
    val notificationHour: StateFlow<Int> = _notificationHour

    private val _notificationMinute = MutableStateFlow(0)
    val notificationMinute: StateFlow<Int> = _notificationMinute

    init {
        viewModelScope.launch(dispatcher) {
            val settings = settingsRepository.loadSettings()
            _githubRepo.value = settings.githubRepo
            _githubBranch.value = settings.githubBranch
            _pat.value = settingsRepository.loadPat()
            _markers.value = settings.journalMarkers
            _affirmation.value = settings.affirmation
            _habits.value = settings.habits
            _strategies.value = settings.strategies
            _notificationHour.value = settings.notificationHour
            _notificationMinute.value = settings.notificationMinute
        }
    }

    fun saveGithubRepo(repo: String) {
        _githubRepo.value = repo
        settingsRepository.saveGithubRepo(repo)
    }

    fun saveGithubBranch(branch: String) {
        _githubBranch.value = branch
        settingsRepository.saveGithubBranch(branch)
    }

    fun savePat(pat: String) {
        _pat.value = pat
        viewModelScope.launch(dispatcher) { settingsRepository.savePat(pat) }
    }

    fun saveAffirmation(affirmation: String) {
        _affirmation.value = affirmation
        settingsRepository.saveAffirmation(affirmation)
    }

    fun saveNotificationTime(hour: Int, minute: Int) {
        _notificationHour.value = hour
        _notificationMinute.value = minute
        settingsRepository.saveNotificationTime(hour, minute)
        alarmScheduler.scheduleNext()
    }

    fun addMarker(marker: String) {
        _markers.value = _markers.value + marker
        settingsRepository.saveMarkers(_markers.value)
    }

    fun removeMarker(index: Int) {
        _markers.value = _markers.value.toMutableList().also { it.removeAt(index) }
        settingsRepository.saveMarkers(_markers.value)
    }

    fun moveMarkerUp(index: Int) {
        if (index <= 0) return
        val list = _markers.value.toMutableList()
        val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
        _markers.value = list
        settingsRepository.saveMarkers(_markers.value)
    }

    fun moveMarkerDown(index: Int) {
        if (index >= _markers.value.size - 1) return
        val list = _markers.value.toMutableList()
        val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
        _markers.value = list
        settingsRepository.saveMarkers(_markers.value)
    }

    fun addHabit(habit: String) {
        _habits.value = _habits.value + habit
        settingsRepository.saveHabits(_habits.value)
    }

    fun removeHabit(index: Int) {
        _habits.value = _habits.value.toMutableList().also { it.removeAt(index) }
        settingsRepository.saveHabits(_habits.value)
    }

    fun moveHabitUp(index: Int) {
        if (index <= 0) return
        val list = _habits.value.toMutableList()
        val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
        _habits.value = list
        settingsRepository.saveHabits(_habits.value)
    }

    fun moveHabitDown(index: Int) {
        if (index >= _habits.value.size - 1) return
        val list = _habits.value.toMutableList()
        val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
        _habits.value = list
        settingsRepository.saveHabits(_habits.value)
    }

    fun addStrategy(strategy: String) {
        _strategies.value = _strategies.value + strategy
        settingsRepository.saveStrategies(_strategies.value)
    }

    fun removeStrategy(index: Int) {
        _strategies.value = _strategies.value.toMutableList().also { it.removeAt(index) }
        settingsRepository.saveStrategies(_strategies.value)
    }
}

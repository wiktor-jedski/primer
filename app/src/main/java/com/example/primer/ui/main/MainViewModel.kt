package com.example.primer.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.primer.data.GitHubRepository
import com.example.primer.data.IGitHubRepository
import com.example.primer.data.ISettingsRepository
import com.example.primer.data.SettingsRepository
import com.example.primer.data.model.AppSettings
import com.example.primer.data.model.JournalSection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MainUiState {
    object Loading : MainUiState()
    data class Ready(
        val settings: AppSettings,
        val journalSections: List<JournalSection>,
        val journalAvailable: Boolean,
        val journalError: String? = null
    ) : MainUiState()
}

class MainViewModel(
    application: Application,
    private val settingsRepository: ISettingsRepository,
    private val gitHubRepository: IGitHubRepository,
    private val dispatcher: CoroutineDispatcher
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        SettingsRepository(application),
        GitHubRepository(SettingsRepository(application)),
        Dispatchers.IO
    )

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState

    private var fetchJob: Job? = null

    init {
        loadData()
    }

    fun refresh() {
        fetchJob?.cancel()
        loadData()
    }

    private fun loadData() {
        fetchJob = viewModelScope.launch(dispatcher) {
            _uiState.value = MainUiState.Loading
            val settings = settingsRepository.loadSettings()
            val result = gitHubRepository.fetchJournalSections(settings.journalMarkers)
            _uiState.value = MainUiState.Ready(
                settings = settings,
                journalSections = result.getOrElse { emptyList() },
                journalAvailable = result.isSuccess,
                journalError = result.exceptionOrNull()?.message
            )
        }
    }
}

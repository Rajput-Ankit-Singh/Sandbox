package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InstalledSandboxAppEntity
import com.example.data.SandboxDatabase
import com.example.data.SandboxLogEntity
import com.example.data.SandboxProfileEntity
import com.example.data.SandboxRepository
import com.example.data.StorageSummary
import com.example.engine.ApkParserEngine
import com.example.engine.DeviceCompatibilityReport
import com.example.engine.ParsedApkMetadata
import com.example.engine.SampleApkPreset
import com.example.engine.SandboxExecutionResult
import com.example.engine.SandboxInstallProgress
import com.example.engine.SandboxRuntimeEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SandboxScreen {
    HOME,
    IMPORT_APK,
    TESTING,
    SETTINGS
}

data class SandboxUiState(
    val currentScreen: SandboxScreen = SandboxScreen.HOME,
    val isDarkTheme: Boolean = true,
    val activeProfileId: String = "profile_default",
    val searchQuery: String = "",
    val inspectedApk: ParsedApkMetadata? = null,
    val isParsingApk: Boolean = false,
    val installTargetProfileIds: Set<String> = setOf("profile_default"),
    val installProgress: SandboxInstallProgress? = null,
    val installErrorMessage: String? = null,
    val installSuccessMessage: String? = null,
    val selectedTestingAppId: Long? = null,
    val lastExecutionResult: SandboxExecutionResult? = null,
    val isRunningContainer: Boolean = false,
    val isolatedFiles: List<Pair<String, Long>> = emptyList(),
    val isolatedPrefs: Map<String, String> = emptyMap(),
    val compatibilityReport: DeviceCompatibilityReport? = null,
    val storageSummary: StorageSummary = StorageSummary(0L, 0L, 0L, 0L),
    val statusBannerMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SandboxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SandboxRepository = SandboxRepository(
        application.applicationContext,
        SandboxDatabase.getInstance(application.applicationContext).sandboxDao()
    )

    private val _uiState = MutableStateFlow(SandboxUiState())
    val uiState: StateFlow<SandboxUiState> = _uiState.asStateFlow()

    private val _activeProfileId = MutableStateFlow("profile_default")

    val profiles: StateFlow<List<SandboxProfileEntity>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInstalledApps: StateFlow<List<InstalledSandboxAppEntity>> = repository.allInstalledApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfileApps: StateFlow<List<InstalledSandboxAppEntity>> = _activeProfileId
        .flatMapLatest { profileId -> repository.getInstalledAppsForProfile(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfileLogs: StateFlow<List<SandboxLogEntity>> = _activeProfileId
        .flatMapLatest { profileId -> repository.getLogsForProfile(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val samplePresets: List<SampleApkPreset> = ApkParserEngine.SAMPLE_PRESETS

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfiles()
            val compat = repository.evaluateCompatibility()
            _uiState.value = _uiState.value.copy(compatibilityReport = compat)
        }
        viewModelScope.launch {
            allInstalledApps.collect { apps ->
                val summary = repository.computeStorageSummary(apps)
                val currentSelectedId = _uiState.value.selectedTestingAppId
                val updatedSelectedId = if (currentSelectedId == null || apps.none { it.id == currentSelectedId }) {
                    apps.firstOrNull { it.profileId == _activeProfileId.value }?.id ?: apps.firstOrNull()?.id
                } else {
                    currentSelectedId
                }
                _uiState.value = _uiState.value.copy(
                    storageSummary = summary,
                    selectedTestingAppId = updatedSelectedId
                )
                updatedSelectedId?.let { id ->
                    apps.find { it.id == id }?.let { refreshIsolatedState(it) }
                }
            }
        }
    }

    fun navigateTo(screen: SandboxScreen) {
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            installErrorMessage = null,
            installSuccessMessage = null
        )
    }

    fun toggleDarkTheme() {
        _uiState.value = _uiState.value.copy(isDarkTheme = !_uiState.value.isDarkTheme)
    }

    fun selectActiveProfile(profileId: String) {
        _activeProfileId.value = profileId
        val matchingApp = allInstalledApps.value.firstOrNull { it.profileId == profileId }
        _uiState.value = _uiState.value.copy(
            activeProfileId = profileId,
            installTargetProfileIds = setOf(profileId),
            selectedTestingAppId = matchingApp?.id ?: _uiState.value.selectedTestingAppId,
            lastExecutionResult = null
        )
        matchingApp?.let { refreshIsolatedState(it) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleTargetInstallProfile(profileId: String) {
        val current = _uiState.value.installTargetProfileIds.toMutableSet()
        if (current.contains(profileId)) {
            if (current.size > 1) current.remove(profileId)
        } else {
            current.add(profileId)
        }
        _uiState.value = _uiState.value.copy(installTargetProfileIds = current)
    }

    fun onExternalApkPicked(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isParsingApk = true,
                installErrorMessage = null,
                installSuccessMessage = null
            )
            val result = repository.parseExternalApkUri(uri, displayName)
            result.onSuccess { metadata ->
                _uiState.value = _uiState.value.copy(
                    isParsingApk = false,
                    inspectedApk = metadata,
                    installTargetProfileIds = setOf(_activeProfileId.value)
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isParsingApk = false,
                    inspectedApk = null,
                    installErrorMessage = err.message ?: "Failed to parse selected APK archive."
                )
            }
        }
    }

    fun onGenerateSampleApk(preset: SampleApkPreset) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isParsingApk = true,
                installErrorMessage = null,
                installSuccessMessage = null
            )
            val result = repository.generateAndParseSampleApk(preset)
            result.onSuccess { metadata ->
                _uiState.value = _uiState.value.copy(
                    isParsingApk = false,
                    inspectedApk = metadata,
                    installTargetProfileIds = setOf(_activeProfileId.value)
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isParsingApk = false,
                    installErrorMessage = err.message ?: "Failed to generate sample APK."
                )
            }
        }
    }

    fun installInspectedApk() {
        val metadata = _uiState.value.inspectedApk ?: return
        val targetProfiles = _uiState.value.installTargetProfileIds.ifEmpty { setOf(_activeProfileId.value) }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                installErrorMessage = null,
                installSuccessMessage = null
            )
            var installedCount = 0
            var lastError: String? = null
            var lastInstalledEntity: InstalledSandboxAppEntity? = null

            for (profileId in targetProfiles) {
                val res = repository.installParsedApk(metadata, profileId) { progress ->
                    _uiState.value = _uiState.value.copy(installProgress = progress)
                }
                res.onSuccess { entity ->
                    installedCount++
                    lastInstalledEntity = entity
                }.onFailure { err ->
                    lastError = err.message
                }
            }

            _uiState.value = _uiState.value.copy(
                installProgress = null,
                installErrorMessage = lastError,
                installSuccessMessage = if (installedCount > 0) {
                    val cloneNote = if (installedCount > 1) " across $installedCount isolated profiles (Cloned)" else ""
                    "Installed ${metadata.appName} v${metadata.versionName}$cloneNote."
                } else null,
                selectedTestingAppId = lastInstalledEntity?.id ?: _uiState.value.selectedTestingAppId
            )
        }
    }

    fun selectTestingApp(app: InstalledSandboxAppEntity) {
        if (app.profileId != _activeProfileId.value) {
            _activeProfileId.value = app.profileId
        }
        _uiState.value = _uiState.value.copy(
            activeProfileId = app.profileId,
            selectedTestingAppId = app.id,
            lastExecutionResult = null
        )
        refreshIsolatedState(app)
    }

    fun launchAndTestApp(
        app: InstalledSandboxAppEntity,
        navigateToTestingScreen: Boolean = true,
        customKey: String? = null,
        customValue: String? = null,
        forceFault: Boolean = false
    ) {
        viewModelScope.launch {
            if (app.profileId != _activeProfileId.value) {
                _activeProfileId.value = app.profileId
            }
            _uiState.value = _uiState.value.copy(
                currentScreen = if (navigateToTestingScreen) SandboxScreen.TESTING else _uiState.value.currentScreen,
                activeProfileId = app.profileId,
                selectedTestingAppId = app.id,
                isRunningContainer = true
            )
            val result = repository.launchSandboxApp(
                app = app,
                customKey = customKey,
                customValue = customValue,
                forceFault = forceFault
            )
            _uiState.value = _uiState.value.copy(
                isRunningContainer = false,
                lastExecutionResult = result,
                isolatedFiles = result.isolatedFilesList,
                isolatedPrefs = result.isolatedPrefsMap
            )
        }
    }

    fun clearAppData(app: InstalledSandboxAppEntity) {
        viewModelScope.launch {
            val updated = repository.clearSandboxAppData(app)
            refreshIsolatedState(updated)
            _uiState.value = _uiState.value.copy(
                lastExecutionResult = null,
                statusBannerMessage = "Cleared isolated data for ${app.appName} in profile '${app.profileId}'."
            )
        }
    }

    fun uninstallApp(app: InstalledSandboxAppEntity) {
        viewModelScope.launch {
            repository.uninstallSandboxApp(app)
            _uiState.value = _uiState.value.copy(
                lastExecutionResult = null,
                statusBannerMessage = "Uninstalled ${app.appName} from profile '${app.profileId}'."
            )
        }
    }

    fun createNewProfile(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val created = repository.createProfile(name, description)
            selectActiveProfile(created.profileId)
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "Created & activated isolated profile '${created.name}'."
            )
        }
    }

    fun resetProfile(profileId: String) {
        viewModelScope.launch {
            repository.resetProfile(profileId)
            _uiState.value = _uiState.value.copy(
                lastExecutionResult = null,
                isolatedFiles = emptyList(),
                isolatedPrefs = emptyMap(),
                statusBannerMessage = "Reset all apps and isolated files in profile '$profileId'."
            )
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
            if (_activeProfileId.value == profileId) {
                selectActiveProfile("profile_default")
            }
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "Deleted profile '$profileId' and purged isolated disk tree."
            )
        }
    }

    fun clearLogsForActiveProfile() {
        viewModelScope.launch {
            repository.clearLogsForProfile(_activeProfileId.value)
        }
    }

    fun clearStagingCache() {
        viewModelScope.launch {
            repository.clearStagingCache()
            val summary = repository.computeStorageSummary(allInstalledApps.value)
            _uiState.value = _uiState.value.copy(
                storageSummary = summary,
                statusBannerMessage = "Cleared temporary APK staging cache."
            )
        }
    }

    fun dismissBannerMessage() {
        _uiState.value = _uiState.value.copy(statusBannerMessage = null)
    }

    private fun refreshIsolatedState(app: InstalledSandboxAppEntity) {
        viewModelScope.launch {
            val (files, prefs) = SandboxRuntimeEngine.inspectIsolatedState(app)
            _uiState.value = _uiState.value.copy(
                isolatedFiles = files,
                isolatedPrefs = prefs
            )
        }
    }
}

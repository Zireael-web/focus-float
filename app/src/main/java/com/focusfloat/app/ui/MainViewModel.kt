package com.focusfloat.app.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.focusfloat.app.AppContainer
import com.focusfloat.app.core.model.AppCategory
import com.focusfloat.app.core.model.AppEntry
import com.focusfloat.app.core.model.AppRef
import com.focusfloat.app.core.model.CategoryType
import com.focusfloat.app.core.time.nowClockText
import com.focusfloat.app.core.time.nowDateText
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationAccessibilityService
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationMode
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationState
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationStatus
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationTarget
import com.focusfloat.app.distracting.DistractingGateApprovals
import com.focusfloat.app.pause.model.PauseSession
import com.focusfloat.app.pause.model.PauseSessionStatus
import com.focusfloat.app.pause.shizuku.ShizukuStatus
import com.focusfloat.app.settings.FocusSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val LABEL_WHITESPACE = Regex("\\s+")
private const val MINUTE_MS = 60_000L
private const val AUTOMATION_RUNNING_POLL_MS = 1_000L
private const val AUTOMATION_IDLE_POLL_MS = 5_000L

data class MainUiState(
    val settings: FocusSettings? = null,
    val clockText: String = "",
    val dateText: String = "",
    val apps: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val categories: List<AppCategory> = emptyList(),
    val primaryCategory: AppCategory? = null,
    val primaryCategoryPackages: List<AppRef> = emptyList(),
    val distractingCategory: AppCategory? = null,
    val distractingPackages: List<AppRef> = emptyList(),
    val activeSession: PauseSession? = null,
    val shizukuStatus: ShizukuStatus = ShizukuStatus.NotRunning,
    val digitalWellbeingAssistantEnabled: Boolean = false,
    val digitalWellbeingAutomationStatus: DigitalWellbeingAutomationStatus? = null,
    val homeRoleHeld: Boolean = false,
    val batteryPct: Float? = null,
    val banner: UiBanner? = null,
    val loading: Boolean = true,
) {
    val visibleApps: List<AppEntry> get() = apps.filterNot { it.isHidden }
    val hiddenApps: List<AppEntry> get() = apps.filter { it.isHidden }
    val primaryCategoryApps: List<AppEntry> get() = apps.filter { it.key.ref in primaryCategoryPackages }
    val distractingApps: List<AppEntry> get() = apps.filter { it.key.ref in distractingPackages }
}

data class UiBanner(
    val title: String,
    val body: String? = null,
    val failed: Boolean = false,
)

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val banner = MutableStateFlow<UiBanner?>(null)
    private val shizukuStatus = MutableStateFlow(container.shizukuPauseExecutor.status())
    private val digitalWellbeingAssistantEnabled = MutableStateFlow(false)
    private val digitalWellbeingAutomationStatus = MutableStateFlow(container.digitalWellbeingAutomationStore.status())
    private val homeRoleHeld = MutableStateFlow(false)
    private var pendingOpenAfterFocusModeOff: AppEntry? = null
    private val categoryPackages = MutableStateFlow<List<AppRef>>(emptyList())
    private val distractingPackages = MutableStateFlow<List<AppRef>>(emptyList())

    private data class BaseState(
        val settings: FocusSettings,
        val apps: List<AppEntry>,
        val categories: List<AppCategory>,
        val sessions: List<PauseSession>,
    )

    private data class AuxState(
        val shizukuStatus: ShizukuStatus,
        val digitalWellbeingAssistantEnabled: Boolean,
        val digitalWellbeingAutomationStatus: DigitalWellbeingAutomationStatus,
        val homeRoleHeld: Boolean,
        val primaryPackages: List<AppRef>,
        val distractingPackages: List<AppRef>,
        val batteryPct: Float?,
        val banner: UiBanner?,
    )

    private data class LauncherState(
        val shizukuStatus: ShizukuStatus,
        val digitalWellbeingAssistantEnabled: Boolean,
        val digitalWellbeingAutomationStatus: DigitalWellbeingAutomationStatus,
        val homeRoleHeld: Boolean,
    )

    private val baseState = combine(
        container.settingsRepository.settings,
        container.installedAppsRepository.observeApps(),
        container.categoryRepository.observeCategories(),
        container.pauseSessionRepository.observeActiveSessions(),
    ) { settings, apps, categories, sessions ->
        BaseState(settings, apps, categories, sessions)
    }

    private val batteryPct = flow {
        while (true) {
            emit(readBatteryPct())
            delay(60_000)
        }
    }

    private val timeTicker = flow {
        while (true) {
            emit(Unit)
            delay(MINUTE_MS - (System.currentTimeMillis() % MINUTE_MS))
        }
    }

    private val launcherState = combine(
        shizukuStatus,
        digitalWellbeingAssistantEnabled,
        digitalWellbeingAutomationStatus,
        homeRoleHeld,
    ) { shizuku, digitalWellbeingAssistantEnabled, automationStatus, homeRoleHeld ->
        LauncherState(shizuku, digitalWellbeingAssistantEnabled, automationStatus, homeRoleHeld)
    }

    private val auxState = combine(
        launcherState,
        categoryPackages,
        distractingPackages,
        batteryPct,
        banner,
    ) { launcherState, packages, distractingPackages, batteryPct, banner ->
        AuxState(
            shizukuStatus = launcherState.shizukuStatus,
            digitalWellbeingAssistantEnabled = launcherState.digitalWellbeingAssistantEnabled,
            digitalWellbeingAutomationStatus = launcherState.digitalWellbeingAutomationStatus,
            homeRoleHeld = launcherState.homeRoleHeld,
            primaryPackages = packages,
            distractingPackages = distractingPackages,
            batteryPct = batteryPct,
            banner = banner,
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        baseState,
        auxState,
        timeTicker,
    ) { base, aux, _ ->
        val primaryCategory = base.categories.firstOrNull { it.id == base.settings.primaryPauseCategoryId }
            ?: base.categories.firstOrNull { it.type == CategoryType.PauseGroup }
        val distractingCategory = base.categories.firstOrNull { it.type == CategoryType.DistractingGroup }
        val activeSession = primaryCategory?.let { category ->
            base.sessions.firstOrNull { it.categoryId == category.id && it.status.isActiveLike() }
        }
        val favorites = base.apps.homeFavorites()
        MainUiState(
            settings = base.settings,
            clockText = nowClockText(),
            dateText = nowDateText(),
            apps = base.apps,
            favorites = favorites,
            categories = base.categories,
            primaryCategory = primaryCategory,
            primaryCategoryPackages = aux.primaryPackages,
            distractingCategory = distractingCategory,
            distractingPackages = aux.distractingPackages,
            activeSession = activeSession,
            shizukuStatus = aux.shizukuStatus,
            digitalWellbeingAssistantEnabled = aux.digitalWellbeingAssistantEnabled,
            digitalWellbeingAutomationStatus = aux.digitalWellbeingAutomationStatus,
            homeRoleHeld = aux.homeRoleHeld,
            batteryPct = aux.batteryPct,
            banner = aux.banner,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            val defaultCategoryId = container.categoryRepository.ensureDefaultPauseCategory()
            val settings = container.settingsRepository.settings.first()
            val categories = container.categoryRepository.observeCategories().first()
            val primaryCategoryId = settings.primaryPauseCategoryId
                ?.takeIf { id -> categories.any { it.id == id } }
                ?: defaultCategoryId.also { container.settingsRepository.setPrimaryPauseCategoryId(it) }
            launch { autoImportSystemPausedApps(primaryCategoryId) }
            container.categoryRepository.observeCategoryItems(primaryCategoryId).collect {
                categoryPackages.value = it
            }
        }
        viewModelScope.launch {
            val distractingCategoryId = container.categoryRepository.ensureDefaultDistractingCategory()
            container.categoryRepository.observeCategoryItems(distractingCategoryId).collect {
                distractingPackages.value = it
            }
        }
        viewModelScope.launch {
            runCatching { container.pauseController.reconcile("launch") }
            refresh()
        }
        viewModelScope.launch {
            digitalWellbeingAutomationStatus.collectLatest { status ->
                val pollDelayMs = if (status.state == DigitalWellbeingAutomationState.Running) {
                    AUTOMATION_RUNNING_POLL_MS
                } else {
                    AUTOMATION_IDLE_POLL_MS
                }
                while (true) {
                    delay(pollDelayMs)
                    refreshDigitalWellbeingAutomationStatus()
                }
            }
        }
    }

    fun launchConfirmedDistractingApp(app: AppEntry) {
        viewModelScope.launch {
            DistractingGateApprovals.approve(app.key.packageName)
            runCatching { container.installedAppsRepository.launch(app) }
                .onSuccess { container.distractingReminderScheduler.schedule(app.key.packageName, app.key.userSerial, app.displayLabel) }
                .onFailure { banner.value = UiBanner("Could not open ${app.displayLabel}", it.message, failed = true) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshPauseSetupState()
            container.installedAppsRepository.refreshApps()
        }
    }

    fun reconcileAndRefresh() {
        viewModelScope.launch {
            runCatching { container.pauseController.reconcile("resume") }
            refreshPauseSetupState()
            container.installedAppsRepository.refreshApps()
        }
    }

    fun setHomeRoleHeld(held: Boolean) {
        homeRoleHeld.value = held
    }

    fun launchApp(app: AppEntry) {
        if (app.isPaused) {
            return
        }
        viewModelScope.launch {
            runCatching { container.installedAppsRepository.launch(app) }
                .onFailure { banner.value = UiBanner("Could not open ${app.displayLabel}", it.message, failed = true) }
        }
    }

    fun unpauseAndOpen(app: AppEntry) {
        if (!app.isPaused) {
            launchApp(app)
            return
        }
        viewModelScope.launch {
            banner.value = null
            if (!isDigitalWellbeingAssistantEnabled()) {
                banner.value = UiBanner(
                    title = "Focus Mode assistant required",
                    body = "Enable FocusFloat Focus Mode assistant in Accessibility settings.",
                )
                openAccessibilitySettings()
                refreshPauseSetupState()
                return@launch
            }

            val request = container.digitalWellbeingAutomationStore.start(
                mode = DigitalWellbeingAutomationMode.DisableFocusMode,
                targets = listOf(
                    DigitalWellbeingAutomationTarget(
                        packageName = app.key.packageName,
                        label = app.displayLabel,
                        userSerial = app.key.userSerial,
                    ),
                ),
            )
            pendingOpenAfterFocusModeOff = app
            refreshDigitalWellbeingAutomationStatus()
            val opened = container.digitalWellbeingLauncher.open()
            banner.value = if (opened) {
                UiBanner(
                    "Turn off Pixel Focus Mode",
                    "FocusFloat will open ${app.displayLabel} after Pixel Focus Mode turns off. This affects every app selected in Pixel Focus Mode.",
                )
            } else {
                pendingOpenAfterFocusModeOff = null
                container.digitalWellbeingAutomationStore.fail(request.id, "Could not open Pixel Focus Mode settings")
                refreshDigitalWellbeingAutomationStatus()
                UiBanner("Could not open Pixel Focus Mode settings", failed = true)
            }
        }
    }

    fun pausePrimaryCategory() {
        viewModelScope.launch {
            banner.value = null
            if (!isDigitalWellbeingAssistantEnabled()) {
                banner.value = UiBanner(
                    title = "Focus Mode assistant required",
                    body = "Enable FocusFloat Focus Mode assistant in Accessibility settings.",
                )
                openAccessibilitySettings()
                refreshPauseSetupState()
                return@launch
            }

            val targetApps = uiState.value.primaryCategoryApps
                .filterNot { it.isProtected }
            val duplicateLabels = targetApps.duplicateNormalizedDisplayLabels()
            if (duplicateLabels.isNotEmpty()) {
                banner.value = UiBanner(
                    title = "Duplicate app names",
                    body = "Focus Mode screen automation cannot disambiguate ${duplicateLabels.joinToString(", ")}. Rename or remove one duplicate before starting.",
                    failed = true,
                )
                return@launch
            }

            val targets = targetApps
                .map { app ->
                    DigitalWellbeingAutomationTarget(
                        packageName = app.key.packageName,
                        label = app.displayLabel,
                        userSerial = app.key.userSerial,
                    )
                }
            if (targets.isEmpty()) {
                banner.value = UiBanner("No apps selected", "Add apps to Paused apps first.")
                return@launch
            }

            container.digitalWellbeingAutomationStore.start(
                mode = DigitalWellbeingAutomationMode.EnableFocusMode,
                targets = targets,
            )
            refreshDigitalWellbeingAutomationStatus()
            val opened = container.digitalWellbeingLauncher.open()
            banner.value = if (opened) {
                UiBanner(
                    title = "Focus Mode assistant started",
                    body = "Keep the phone unlocked and do not touch the screen while FocusFloat selects ${targets.size} apps.",
                )
            } else {
                container.digitalWellbeingAutomationStore.fail("Could not open Pixel Focus Mode settings")
                refreshDigitalWellbeingAutomationStatus()
                UiBanner("Could not open Pixel Focus Mode settings", failed = true)
            }
        }
    }

    fun unpausePrimaryCategory() {
        viewModelScope.launch {
            if (!isDigitalWellbeingAssistantEnabled()) {
                banner.value = UiBanner(
                    title = "Focus Mode assistant required",
                    body = "Enable FocusFloat Focus Mode assistant in Accessibility settings.",
                )
                openAccessibilitySettings()
                refreshPauseSetupState()
                return@launch
            }
            val targets = uiState.value.primaryCategoryApps
                .filterNot { it.isProtected }
                .map { app ->
                    DigitalWellbeingAutomationTarget(
                        packageName = app.key.packageName,
                        label = app.displayLabel,
                        userSerial = app.key.userSerial,
                    )
                }
            container.digitalWellbeingAutomationStore.start(
                mode = DigitalWellbeingAutomationMode.DisableFocusMode,
                targets = targets,
            )
            refreshDigitalWellbeingAutomationStatus()
            val opened = container.digitalWellbeingLauncher.open()
            banner.value = if (opened) {
                UiBanner(
                    "Turn off Pixel Focus Mode",
                    "This turns off the current Pixel Focus Mode session.",
                )
            } else {
                container.digitalWellbeingAutomationStore.fail("Could not open Pixel Focus Mode settings")
                refreshDigitalWellbeingAutomationStatus()
                UiBanner("Could not open Pixel Focus Mode settings", failed = true)
            }
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { container.appContext.startActivity(intent) }
            .onFailure { banner.value = UiBanner("Could not open Accessibility settings", it.message, failed = true) }
    }

    fun openPixelFocusModeSettings() {
        val opened = container.digitalWellbeingLauncher.open()
        if (!opened) {
            banner.value = UiBanner("Could not open Pixel Focus Mode settings", failed = true)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            container.settingsRepository.setOnboardingCompleted(true)
        }
    }

    fun showOnboardingAgain() {
        viewModelScope.launch {
            container.settingsRepository.setOnboardingCompleted(false)
        }
    }

    fun addToPrimaryPauseCategory(app: AppEntry) {
        val categoryId = uiState.value.primaryCategory?.id ?: return
        if (app.isProtected) {
            banner.value = UiBanner("Package is protected", "${app.displayLabel} cannot be added to the pause list.")
            return
        }
        viewModelScope.launch {
            container.categoryRepository.addPackage(categoryId, app.key.packageName, app.key.userSerial)
        }
    }

    fun removeFromPrimaryPauseCategory(app: AppEntry) {
        val categoryId = uiState.value.primaryCategory?.id ?: return
        viewModelScope.launch {
            container.categoryRepository.removePackage(categoryId, app.key.packageName, app.key.userSerial)
        }
    }

    fun addToDistractingCategory(app: AppEntry) {
        val categoryId = uiState.value.distractingCategory?.id ?: return
        if (app.isProtected) {
            banner.value = UiBanner("Package is protected", "${app.displayLabel} cannot be added to Distracting apps.")
            return
        }
        viewModelScope.launch {
            container.categoryRepository.addPackage(categoryId, app.key.packageName, app.key.userSerial)
            banner.value = UiBanner("${app.displayLabel} added to Distracting apps")
        }
    }

    fun removeFromDistractingCategory(app: AppEntry) {
        val categoryId = uiState.value.distractingCategory?.id ?: return
        viewModelScope.launch {
            container.categoryRepository.removePackage(categoryId, app.key.packageName, app.key.userSerial)
            banner.value = UiBanner("${app.displayLabel} removed from Distracting apps")
        }
    }

    fun toggleFavorite(app: AppEntry) {
        viewModelScope.launch {
            container.appOverridesRepository.setFavorite(app.key.packageName, app.key.userSerial, !app.isFavorite)
        }
    }

    fun hideApp(app: AppEntry, hidden: Boolean = true) {
        viewModelScope.launch {
            container.appOverridesRepository.setHidden(app.key.packageName, app.key.userSerial, hidden)
        }
    }

    fun renameApp(app: AppEntry, label: String?) {
        viewModelScope.launch {
            container.appOverridesRepository.rename(app.key.packageName, app.key.userSerial, label)
        }
    }

    fun openAppInfo(app: AppEntry) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData("package:${app.key.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        container.appContext.startActivity(intent)
    }

    fun clearBanner() {
        banner.value = null
    }

    private fun readBatteryPct(): Float? {
        val battery = container.appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return (level.toFloat() / scale.toFloat()).coerceIn(0f, 1f)
    }

    private suspend fun refreshPauseSetupState() {
        shizukuStatus.value = container.shizukuPauseExecutor.status()
        digitalWellbeingAssistantEnabled.value = isDigitalWellbeingAssistantEnabled()
    }

    private suspend fun refreshDigitalWellbeingAutomationStatus() {
        val previous = digitalWellbeingAutomationStatus.value.state
        val current = container.digitalWellbeingAutomationStore.status()
        digitalWellbeingAutomationStatus.value = current
        if (previous == DigitalWellbeingAutomationState.Running && current.state.isTerminal()) {
            refreshPauseSetupState()
            container.installedAppsRepository.refreshApps()
            val appToOpen = pendingOpenAfterFocusModeOff
            pendingOpenAfterFocusModeOff = null
            if (current.state == DigitalWellbeingAutomationState.Completed && appToOpen != null) {
                runCatching { container.installedAppsRepository.launch(appToOpen) }
                    .onFailure {
                        banner.value = UiBanner(
                            "Could not open ${appToOpen.displayLabel}",
                            it.message,
                            failed = true,
                        )
                    }
            }
        }
    }

    private fun isDigitalWellbeingAssistantEnabled(): Boolean {
        return container.accessibilityServiceStatus.isEnabled(DigitalWellbeingAutomationAccessibilityService::class.java)
    }

    private suspend fun autoImportSystemPausedApps(categoryId: Long) {
        val settings = container.settingsRepository.settings.first()
        if (settings.systemPausedAppsImported) return

        val apps = container.installedAppsRepository.observeApps().first { it.isNotEmpty() }
        val existingRefs = container.categoryRepository.getPackageRefs(categoryId).toSet()
        val pausedRefs = apps.asSequence()
            .filter { it.isPaused && it.pausedUntil == null && !it.isProtected }
            .map { it.key.ref }
            .filterNot { it in existingRefs }
            .distinct()
            .toList()

        for (ref in pausedRefs) {
            container.categoryRepository.addPackage(categoryId, ref.packageName, ref.userSerial)
        }
        container.settingsRepository.setSystemPausedAppsImported(true)
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}

private fun PauseSessionStatus.isActiveLike(): Boolean {
    return this in setOf(
        PauseSessionStatus.Pending,
        PauseSessionStatus.Active,
        PauseSessionStatus.PartiallyFailed,
        PauseSessionStatus.WaitingForShizuku,
        PauseSessionStatus.FailedToUnpause,
    )
}

internal fun List<AppEntry>.homeFavorites(): List<AppEntry> {
    return filter { it.isFavorite && !it.isHidden }
        .sortedBy { it.favoriteOrder ?: Int.MAX_VALUE }
}

private fun List<AppEntry>.duplicateNormalizedDisplayLabels(): List<String> {
    return groupBy { it.displayLabel.normalizedDisplayLabel() }
        .filterValues { it.size > 1 }
        .values
        .map { apps -> apps.first().displayLabel.ifBlank { "blank label" } }
}

private fun String.normalizedDisplayLabel(): String {
    return trim()
        .lowercase(Locale.ROOT)
        .replace(LABEL_WHITESPACE, " ")
}

private fun DigitalWellbeingAutomationState.isTerminal(): Boolean {
    return this == DigitalWellbeingAutomationState.Completed || this == DigitalWellbeingAutomationState.Failed
}

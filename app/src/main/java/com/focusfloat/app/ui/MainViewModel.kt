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
import com.focusfloat.app.distracting.DistractingGateApprovals
import com.focusfloat.app.settings.FocusSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MINUTE_MS = 60_000L

data class MainUiState(
    val settings: FocusSettings? = null,
    val clockText: String = "",
    val dateText: String = "",
    val apps: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val distractingCategory: AppCategory? = null,
    val distractingPackages: List<AppRef> = emptyList(),
    val homeRoleHeld: Boolean = false,
    val batteryPct: Float? = null,
    val banner: UiBanner? = null,
    val loading: Boolean = true,
) {
    val visibleApps: List<AppEntry> get() = apps.filterNot { it.isHidden }
    val hiddenApps: List<AppEntry> get() = apps.filter { it.isHidden }
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
    private val homeRoleHeld = MutableStateFlow(false)
    private val distractingPackages = MutableStateFlow<List<AppRef>>(emptyList())

    private data class BaseState(
        val settings: FocusSettings,
        val apps: List<AppEntry>,
        val categories: List<AppCategory>,
    )

    private data class AuxState(
        val homeRoleHeld: Boolean,
        val distractingPackages: List<AppRef>,
        val batteryPct: Float?,
        val banner: UiBanner?,
    )

    private val baseState = combine(
        container.settingsRepository.settings,
        container.installedAppsRepository.observeApps(),
        container.categoryRepository.observeCategories(),
    ) { settings, apps, categories ->
        BaseState(settings, apps, categories)
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

    private val auxState = combine(
        homeRoleHeld,
        distractingPackages,
        batteryPct,
        banner,
    ) { homeRoleHeld, distractingPackages, batteryPct, banner ->
        AuxState(
            homeRoleHeld = homeRoleHeld,
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
        val distractingCategory = base.categories.firstOrNull { it.type == CategoryType.DistractingGroup }
        val favorites = base.apps.homeFavorites()
        MainUiState(
            settings = base.settings,
            clockText = nowClockText(),
            dateText = nowDateText(),
            apps = base.apps,
            favorites = favorites,
            distractingCategory = distractingCategory,
            distractingPackages = aux.distractingPackages,
            homeRoleHeld = aux.homeRoleHeld,
            batteryPct = aux.batteryPct,
            banner = aux.banner,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            val distractingCategoryId = container.categoryRepository.ensureDefaultDistractingCategory()
            container.categoryRepository.observeCategoryItems(distractingCategoryId).collect {
                distractingPackages.value = it
            }
        }
        viewModelScope.launch {
            refresh()
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
            container.installedAppsRepository.refreshApps()
        }
    }

    fun reconcileAndRefresh() {
        viewModelScope.launch {
            container.installedAppsRepository.refreshApps()
        }
    }

    fun setHomeRoleHeld(held: Boolean) {
        homeRoleHeld.value = held
    }

    fun launchApp(app: AppEntry) {
        viewModelScope.launch {
            runCatching { container.installedAppsRepository.launch(app) }
                .onFailure { banner.value = UiBanner("Could not open ${app.displayLabel}", it.message, failed = true) }
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

    fun addToDistractingCategory(app: AppEntry) {
        val categoryId = uiState.value.distractingCategory?.id ?: return
        if (app.isProtected) {
            banner.value = UiBanner("Package is protected", "${app.displayLabel} cannot be added to Distracting apps.")
            return
        }
        if (app.isPaused) {
            return
        }
        viewModelScope.launch {
            container.categoryRepository.addPackage(categoryId, app.key.packageName, app.key.userSerial)
        }
    }

    fun removeFromDistractingCategory(app: AppEntry) {
        val categoryId = uiState.value.distractingCategory?.id ?: return
        viewModelScope.launch {
            container.categoryRepository.removePackage(categoryId, app.key.packageName, app.key.userSerial)
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

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}

internal fun List<AppEntry>.homeFavorites(): List<AppEntry> {
    return filter { it.isFavorite && !it.isHidden }
        .sortedBy { it.favoriteOrder ?: Int.MAX_VALUE }
}

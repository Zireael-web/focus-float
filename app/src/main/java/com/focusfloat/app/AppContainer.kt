package com.focusfloat.app

import android.content.Context
import androidx.room.Room
import com.focusfloat.app.core.time.FreshSystemDefaultZoneClock
import com.focusfloat.app.data.FocusFloatDatabase
import com.focusfloat.app.digitalwellbeing.AccessibilityServiceStatus
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingAutomationStore
import com.focusfloat.app.digitalwellbeing.DigitalWellbeingLauncher
import com.focusfloat.app.distracting.DistractingNotificationController
import com.focusfloat.app.distracting.DistractingReminderScheduler
import com.focusfloat.app.launcher.apps.AndroidInstalledAppsRepository
import com.focusfloat.app.launcher.apps.AndroidPackageStateReader
import com.focusfloat.app.launcher.data.AppOverridesRepository
import com.focusfloat.app.pause.controller.PauseController
import com.focusfloat.app.pause.data.RoomCategoryRepository
import com.focusfloat.app.pause.data.RoomPauseSessionRepository
import com.focusfloat.app.pause.deviceowner.DeviceOwnerPauseExecutor
import com.focusfloat.app.pause.executor.ManualPauseExecutor
import com.focusfloat.app.pause.executor.PauseExecutorSelector
import com.focusfloat.app.pause.model.PauseExecutorAvailability
import com.focusfloat.app.pause.notifications.PauseNotificationController
import com.focusfloat.app.pause.scheduler.PauseExpiryScheduler
import com.focusfloat.app.pause.shizuku.ShizukuPauseExecutor
import com.focusfloat.app.pause.shizuku.ShizukuShell
import com.focusfloat.app.pause.wellbeing.WellbeingRootPauseExecutor
import com.focusfloat.app.settings.SettingsRepository
import java.time.Clock

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val clock: Clock = FreshSystemDefaultZoneClock()

    val database: FocusFloatDatabase = Room.databaseBuilder(
        appContext,
        FocusFloatDatabase::class.java,
        "focusfloat.db",
    ).addMigrations(FocusFloatDatabase.MIGRATION_1_2).build()

    val settingsRepository = SettingsRepository(appContext)
    val accessibilityServiceStatus = AccessibilityServiceStatus(appContext)
    val digitalWellbeingAutomationStore = DigitalWellbeingAutomationStore(appContext)
    val digitalWellbeingLauncher = DigitalWellbeingLauncher(appContext)
    val packageStateReader = AndroidPackageStateReader(appContext)
    val categoryRepository = RoomCategoryRepository(database.categoryDao(), clock)
    val pauseSessionRepository = RoomPauseSessionRepository(database.pauseSessionDao(), clock)
    val appOverridesRepository = AppOverridesRepository(database.appOverrideDao(), clock)
    val installedAppsRepository = AndroidInstalledAppsRepository(
        context = appContext,
        overrideDao = database.appOverrideDao(),
        pauseSessionRepository = pauseSessionRepository,
        packageStateReader = packageStateReader,
    )

    private val shizukuShell = ShizukuShell(appContext)
    val shizukuPauseExecutor = ShizukuPauseExecutor(
        context = appContext,
        shell = shizukuShell,
        packageStateReader = packageStateReader,
    )
    private val wellbeingRootPauseExecutor = WellbeingRootPauseExecutor(
        context = appContext,
        shell = shizukuShell,
        packageStateReader = packageStateReader,
    )
    private val deviceOwnerPauseExecutor = DeviceOwnerPauseExecutor(appContext, packageStateReader)
    private val manualPauseExecutor = ManualPauseExecutor(appContext, packageStateReader)
    private val pauseExecutorSelector = PauseExecutorSelector(
        wellbeingRoot = wellbeingRootPauseExecutor,
        shizuku = shizukuPauseExecutor,
        deviceOwner = deviceOwnerPauseExecutor,
        manual = manualPauseExecutor,
    )
    private val pauseExpiryScheduler = PauseExpiryScheduler(appContext)
    val pauseNotificationController = PauseNotificationController(appContext)
    val distractingNotificationController = DistractingNotificationController(appContext)
    val distractingReminderScheduler = DistractingReminderScheduler(appContext)

    val pauseController = PauseController(
        categoryRepository = categoryRepository,
        sessionRepository = pauseSessionRepository,
        executorSelector = pauseExecutorSelector,
        scheduler = pauseExpiryScheduler,
        notificationController = pauseNotificationController,
        clock = clock,
        context = appContext,
    )

    suspend fun selectedPauseExecutorAvailability(): PauseExecutorAvailability {
        return pauseExecutorSelector.select().availability()
    }
}

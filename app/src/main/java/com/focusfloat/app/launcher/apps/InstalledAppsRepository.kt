package com.focusfloat.app.launcher.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process as AndroidProcess
import android.os.UserHandle
import android.os.UserManager
import com.focusfloat.app.core.model.AppEntry
import com.focusfloat.app.core.model.AppKey
import com.focusfloat.app.core.model.AppRef
import com.focusfloat.app.pause.data.AppOverrideDao
import com.focusfloat.app.pause.data.PauseSessionRepository
import com.focusfloat.app.pause.model.PauseSessionStatus
import com.focusfloat.app.pause.safety.ProtectedPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

interface InstalledAppsRepository {
    fun observeApps(): Flow<List<AppEntry>>
    suspend fun refreshApps()
    suspend fun launch(app: AppEntry)
    fun userHandleForSerial(userSerial: Long): UserHandle?
}

class AndroidInstalledAppsRepository(
    private val context: Context,
    private val overrideDao: AppOverrideDao,
    private val pauseSessionRepository: PauseSessionRepository,
    private val packageStateReader: PackageStateReader,
) : InstalledAppsRepository {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val userResolver = UserResolver(context)
    private val refreshTick = MutableStateFlow(0)

    init {
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = refreshFromPackageCallback()
            override fun onPackageRemoved(packageName: String, user: UserHandle) = refreshFromPackageCallback()
            override fun onPackageChanged(packageName: String, user: UserHandle) = refreshFromPackageCallback()
            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = refreshFromPackageCallback()

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = refreshFromPackageCallback()
        })
    }

    override fun observeApps(): Flow<List<AppEntry>> {
        return combine(
            refreshTick,
            overrideDao.observeAll(),
            pauseSessionRepository.observeActiveSessions(),
        ) { _, overrides, sessions ->
            val overrideByRef = overrides.associateBy { AppRef(it.packageName, it.userSerial) }
            val activePausedUntil = mutableMapOf<AppRef, Instant>()
            sessions
                .filter { it.status in activeStatuses }
                .forEach { session ->
                    session.appRefs.forEach { appRef ->
                        activePausedUntil.putIfAbsent(appRef, session.pausedUntil)
                    }
                }

            val launchableApps = loadLaunchableApps()
            val protectedPackages = ProtectedPackages.protectedSet(context, launchableApps.map { it.packageName })
            val suspendedByRef = launchableApps
                .map { AppRef(it.packageName, it.userSerial) }
                .distinct()
                .associateWith { appRef -> packageStateReader.isSuspended(appRef.packageName, appRef.userSerial) }

            launchableApps.map { launchable ->
                val appRef = AppRef(launchable.packageName, launchable.userSerial)
                val override = overrideByRef[appRef]
                val pausedUntil = activePausedUntil[appRef]
                val isActuallyPaused = suspendedByRef[appRef] == true
                AppEntry(
                    key = AppKey(
                        packageName = launchable.packageName,
                        className = launchable.className,
                        userSerial = launchable.userSerial,
                    ),
                    label = launchable.label,
                    customLabel = override?.customLabel,
                    isFavorite = override?.favoriteOrder != null,
                    favoriteOrder = override?.favoriteOrder,
                    isHidden = override?.hidden ?: false,
                    isPaused = pausedUntil != null || isActuallyPaused,
                    pausedUntil = pausedUntil,
                    isProtected = launchable.packageName in protectedPackages,
                )
            }
                .sortedWith(compareBy<AppEntry> { it.displayLabel.lowercase() }.thenBy { it.key.packageName })
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun refreshApps() {
        refreshFromPackageCallback()
    }

    override suspend fun launch(app: AppEntry) {
        val className = app.key.className ?: return
        val userHandle = userHandleForSerial(app.key.userSerial) ?: return
        launcherApps.startMainActivity(
            ComponentName(app.key.packageName, className),
            userHandle,
            null,
            null,
        )
    }

    override fun userHandleForSerial(userSerial: Long): UserHandle? {
        return userResolver.userHandleForSerial(userSerial)
            ?: launcherApps.profiles.firstOrNull { userManager.getSerialNumberForUser(it) == userSerial }
    }

    private fun loadLaunchableApps(): List<LaunchableApp> {
        return (loadFromLauncherApps() + loadCurrentUserAppsFromPackageManager())
            .distinctBy { "${it.packageName}/${it.className}/${it.userSerial}" }
    }

    private fun loadFromLauncherApps(): List<LaunchableApp> {
        return runCatching {
            launcherApps.profiles.flatMap { user ->
                val serial = userManager.getSerialNumberForUser(user)
                launcherApps.getActivityList(null, user).map { info ->
                    LaunchableApp(
                        packageName = info.applicationInfo.packageName,
                        className = info.componentName.className,
                        label = info.label?.toString()?.takeIf { it.isNotBlank() }
                            ?: info.applicationInfo.loadLabel(context.packageManager).toString(),
                        userSerial = serial,
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadCurrentUserAppsFromPackageManager(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packageManager = context.packageManager
        val currentUserSerial = userManager.getSerialNumberForUser(AndroidProcess.myUserHandle())
        val resolveInfos = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        return resolveInfos.mapNotNull { info ->
            val activityInfo = info.activityInfo ?: return@mapNotNull null
            LaunchableApp(
                packageName = activityInfo.packageName,
                className = activityInfo.name,
                label = info.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() }
                    ?: activityInfo.applicationInfo?.loadLabel(packageManager)?.toString()
                    ?: activityInfo.packageName,
                userSerial = currentUserSerial,
            )
        }
    }

    private data class LaunchableApp(
        val packageName: String,
        val className: String,
        val label: String,
        val userSerial: Long,
    )

    private fun refreshFromPackageCallback() {
        refreshTick.update { it + 1 }
    }

    private companion object {
        val activeStatuses = setOf(
            PauseSessionStatus.Pending,
            PauseSessionStatus.Active,
            PauseSessionStatus.PartiallyFailed,
            PauseSessionStatus.WaitingForShizuku,
            PauseSessionStatus.FailedToUnpause,
        )
    }
}

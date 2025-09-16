package com.focusfloat.app.pause.shizuku

import android.content.Context
import android.content.pm.PackageManager
import com.focusfloat.app.launcher.apps.PackageStateReader
import com.focusfloat.app.launcher.apps.PackageSuspensionState
import com.focusfloat.app.launcher.apps.UserResolver
import com.focusfloat.app.pause.executor.LocalShell
import com.focusfloat.app.pause.executor.PauseExecutor
import com.focusfloat.app.pause.executor.ShellResult
import com.focusfloat.app.pause.executor.isValidPackageName
import com.focusfloat.app.pause.model.PauseExecutorAvailability
import com.focusfloat.app.pause.model.PauseExecutorType
import com.focusfloat.app.pause.model.PauseOperationResult
import com.focusfloat.app.pause.model.PausePackageFailure
import com.focusfloat.app.pause.model.PauseSetupAction
import com.focusfloat.app.pause.safety.ProtectedPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class ShizukuPauseExecutor(
    private val context: Context,
    private val shell: LocalShell,
    private val packageStateReader: PackageStateReader,
) : PauseExecutor {
    override val type = PauseExecutorType.ShizukuShell
    private val userResolver = UserResolver(context)

    override suspend fun availability(): PauseExecutorAvailability = withContext(Dispatchers.IO) {
        when (status()) {
            ShizukuStatus.NotInstalled -> PauseExecutorAvailability(
                available = false,
                type = type,
                reason = "Shizuku is not installed",
                setupAction = PauseSetupAction.OpenShizuku,
            )
            ShizukuStatus.NotRunning -> PauseExecutorAvailability(
                available = false,
                type = type,
                reason = "Shizuku is not running",
                setupAction = PauseSetupAction.OpenShizuku,
            )
            ShizukuStatus.PermissionRequired -> PauseExecutorAvailability(
                available = false,
                type = type,
                reason = "Shizuku permission required",
                setupAction = PauseSetupAction.RequestShizukuPermission,
            )
            ShizukuStatus.Ready -> PauseExecutorAvailability(available = true, type = type)
        }
    }

    fun status(): ShizukuStatus {
        if (!isShizukuInstalled()) return ShizukuStatus.NotInstalled
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binderAlive) return ShizukuStatus.NotRunning
        val permission = runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
            .getOrDefault(false)
        return if (permission) ShizukuStatus.Ready else ShizukuStatus.PermissionRequired
    }

    fun requestPermission(requestCode: Int = REQUEST_CODE_SHIZUKU) {
        if (runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            Shizuku.requestPermission(requestCode)
        }
    }

    fun openShizuku() {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override suspend fun pausePackages(
        packageNames: List<String>,
        userSerial: Long,
        dialogMessage: String?,
    ): PauseOperationResult {
        val requested = packageNames.distinct()
        val succeeded = mutableListOf<String>()
        val failed = mutableListOf<PausePackageFailure>()
        val userId = userResolver.userIdForSerial(userSerial)
            ?: return unresolvedUserResult(requested, userSerial)

        for (pkg in requested) {
            val validationFailure = validationFailure(pkg)
            if (validationFailure != null) {
                failed += validationFailure
                continue
            }

            val args = buildList {
                add("cmd")
                add("package")
                add("suspend")
                add("--user")
                add(userId.toString())
                if (!dialogMessage.isNullOrBlank()) {
                    add("--dialogMessage")
                    add(dialogMessage)
                }
                add(pkg)
            }
            val result = try {
                shell.exec(args)
            } catch (t: Throwable) {
                failed += PausePackageFailure(pkg, t.message ?: "Shell command failed")
                continue
            }
            if (result.exitCode != 0) {
                failed += shellFailure(pkg, "Suspend command failed", result)
                continue
            }
            when (val state = packageStateReader.suspensionState(pkg, userSerial)) {
                PackageSuspensionState.Suspended -> succeeded += pkg
                else -> failed += shellFailure(pkg, "Suspend verification failed: $state", result)
            }
        }
        return PauseOperationResult(requested, succeeded, failed)
    }

    override suspend fun unpausePackages(packageNames: List<String>, userSerial: Long): PauseOperationResult {
        val requested = packageNames.distinct()
        val succeeded = mutableListOf<String>()
        val failed = mutableListOf<PausePackageFailure>()
        val userId = userResolver.userIdForSerial(userSerial)
            ?: return unresolvedUserResult(requested, userSerial)

        for (pkg in requested) {
            if (!isValidPackageName(pkg)) {
                failed += PausePackageFailure(pkg, "Invalid package name")
                continue
            }
            val result = try {
                shell.exec(listOf("cmd", "package", "unsuspend", "--user", userId.toString(), pkg))
            } catch (t: Throwable) {
                failed += PausePackageFailure(pkg, t.message ?: "Shell command failed")
                continue
            }
            if (result.exitCode != 0) {
                failed += shellFailure(pkg, "Unsuspend command failed", result)
                continue
            }
            when (val state = packageStateReader.suspensionState(pkg, userSerial)) {
                PackageSuspensionState.NotSuspended,
                PackageSuspensionState.PackageNotFound -> succeeded += pkg
                else -> failed += shellFailure(pkg, "Unsuspend verification failed: $state", result)
            }
        }
        return PauseOperationResult(requested, succeeded, failed)
    }

    override suspend fun isPackagePaused(packageName: String, userSerial: Long): Boolean {
        return packageStateReader.isSuspended(packageName, userSerial)
    }

    private fun validationFailure(packageName: String): PausePackageFailure? {
        if (!isValidPackageName(packageName)) return PausePackageFailure(packageName, "Invalid package name")
        if (ProtectedPackages.isProtected(packageName, context)) return PausePackageFailure(packageName, "Protected package")
        return null
    }

    private fun unresolvedUserResult(packageNames: List<String>, userSerial: Long): PauseOperationResult {
        return PauseOperationResult(
            requested = packageNames,
            succeeded = emptyList(),
            failed = packageNames.map {
                PausePackageFailure(it, "Could not resolve Android user for serial $userSerial")
            },
        )
    }

    private fun shellFailure(packageName: String, reason: String, result: ShellResult): PausePackageFailure {
        return PausePackageFailure(
            packageName = packageName,
            reason = reason,
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr,
        )
    }

    private fun isShizukuInstalled(): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_SHIZUKU = 60_901
    }
}

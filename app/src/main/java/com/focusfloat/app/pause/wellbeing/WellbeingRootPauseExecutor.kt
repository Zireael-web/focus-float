package com.focusfloat.app.pause.wellbeing

import android.content.Context
import android.content.pm.PackageManager
import com.focusfloat.app.launcher.apps.PackageStateReader
import com.focusfloat.app.launcher.apps.PackageSuspensionState
import com.focusfloat.app.launcher.apps.UserResolver
import com.focusfloat.app.pause.executor.PauseExecutor
import com.focusfloat.app.pause.executor.ShellResult
import com.focusfloat.app.pause.executor.isValidPackageName
import com.focusfloat.app.pause.model.PauseExecutorAvailability
import com.focusfloat.app.pause.model.PauseExecutorType
import com.focusfloat.app.pause.model.PauseOperationResult
import com.focusfloat.app.pause.model.PausePackageFailure
import com.focusfloat.app.pause.model.PauseSetupAction
import com.focusfloat.app.pause.safety.ProtectedPackages
import com.focusfloat.app.pause.shizuku.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class WellbeingRootPauseExecutor(
    private val context: Context,
    private val shell: ShizukuShell,
    private val packageStateReader: PackageStateReader,
) : PauseExecutor {
    override val type = PauseExecutorType.WellbeingRoot
    private val userResolver = UserResolver(context)

    override suspend fun availability(): PauseExecutorAvailability = withContext(Dispatchers.IO) {
        when {
            !isShizukuInstalled() -> unavailable("Shizuku is not installed")
            !runCatching { Shizuku.pingBinder() }.getOrDefault(false) -> unavailable("Shizuku is not running")
            runCatching { Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED }.getOrDefault(true) ->
                unavailable("Shizuku permission required", PauseSetupAction.RequestShizukuPermission)
            runCatching { Shizuku.getUid() }.getOrDefault(-1) != ROOT_UID -> unavailable(
                "Root Shizuku required for Pixel Digital Wellbeing pause. Current Shizuku mode is ADB/shell.",
            )
            else -> {
                val status = shell.wellbeingStatus(PRIMARY_USER_ID)
                if (status.exitCode == 0) {
                    PauseExecutorAvailability(available = true, type = type)
                } else {
                    unavailable(status.stderr.ifBlank { "Root Wellbeing pause engine is not available" })
                }
            }
        }
    }

    override suspend fun pausePackages(
        packageNames: List<String>,
        userSerial: Long,
        dialogMessage: String?,
    ): PauseOperationResult {
        val requested = packageNames.distinct()
        val validationFailures = requested.mapNotNull { validationFailure(it) }
        val validPackages = requested.filterNot { pkg -> validationFailures.any { it.packageName == pkg } }
        if (validPackages.isEmpty()) {
            return PauseOperationResult(requested, emptyList(), validationFailures)
        }
        val userId = userResolver.userIdForSerial(userSerial)
            ?: return unresolvedUserResult(requested, userSerial)

        val result = shell.setWellbeingSuspension(
            packageNames = validPackages,
            suspended = true,
            targetUserId = userId,
            suspendingUserId = userId,
            dialogMessage = dialogMessage,
        )
        return classifyResult(
            requested = requested,
            attempted = validPackages,
            validationFailures = validationFailures,
            result = result,
            userSerial = userSerial,
            expectedState = PackageSuspensionState.Suspended,
            verificationFailurePrefix = "Wellbeing suspend verification failed",
            allowPackageNotFoundSuccess = false,
        )
    }

    override suspend fun unpausePackages(packageNames: List<String>, userSerial: Long): PauseOperationResult {
        val requested = packageNames.distinct()
        val validationFailures = requested.mapNotNull { validationFailure(it, protectSystemPackages = false) }
        val validPackages = requested.filterNot { pkg -> validationFailures.any { it.packageName == pkg } }
        if (validPackages.isEmpty()) {
            return PauseOperationResult(requested, emptyList(), validationFailures)
        }
        val userId = userResolver.userIdForSerial(userSerial)
            ?: return unresolvedUserResult(requested, userSerial)

        val result = shell.setWellbeingSuspension(
            packageNames = validPackages,
            suspended = false,
            targetUserId = userId,
            suspendingUserId = userId,
            dialogMessage = null,
        )
        return classifyResult(
            requested = requested,
            attempted = validPackages,
            validationFailures = validationFailures,
            result = result,
            userSerial = userSerial,
            expectedState = PackageSuspensionState.NotSuspended,
            verificationFailurePrefix = "Wellbeing unsuspend verification failed",
            allowPackageNotFoundSuccess = true,
        )
    }

    override suspend fun isPackagePaused(packageName: String, userSerial: Long): Boolean {
        return packageStateReader.isSuspended(packageName, userSerial)
    }

    private fun classifyResult(
        requested: List<String>,
        attempted: List<String>,
        validationFailures: List<PausePackageFailure>,
        result: ShellResult,
        userSerial: Long,
        expectedState: PackageSuspensionState,
        verificationFailurePrefix: String,
        allowPackageNotFoundSuccess: Boolean,
    ): PauseOperationResult {
        if (result.exitCode != 0) {
            return PauseOperationResult(
                requested = requested,
                succeeded = emptyList(),
                failed = validationFailures + attempted.map {
                    shellFailure(it, "Wellbeing PackageManager call failed", result)
                },
            )
        }

        val succeeded = mutableListOf<String>()
        val failed = validationFailures.toMutableList()
        val rejectedPackages = result.rejectedPackages()
        for (pkg in attempted) {
            val rejected = pkg in rejectedPackages
            if (rejected && expectedState == PackageSuspensionState.Suspended) {
                failed += shellFailure(pkg, "Wellbeing PackageManager rejected package", result)
                continue
            }
            val state = packageStateReader.suspensionState(pkg, userSerial)
            if (state == expectedState || (allowPackageNotFoundSuccess && state == PackageSuspensionState.PackageNotFound)) {
                succeeded += pkg
            } else if (rejected) {
                failed += shellFailure(pkg, "Wellbeing PackageManager rejected package", result)
            } else {
                failed += shellFailure(pkg, "$verificationFailurePrefix: $state", result)
            }
        }
        return PauseOperationResult(requested, succeeded, failed)
    }

    private fun validationFailure(
        packageName: String,
        protectSystemPackages: Boolean = true,
    ): PausePackageFailure? {
        if (!isValidPackageName(packageName)) return PausePackageFailure(packageName, "Invalid package name")
        if (protectSystemPackages && ProtectedPackages.isProtected(packageName, context)) {
            return PausePackageFailure(packageName, "Protected package")
        }
        return null
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

    private fun ShellResult.rejectedPackages(): Set<String> {
        val fromStdout = stdout.substringAfter("rejectedPackages=", missingDelimiterValue = "")
            .substringBefore(";")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        if (fromStdout.isNotEmpty()) return fromStdout

        return stderr.substringAfter("PackageManager rejected:", missingDelimiterValue = "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
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

    private fun unavailable(
        reason: String,
        setupAction: PauseSetupAction = PauseSetupAction.OpenShizuku,
    ) = PauseExecutorAvailability(
        available = false,
        type = type,
        reason = reason,
        setupAction = setupAction,
    )

    private fun isPackageInstalled(packageName: String): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }

    private fun isShizukuInstalled(): Boolean {
        return isPackageInstalled(SHIZUKU_PACKAGE)
    }

    private companion object {
        const val ROOT_UID = 0
        const val PRIMARY_USER_ID = 0
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}

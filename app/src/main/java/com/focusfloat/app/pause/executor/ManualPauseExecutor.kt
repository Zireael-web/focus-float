package com.focusfloat.app.pause.executor

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.focusfloat.app.launcher.apps.PackageStateReader
import com.focusfloat.app.pause.model.PauseExecutorAvailability
import com.focusfloat.app.pause.model.PauseExecutorType
import com.focusfloat.app.pause.model.PauseOperationResult
import com.focusfloat.app.pause.model.PausePackageFailure
import com.focusfloat.app.pause.model.PauseSetupAction

class ManualPauseExecutor(
    private val context: Context,
    private val packageStateReader: PackageStateReader,
) : PauseExecutor {
    override val type = PauseExecutorType.ManualOnly

    override suspend fun availability() = PauseExecutorAvailability(
        available = false,
        type = type,
        reason = "Pause setup required",
        setupAction = PauseSetupAction.OpenShizuku,
    )

    override suspend fun pausePackages(
        packageNames: List<String>,
        userSerial: Long,
        dialogMessage: String?,
    ): PauseOperationResult {
        return PauseOperationResult(
            requested = packageNames,
            succeeded = emptyList(),
            failed = packageNames.map { PausePackageFailure(it, "Pause setup required") },
        )
    }

    override suspend fun unpausePackages(packageNames: List<String>, userSerial: Long): PauseOperationResult {
        return PauseOperationResult(
            requested = packageNames,
            succeeded = emptyList(),
            failed = packageNames.map { PausePackageFailure(it, "Pause setup required") },
        )
    }

    override suspend fun isPackagePaused(packageName: String, userSerial: Long): Boolean {
        return packageStateReader.isSuspended(packageName, userSerial)
    }

    fun openDigitalWellbeingOrSettings() {
        val intents = listOf(
            Intent("com.google.android.apps.wellbeing.action.FOCUS_MODE"),
            Intent(Settings.ACTION_SETTINGS),
        )
        val intent = intents.firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?: Intent(Settings.ACTION_SETTINGS)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

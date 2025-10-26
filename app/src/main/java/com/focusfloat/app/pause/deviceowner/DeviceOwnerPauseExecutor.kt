package com.focusfloat.app.pause.deviceowner

import android.content.Context
import com.focusfloat.app.launcher.apps.PackageStateReader
import com.focusfloat.app.pause.executor.PauseExecutor
import com.focusfloat.app.pause.model.PauseExecutorAvailability
import com.focusfloat.app.pause.model.PauseExecutorType
import com.focusfloat.app.pause.model.PauseOperationResult
import com.focusfloat.app.pause.model.PausePackageFailure
import com.focusfloat.app.pause.model.PauseSetupAction

class DeviceOwnerPauseExecutor(
    private val context: Context,
    private val packageStateReader: PackageStateReader,
) : PauseExecutor {
    override val type = PauseExecutorType.DeviceOwner

    override suspend fun availability(): PauseExecutorAvailability {
        return PauseExecutorAvailability(
            available = false,
            type = type,
            reason = "Device Owner executor is not implemented in this build",
            setupAction = PauseSetupAction.OpenDeviceOwnerHelp,
        )
    }

    override suspend fun pausePackages(
        packageNames: List<String>,
        userSerial: Long,
        dialogMessage: String?,
    ): PauseOperationResult {
        return unsupported(packageNames)
    }

    override suspend fun unpausePackages(packageNames: List<String>, userSerial: Long): PauseOperationResult {
        return unsupported(packageNames)
    }

    override suspend fun isPackagePaused(packageName: String, userSerial: Long): Boolean {
        return packageStateReader.isSuspended(packageName, userSerial)
    }

    private fun unsupported(packageNames: List<String>): PauseOperationResult {
        return PauseOperationResult(
            requested = packageNames,
            succeeded = emptyList(),
            failed = packageNames.map { PausePackageFailure(it, "Device Owner executor is not implemented in MVP") },
        )
    }
}

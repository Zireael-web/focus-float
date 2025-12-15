package com.focusfloat.app.pause.model

import com.focusfloat.app.core.model.AppRef
import java.time.Instant

data class PauseSession(
    val id: Long,
    val categoryId: Long,
    val packageNames: List<String>,
    val userSerial: Long,
    val startedAt: Instant,
    val pausedUntil: Instant,
    val status: PauseSessionStatus,
    val executorType: PauseExecutorType,
    val failureSummary: String?,
    val packageListCorrupt: Boolean = false,
) {
    val appRefs: List<AppRef> get() = packageNames.map { AppRef(packageName = it, userSerial = userSerial) }
}

data class NewPauseSession(
    val categoryId: Long,
    val packageNames: List<String>,
    val userSerial: Long,
    val startedAt: Instant,
    val pausedUntil: Instant,
    val status: PauseSessionStatus,
    val executorType: PauseExecutorType,
)

enum class PauseSessionStatus {
    Pending,
    Active,
    PartiallyFailed,
    Cancelled,
    Expired,
    WaitingForShizuku,
    FailedToUnpause,
}

enum class PauseExecutorType {
    WellbeingRoot,
    ShizukuShell,
    DeviceOwner,
    ManualOnly,
}

enum class PauseAction {
    Pause,
    Unpause,
}

data class PauseOperationResult(
    val requested: List<String>,
    val succeeded: List<String>,
    val failed: List<PausePackageFailure>,
    val warnings: List<String> = emptyList(),
) {
    val allSucceeded: Boolean get() = failed.isEmpty()
}

data class PausePackageFailure(
    val packageName: String,
    val reason: String,
    val exitCode: Int? = null,
    val stdout: String? = null,
    val stderr: String? = null,
)

data class PauseExecutorAvailability(
    val available: Boolean,
    val type: PauseExecutorType,
    val reason: String? = null,
    val setupAction: PauseSetupAction? = null,
)

enum class PauseSetupAction {
    OpenShizuku,
    RequestShizukuPermission,
    OpenDeviceOwnerHelp,
    OpenDigitalWellbeing,
}

class PauseExecutorUnavailableException(
    val availability: PauseExecutorAvailability,
) : IllegalStateException(availability.reason ?: "Pause executor unavailable")

class PauseReconcileRetryException(
    message: String,
) : IllegalStateException(message)

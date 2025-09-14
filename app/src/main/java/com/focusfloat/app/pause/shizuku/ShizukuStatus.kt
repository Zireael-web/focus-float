package com.focusfloat.app.pause.shizuku

sealed interface ShizukuStatus {
    data object NotInstalled : ShizukuStatus
    data object NotRunning : ShizukuStatus
    data object PermissionRequired : ShizukuStatus
    data object Ready : ShizukuStatus
}

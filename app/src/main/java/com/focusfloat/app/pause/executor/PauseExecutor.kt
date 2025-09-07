package com.focusfloat.app.pause.executor

import com.focusfloat.app.pause.model.PauseExecutorAvailability
import com.focusfloat.app.pause.model.PauseExecutorType
import com.focusfloat.app.pause.model.PauseOperationResult

interface PauseExecutor {
    val type: PauseExecutorType

    suspend fun availability(): PauseExecutorAvailability

    suspend fun pausePackages(
        packageNames: List<String>,
        userSerial: Long,
        dialogMessage: String?,
    ): PauseOperationResult

    suspend fun unpausePackages(
        packageNames: List<String>,
        userSerial: Long,
    ): PauseOperationResult

    suspend fun isPackagePaused(packageName: String, userSerial: Long): Boolean
}

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

interface LocalShell {
    suspend fun exec(args: List<String>): ShellResult
}

val PackageNameRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

fun isValidPackageName(packageName: String): Boolean = PackageNameRegex.matches(packageName)

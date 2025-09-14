package com.focusfloat.app.pause.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import com.focusfloat.app.pause.executor.LocalShell
import com.focusfloat.app.pause.executor.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

class ShizukuShell(private val context: Context) : LocalShell {
    private val bindLock = Mutex()
    private var userServiceBinder: IBinder? = null
    private var userServiceConnection: ServiceConnection? = null

    override suspend fun exec(args: List<String>): ShellResult = withContext(Dispatchers.IO) {
        require(args.isNotEmpty()) { "Shell command args cannot be empty" }
        execViaUserService(args) ?: ShellResult(
            exitCode = 126,
            stdout = "",
            stderr = "Could not bind Shizuku UserService",
        )
    }

    suspend fun setWellbeingSuspension(
        packageNames: List<String>,
        suspended: Boolean,
        targetUserId: Int,
        suspendingUserId: Int,
        dialogMessage: String?,
    ): ShellResult = withContext(Dispatchers.IO) {
        val binder = binder() ?: return@withContext ShellResult(
            exitCode = 126,
            stdout = "",
            stderr = "Could not bind Shizuku UserService",
        )
        withTimeoutOrNull(SHELL_TIMEOUT_MS) {
            runCatching {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(ShizukuShellUserService.INTERFACE_DESCRIPTOR)
                    data.writeStringArray(packageNames.toTypedArray())
                    data.writeInt(if (suspended) 1 else 0)
                    data.writeInt(targetUserId)
                    data.writeInt(suspendingUserId)
                    data.writeString(dialogMessage)
                    binder.transact(ShizukuShellUserService.TRANSACTION_WELLBEING_SUSPENSION, data, reply, 0)
                    ShellResult(
                        exitCode = reply.readInt(),
                        stdout = reply.readString().orEmpty(),
                        stderr = reply.readString().orEmpty(),
                    )
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }.getOrElse {
                userServiceBinder = null
                ShellResult(1, "", it.message ?: "Wellbeing suspension transaction failed")
            }
        } ?: ShellResult(
            exitCode = 124,
            stdout = "",
            stderr = "Shizuku UserService command timed out after ${SHELL_TIMEOUT_MS}ms",
        )
    }

    suspend fun wellbeingStatus(targetUserId: Int): ShellResult = withContext(Dispatchers.IO) {
        val binder = binder() ?: return@withContext ShellResult(
            exitCode = 126,
            stdout = "",
            stderr = "Could not bind Shizuku UserService",
        )
        withTimeoutOrNull(SHELL_TIMEOUT_MS) {
            runCatching {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(ShizukuShellUserService.INTERFACE_DESCRIPTOR)
                    data.writeInt(targetUserId)
                    binder.transact(ShizukuShellUserService.TRANSACTION_WELLBEING_STATUS, data, reply, 0)
                    ShellResult(
                        exitCode = reply.readInt(),
                        stdout = reply.readString().orEmpty(),
                        stderr = reply.readString().orEmpty(),
                    )
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }.getOrElse {
                userServiceBinder = null
                ShellResult(1, "", it.message ?: "Wellbeing status transaction failed")
            }
        } ?: ShellResult(
            exitCode = 124,
            stdout = "",
            stderr = "Shizuku UserService command timed out after ${SHELL_TIMEOUT_MS}ms",
        )
    }

    private suspend fun execViaUserService(args: List<String>): ShellResult? {
        val binder = binder() ?: return null
        return withTimeoutOrNull(SHELL_TIMEOUT_MS) {
            runCatching {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(ShizukuShellUserService.INTERFACE_DESCRIPTOR)
                    data.writeStringArray(args.toTypedArray())
                    binder.transact(ShizukuShellUserService.TRANSACTION_EXEC, data, reply, 0)
                    ShellResult(
                        exitCode = reply.readInt(),
                        stdout = reply.readString().orEmpty(),
                        stderr = reply.readString().orEmpty(),
                    )
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }.getOrElse {
                userServiceBinder = null
                null
            }
        } ?: ShellResult(
            exitCode = 124,
            stdout = "",
            stderr = "Shizuku UserService command timed out after ${SHELL_TIMEOUT_MS}ms",
        )
    }

    private suspend fun binder(): IBinder? = bindLock.withLock {
        userServiceBinder?.takeIf { it.pingBinder() }?.let { return@withLock it }
        withTimeoutOrNull(BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine<IBinder?> { continuation ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        userServiceBinder = service
                        userServiceConnection = this
                        if (continuation.isActive) continuation.resume(service)
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        userServiceBinder = null
                        userServiceConnection = null
                    }
                }
                userServiceConnection = connection
                runCatching {
                    Shizuku.bindUserService(userServiceArgs(), connection)
                }.onFailure {
                    userServiceConnection = null
                    if (continuation.isActive) continuation.resume(null)
                }
                continuation.invokeOnCancellation {
                    runCatching { Shizuku.unbindUserService(userServiceArgs(), connection, false) }
                    userServiceConnection = null
                }
            }
        }
    }

    private fun userServiceArgs(): Shizuku.UserServiceArgs {
        return Shizuku.UserServiceArgs(ComponentName(context, ShizukuShellUserService::class.java))
            .daemon(false)
            .tag("focusfloat_shell")
            .version(3)
            .debuggable(false)
            .processNameSuffix("shell")
    }

    private companion object {
        const val BIND_TIMEOUT_MS = 5_000L
        const val SHELL_TIMEOUT_MS = 30_000L
    }
}

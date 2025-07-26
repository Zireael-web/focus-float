package com.focusfloat.app.launcher.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle

interface PackageStateReader {
    fun suspensionState(packageName: String, userSerial: Long = 0): PackageSuspensionState

    fun isSuspended(packageName: String, userSerial: Long = 0): Boolean
}

enum class PackageSuspensionState {
    Suspended,
    NotSuspended,
    PackageNotFound,
    UserUnavailable,
    Unknown,
}

class AndroidPackageStateReader(
    private val context: Context,
) : PackageStateReader {
    private val userResolver = UserResolver(context)

    override fun suspensionState(packageName: String, userSerial: Long): PackageSuspensionState {
        return try {
            val userContext = contextForUserSerial(userSerial) ?: return PackageSuspensionState.UserUnavailable
            val info = if (Build.VERSION.SDK_INT >= 33) {
                userContext.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                userContext.packageManager.getApplicationInfo(packageName, 0)
            }
            if (info.flags and ApplicationInfo.FLAG_SUSPENDED != 0) {
                PackageSuspensionState.Suspended
            } else {
                PackageSuspensionState.NotSuspended
            }
        } catch (_: PackageManager.NameNotFoundException) {
            PackageSuspensionState.PackageNotFound
        } catch (_: RuntimeException) {
            PackageSuspensionState.Unknown
        }
    }

    override fun isSuspended(packageName: String, userSerial: Long): Boolean {
        return suspensionState(packageName, userSerial) == PackageSuspensionState.Suspended
    }

    private fun contextForUserSerial(userSerial: Long): Context? {
        val userHandle = userResolver.userHandleForSerial(userSerial) ?: return null
        return runCatching {
            val method = Context::class.java.getMethod(
                "createContextAsUser",
                UserHandle::class.java,
                Int::class.javaPrimitiveType,
            )
            method.invoke(context, userHandle, 0) as Context
        }.getOrNull()
    }
}

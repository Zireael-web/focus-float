package com.focusfloat.app.launcher.apps

import android.content.Context
import android.os.Process as AndroidProcess
import android.os.UserHandle
import android.os.UserManager

class UserResolver(context: Context) {
    private val userManager = context.getSystemService(UserManager::class.java)
    private val currentUserSerial = userManager.getSerialNumberForUser(AndroidProcess.myUserHandle())

    fun userHandleForSerial(userSerial: Long): UserHandle? {
        return userManager.getUserForSerialNumber(userSerial)
    }

    fun userIdForSerial(userSerial: Long): Int? {
        val handle = userHandleForSerial(userSerial) ?: return null
        val reflected = runCatching {
            val method = UserHandle::class.java.getDeclaredMethod("getIdentifier")
            method.isAccessible = true
            method.invoke(handle) as Int
        }.getOrNull()
        if (reflected != null) return reflected

        return if (userSerial == currentUserSerial) 0 else null
    }
}

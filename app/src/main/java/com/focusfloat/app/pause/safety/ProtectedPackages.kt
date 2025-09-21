package com.focusfloat.app.pause.safety

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager

object ProtectedPackages {
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    fun hardcoded(context: Context): Set<String> = setOf(
        context.packageName,
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.google.android.apps.nbu.files",
        "com.android.phone",
        "com.google.android.dialer",
        "com.google.android.gms",
        "com.android.vending",
        "com.google.android.apps.wellbeing",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
        SHIZUKU_PACKAGE,
    )

    fun isProtected(packageName: String, context: Context): Boolean {
        if (packageName in hardcoded(context)) return true
        if (isHomeLauncher(packageName, context)) return true
        if (isDefaultInputMethod(packageName, context)) return true
        if (isDefaultDialer(packageName, context)) return true
        if (isDefaultSms(packageName, context)) return true
        return false
    }

    private fun isHomeLauncher(packageName: String, context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val current = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName == packageName
        if (current) return true

        val homeCandidates = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return homeCandidates.any { it.activityInfo?.packageName == packageName }
    }

    private fun isDefaultInputMethod(packageName: String, context: Context): Boolean {
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore("/")
                ?.takeIf { it.isNotBlank() } == packageName
        }.getOrDefault(false)
    }

    private fun isDefaultDialer(packageName: String, context: Context): Boolean {
        return runCatching {
            context.getSystemService(TelecomManager::class.java).defaultDialerPackage == packageName
        }.getOrDefault(false)
    }

    private fun isDefaultSms(packageName: String, context: Context): Boolean {
        return runCatching { Telephony.Sms.getDefaultSmsPackage(context) == packageName }.getOrDefault(false)
    }
}

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
        if (packageName in homeLauncherPackages(context)) return true
        if (packageName == defaultInputMethodPackage(context)) return true
        if (packageName == defaultDialerPackage(context)) return true
        if (packageName == defaultSmsPackage(context)) return true
        return false
    }

    fun protectedSet(context: Context, packageNames: Collection<String>): Set<String> {
        val candidates = packageNames.toSet()
        return buildSet {
            addAll(hardcoded(context).filter { it in candidates })
            addAll(homeLauncherPackages(context).filter { it in candidates })
            defaultInputMethodPackage(context)?.takeIf { it in candidates }?.let(::add)
            defaultDialerPackage(context)?.takeIf { it in candidates }?.let(::add)
            defaultSmsPackage(context)?.takeIf { it in candidates }?.let(::add)
        }
    }

    private fun homeLauncherPackages(context: Context): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val current = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName

        val homeCandidates = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return buildSet {
            current?.let(::add)
            homeCandidates.mapNotNullTo(this) { it.activityInfo?.packageName }
        }
    }

    private fun defaultInputMethodPackage(context: Context): String? {
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore("/")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun defaultDialerPackage(context: Context): String? {
        return runCatching {
            context.getSystemService(TelecomManager::class.java).defaultDialerPackage
        }.getOrNull()
    }

    private fun defaultSmsPackage(context: Context): String? {
        return runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()
    }
}

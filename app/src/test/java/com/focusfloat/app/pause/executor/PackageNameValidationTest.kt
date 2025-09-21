package com.focusfloat.app.pause.executor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageNameValidationTest {
    @Test
    fun acceptsAndroidPackageNames() {
        assertTrue(isValidPackageName("com.google.android.youtube"))
        assertTrue(isValidPackageName("moe.shizuku.privileged.api"))
        assertTrue(isValidPackageName("com.example_app.launcher2"))
    }

    @Test
    fun rejectsShellLikeInput() {
        assertFalse(isValidPackageName("youtube"))
        assertFalse(isValidPackageName("com.example;cmd package list packages"))
        assertFalse(isValidPackageName("com.example.$"))
        assertFalse(isValidPackageName("1com.example.app"))
    }
}

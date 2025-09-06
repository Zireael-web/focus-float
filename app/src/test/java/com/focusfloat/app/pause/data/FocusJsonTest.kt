package com.focusfloat.app.pause.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusJsonTest {
    @Test
    fun parsePackageListReturnsPackagesForValidJson() {
        assertEquals(
            listOf("com.google.android.gm", "com.android.chrome"),
            """["com.google.android.gm","com.android.chrome"]""".toStringListFromJsonArrayOrNull(),
        )
    }

    @Test
    fun parsePackageListReturnsNullForMalformedJson() {
        assertNull("[\"com.google.android.gm\"".toStringListFromJsonArrayOrNull())
    }
}

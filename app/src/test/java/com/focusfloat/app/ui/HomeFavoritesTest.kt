package com.focusfloat.app.ui

import com.focusfloat.app.core.model.AppEntry
import com.focusfloat.app.core.model.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFavoritesTest {
    @Test
    fun homeFavoritesDoesNotFallbackToInstalledApps() {
        val apps = (1..6).map { index ->
            appEntry(packageName = "com.example.app$index", label = "App $index")
        }

        assertTrue(apps.homeFavorites().isEmpty())
    }

    @Test
    fun homeFavoritesReturnsOnlyExplicitVisibleFavoritesInOrder() {
        val hiddenFavorite = appEntry(
            packageName = "com.example.hidden",
            label = "Hidden",
            favoriteOrder = 1,
            isHidden = true,
        )
        val second = appEntry(
            packageName = "com.example.second",
            label = "Second",
            favoriteOrder = 20,
        )
        val first = appEntry(
            packageName = "com.example.first",
            label = "First",
            favoriteOrder = 3,
        )
        val normal = appEntry(packageName = "com.example.normal", label = "Normal")

        assertEquals(listOf(first, second), listOf(hiddenFavorite, second, first, normal).homeFavorites())
    }

    private fun appEntry(
        packageName: String,
        label: String,
        favoriteOrder: Int? = null,
        isHidden: Boolean = false,
    ): AppEntry {
        return AppEntry(
            key = AppKey(packageName = packageName, className = null),
            label = label,
            customLabel = null,
            isFavorite = favoriteOrder != null,
            favoriteOrder = favoriteOrder,
            isHidden = isHidden,
            isPaused = false,
            pausedUntil = null,
            isProtected = false,
        )
    }
}

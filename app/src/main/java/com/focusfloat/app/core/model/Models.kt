package com.focusfloat.app.core.model

import java.time.Instant

data class AppKey(
    val packageName: String,
    val className: String?,
    val userSerial: Long = 0,
) {
    val ref: AppRef get() = AppRef(packageName = packageName, userSerial = userSerial)
}

data class AppRef(
    val packageName: String,
    val userSerial: Long = 0,
) {
    val stableKey: String get() = "$packageName@$userSerial"
}

data class AppEntry(
    val key: AppKey,
    val label: String,
    val customLabel: String?,
    val isFavorite: Boolean,
    val favoriteOrder: Int?,
    val isHidden: Boolean,
    val isPaused: Boolean,
    val pausedUntil: Instant?,
    val isProtected: Boolean,
) {
    val displayLabel: String get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}

enum class CategoryType {
    Folder,
    PauseGroup,
    DistractingGroup,
}

data class AppCategory(
    val id: Long,
    val name: String,
    val type: CategoryType,
    val showOnHome: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

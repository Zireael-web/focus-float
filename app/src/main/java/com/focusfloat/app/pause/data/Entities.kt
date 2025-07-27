package com.focusfloat.app.pause.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_overrides",
    primaryKeys = ["packageName", "userSerial"],
    indices = [Index("packageName")],
)
data class AppOverrideEntity(
    val packageName: String,
    val userSerial: Long,
    val customLabel: String?,
    val hidden: Boolean,
    val favoriteOrder: Int?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val showOnHome: Boolean,
    val sortOrder: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "category_items",
    primaryKeys = ["categoryId", "packageName", "userSerial"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packageName"), Index("categoryId")],
)
data class CategoryItemEntity(
    val categoryId: Long,
    val packageName: String,
    val userSerial: Long,
    val addedAtEpochMs: Long,
)

@Entity(
    tableName = "pause_sessions",
    indices = [Index("categoryId"), Index("status"), Index("pausedUntilEpochMs")],
)
data class PauseSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val packageNamesJson: String,
    val userSerial: Long,
    val startedAtEpochMs: Long,
    val pausedUntilEpochMs: Long,
    val status: String,
    val executorType: String,
    val failureSummary: String?,
)

@Entity(
    tableName = "pause_package_results",
    primaryKeys = ["sessionId", "packageName", "userSerial", "action"],
    indices = [Index("packageName"), Index("sessionId")],
)
data class PausePackageResultEntity(
    val sessionId: Long,
    val packageName: String,
    val userSerial: Long,
    val action: String,
    val success: Boolean,
    val exitCode: Int?,
    val stdout: String?,
    val stderr: String?,
    val createdAtEpochMs: Long,
)

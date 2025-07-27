package com.focusfloat.app.pause.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.focusfloat.app.core.model.AppRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOverrideDao {
    @Query("SELECT * FROM app_overrides")
    fun observeAll(): Flow<List<AppOverrideEntity>>

    @Query("SELECT * FROM app_overrides")
    suspend fun getAll(): List<AppOverrideEntity>

    @Query("SELECT * FROM app_overrides WHERE packageName = :packageName AND userSerial = :userSerial")
    suspend fun get(packageName: String, userSerial: Long): AppOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppOverrideEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun observeCategory(categoryId: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategory(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC LIMIT 1")
    suspend fun firstByType(type: String): CategoryEntity?

    @Query("SELECT packageName, userSerial FROM category_items WHERE categoryId = :categoryId ORDER BY addedAtEpochMs ASC")
    fun observeCategoryPackages(categoryId: Long): Flow<List<AppRef>>

    @Query("SELECT packageName, userSerial FROM category_items WHERE categoryId = :categoryId ORDER BY addedAtEpochMs ASC")
    suspend fun getCategoryPackages(categoryId: Long): List<AppRef>

    @Insert
    suspend fun insertCategory(entity: CategoryEntity): Long

    @Update
    suspend fun updateCategory(entity: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(entity: CategoryItemEntity)

    @Query("DELETE FROM category_items WHERE categoryId = :categoryId AND packageName = :packageName AND userSerial = :userSerial")
    suspend fun removeItem(categoryId: Long, packageName: String, userSerial: Long)

    @Transaction
    suspend fun ensureDefaultPauseCategory(nowEpochMs: Long): Long {
        val existing = firstByType("PauseGroup")
        if (existing != null) {
            if (existing.name == "Dopamine" || existing.name == "Focus Pause") {
                updateCategory(existing.copy(name = "Paused apps", updatedAtEpochMs = nowEpochMs))
            }
            return existing.id
        }
        return insertCategory(
            CategoryEntity(
                name = "Paused apps",
                type = "PauseGroup",
                showOnHome = true,
                sortOrder = 0,
                createdAtEpochMs = nowEpochMs,
                updatedAtEpochMs = nowEpochMs,
            ),
        )
    }

    @Transaction
    suspend fun ensureDefaultDistractingCategory(nowEpochMs: Long): Long {
        val existing = firstByType("DistractingGroup")
        if (existing != null) return existing.id
        return insertCategory(
            CategoryEntity(
                name = "Distracting apps",
                type = "DistractingGroup",
                showOnHome = false,
                sortOrder = 1,
                createdAtEpochMs = nowEpochMs,
                updatedAtEpochMs = nowEpochMs,
            ),
        )
    }
}

@Dao
interface PauseSessionDao {
    @Query(
        """
        SELECT * FROM pause_sessions
        WHERE categoryId = :categoryId
          AND status NOT IN ('Cancelled', 'Expired')
        ORDER BY startedAtEpochMs DESC
        LIMIT 1
        """,
    )
    fun observeActiveSessionForCategory(categoryId: Long): Flow<PauseSessionEntity?>

    @Query(
        """
        SELECT * FROM pause_sessions
        WHERE categoryId = :categoryId
          AND status NOT IN ('Cancelled', 'Expired')
        ORDER BY startedAtEpochMs DESC
        LIMIT 1
        """,
    )
    suspend fun getActiveSessionForCategory(categoryId: Long): PauseSessionEntity?

    @Query(
        """
        SELECT * FROM pause_sessions
        WHERE categoryId = :categoryId
          AND status NOT IN ('Cancelled', 'Expired')
        ORDER BY startedAtEpochMs DESC
        """,
    )
    suspend fun getActiveSessionsForCategory(categoryId: Long): List<PauseSessionEntity>

    @Query(
        """
        SELECT * FROM pause_sessions
        WHERE status NOT IN ('Cancelled', 'Expired')
        ORDER BY startedAtEpochMs DESC
        """,
    )
    fun observeActiveSessions(): Flow<List<PauseSessionEntity>>

    @Query(
        """
        SELECT * FROM pause_sessions
        WHERE status NOT IN ('Cancelled', 'Expired')
        ORDER BY pausedUntilEpochMs ASC
        """,
    )
    suspend fun getSessionsNeedingReconcile(): List<PauseSessionEntity>

    @Insert
    suspend fun insertSession(entity: PauseSessionEntity): Long

    @Query("UPDATE pause_sessions SET status = :status, failureSummary = :failureSummary WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: Long, status: String, failureSummary: String?)

    @Query("UPDATE pause_sessions SET packageNamesJson = :packageNamesJson WHERE id = :sessionId")
    suspend fun updatePackages(sessionId: Long, packageNamesJson: String)

    @Query("UPDATE pause_sessions SET pausedUntilEpochMs = :pausedUntilEpochMs WHERE id = :sessionId")
    suspend fun updatePausedUntil(sessionId: Long, pausedUntilEpochMs: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<PausePackageResultEntity>)
}

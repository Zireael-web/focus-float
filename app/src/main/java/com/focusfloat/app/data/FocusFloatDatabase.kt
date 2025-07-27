package com.focusfloat.app.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.focusfloat.app.pause.data.AppOverrideDao
import com.focusfloat.app.pause.data.AppOverrideEntity
import com.focusfloat.app.pause.data.CategoryDao
import com.focusfloat.app.pause.data.CategoryEntity
import com.focusfloat.app.pause.data.CategoryItemEntity
import com.focusfloat.app.pause.data.PausePackageResultEntity
import com.focusfloat.app.pause.data.PauseSessionDao
import com.focusfloat.app.pause.data.PauseSessionEntity

@Database(
    entities = [
        AppOverrideEntity::class,
        CategoryEntity::class,
        CategoryItemEntity::class,
        PauseSessionEntity::class,
        PausePackageResultEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class FocusFloatDatabase : RoomDatabase() {
    abstract fun appOverrideDao(): AppOverrideDao
    abstract fun categoryDao(): CategoryDao
    abstract fun pauseSessionDao(): PauseSessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_overrides_new` (
                        `packageName` TEXT NOT NULL,
                        `userSerial` INTEGER NOT NULL DEFAULT 0,
                        `customLabel` TEXT,
                        `hidden` INTEGER NOT NULL,
                        `favoriteOrder` INTEGER,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`, `userSerial`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `app_overrides_new` (
                        `packageName`, `userSerial`, `customLabel`, `hidden`, `favoriteOrder`,
                        `createdAtEpochMs`, `updatedAtEpochMs`
                    )
                    SELECT `packageName`, 0, `customLabel`, `hidden`, `favoriteOrder`,
                        `createdAtEpochMs`, `updatedAtEpochMs`
                    FROM `app_overrides`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `app_overrides`")
                db.execSQL("ALTER TABLE `app_overrides_new` RENAME TO `app_overrides`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_overrides_packageName` ON `app_overrides` (`packageName`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pause_package_results_new` (
                        `sessionId` INTEGER NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `userSerial` INTEGER NOT NULL DEFAULT 0,
                        `action` TEXT NOT NULL,
                        `success` INTEGER NOT NULL,
                        `exitCode` INTEGER,
                        `stdout` TEXT,
                        `stderr` TEXT,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`sessionId`, `packageName`, `userSerial`, `action`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `pause_package_results_new` (
                        `sessionId`, `packageName`, `userSerial`, `action`, `success`,
                        `exitCode`, `stdout`, `stderr`, `createdAtEpochMs`
                    )
                    SELECT `sessionId`, `packageName`, 0, `action`, `success`,
                        `exitCode`, `stdout`, `stderr`, `createdAtEpochMs`
                    FROM `pause_package_results`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `pause_package_results`")
                db.execSQL("ALTER TABLE `pause_package_results_new` RENAME TO `pause_package_results`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pause_package_results_packageName` ON `pause_package_results` (`packageName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pause_package_results_sessionId` ON `pause_package_results` (`sessionId`)")
            }
        }
    }
}

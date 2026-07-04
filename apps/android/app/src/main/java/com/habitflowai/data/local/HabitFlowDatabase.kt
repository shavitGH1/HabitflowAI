package com.habitflowai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.habitflowai.data.local.dao.DriftCheckDao
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.dao.LocationDao
import com.habitflowai.data.local.dao.UserDao
import com.habitflowai.data.local.entity.DriftCheckEntity
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, HabitEntity::class, LocationEntity::class, DriftCheckEntity::class],
    version = 3
)
@TypeConverters(Converters::class)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun locationDao(): LocationDao
    abstract fun driftCheckDao(): DriftCheckDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `locations` (
                        `id` TEXT NOT NULL,
                        `habitId` TEXT,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL DEFAULT 0,
                        `syncStatus` TEXT NOT NULL DEFAULT 'PENDING_CREATE',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `drift_checks` (
                        `id` TEXT NOT NULL,
                        `driftDetected` INTEGER NOT NULL,
                        `driftScore` REAL NOT NULL,
                        `newSuggestedPersona` TEXT,
                        `rationale` TEXT,
                        `checkedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}

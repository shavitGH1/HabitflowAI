package com.habitflowai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.habitflowai.data.local.dao.ChatDao
import com.habitflowai.data.local.dao.DriftCheckDao
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.dao.LocationDao
import com.habitflowai.data.local.dao.UserDao
import com.habitflowai.data.local.entity.ChatEntity
import com.habitflowai.data.local.entity.DriftCheckEntity
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.local.entity.MessageEntity
import com.habitflowai.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class, 
        HabitEntity::class, 
        LocationEntity::class, 
        DriftCheckEntity::class,
        ChatEntity::class,
        MessageEntity::class,
    ],
    version = 5
)
@TypeConverters(Converters::class)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun locationDao(): LocationDao
    abstract fun driftCheckDao(): DriftCheckDao
    abstract fun chatDao(): ChatDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // To change primary key, we must recreate the table
                db.execSQL("DROP TABLE IF EXISTS `chats_old`")
                db.execSQL("ALTER TABLE `chats` RENAME TO `chats_old`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chats` (
                        `id` TEXT NOT NULL, 
                        `userId` TEXT NOT NULL, 
                        `name` TEXT, 
                        `isGroup` INTEGER NOT NULL, 
                        `lastMessage` TEXT, 
                        `participantIds` TEXT NOT NULL, 
                        `admins` TEXT NOT NULL, 
                        `owner` TEXT, 
                        `description` TEXT, 
                        `imageUrl` TEXT, 
                        PRIMARY KEY(`id`, `userId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO `chats` SELECT * FROM `chats_old`")
                db.execSQL("DROP TABLE `chats_old`")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chats` (
                        `id` TEXT NOT NULL, 
                        `userId` TEXT NOT NULL, 
                        `name` TEXT, 
                        `isGroup` INTEGER NOT NULL, 
                        `lastMessage` TEXT, 
                        `participantIds` TEXT NOT NULL, 
                        `admins` TEXT NOT NULL, 
                        `owner` TEXT, 
                        `description` TEXT, 
                        `imageUrl` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `messages` (
                        `id` TEXT NOT NULL, 
                        `chatId` TEXT NOT NULL, 
                        `text` TEXT NOT NULL, 
                        `senderId` TEXT NOT NULL, 
                        `isFromBot` INTEGER NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `imageUrl` TEXT, 
                        `likedBy` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
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
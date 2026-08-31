package com.habitflowai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.habitflowai.data.local.dao.ChatDao
import com.habitflowai.data.local.dao.DailyTaskDao
import com.habitflowai.data.local.dao.DriftCheckDao
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.dao.LocationDao
import com.habitflowai.data.local.dao.UserDao
import com.habitflowai.data.local.entity.ChatEntity
import com.habitflowai.data.local.entity.DailyTaskEntity
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
        DailyTaskEntity::class
    ],
    version = 16
)
@TypeConverters(Converters::class)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun locationDao(): LocationDao
    abstract fun driftCheckDao(): DriftCheckDao
    abstract fun chatDao(): ChatDao
    abstract fun dailyTaskDao(): DailyTaskDao

    companion object {
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `streak` INTEGER NOT NULL DEFAULT 0")
            }
        }
        // Version collision: fix/habits-page and fix/homescreen_tasks (merged to master) both
        // independently used 12->13 for unrelated changes. Renumbered on merge so both survive:
        // 12->13 stays this branch's habits.implementedAt column; the daily_tasks migrations
        // from master are shifted up to 13->14 and 14->15.
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `implementedAt` TEXT")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_tasks` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `habitId` TEXT NOT NULL,
                        `habitTitle` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_userId` ON `daily_tasks` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_habitId` ON `daily_tasks` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_tasks_date` ON `daily_tasks` (`date`)")
            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_tasks` ADD COLUMN `genre` TEXT NOT NULL DEFAULT 'persona'")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habits_serverId` ON `habits` (`serverId`)")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `goalId` TEXT")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `relevanceWarning` TEXT")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `verificationWarning` TEXT")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `pinnedBy` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `mutedBy` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `lastMessageAt` INTEGER")
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `lastMessageSenderId` TEXT")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `profilePicture` TEXT")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `locations` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'task'")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `locations` ADD COLUMN `isPublic` INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN completionHistory TEXT NOT NULL DEFAULT '[]'")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate `locations` to match the v5 entity: the old v3->v4 migration added
                // taskTitle/placeName/address columns (and DEFAULT clauses) that no longer exist.
                db.execSQL("DROP TABLE IF EXISTS `locations_new`")
                db.execSQL("CREATE TABLE `locations_new` (`id` TEXT NOT NULL, `habitId` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `locations_new` (`id`, `habitId`, `latitude`, `longitude`, `timestamp`, `syncStatus`) SELECT `id`, `habitId`, `latitude`, `longitude`, `timestamp`, `syncStatus` FROM `locations`")
                db.execSQL("DROP TABLE `locations`")
                db.execSQL("ALTER TABLE `locations_new` RENAME TO `locations`")
                // Recreate `drift_checks` to match the v5 entity (drop the DEFAULT clause).
                db.execSQL("DROP TABLE IF EXISTS `drift_checks_new`")
                db.execSQL("CREATE TABLE `drift_checks_new` (`id` TEXT NOT NULL, `driftDetected` INTEGER NOT NULL, `driftScore` REAL NOT NULL, `newSuggestedPersona` TEXT, `rationale` TEXT, `checkedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `drift_checks_new` (`id`, `driftDetected`, `driftScore`, `newSuggestedPersona`, `rationale`, `checkedAt`) SELECT `id`, `driftDetected`, `driftScore`, `newSuggestedPersona`, `rationale`, `checkedAt` FROM `drift_checks`")
                db.execSQL("DROP TABLE `drift_checks`")
                db.execSQL("ALTER TABLE `drift_checks_new` RENAME TO `drift_checks`")
                val hasChats = db.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='chats'").use { cursor ->
                    cursor.moveToFirst()
                    cursor.getInt(0) > 0
                }
                if (hasChats) {
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
                } else {
                    // v4 databases never had a chats table; the feature was added in v5
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

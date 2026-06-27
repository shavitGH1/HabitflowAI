package com.habitflowai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.dao.UserDao
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.UserEntity

@Database(entities = [UserEntity::class, HabitEntity::class], version = 2)
@TypeConverters(Converters::class)
abstract class HabitFlowDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
}

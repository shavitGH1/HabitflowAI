package com.habitflowai.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.habitflowai.data.local.HabitFlowDatabase
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHabitFlowDatabase(@ApplicationContext context: Context): HabitFlowDatabase {
        return Room.databaseBuilder(
            context,
            HabitFlowDatabase::class.java,
            "habitflow_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUserDao(database: HabitFlowDatabase): UserDao = database.userDao()

    @Provides
    fun provideHabitDao(database: HabitFlowDatabase): HabitDao = database.habitDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}

package com.habitflowai.data.local.dao

import androidx.room.*
import com.habitflowai.data.local.entity.DailyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {
    @Query("SELECT * FROM daily_tasks WHERE userId = :userId AND date = :date")
    fun getTasksForDate(userId: String, date: String): Flow<List<DailyTaskEntity>>

    @Query("SELECT * FROM daily_tasks WHERE userId = :userId AND date = :date")
    suspend fun getTasksForDateSync(userId: String, date: String): List<DailyTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DailyTaskEntity>)

    @Query("DELETE FROM daily_tasks WHERE userId = :userId AND date = :date")
    suspend fun deleteTasksForDate(userId: String, date: String)

    @Query("UPDATE daily_tasks SET isCompleted = :completed WHERE id = :taskId AND userId = :userId")
    suspend fun updateTaskCompletion(userId: String, taskId: String, completed: Boolean)

    @Query("DELETE FROM daily_tasks WHERE userId = :userId AND date < :thresholdDate")
    suspend fun deleteOldTasks(userId: String, thresholdDate: String)

    @Query("SELECT DISTINCT date FROM daily_tasks WHERE userId = :userId AND isCompleted = 1")
    fun getDatesWithCompletions(userId: String): Flow<List<String>>
}

package com.habitflowai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.habitflowai.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE userId = :userId")
    fun getHabitsByUserId(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: String): HabitEntity?
}

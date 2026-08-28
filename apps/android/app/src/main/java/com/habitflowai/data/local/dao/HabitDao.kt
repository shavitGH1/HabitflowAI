package com.habitflowai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE userId = :userId AND syncStatus != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun getHabitsByUserId(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE serverId = :serverId")
    suspend fun getHabitByServerId(serverId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE userId = :userId AND syncStatus != :status")
    suspend fun getUnsyncedHabits(userId: String, status: SyncStatus = SyncStatus.SYNCED): List<HabitEntity>

    @Query("UPDATE habits SET syncStatus = :status WHERE id = :habitId")
    suspend fun markSynced(habitId: String, status: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM habits WHERE userId = :userId AND syncStatus = :status")
    suspend fun deleteBySyncStatus(userId: String, status: SyncStatus = SyncStatus.PENDING_DELETE)

    @Query("UPDATE habits SET syncStatus = :status, updatedAt = :updatedAt WHERE id = :habitId")
    suspend fun updateSyncStatus(habitId: String, status: SyncStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM habits WHERE userId = :userId")
    suspend fun getAllForUser(userId: String): List<HabitEntity>
}

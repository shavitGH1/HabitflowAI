package com.habitflowai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.habitflowai.data.local.entity.DriftCheckEntity

@Dao
interface DriftCheckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: DriftCheckEntity)

    @Query("SELECT * FROM drift_checks ORDER BY checkedAt DESC LIMIT 1")
    suspend fun getLatestDriftCheck(): DriftCheckEntity?

    @Query("SELECT * FROM drift_checks ORDER BY checkedAt DESC")
    suspend fun getAllDriftChecks(): List<DriftCheckEntity>

    @Query("DELETE FROM drift_checks WHERE checkedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

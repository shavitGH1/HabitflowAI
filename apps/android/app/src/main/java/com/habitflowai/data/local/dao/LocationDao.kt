package com.habitflowai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.local.entity.SyncStatus

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity)

    @Query("SELECT * FROM locations WHERE habitId = :habitId ORDER BY timestamp DESC")
    suspend fun getLocationsByHabitId(habitId: String): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE syncStatus != :status ORDER BY timestamp ASC")
    suspend fun getUnsyncedLocations(status: SyncStatus = SyncStatus.SYNCED): List<LocationEntity>

    @Query("UPDATE locations SET syncStatus = :status WHERE id = :locationId")
    suspend fun markSynced(locationId: String, status: SyncStatus = SyncStatus.SYNCED)

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLocation(): LocationEntity?

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    suspend fun getAllLocations(): List<LocationEntity>
}

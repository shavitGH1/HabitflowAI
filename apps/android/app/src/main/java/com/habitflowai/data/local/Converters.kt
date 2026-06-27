package com.habitflowai.data.local

import androidx.room.TypeConverter
import com.habitflowai.data.local.entity.SyncStatus

class Converters {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}

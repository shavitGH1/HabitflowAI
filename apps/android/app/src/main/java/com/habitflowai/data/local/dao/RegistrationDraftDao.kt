package com.habitflowai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.habitflowai.data.local.entity.RegistrationDraftEntity

@Dao
interface RegistrationDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(draft: RegistrationDraftEntity)

    @Query("SELECT * FROM registration_drafts WHERE email = :email")
    suspend fun getByEmail(email: String): RegistrationDraftEntity?

    @Query("DELETE FROM registration_drafts WHERE email = :email")
    suspend fun delete(email: String)
}

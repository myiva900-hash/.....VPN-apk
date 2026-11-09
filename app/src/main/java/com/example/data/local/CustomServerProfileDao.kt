package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomServerProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServerProfile(profile: CustomServerProfile): Long

    @Update
    suspend fun updateServerProfile(profile: CustomServerProfile)

    @Query("DELETE FROM custom_server_profiles WHERE id = :id")
    suspend fun deleteServerProfile(id: String)

    @Query("SELECT * FROM custom_server_profiles ORDER BY updatedAt DESC")
    fun getAllServerProfiles(): Flow<List<CustomServerProfile>>

    @Query("SELECT * FROM custom_server_profiles WHERE id = :id LIMIT 1")
    suspend fun getServerProfileById(id: String): CustomServerProfile?
}

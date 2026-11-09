package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerValidationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValidation(result: ServerValidationResult): Long

    @Query("SELECT * FROM server_validations ORDER BY timestamp DESC LIMIT 30")
    fun getAllValidations(): Flow<List<ServerValidationResult>>

    @Query("SELECT * FROM server_validations WHERE serverId = :serverId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForServer(serverId: String): ServerValidationResult?

    @Query("DELETE FROM server_validations")
    suspend fun clearAll()
}

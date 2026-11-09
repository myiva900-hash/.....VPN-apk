package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ServerValidationResult::class, CustomServerProfile::class],
    version = 2,
    exportSchema = false
)
abstract class VpnDatabase : RoomDatabase() {
    abstract fun serverValidationDao(): ServerValidationDao
    abstract fun customServerProfileDao(): CustomServerProfileDao

    companion object {
        @Volatile
        private var instance: VpnDatabase? = null

        fun getInstance(context: Context): VpnDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VpnDatabase::class.java,
                    "mutiganda_vpn_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

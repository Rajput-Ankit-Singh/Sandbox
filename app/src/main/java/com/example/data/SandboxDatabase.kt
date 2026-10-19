package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SandboxProfileEntity::class,
        InstalledSandboxAppEntity::class,
        SandboxLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SandboxDatabase : RoomDatabase() {
    abstract fun sandboxDao(): SandboxDao

    companion object {
        @Volatile
        private var INSTANCE: SandboxDatabase? = null

        fun getInstance(context: Context): SandboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SandboxDatabase::class.java,
                    "droidsandbox.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CompositionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AfterMotionDatabase : RoomDatabase() {
    abstract fun compositionDao(): CompositionDao

    companion object {
        @Volatile
        private var INSTANCE: AfterMotionDatabase? = null

        fun getInstance(context: Context): AfterMotionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AfterMotionDatabase::class.java,
                    "aftermotion_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

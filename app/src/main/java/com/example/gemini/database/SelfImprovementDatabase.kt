package com.example.gemini.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MemoryFactEntity::class,
        LearnedPatternEntity::class,
        FeedbackSignalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SelfImprovementDatabase : RoomDatabase() {

    abstract fun dao(): SelfImprovementDao

    companion object {
        @Volatile
        private var INSTANCE: SelfImprovementDatabase? = null

        fun getInstance(context: Context): SelfImprovementDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SelfImprovementDatabase::class.java,
                    "r7_self_improvement.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

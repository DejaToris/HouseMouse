package com.example.housemouse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "house_mouse_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // Migration is not covered in this scope.
                    .fallbackToDestructiveMigration() // For simplicity, otherwise implement migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
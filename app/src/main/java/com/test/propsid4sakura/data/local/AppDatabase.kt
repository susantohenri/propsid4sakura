package com.test.propsid4sakura.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PropEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propDao(): PropDao

    companion object {
        const val DATABASE_NAME = "propsid4sakura.db"
    }
}

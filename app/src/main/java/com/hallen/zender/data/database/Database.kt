package com.hallen.zender.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hallen.zender.data.database.dao.HistoryDao
import com.hallen.zender.data.database.entities.HistoryEntity

@Database(
    entities = [
        HistoryEntity::class
    ], version = 1
)
abstract class Database : RoomDatabase() {
    abstract fun getHistoryDao(): HistoryDao
}



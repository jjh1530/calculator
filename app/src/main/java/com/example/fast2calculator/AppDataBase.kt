package com.example.fast2calculator

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.fast2calculator.dao.HistoryDao
import com.example.fast2calculator.model.History

@Database(entities = [History::class],version = 1)
abstract class AppDataBase: RoomDatabase() {
    abstract fun historyDao(): HistoryDao

}
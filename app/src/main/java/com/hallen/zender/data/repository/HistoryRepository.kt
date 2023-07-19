package com.hallen.zender.data.repository

import com.hallen.zender.data.database.dao.HistoryDao
import com.hallen.zender.data.database.entities.toDataBase
import com.hallen.zender.model.History
import com.hallen.zender.model.toDomain
import javax.inject.Inject

class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    suspend fun getAllHistory(): List<History> {
        return historyDao.getAllHistory().map { it.toDomain() }
    }

    suspend fun insertHistory(history: History) {
        historyDao.insertHistory(history.toDataBase())
    }

    suspend fun deleteHistoriesById(historiesId: ArrayList<Long>) {
        historyDao.deleteHistoriesById(historiesId)
    }
}
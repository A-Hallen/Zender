package com.hallen.zender.viewmodel

import com.hallen.zender.data.repository.HistoryRepository
import com.hallen.zender.model.History
import javax.inject.Inject

class HistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend fun getAllHistory(): List<History> {
        return historyRepository.getAllHistory()
    }

    suspend fun insertHistory(history: History) {
        historyRepository.insertHistory(history)
    }

    suspend fun deleteHistoriesById(historiesId: ArrayList<Long>) {
        historyRepository.deleteHistoriesById(historiesId)
    }
}
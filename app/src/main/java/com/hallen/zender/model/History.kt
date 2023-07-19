package com.hallen.zender.model

import com.hallen.zender.data.database.entities.HistoryEntity
import java.io.File

data class History(
    val id: Long = 0,
    val date: Long = 0,
    val file: File = File(""),
    val state: Int = 0,
    val target: String = "",
    val send: Boolean = false
) {
    companion object {
        const val COMPLETED = 1
        const val ERROR = 2
        const val IN_PROGRESS = 3
        const val CANCELLED = 4
        const val WAITING = 5
        const val PENDING = 6
    }
}

fun HistoryEntity.toDomain() = History(
    id = id,
    date = date,
    file = File(path),
    state = state,
    target = target,
    send = send
)

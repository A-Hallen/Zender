package com.hallen.zender.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hallen.zender.model.History

@Entity(tableName = "history_table")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "date") var date: Long = 0,
    @ColumnInfo(name = "path") var path: String = "",
    @ColumnInfo(name = "state") var state: Int = 0,
    @ColumnInfo(name = "target") var target: String = "",
    @ColumnInfo(name = "send") var send: Boolean = false
)

fun History.toDataBase() = HistoryEntity(
    id = id,
    date = date,
    path = file.path,
    state = state,
    target = target,
    send = send
)
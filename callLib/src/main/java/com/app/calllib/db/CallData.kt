package com.app.calllib.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls_table")
data class CallData(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    var id: Int? = null,
    @ColumnInfo(name = "callerID")
    var callerID: String = "",
    @ColumnInfo(name = "name")
    var name: String = "",
    @ColumnInfo(name = "datetime")
    var datetime: String = "",
    @ColumnInfo(name = "duration")
    var duration: String = "",
    @ColumnInfo(name = "number")
    var number: String = "",
    @ColumnInfo(name = "type")
    var type: String = "",
    @ColumnInfo(name = "isSent")
    var isSent: Boolean = false,
    @ColumnInfo(name = "timeMilli")
    var timeMilli: Long = 0L,
    @ColumnInfo(name = "errorResponse")
    var errorResponse: String = ""
)

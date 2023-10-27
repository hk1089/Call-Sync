package com.app.calllib.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls_table")
data class CallData(
    @PrimaryKey(autoGenerate = false) var id: Int? = null,
    var callerID: String = "",
    var name: String = "",
    var datetime: String = "",
    var duration: String = "",
    var number: String = "",
    var type: String = "",
    var isSent: Boolean = false,
    var timeMilli: Long = 0L
)

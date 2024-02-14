package com.app.calllib.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
@kotlinx.parcelize.Parcelize
data class Calls(
    @ColumnInfo(name = "id") var id: Int? = null,
    @ColumnInfo(name = "callerID") var callerID: String = "",
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "datetime") var datetime: String = "",
    @ColumnInfo(name = "duration") var duration: String = "",
    @ColumnInfo(name = "number") var number: String = "",
    @ColumnInfo(name = "type") var type: String = "",
    @ColumnInfo(name = "isSent") var isSent: Boolean = false,
    @ColumnInfo(name = "timeMilli") var timeMilli: Long = 0L,
    @ColumnInfo(name = "errorResponse") var errorResponse: String = ""
): Parcelable

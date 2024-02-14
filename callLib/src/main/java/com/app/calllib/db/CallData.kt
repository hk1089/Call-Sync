package com.app.calllib.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
@kotlinx.parcelize.Parcelize
@Entity(tableName = "calls_table")
data class CallData(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("id") var id: Int? = null,
    @SerializedName("callerID") var callerID: String = "",
    @SerializedName("name") var name: String = "",
    @SerializedName("datetime") var datetime: String = "",
    @SerializedName("duration") var duration: String = "",
    @SerializedName("number") var number: String = "",
    @SerializedName("type") var type: String = "",
    @SerializedName("isSent") var isSent: Boolean = false,
    @SerializedName("timeMilli") var timeMilli: Long = 0L,
    @SerializedName("errorResponse") var errorResponse: String = ""
): Parcelable

package com.app.calllib

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.CallLog
import androidx.fragment.app.FragmentActivity
import com.app.calllib.db.CallData
import com.permissionx.guolindev.PermissionX
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

const val WORK_NAME = "CallFetch"
const val WORK_TAG = "PeriodicCallWork"
val sendDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)


@SuppressLint("Recycle", "Range")
fun Context.getCallLogs(temp: String, listener: (MutableList<CallData>) -> Unit) {
    val contentResolver = contentResolver
    val filter = convertReverseTime(temp).toString()
    val mSelectionClause = CallLog.Calls.DATE + " >= ?"
    Timber.d("getCallLogs>>", "CallLogIsStart>> $filter")
    val mSelectionArgs = arrayOf(filter)
    val cursor = if (filter.isNotEmpty())
        contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            mSelectionClause,
            mSelectionArgs,
            CallLog.Calls.DATE + " Desc"
        )
    else
        contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            CallLog.Calls.DATE + " Desc"
        )
    try {
        if (cursor != null) {
            val totalCalls = cursor.count
            val logList = mutableListOf<CallData>()
            if (cursor.moveToFirst()) {
                for (i in 0 until totalCalls) {
                    val callLogsData = CallData()

                    val subscriberId =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)) != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                cursor.getString(cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID))
                            } else {
                                ""
                            }
                        } else {
                            ""
                        }
                    callLogsData.callerID = if (subscriberId.isEmpty())
                        "0"
                    else
                        "-1"
                    callLogsData.name =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)) != null)
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME))
                        else
                            ""
                    callLogsData.id =
                        cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID)).toInt()

                    callLogsData.number =
                        cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                    callLogsData.datetime = sendDateFormat.format(
                        cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)).toLong()
                    )

                    callLogsData.duration =
                        cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION))

                    callLogsData.type =
                        when {
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                                .toInt() == CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                                    == CallLog.Calls.OUTGOING_TYPE.toString() -> "Outgoing"
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                                .toInt() == CallLog.Calls.MISSED_TYPE -> "Missed"
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                                .toInt() == CallLog.Calls.REJECTED_TYPE -> "Rejected"
                            else -> ""
                        }

                    if (prefStorage.selectedSim == subscriberId) {
                        logList.add(callLogsData)
                    } else if (prefStorage.selectedSim == "SINGLE_SIM") {
                        logList.add(callLogsData)
                    }
                    cursor.moveToNext()
                }
            }

            listener.invoke(logList)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun convertReverseTime(date: String): Long {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
    return formatter.parse(date)!!.time
}


fun Context.getNetworkStatus(listener: (Boolean) -> Unit) {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nw = connectivityManager.activeNetwork ?: return
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return
        when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> listener.invoke(true)
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> listener.invoke(true)
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> listener.invoke(true)
            else -> listener.invoke(false)
        }
    } else {
        @Suppress("DEPRECATION")
        connectivityManager.run {
            activeNetworkInfo?.run {
                when (type) {
                    ConnectivityManager.TYPE_WIFI -> listener.invoke(true)
                    ConnectivityManager.TYPE_MOBILE -> listener.invoke(true)
                    ConnectivityManager.TYPE_ETHERNET -> listener.invoke(true)
                    else -> listener.invoke(false)
                }
            }
        }
    }
}

fun getCurrentTime(listener: (String) -> Unit){
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
    listener.invoke(formatter.format(System.currentTimeMillis()))
}

fun FragmentActivity.permissions(
    list: List<String>,
    listener: (Boolean, List<String>, List<String>) -> Unit
) {

    PermissionX.init(this)
        .permissions(list)
        .explainReasonBeforeRequest()
        .onExplainRequestReason { scope, deniedList ->
            scope.showRequestReasonDialog(
                deniedList,
                "For Better Experience,Please Allow Required Permission",
                "OK",
            )
        }
        .onForwardToSettings { scope, deniedList ->
            scope.showForwardToSettingsDialog(
                deniedList,
                "You need to allow necessary permissions in Settings manually",
                "OK"
            )
        }
        .request { allGranted, grantedList, deniedList ->
            listener.invoke(allGranted, grantedList, deniedList)
        }
}
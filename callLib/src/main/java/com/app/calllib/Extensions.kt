package com.app.calllib

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.CallLog
import androidx.fragment.app.FragmentActivity
import com.app.calllib.db.CallData
import com.permissionx.guolindev.PermissionX
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val WORK_NAME = "CallFetch"
const val WORK_TAG = "PeriodicCallWork"
const val ONE_TIME_WORK_NAME = "oneTimeWorkNAME"
const val ONE_TIME_WORK_TAG = "oneTimeWork"
val sendDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.ENGLISH)


@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
    return (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == service.name }
}

fun isValidDate(date: String): Boolean {
    sendDateFormat.isLenient = false
    return try{
        sendDateFormat.parse(date)
        println("date is valid")
        true
    }catch(e:Exception){
        println("date is invalid")
        false
    }

}
fun getDaysAgo(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    return calendar.timeInMillis
}
@SuppressLint("Recycle", "Range")
fun Context.getCallLogs(temp: String, listener: (MutableList<CallData>) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val filter = if (temp.isNotEmpty())
        convertReverseTime(temp).toString()
    else
        getDaysAgo().toString()

    val mSelectionClause = CallLog.Calls.DATE + " >= ?"

    val mSelectionArgs = arrayOf(filter)
    val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            mSelectionClause,
            mSelectionArgs,
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
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)).replace("'", "")
                        else
                            ""
                    callLogsData.id =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID)) != null)
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID)).toInt()
                        else
                            -1

                    callLogsData.number =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER)) != null) {
                            val number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                            if (number.contains("-"))
                                number.replace("-", "")
                            if (android.util.Patterns.PHONE.matcher(number).matches())
                                number
                            else
                                ""
                        }
                        else
                            ""
                    if (callLogsData.number.isEmpty())
                        cursor.moveToNext()

                    if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)) == null)
                        cursor.moveToNext()
                    else if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE))
                            .toLong() > System.currentTimeMillis()
                    )
                        cursor.moveToNext()
                    else {
                        val callDate =
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)).toLong()
                        if (callDate < calendar.timeInMillis)
                            break
                        callLogsData.datetime = sendDateFormat.format(
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)).toLong()
                        )
                    }
                    if (!isValidDate(callLogsData.datetime))
                        cursor.moveToNext()

                    callLogsData.timeMilli = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)).toLong()

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

                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                                .toInt() == 10 -> "VoWiFi Outgoing"

                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                                .toInt() == 20 -> "VoWiFi Incoming"

                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                                .toInt() == 100 -> "VoWiFi Outgoing"

                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                                .toInt() == 101 -> "VoWiFi Incoming"

                            else -> ""
                        }
                    if (callLogsData.timeMilli > System.currentTimeMillis())
                        cursor.moveToNext()
                    if (prefStorage.selectedSim == subscriberId) {
                        logList.add(callLogsData)
                    } else {
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
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.ENGLISH)
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

fun getCurrentTime(listener: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.ENGLISH)
    listener.invoke(formatter.format(calendar.timeInMillis))
}
fun get7DaysAgo(listener: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.ENGLISH)
    listener.invoke(formatter.format(calendar.timeInMillis))
}

fun FragmentActivity.permissions(
    list: List<String>,
    listener: (Boolean, List<String>, List<String>) -> Unit
) {

    PermissionX.init(this)
        .permissions(list)
        .explainReasonBeforeRequest()
        /*.onExplainRequestReason { scope, deniedList ->
            scope.showRequestReasonDialog(
                deniedList,
                "For Better Experience,Please Allow Required Permission",
                "OK",
            )
        }*/
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

@Suppress("DEPRECATION")
fun Context.createAppPackageMediaDir(): String? {
    val directory: Array<File?> = externalMediaDirs
    for (i in directory.indices) {
        if (directory[i]!!.name.contains(packageName)) {
            return directory[i]!!.absolutePath
        }
    }
    return null
}

fun convertMillisecondsToUTC(milliseconds: Long): String {
    val date = Date(milliseconds)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
}

fun Context.getDbFolder(): File{
    val parent = createAppPackageMediaDir()
    val appFolder = File("$parent${File.separator}Call Data")
    if (!appFolder.exists())
        appFolder.mkdir()
    return appFolder
}

fun Context.getApiFolder(): File{
    val parent = createAppPackageMediaDir()
    val appFolder = File("$parent${File.separator}Api Data")
    if (!appFolder.exists())
        appFolder.mkdir()
    return appFolder
}
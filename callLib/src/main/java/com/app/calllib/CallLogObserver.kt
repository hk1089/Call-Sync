package com.app.calllib

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.CallLog
import android.util.Log
import com.app.calllib.db.CallDao
import com.app.calllib.db.CallData
import com.app.calllib.db.CallsDatabase
import com.app.calllib.modules.Scopes
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import toothpick.Toothpick
import java.util.Calendar

class CallLogObserver(
    private val context: Context,
    private val contentResolver: ContentResolver,
    handler: Handler
) :
    ContentObserver(handler) {

    companion object {
        private const val TAG = "CallLogObserver"
    }

    private val prefStorage: PrefStorage =
        Toothpick.openScope(Scopes.APP).getInstance(PrefStorage::class.java)

    @SuppressLint("Range")
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "Call log changed")
        val calendar = Calendar.getInstance()
        // Query the call log for the latest call
        try {


            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " Desc"
            )
            val logList = mutableListOf<CallData>()
            cursor?.use {
                if (it.moveToFirst()) {
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
                                .replace("'", "")
                        else
                            ""
                    callLogsData.id =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID)) != null)
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID)).toInt()
                        else
                            -1

                    callLogsData.number =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER)) != null)
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                        else
                            ""
                    if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)) == null)
                        return
                    else if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE))
                            .toLong() > System.currentTimeMillis()
                    )
                        return
                    else {
                        callLogsData.datetime = sendDateFormat.format(
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)).toLong()
                        )
                    }
                    if (!isValidDate(callLogsData.datetime))
                        return

                    callLogsData.timeMilli =
                        cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)).toLong()

                    callLogsData.duration =
                        cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION))
                    Timber.d("callType>>> ${cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
                        .toInt()}")
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
                        return
                    val db: CallDao = CallsDatabase.getInstance(context)?.callDao()!!
                    val jsonArray = JSONArray()
                    val jsObject = JSONObject()
                    jsObject.put("callerID", callLogsData.callerID)
                    jsObject.put("name", callLogsData.name)
                    jsObject.put("number", callLogsData.number)
                    jsObject.put("datetime", convertMillisecondsToUTC(callLogsData.timeMilli))
                    jsObject.put("duration", callLogsData.duration)
                    jsObject.put("type", callLogsData.type)
                    if (isValidDate(callLogsData.datetime))
                        jsonArray.put(jsObject)
                    Timber.d("calls_request>> $jsonArray")
                    val jsonObject = JSONObject()
                    jsonObject.put("isUtc", true)
                    jsonObject.put("simSlotNumber", prefStorage.simSlotIndex)
                    jsonObject.put("mobileNo", prefStorage.selectedSim)
                    jsonObject.put("aduserid", prefStorage.userId)
                    jsonObject.put("entrymode", prefStorage.entryMode)
                    jsonObject.put("callInfo", jsonArray.toString())
                    Log.d("CallLogObserver", "saved subscriberId>> ${prefStorage.selectedSim}")
                    Log.d("CallLogObserver", "subscriberId>> ${subscriberId}")
                    if (prefStorage.selectedSim == subscriberId) {
                        db.insertCalls(logList)
                        logList.add(callLogsData)
                        callLogsData.isSent = true
                        context.getNetworkStatus { connected ->
                            if (connected) {
                                ApiTask().sendCallLogs(
                                    context,
                                    com.app.calllib.prefStorage.apiHeader,
                                    jsonObject, db, logList
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
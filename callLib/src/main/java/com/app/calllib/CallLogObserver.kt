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
import java.util.concurrent.ConcurrentHashMap

class CallLogObserver(
    private val context: Context,
    private val contentResolver: ContentResolver,
    handler: Handler
) :
    ContentObserver(handler) {

    companion object {
        private const val TAG = "CallLogObserver"
        private const val DEBOUNCE_DELAY = 2000L // 2 seconds debounce
        private val processedCallIds = ConcurrentHashMap<String, Long>() // Track processed call IDs with timestamp
        private val lastChangeTime = ConcurrentHashMap<String, Long>() // Track last change time per URI
    }

    private val prefStorage: PrefStorage =
        Toothpick.openScope(Scopes.APP).getInstance(PrefStorage::class.java)

    @SuppressLint("Range")
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "Call log changed > $selfChange, URI: $uri")
        
        // Debounce mechanism - prevent multiple rapid calls
        val uriKey = uri?.toString() ?: "default"
        val currentTime = System.currentTimeMillis()
        val lastTime = if (lastChangeTime.containsKey(uriKey)) lastChangeTime[uriKey]!! else 0L
        
        if (currentTime - lastTime < DEBOUNCE_DELAY) {
            Log.d(TAG, "Debouncing call log change - too soon since last change")
            return
        }
        
        lastChangeTime[uriKey] = currentTime
        
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
                    Log.d("CallLogObserver", "COUNTRY_ISO>> ${cursor.getColumnIndex(CallLog.Calls.COUNTRY_ISO)}")
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
                        
                    // Check if this call has already been processed recently
                    val callKey = "${callLogsData.id}_${callLogsData.timeMilli}_${callLogsData.number}"
                    val lastProcessedTime = if (processedCallIds.containsKey(callKey)) processedCallIds[callKey]!! else 0L
                    if (currentTime - lastProcessedTime < DEBOUNCE_DELAY) {
                        Log.d(TAG, "Call already processed recently: $callKey")
                        return
                    }
                    
                    // Mark this call as processed
                    processedCallIds[callKey] = currentTime
                    Log.d(TAG, "Processing call: $callKey")
                    
                    // Clean up old entries (older than 1 hour)
                    val oneHourAgo = currentTime - (60 * 60 * 1000)
                    val iterator = processedCallIds.entries.iterator()
                    var removedCount = 0
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (entry.value < oneHourAgo) {
                            iterator.remove()
                            removedCount++
                        }
                    }
                    if (removedCount > 0) {
                        Log.d(TAG, "Cleaned up $removedCount old call entries")
                    }
                    
                    val db: CallDao = CallsDatabase.getInstance(context)?.callDao()!!
                    val jsonArray = JSONArray()
                    val jsObject = JSONObject()
                    Log.d("CallLogObserver", "callExistsByTimeMilli>> ${db.callExistsByTimeMilli(callLogsData.timeMilli)}")
                    if (db.callExistsByTimeMilli(callLogsData.timeMilli)) return
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
package com.app.calllib

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.CallLog
import android.util.Log
import com.app.calllib.modules.Scopes
import toothpick.Toothpick

class CallLogObserver(private val contentResolver: ContentResolver, handler: Handler) :
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

        // Query the call log for the latest call
        try {


            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " Desc"
            )

            cursor?.use {
                if (it.moveToFirst()) {
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
                    /*callLogsData.callerID = if (subscriberId.isEmpty())
                        "0"
                    else
                        "-1"*/
                    val name =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)) != null)
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME))
                                .replace("'", "")
                        else
                            ""
                    val id =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID)) != null)
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID)).toInt()
                        else
                            -1

                    val number =
                        if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER)) != null)
                            cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                        else
                            ""
                    /*if (cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE)) == null)
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
    */
                    val duration =
                        cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION))

                    val type =
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

                    Log.d(TAG, "Number: $number, Type: $type, Duration: $duration seconds")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
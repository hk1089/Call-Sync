package com.app.calllib

import android.content.Context
import android.os.Build
import androidx.work.*
import com.app.calllib.db.CallDao
import com.app.calllib.db.CallsDatabase
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class PeriodicHelper(private val context: Context) {

    fun startLog() {
        val mWorkManager = WorkManager.getInstance(context)
        val mConstraints = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Constraints.Builder()
                .setRequiresStorageNotLow(false)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
        } else {
            Constraints.Builder()
                .setRequiresStorageNotLow(false)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
        }
        val mPeriodicWorkRequest = PeriodicWorkRequest
            .Builder(PeriodicWork::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(mConstraints)
            .addTag(WORK_TAG)
            .build()

        mWorkManager
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                mPeriodicWorkRequest
            )


    }

    fun executeTask() {

        context.getCallLogs(prefStorage.lastCallLogSync) { dataList ->

            val db: CallDao = CallsDatabase.getInstance(context)?.callDao()!!
            db.insertCalls(dataList)
            val fetchList = db.gelCallsForSend(false)
            val jsonArray = JSONArray()
            fetchList.forEach { callData ->
                val jsObject = JSONObject()
                jsObject.put("callerID", callData.callerID)
                jsObject.put("name", callData.name)
                jsObject.put("number", callData.number)
                jsObject.put("datetime", callData.datetime)
                jsObject.put("duration", callData.duration)
                jsObject.put("type", callData.type)
                jsonArray.put(jsObject)
                callData.isSent = true
            }
            Timber.d("callss>> $jsonArray")
            val jsonObject = JSONObject()
            jsonObject.put("aduserid", prefStorage.userId)
            jsonObject.put("entrymode", prefStorage.entryMode)
            jsonObject.put("callInfo", jsonArray.toString())
            context.getNetworkStatus { connected ->
                if (connected) {
                    ApiTask().sendCallLogs(
                        context,
                        prefStorage.apiHeader,
                        jsonObject, db, fetchList
                    )
                }
            }
        }
    }
    fun stopLog(){
        WorkManager.getInstance(context).cancelAllWork()
        Timber.d("stopService>>>>>>> Success")
    }
}
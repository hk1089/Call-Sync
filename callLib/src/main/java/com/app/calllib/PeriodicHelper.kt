package com.app.calllib

import android.content.Context
import android.os.Build
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.app.calllib.db.CallDao
import com.app.calllib.db.CallsDatabase
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.ExecutionException
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

    fun startSingleLog() {
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
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
        val mOneTimeWorkRequest = OneTimeWorkRequest
            .Builder(OneTimeWork::class.java)
            .setInitialDelay(1L, TimeUnit.MINUTES)
            .setConstraints(mConstraints)
            .addTag(ONE_TIME_WORK_TAG)
            .build()

        mWorkManager
            .enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                mOneTimeWorkRequest
            )

    }


    fun executeTask() {

        context.getCallLogs(prefStorage.lastCallLogSync) { dataList ->
            val db: CallDao = CallsDatabase.getInstance(context)?.callDao()!!
            db.insertCalls(dataList)
            val query = SimpleSQLiteQuery("Select * from calls_table ct where isSent=? order by timeMilli desc", arrayOf(false))
            val fetchList = db.gelCallsForSend(query)
            // db.gelCallsForSend(false)
            Timber.d("callss>> ${Gson().toJson(fetchList)}")
            if(fetchList.isEmpty()) return@getCallLogs
            val jsonArray = JSONArray()
            fetchList.forEach { callData ->
                val jsObject = JSONObject()
                jsObject.put("callerID", callData.callerID)
                jsObject.put("name", callData.name)
                jsObject.put("number", callData.number)
                jsObject.put("datetime", convertMillisecondsToUTC(callData.timeMilli))
                jsObject.put("duration", callData.duration)
                jsObject.put("type", callData.type)
                if (isValidDate(callData.datetime))
                    jsonArray.put(jsObject)
                callData.isSent = true
            }
            Timber.d("calls_request>> $jsonArray")
            val jsonObject = JSONObject()
            jsonObject.put("isUtc", true)
            jsonObject.put("simSlotNumber", prefStorage.simSlotIndex)
            jsonObject.put("mobileNo", prefStorage.selectedSim)
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

    fun stopLog(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    private fun getStateOfWork(): WorkInfo.State {
        return try {
            if (WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME)
                    .get().size > 0
            ) {
                WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME)
                    .get()[0].state
            } else {
                WorkInfo.State.CANCELLED
            }
        } catch (e: ExecutionException) {
            e.printStackTrace()
            WorkInfo.State.CANCELLED
        } catch (e: InterruptedException) {
            e.printStackTrace()
            WorkInfo.State.CANCELLED
        }
    }
}
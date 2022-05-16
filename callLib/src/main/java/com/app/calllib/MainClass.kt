package com.app.calllib

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.fragment.app.FragmentActivity
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.app.calllib.modules.Scopes
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import toothpick.Toothpick
import java.util.concurrent.ExecutionException
import javax.inject.Inject


val prefStorage: PrefStorage =
    Toothpick.openScope(Scopes.APP).getInstance(PrefStorage::class.java)

class MainClass @Inject constructor(val context: Context) {

    private var periodicHelper: PeriodicHelper = PeriodicHelper(context)

    fun initializeValue(map: HashMap<String, Any>) {
        prefStorage.setCallLogsUrl = map["URL_CL"] as String
        prefStorage.apiHeader = Gson().toJson(map["headers"])
        val header = map["headers"] as HashMap<*, *>
        prefStorage.entryMode = header["entrymode"] as String
        val userId = map["aduserid"] as Int
        prefStorage.userId = userId.toString()
        val lastSync = map["LAST_LOG_TIME"] as String?
        if (prefStorage.lastCallLogSync.isEmpty()) {
            if (lastSync.isNullOrEmpty())
                getCurrentTime { prefStorage.lastCallLogSync = it }
            else
                prefStorage.lastCallLogSync = lastSync
        }
        prefStorage.selectedSim = map["isSimSlot"] as String
        doTask()

    }

    fun stopService() {
        Timber.d("stopService")
        if (getStateOfWork() == WorkInfo.State.ENQUEUED && getStateOfWork() == WorkInfo.State.RUNNING)
            periodicHelper.stopLog()

    }

    fun doTask() {
        if (context is FragmentActivity) {
            val permissionList = mutableListOf<String>()
            permissionList.add(Manifest.permission.READ_CALL_LOG)
            if (getStateOfWork() == WorkInfo.State.ENQUEUED && getStateOfWork() == WorkInfo.State.RUNNING)
                return
            checkPermission(permissionList)
        } else {
            checkPermission(mutableListOf())
        }
    }

    private fun checkPermission(permissionList: MutableList<String>) {
        (context as FragmentActivity).permissions(permissionList) { allGranted, _, deniedList ->
            if (allGranted && deniedList.isEmpty()) {
                periodicHelper.startLog()
            }
        }

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

    fun checkPermissions(listener: (Boolean) -> Unit) {
        val permissionList = mutableListOf<String>()
        permissionList.add(Manifest.permission.READ_CALL_LOG)
        (context as FragmentActivity).permissions(
            permissionList
        ) { allGranted, _, deniedList ->
            if (allGranted && deniedList.isEmpty())
                listener.invoke(true)
            else
                listener.invoke(false)

        }
    }

    fun onNotification() {
        Timber.d("WorkStatus>> ${getStateOfWork()}")
        if (getStateOfWork() != WorkInfo.State.ENQUEUED && getStateOfWork() != WorkInfo.State.RUNNING)
            periodicHelper.startLog()
    }

    @SuppressLint("MissingPermission")
    fun getSim(listener: (String) -> Unit) {
        val list = mutableListOf<String>()
        list.add(Manifest.permission.READ_PHONE_STATE)
        (context as FragmentActivity).permissions(list) { isGrant, _, denied ->
            if (isGrant && denied.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val resultList = ArrayList<Any>()
                    val jsonArray = JSONArray()
                    val subscriptionInfoList: MutableList<SubscriptionInfo> =
                        SubscriptionManager.from(context).activeSubscriptionInfoList
                    if (subscriptionInfoList.isNotEmpty()) {
                        subscriptionInfoList.forEach {
                            val resultMap = java.util.HashMap<String, String>()
                            resultMap["id"] = it.subscriptionId.toString()
                            resultMap["name"] = it.carrierName.toString()
                            val jsonObject = JSONObject()
                            jsonObject.put("id", it.subscriptionId)
                            jsonObject.put("name", it.carrierName)
                            jsonArray.put(jsonObject)
                            resultList.add(resultMap)
                            Timber.d("Info>> ${it.carrierName}, ${it.subscriptionId}")
                        }
                    }
                    listener.invoke(jsonArray.toString())
                }
            }
        }
    }
}
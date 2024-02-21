package com.app.calllib

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.app.calllib.db.CallDao
import com.app.calllib.db.CallsDatabase
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
        val isClear = map["is_cache_clear"] as Boolean
        if (isClear)
            prefStorage.clearData()

        prefStorage.setCallLogsUrl = map["URL_CL"] as String
        prefStorage.apiHeader = Gson().toJson(map["headers"])
        val header = map["headers"] as HashMap<*, *>
        prefStorage.entryMode = header["entrymode"] as String
        prefStorage.authToken = header["authkey"] as String
        val userId = map["aduserid"] as Int
        prefStorage.userId = userId.toString()
        val lastSync = map["LAST_LOG_TIME"] as String?
        val isDashboard = map["isDashboard"] as Boolean
        if (!isDashboard) {
            if (prefStorage.lastCallLogSync.isEmpty()) {
                if (lastSync.isNullOrEmpty())
                    get7DaysAgo { prefStorage.lastCallLogSync = it }
                else
                    prefStorage.lastCallLogSync = lastSync
            }
        }else{
            if (prefStorage.lastCallLogSync.isEmpty()) {
                get7DaysAgo { prefStorage.lastCallLogSync = it }
            }
        }
        prefStorage.selectedSim = map["isSimSlot"] as String
        if (!context.isServiceRunning(CallLogService::class.java)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, CallLogService::class.java))
            } else {
                context.startService(Intent(context, CallLogService::class.java))
            }
        }
       // val callLogObserver = CallLogObserver(context.contentResolver, Handler(Looper.getMainLooper()))
       // context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver)

        doTask()

    }

    fun stopService(context: Context) {
        if (getStateOfWork() != WorkInfo.State.ENQUEUED && getStateOfWork() != WorkInfo.State.RUNNING)
            doTask()
        else
            periodicHelper.stopLog(context)


    }

    fun doTask() {
       // prefStorage.lastCallLogSync = "2022-05-02 12:00:00"
        if (context is FragmentActivity) {
            val permissionList = mutableListOf<String>()
            permissionList.add(Manifest.permission.READ_CALL_LOG)
            permissionList.add(Manifest.permission.CALL_PHONE)
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
        permissionList.add(Manifest.permission.READ_PHONE_STATE)
        permissionList.add(Manifest.permission.READ_CALL_LOG)
        permissionList.add(Manifest.permission.CALL_PHONE)
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

    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

    }

    fun overlayPop(){
        AlertDialog.Builder(context)
            .setTitle("Please Enable the Draw Overlay permission")
            .setMessage("You will not receive notifications while the app is in background if you disable these permissions")
            .setPositiveButton(
                "Go to Settings"
            ) { dialog, which ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:com.elogist.dost")
                context.startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    fun sendLogs(){
        val db: CallDao = CallsDatabase.getInstance(context)?.callDao()!!
        val dataList = db.getCalls()
        val map = HashMap<String, String>()
        map["authkey"] = prefStorage.authToken
        map["entryMode"] = prefStorage.entryMode
        map["foAdminId"] = prefStorage.userId
        Log.d("MainClass", "dataList>>> ${Gson().toJson(dataList)}")
        val jsArray = JSONArray(Gson().toJson(dataList))
        Log.d("MainClass", "jsArray>>> $jsArray")
        ApiTask().sendLogs(map, jsArray)
    }
}
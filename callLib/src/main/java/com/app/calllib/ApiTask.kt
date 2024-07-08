package com.app.calllib

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.sqlite.db.SimpleSQLiteQuery
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.app.calllib.db.CallDao
import com.app.calllib.db.CallData
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Calendar


class ApiTask {

    fun sendCallLogs(context: Context, headerStr: String, jsonObject: JSONObject, db: CallDao, list: List<CallData>) {
        val prefStorage = PrefStorage(context)
        val header = Gson().fromJson(headerStr, Map::class.java)
        AndroidNetworking.post(prefStorage.setCallLogsUrl)
            .addHeaders(header)
            .addJSONObjectBody(jsonObject)
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    if (response != null && response.getBoolean("success")) {
                        db.update(list)
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        Handler(Looper.getMainLooper()).postDelayed({
                            val query = SimpleSQLiteQuery("DELETE FROM calls_table WHERE timeMilli < ? AND isSent = ?", arrayOf(calendar.timeInMillis, true))
                            db.deleteAll(query)
                        }, 10000)
                        getCurrentTime { prefStorage.lastCallLogSync = it }
                    }else{
                        list.forEach{
                            it.isSent = false
                            it.errorResponse = response.toString()
                        }
                        db.update(list)
                    }

                }

                override fun onError(anError: ANError?) {
                    anError?.printStackTrace()
                }

            })

    }

    fun sendLogs(header: Map<String, String>, jsonObject: JSONArray) {
        AndroidNetworking.post("https://xswift.biz/api//drivers/callSynchronus")
            .addHeaders(header)
            .addJSONArrayBody(jsonObject)
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    if (response != null && response.getBoolean("success")) {

                    }else{

                    }

                }

                override fun onError(anError: ANError?) {
                    anError?.printStackTrace()
                }

            })

    }
}
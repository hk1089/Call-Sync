package com.app.calllib

import android.content.Context
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.app.calllib.db.CallDao
import com.app.calllib.db.CallData
import com.google.gson.Gson
import org.json.JSONObject


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

                    }
                }

                override fun onError(anError: ANError?) {
                    anError?.printStackTrace()
                }

            })

    }
}
package com.app.calllib

import android.content.Context
import com.f2prateek.rx.preferences2.RxSharedPreferences
import javax.inject.Inject

class PrefStorage @Inject constructor(context: Context) {
    private val prefs =
        RxSharedPreferences.create(context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE))

    private val logSyncPreferences = prefs.getString("call_logs_sync", "")
    private val selectedSimPreferences = prefs.getString("sim_selection")
    private val setCallLogsPreferences = prefs.getString("set_call_logs")
    private val userIdPreferences = prefs.getString("userId")
    private val entryModePreferences = prefs.getString("entryMode")
    private val apiHeaderPreferences = prefs.getString("api_header")
    private val authTokenPreferences = prefs.getString("auth_token")


    var lastCallLogSync
        get() = logSyncPreferences.get()
        set(value) = logSyncPreferences.set(value)
    var authToken
        get() = authTokenPreferences.get()
        set(value) = authTokenPreferences.set(value)

    var selectedSim
        get() = selectedSimPreferences.get()
        set(value) = selectedSimPreferences.set(value)

    var setCallLogsUrl
        get() = setCallLogsPreferences.get()
        set(value) = setCallLogsPreferences.set(value)

    var userId
        get() = userIdPreferences.get()
        set(value) = userIdPreferences.set(value)

    var entryMode
        get() = entryModePreferences.get()
        set(value) = entryModePreferences.set(value)

    var apiHeader
        get() = apiHeaderPreferences.get()
        set(value) = apiHeaderPreferences.set(value)
}
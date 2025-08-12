package com.app.calllib

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.app.calllib.modules.Scopes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import toothpick.Toothpick
import java.util.concurrent.TimeUnit

class OneTimeWork(private val context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    private lateinit var periodicHelper: PeriodicHelper


    override fun doWork(): Result {
        periodicHelper = PeriodicHelper(context)

        val prefStorage: PrefStorage =
            Toothpick.openScope(Scopes.APP).getInstance(PrefStorage::class.java)
        periodicHelper.executeTask()
        CoroutineScope(Dispatchers.IO).launch {
            periodicHelper.startSingleLog()
        }
        return Result.success()
    }
}
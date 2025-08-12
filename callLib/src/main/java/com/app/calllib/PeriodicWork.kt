package com.app.calllib

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PeriodicWork(private val context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    private lateinit var periodicHelper: PeriodicHelper
    override fun doWork(): Result {
        return try {

            periodicHelper = PeriodicHelper(context)
            periodicHelper.executeTask()
            CoroutineScope(Dispatchers.IO).launch {
                periodicHelper.startSingleLog()
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()

        }
    }


}
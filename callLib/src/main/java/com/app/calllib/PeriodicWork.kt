package com.app.calllib

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class PeriodicWork(private val context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    private lateinit var periodicHelper: PeriodicHelper
    override fun doWork(): Result {
        return try {
            Log.d("PeriodicWork","doWork")
            periodicHelper = PeriodicHelper(context)
            periodicHelper.executeTask()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()

        }
    }


}
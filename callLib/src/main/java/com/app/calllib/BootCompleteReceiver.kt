package com.app.calllib

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class BootCompleteReceiver: BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
                context?.let { ctx ->
                    ctx.apply {
                        // Check if service is already running using singleton pattern
                        if (!CallLogService.isServiceRunning() && !isServiceRunning(CallLogService::class.java)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ContextCompat.startForegroundService(this, Intent(this, CallLogService::class.java))
                            } else {
                                startService(Intent(this, CallLogService::class.java))
                            }
                        } else {
                            Log.d("BootCompleteReceiver", "CallLogService already running, skipping start")
                        }
                    }



            }
        }
    }
}
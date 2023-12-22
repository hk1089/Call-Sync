package com.app.callsync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.app.calllib.MainClass
import com.app.callsync.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val mainClass = MainClass(this)

        binding.fab.setOnClickListener { view ->
           /* Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()*/

            mainClass.sendLogs()
        }

        binding.fabStart.setOnClickListener {view ->
            val map = HashMap<String, Any>()
            map["aduserid"] = 335
            map["isSimSlot"] = "SINGLE_SIM"
            map["URL_CL"] = "https://xswift.biz/booster_webservices/Admin/setUserCallLogs"
            map["LAST_LOG_TIME"] = ""
            map["isDashboard"] = false
            map["trackCallLog"] = true
            val headerMap = HashMap<String, Any>()
            headerMap["Content-Type"] = "application/json"
            headerMap["entrymode"] = "1"
            headerMap["authkey"] = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6MzM1LCJuYW1lIjoiSGltYW5zaHUtVGVzdCIsIm1vYmlsZW5vIjoiOTExNjY1NjU0MyIsImVtYWlsIjpudWxsLCJ0aW1lIjoiMjAyMy0xMi0wN1QxMTowNjoyMyswNTozMCIsImVudHJ5bW9kZSI6MSwiZm9pZCI6bnVsbCwidHpvbmUiOjAsInNhdmVkIjoxLCJleHAiOjE3MzM0NjMzODN9.9WSGb2ZDie4ZZujAzEfdjjk7cro1HhDjua6ulHIJGO8"
            map["headers"] = headerMap
            mainClass.initializeValue(map)
            mainClass.doTask()
        }
    }


}
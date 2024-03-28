package com.app.callsync

import android.os.Bundle
import android.widget.Toast
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
            if(android.util.Patterns.PHONE.matcher("+26-6057055113").matches())
            // using android available method of checking phone
            {
                Toast.makeText(this, "MATCH", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(this, "NO MATCH", Toast.LENGTH_LONG).show();
            }
            //mainClass.sendLogs()
        }

        binding.fabStart.setOnClickListener {view ->
            val map = HashMap<String, Any>()
            map["aduserid"] = 31288
            map["isSimSlot"] = "SINGLE_SIM"
            map["URL_CL"] = "https://xswift.biz/api//lr-module/setUserCallLogs"
            map["LAST_LOG_TIME"] = ""
            map["is_cache_clear"] = false
            map["isDashboard"] = false
            map["trackCallLog"] = true
            val headerMap = HashMap<String, Any>()
            headerMap["Content-Type"] = "application/json"
            headerMap["entrymode"] = "3"
            headerMap["authkey"] = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6NzE4NiwibmFtZSI6IkphaSBSdXBhbmEgRGhhbSBBc2hvayBSdW5ndGEiLCJtb2JpbGVubyI6OTkyODk0NDQ1NSwiZW1haWwiOiIiLCJ0aW1lIjoiMjAyNC0wMi0xNVQxNDoxNzoxMiswNTozMCIsImVudHJ5bW9kZSI6MywiZm9pZCI6NTEyLCJ0em9uZSI6LTIxMCwic2F2ZWQiOjEsImV4cCI6MTczOTUyMjgzMn0.JWsxRpkdIaUm7S0MATHgnQ3pCcc9TB-oi-fE9BTlWsc"
            map["headers"] = headerMap
            mainClass.initializeValue(map)
            mainClass.doTask()
        }
    }


}
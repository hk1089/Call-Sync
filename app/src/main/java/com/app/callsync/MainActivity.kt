package com.app.callsync

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.app.calllib.MainClass
import com.app.calllib.convertMillisecondsToUTC
import com.app.callsync.databinding.ActivityMainBinding
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import java.util.Locale


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
            //val map = mainClass.checkStorage()
         //   val utc = convertMillisecondsToUTC(System.currentTimeMillis())
            //println("Total Storage: ${map["total"]}")
           // println("Available Storage: ${map["available"]}")
          //  println("Available utc: ${utc}")
            getRegionFromPhoneNumber(binding.edtPhone.text.toString().trim(), "")
            /*val telecomManager =
                getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager*/
           // val indext = getSimSlotIndexFromAccountId("8991000905134583709F")
         //   Log.d("MainActivity", "indext>> ${indext}")
         //   Log.d("MainActivity", "isSimAvailable>> ${simAvailable()}")
         //   Log.d("MainActivity", "isSimAvailable>> ${isSimAvailable()}")
            /* Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()*/
            /*if(android.util.Patterns.PHONE.matcher("+26-6057055113").matches())
            // using android available method of checking phone
            {
                Toast.makeText(this, "MATCH", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(this, "NO MATCH", Toast.LENGTH_LONG).show();
            }*/
            //mainClass.sendLogs()
        }

        binding.fabStart.setOnClickListener { view ->
            val map = HashMap<String, Any>()
            map["sim_slot_index"] = "1"
            map["aduserid"] = 419
            map["isSimSlot"] = "89918680400238854737"
            map["URL_CL"] = "https://xswift.biz/api//lr-module/setUserCallLogs"
            map["LAST_LOG_TIME"] = ""
            map["is_cache_clear"] = false
            map["isDashboard"] = false
            map["trackCallLog"] = true
            val headerMap = HashMap<String, Any>()
            headerMap["Content-Type"] = "application/json"
            headerMap["entrymode"] = "1"
            headerMap["authkey"] =
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6NDE5LCJuYW1lIjoiQW5raXQgSmFuZ2lkIiwibW9iaWxlbm8iOiI2Mzc2NzYwMDc4IiwiZW1haWwiOm51bGwsInRpbWUiOiIyMDI0LTA0LTIzVDE3OjAwOjMzKzA1OjMwIiwiZW50cnltb2RlIjoxLCJmb2lkIjpudWxsLCJ0em9uZSI6MCwic2F2ZWQiOjEsImV4cCI6MTc0NTQwNzgzM30.T3MQpxpc94o6E-ZO_DD5HMUm7O5rZyvQuD0dBDJ2XtY"
            map["headers"] = headerMap
            mainClass.initializeValue(map)
            mainClass.doTask()
        }
    }

    fun findSlotFromSubId(sm: SubscriptionManager, subId: Int): Int {
        try {
            for (s in sm.activeSubscriptionInfoList) {
                Log.d("MainActivity", "rounding>> ${s.subscriptionId} ${s.number}")
                if (s.subscriptionId == subId) {
                    return s.simSlotIndex + 1
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return -1
    }

    @SuppressLint("MissingPermission")
    fun simAvailable(): Int {
        var count = 0
        val sManager =
            getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val infoSim1 = sManager.getActiveSubscriptionInfoForSimSlotIndex(0)
        val infoSim2 = sManager.getActiveSubscriptionInfoForSimSlotIndex(1)
        if (infoSim1 != null)
            count = count.plus(1)
        if (infoSim2 != null)
            count = count.plus(1)
        return count
    }

    @SuppressLint("MissingPermission")
    fun isSimAvailable(): Boolean {
        val sManager =
            getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val infoSim1 = sManager.getActiveSubscriptionInfoForSimSlotIndex(0)
        val infoSim2 = sManager.getActiveSubscriptionInfoForSimSlotIndex(1)
        var slot1 = -1
        var slot2 = -1
        if (infoSim1 != null) {
            slot1 = findSlotFromSubId(sManager, infoSim1.subscriptionId)
            Log.d(
                "MainActivity",
                "infoSim1 simSlotIndex>> $slot1"
            )
        }
        if (infoSim2 != null) {
            slot2 = findSlotFromSubId(sManager, infoSim2.subscriptionId)
            Log.d(
                "MainActivity",
                "infoSim2 simSlotIndex>> $slot2"
            )
        }
        getSimSlotIndexFromAccountId(slot1, slot2)
        return infoSim1 != null && infoSim2 != null
    }

    @SuppressLint("MissingPermission")
    fun getSimSlotIndexFromAccountId(slot1: Int, slot2: Int): Int {
// This is actually the official data that should be found, as on the emulator, but sadly not all phones return here a proper value
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val map = mutableMapOf<String, Int>()
        telecomManager.callCapablePhoneAccounts.forEachIndexed { index, account: PhoneAccountHandle ->
            val phoneAccount: PhoneAccount = telecomManager.getPhoneAccount(account)
            val accountId: String = phoneAccount.accountHandle.id
            if (slot1 != -1 && index == 0)
                map[accountId] = slot1
            else if (slot2 != -1)
                map[accountId] = slot2
        }
        Log.d("MainActivity", "subId>> ${map}")
        return -1
    }

    fun getRegionFromPhoneNumber(phoneNumber: String, defaultRegion: String) {
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        try {
            val parsedNumber = phoneNumberUtil.parse(phoneNumber, Locale.getDefault().country)
            Log.d("MainActivity", "countryRegion>> ${Locale.getDefault().country}")
            Log.d("MainActivity", "hasCountryCode>> ${parsedNumber.hasCountryCode()}")
            Log.d("MainActivity", "hasNationalNumber>> ${parsedNumber.hasNationalNumber()}")
            Log.d("MainActivity", "nationalNumber>> ${parsedNumber.nationalNumber}")
            Log.d("MainActivity", "hasNumberOfLeadingZeros>> ${parsedNumber.hasNumberOfLeadingZeros()}")
            Log.d("MainActivity", "hasPreferredDomesticCarrierCode>> ${parsedNumber.hasPreferredDomesticCarrierCode()}")

            val region = phoneNumberUtil.getRegionCodeForNumber(parsedNumber)
            binding.txtCode.text = "Country Code : ${region}"
        } catch (e: NumberParseException) {
            println("Error parsing number: ${e.message}")
            null
        }
    }
}
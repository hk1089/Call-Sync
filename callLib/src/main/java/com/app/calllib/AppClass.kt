package com.app.calllib

import android.app.Application
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.interceptors.HttpLoggingInterceptor
import com.app.calllib.modules.AppModule
import com.app.calllib.modules.Scopes
import timber.log.Timber
import toothpick.Toothpick

class AppClass : Application() {

    override fun onCreate() {
        super.onCreate()
        initDI()
        initLogger()
    }

    private fun initDI() {
        Toothpick.openScope(Scopes.APP).installModules(
            AppModule(
                applicationContext
            )
        )
        AndroidNetworking.initialize(applicationContext)
        AndroidNetworking.enableLogging(HttpLoggingInterceptor.Level.BODY)
    }

    private fun initLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
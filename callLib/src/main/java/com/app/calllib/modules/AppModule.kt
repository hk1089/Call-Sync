package com.app.calllib.modules

import android.content.Context
import com.app.calllib.MainClass
import com.app.calllib.PrefStorage
import toothpick.config.Module

class AppModule(context: Context): Module() {

    init {
        bind(Context::class.java).toInstance(context)
        bind(PrefStorage::class.java).toInstance(PrefStorage(context))
        bind(MainClass::class.java).singleton()
    }
}
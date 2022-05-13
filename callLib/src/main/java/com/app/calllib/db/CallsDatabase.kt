package com.app.calllib.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CallData::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CallsDatabase : RoomDatabase() {

    abstract fun callDao(): CallDao


    companion object {
        private var INSTANCE: CallsDatabase? = null

        fun getInstance(context: Context): CallsDatabase? {
            if (INSTANCE == null) {
                synchronized(CallsDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        CallsDatabase::class.java, "CallsDost.db"
                    ).allowMainThreadQueries()
                        .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
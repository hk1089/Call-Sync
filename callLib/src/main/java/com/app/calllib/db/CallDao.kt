package com.app.calllib.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CallDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCalls(list: List<CallData>)

    @Query("Select * from calls_table where isSent=:status order by timeMilli desc")
    fun gelCallsForSend(status: Boolean): List<CallData>

    @Query("Select * from calls_table order by timeMilli desc")
    fun getCalls(): List<CallData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(list: List<CallData>)

    @Query("DELETE FROM calls_table WHERE timeMilli <:time AND isSent =:status")
    fun deleteAll(time: Long, status: Boolean)
}
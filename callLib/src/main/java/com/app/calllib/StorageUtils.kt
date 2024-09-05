package com.app.calllib

import android.os.Environment
import android.os.StatFs

object StorageUtils {

    fun getTotalStorage(): Long {
        val statFs = StatFs(Environment.getExternalStorageDirectory().path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        return totalBlocks * blockSize
    }

    // Function to get available storage
    fun getAvailableStorage(): Long {
        val statFs = StatFs(Environment.getExternalStorageDirectory().path)
        val blockSize = statFs.blockSizeLong
        val availableBlocks = statFs.availableBlocksLong
        return availableBlocks * blockSize
    }

    // Function to format the size into a human-readable string
    fun formatSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var unitIndex = 0
        var sizeInDouble = size.toDouble()
        while (sizeInDouble >= 1024 && unitIndex < units.size - 1) {
            sizeInDouble /= 1024
            unitIndex++
        }
        return String.format("%.2f %s", sizeInDouble, units[unitIndex])
    }
}
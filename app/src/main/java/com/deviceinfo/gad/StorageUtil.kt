package com.deviceinfo.gad
import com.deviceinfo.gad.R

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager

object StorageUtil {
    fun getReadablePath(context: Context, uri: Uri?): String {
        if (uri == null) return "Downloads"
        val rawSegment = uri.lastPathSegment ?: return "Downloads"
        val parts = rawSegment.split(":")
        if (parts.size != 2) return rawSegment
        
        val volumeId = parts[0]
        val folder = parts[1]
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = context.getSystemService(StorageManager::class.java)
            if (sm != null) {
                val volumes = sm.storageVolumes
                for (volume in volumes) {
                    val volUuid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        volume.mediaStoreVolumeName // not always uuid
                    } else null
                    
                    val uuidMethod = try { volume.javaClass.getMethod("getUuid") } catch(e: Exception) { null }
                    val uuidStr = try { uuidMethod?.invoke(volume) as? String } catch(e: Exception) { null }

                    if ((uuidStr != null && uuidStr == volumeId) || (volumeId == "primary" && volume.isPrimary)) {
                        return "${volume.getDescription(context)} / $folder"
                    }
                }
            }
        }
        
        val internalStr = context.getString(R.string.sys_internal_storage)
        return rawSegment.replace("primary:", "$internalStr / ").replace(":", " / ")
    }
}

package com.deviceinfo.gad

import android.app.Application
import android.content.Intent

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("MyApplication", "CRITICAL CRASH: uncaught exception -> ${throwable.message}", throwable)
            try {
                val crashFile = java.io.File(filesDir, "last_crash.txt")
                val stackTrace = "Time: ${java.util.Date()}\nThread: ${thread.name}\nException: ${throwable.javaClass.name}\nMessage: ${throwable.message}\nStackTrace:\n${android.util.Log.getStackTraceString(throwable)}"
                crashFile.writeText(stackTrace)
                
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("EXTRA_STACK_TRACE", stackTrace)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(1)
            } catch (e: Exception) {
                // Ignore if we can't write the crash log or start activity
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

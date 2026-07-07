package com.deviceinfo.gad
import com.deviceinfo.gad.R

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AppInfoModel(val appName: String, val packageName: String, val versionName: String, val sizeBytes: Long, val installDate: Long, val isSystemApp: Boolean, val apkPath: String, val iconUri: String = "", val versionCode: Long = 0L, val lastUpdateDate: Long = 0L, val splitApkPaths: List<String> = emptyList())

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    
    val appList = MutableStateFlow<List<AppInfoModel>>(emptyList())
    val isLoadingApps = MutableStateFlow(false)
    val backupDestinationUri = MutableStateFlow<Uri?>(null)
    
    private var loadAppsJob: kotlinx.coroutines.Job? = null

    fun loadApps(forceRefresh: Boolean = false) {
        if (!forceRefresh && appList.value.isNotEmpty()) return
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch(Dispatchers.IO) {
            isLoadingApps.value = true
            try {
                android.util.Log.i("AppsViewModel", "Starting loadApps")
                val pm = context.packageManager
                val flags = 0
                val applications = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getInstalledApplications(flags)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppsViewModel", "Error getting installed applications: ${e.message}")
                    emptyList()
                }
                
                android.util.Log.i("AppsViewModel", "Found ${applications.size} installed applications. Fetching details...")

                var processedCount = 0
                val apps = applications
                    .mapNotNull { appInfo ->
                        processedCount++
                        if (processedCount % 20 == 0) {
                            delay(2) // Relieve binder pressure
                        }
                        
                        val pkgInfo = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pm.getPackageInfo(appInfo.packageName, PackageManager.PackageInfoFlags.of(0L))
                            } else {
                                @Suppress("DEPRECATION")
                                pm.getPackageInfo(appInfo.packageName, 0)
                            }
                        } catch(e: Exception) { null }
                        
                        val apkFile = if (appInfo.sourceDir != null) File(appInfo.sourceDir) else null
                        val splits = appInfo.splitSourceDirs?.toList() ?: emptyList()
                        var totalSize = if (apkFile?.exists() == true) apkFile.length() else 0L
                        splits.forEach { path ->
                            val sFile = File(path)
                            if (sFile.exists()) totalSize += sFile.length()
                        }
                        
                        AppInfoModel(
                            packageName = appInfo.packageName ?: "",
                            appName = try { appInfo.loadLabel(pm).toString() } catch (e: Exception) { appInfo.packageName ?: "Unknown" },
                            versionName = pkgInfo?.versionName ?: "N/A",
                            versionCode = if (pkgInfo != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkgInfo.longVersionCode 
                                else @Suppress("DEPRECATION") pkgInfo.versionCode.toLong()
                            } else 0L,
                            apkPath = appInfo.sourceDir ?: "",
                            installDate = pkgInfo?.firstInstallTime ?: 0L,
                            lastUpdateDate = pkgInfo?.lastUpdateTime ?: 0L,
                            sizeBytes = totalSize,
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            splitApkPaths = splits
                        )
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.appName.lowercase() }
                    
                android.util.Log.i("AppsViewModel", "Finished fetching details. Publishing ${apps.size} apps to UI.")

                var currentList = emptyList<AppInfoModel>()
                if (apps.isEmpty()) {
                    appList.value = emptyList()
                    isLoadingApps.value = false
                    return@launch
                }

                apps.chunked(60).forEachIndexed { index, batch ->
                    currentList = currentList + batch
                    appList.value = currentList
                    if (index == 0) {
                        isLoadingApps.value = false
                    }
                    delay(50) 
                }
                
            } catch (e: Exception) {
                appList.value = emptyList()
                isLoadingApps.value = false
            }
        }
    }

    fun backupApks(
        context: Context,
        packages: List<AppInfoModel>,
        destinationUri: Uri?,
        onProgress: (String) -> Unit,
        onDone: (successCount: Int, failCount: Int, destPath: String, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var success = 0
            var fail = 0
            var lastDestPath = ""
            var lastError: String? = null

            packages.forEach { app ->
                try {
                    val filesToBackup = mutableListOf<File>()
                    val srcFile = File(app.apkPath)
                    if (srcFile.exists()) filesToBackup.add(srcFile)
                    app.splitApkPaths.forEach { p ->
                        val f = File(p)
                        if (f.exists()) filesToBackup.add(f)
                    }
                    
                    if (filesToBackup.isEmpty()) {
                        throw java.io.FileNotFoundException("Source APK not found")
                    }

                    if (destinationUri != null) {
                        val docDir = DocumentFile.fromTreeUri(context, destinationUri)
                        filesToBackup.forEachIndexed { i, f ->
                            val suffix = if (filesToBackup.size == 1) ".apk" else if (i == 0) "_base.apk" else "_${f.nameWithoutExtension}.apk"
                            val destFile = docDir?.createFile("application/vnd.android.package-archive",
                                "${app.appName.replace(" ", "_")}_${app.versionName}$suffix")
                            destFile?.uri?.let { uri ->
                                context.contentResolver.openOutputStream(uri)?.use { out ->
                                    f.inputStream().use { it.copyTo(out) }
                                }
                            } ?: run { throw java.io.IOException("Failed to create destination file") }
                        }
                        lastDestPath = destinationUri.toString()
                        success++
                    } else {
                        val destDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        } else {
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        } ?: context.filesDir
                        
                        destDir.mkdirs()
                        filesToBackup.forEachIndexed { i, f ->
                            val suffix = if (filesToBackup.size == 1) ".apk" else if (i == 0) "_base.apk" else "_${f.nameWithoutExtension}.apk"
                            val destFile = File(destDir, "${app.appName.replace(" ", "_")}_${app.versionName}$suffix")
                            f.copyTo(destFile, overwrite = true)
                        }
                        lastDestPath = destDir.absolutePath
                        success++
                    }
                    onProgress(app.appName)
                } catch (e: Exception) {
                    fail++
                    if (e is SecurityException || e is java.io.FileNotFoundException || e.javaClass.name == "java.nio.file.AccessDeniedException") {
                        lastError = context.getString(R.string.err_backup_restricted)
                    } else {
                        lastError = e.localizedMessage ?: "Unknown error"
                    }
                }
            }
            onDone(success, fail, lastDestPath, lastError)
        }
    }
}

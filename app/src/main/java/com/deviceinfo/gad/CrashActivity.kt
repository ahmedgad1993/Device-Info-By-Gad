package com.deviceinfo.gad
import com.deviceinfo.gad.R
import androidx.compose.ui.res.stringResource

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stackTrace = intent.getStringExtra("EXTRA_STACK_TRACE") ?: "Unknown error"
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.ui_oops_something_went_), style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.ui_the_app_has_crashed_), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { sendCrashReport(stackTrace) }) {
                            Text(stringResource(R.string.ui_send_report_via_emai))
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(stringResource(R.string.ui_technical_details), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stackTrace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    private fun sendCrashReport(stackTrace: String) {
        try {
            val crashFile = File(cacheDir, "crash_log.txt")
            crashFile.writeText(stackTrace)
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", crashFile)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("wekagad1993@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Crash Log")
                putExtra(Intent.EXTRA_TEXT, "Please see the attached crash log.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Send Crash Report"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.deviceinfo.gad

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object ExportUtil {

    fun exportAsTxt(context: Context, data: String) {
        try {
            val file = File(context.cacheDir, "device_report.txt")
            FileWriter(file).use { it.write(data) }
            shareFile(context, file, "text/plain")
        } catch (e: Exception) {
            android.util.Log.e("ExportUtil", "Error exporting to TXT", e)
        }
    }

    fun exportAsPdf(context: Context, data: String) {
        try {
            val document = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
            }
            
            var y = 30f
            data.split("\n").forEach { line ->
                if (y > 810f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 30f
                }
                canvas.drawText(line, 30f, y, paint)
                y += 15f
            }
            document.finishPage(page)
            
            val file = File(context.cacheDir, "device_report.pdf")
            java.io.FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            android.util.Log.e("ExportUtil", "Error exporting to PDF", e)
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Report"))
    }
}

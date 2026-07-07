package com.deviceinfo.gad
import com.deviceinfo.gad.R
import androidx.compose.ui.res.stringResource

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

class DeviceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeviceWidget()
}

class DeviceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val stats = loadWidgetStats(context)
                WidgetContent(stats)
            }
        }
    }
}

data class WidgetStats(val battery: String, val ram: String, val storage: String)

fun loadWidgetStats(context: Context): WidgetStats {
    // Basic synchronous check since this runs on background update
    var batt = "N/A"
    try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val current = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (current > 0) batt = "$current%"
    } catch(e:Exception){}

    var ramStr = "N/A"
    try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val z = (63 - java.lang.Long.numberOfLeadingZeros(mi.availMem)) / 10
        val suffix = " KMGTPE"[z]
        val v = mi.availMem.toDouble() / (1L shl (z * 10))
        val availStr = String.format("%.1f %sB", v, suffix)
        
        val zTotal = (63 - java.lang.Long.numberOfLeadingZeros(mi.totalMem)) / 10
        val vTot = mi.totalMem.toDouble() / (1L shl (zTotal * 10))
        val totStr = String.format("%.1f %sB", vTot, " KMGTPE"[zTotal])
        
        ramStr = "$availStr / $totStr"
    } catch(e:Exception){}

    var stoStr = "N/A"
    try {
        val statFs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
        val zSto = (63 - java.lang.Long.numberOfLeadingZeros(statFs.availableBytes)) / 10
        val vSto = statFs.availableBytes.toDouble() / (1L shl (zSto * 10))
        stoStr = String.format("%.1f %sB Free", vSto, " KMGTPE"[zSto])
    } catch(e:Exception){}

    return WidgetStats(batt, ramStr, stoStr)
}

@androidx.compose.runtime.Composable
fun WidgetContent(stats: WidgetStats) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
            .clickable(actionRunCallback<RefreshAction>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Text(stringResource(R.string.ui_device_stats), style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 16.sp), modifier = GlanceModifier.padding(bottom = 8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(stringResource(R.string.ui_battery), style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Gray), fontSize = 14.sp), modifier = GlanceModifier.width(64.dp))
            Text(stats.battery, style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Green), fontSize = 14.sp, fontWeight = FontWeight.Medium))
        }
        Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(stringResource(R.string.ui_ram), style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Gray), fontSize = 14.sp), modifier = GlanceModifier.width(64.dp))
            Text(stats.ram, style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFFFA726)), fontSize = 14.sp, fontWeight = FontWeight.Medium))
        }
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(stringResource(R.string.ui_storage), style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Gray), fontSize = 14.sp), modifier = GlanceModifier.width(64.dp))
            Text(stats.storage, style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF42A5F5)), fontSize = 14.sp, fontWeight = FontWeight.Medium))
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        DeviceWidget().update(context, glanceId)
    }
}

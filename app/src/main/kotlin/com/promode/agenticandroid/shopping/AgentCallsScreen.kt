package com.promode.agenticandroid.shopping

import android.app.ActivityManager
import android.app.ApplicationStartInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Shows what happens in this app's process when an agent calls one of its functions.
 *
 * - The start records come from ActivityManager.getHistoricalProcessStartReasons(): the system's
 *   own record of why each process started and when fork, bindApplication and
 *   Application.onCreate happened. A start caused by an agent call shows reason SERVICE.
 * - The event log is written by ShoppingApplication and the app functions themselves.
 */
@Composable
fun AgentCallsScreen() {
    val context = LocalContext.current
    val log = remember { AgentCallLog.get(context) }
    val events by log.events.collectAsStateWithLifecycle()
    var heavyInit by remember { mutableStateOf(DemoSettings.isHeavyInitEnabled(context)) }
    val starts = remember(events) {
        context.getSystemService(ActivityManager::class.java).getHistoricalProcessStartReasons(3)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Force-stop the app, call a function with adb, then open this tab. Opening the " +
                    "app adds a START_ACTIVITY record on top; the SERVICE record below it is the " +
                    "cold start the agent's call caused.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Simulate a heavy Application.onCreate (+${DemoSettings.HEAVY_INIT_MS} ms)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = heavyInit, onCheckedChange = {
                    heavyInit = it
                    DemoSettings.setHeavyInitEnabled(context, it)
                })
            }
        }
        item { Text("Process starts (newest first)", style = MaterialTheme.typography.titleMedium) }
        items(starts) { StartInfoCard(it) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Event log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { log.clear() }) { Text("Clear") }
            }
        }
        items(events) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun StartInfoCard(info: ApplicationStartInfo) {
    val ts = info.startupTimestamps
    fun ms(from: Int, to: Int): String {
        val a = ts[from] ?: return "n/a"
        val b = ts[to] ?: return "n/a"
        return "%.1f ms".format((b - a) / 1_000_000.0)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "reason ${reasonName(info.reason)} · ${typeName(info.startType)} start",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("fork → bindApplication: ${ms(ApplicationStartInfo.START_TIMESTAMP_FORK, ApplicationStartInfo.START_TIMESTAMP_BIND_APPLICATION)}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text("bindApplication → Application.onCreate: ${ms(ApplicationStartInfo.START_TIMESTAMP_BIND_APPLICATION, ApplicationStartInfo.START_TIMESTAMP_APPLICATION_ONCREATE)}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text("Application.onCreate → first app function: ${ms(ApplicationStartInfo.START_TIMESTAMP_APPLICATION_ONCREATE, BaseShoppingFunctionService.START_TIMESTAMP_FIRST_APP_FUNCTION)}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text("fork → first app function: ${ms(ApplicationStartInfo.START_TIMESTAMP_FORK, BaseShoppingFunctionService.START_TIMESTAMP_FIRST_APP_FUNCTION)}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun reasonName(reason: Int): String = when (reason) {
    ApplicationStartInfo.START_REASON_SERVICE -> "SERVICE"
    ApplicationStartInfo.START_REASON_LAUNCHER -> "LAUNCHER"
    ApplicationStartInfo.START_REASON_LAUNCHER_RECENTS -> "LAUNCHER_RECENTS"
    ApplicationStartInfo.START_REASON_START_ACTIVITY -> "START_ACTIVITY"
    ApplicationStartInfo.START_REASON_BROADCAST -> "BROADCAST"
    ApplicationStartInfo.START_REASON_CONTENT_PROVIDER -> "CONTENT_PROVIDER"
    ApplicationStartInfo.START_REASON_JOB -> "JOB"
    ApplicationStartInfo.START_REASON_ALARM -> "ALARM"
    ApplicationStartInfo.START_REASON_PUSH -> "PUSH"
    ApplicationStartInfo.START_REASON_BOOT_COMPLETE -> "BOOT_COMPLETE"
    ApplicationStartInfo.START_REASON_BACKUP -> "BACKUP"
    else -> "OTHER ($reason)"
}

private fun typeName(type: Int): String = when (type) {
    ApplicationStartInfo.START_TYPE_COLD -> "cold"
    ApplicationStartInfo.START_TYPE_WARM -> "warm"
    ApplicationStartInfo.START_TYPE_HOT -> "hot"
    else -> "unset"
}

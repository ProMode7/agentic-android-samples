package com.promode.agenticandroid.shopping

import android.content.Context
import android.content.pm.PackageManager
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Who may call this app's functions, and the one switch the app itself controls.
 *
 * - The platform decides which callers get through: a caller needs EXECUTE_APP_FUNCTIONS or
 *   EXECUTE_APP_FUNCTIONS_SYSTEM. The list below shows which packages on this device hold them.
 * - The app decides whether each function is enabled. clearList ships disabled
 *   (isEnabled = false) and only this switch turns it on, through setAppFunctionEnabled.
 */
@Composable
fun AgentAccessScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { AppFunctionManager.getInstance(context) }
    var clearListEnabled by remember { mutableStateOf<Boolean?>(null) }
    val holders = remember { packagesHoldingAppFunctionPermissions(context) }

    LaunchedEffect(manager) {
        clearListEnabled = manager?.let { readClearListEnabled(it, context) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("What this app allows", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Let agents clear the whole list (clearList)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = clearListEnabled == true,
                    enabled = manager != null && clearListEnabled != null,
                    onCheckedChange = { checked ->
                        val m = manager ?: return@Switch
                        scope.launch {
                            m.setAppFunctionEnabled(
                                BaseShoppingFunctionService.CLEAR_LIST_ID,
                                if (checked) AppFunctionManager.APP_FUNCTION_STATE_ENABLED
                                else AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
                            )
                            clearListEnabled = readClearListEnabled(m, context)
                        }
                    },
                )
            }
        }
        item {
            Text(
                "Off by default. While it's off, the system refuses every clearList call before " +
                    "this app's process hears about it. While it's on, clearList still refuses " +
                    "when the phone is locked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { Text("Who can call this app on this device", style = MaterialTheme.typography.titleMedium) }
        if (holders.isEmpty()) {
            item { Text("No package holds an AppFunctions execute permission.") }
        }
        items(holders) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        item {
            Text(
                "Holding EXECUTE_APP_FUNCTIONS is not the whole check: on Android 17 the caller " +
                    "must also be on the device allowlist for this app. Holders of " +
                    "EXECUTE_APP_FUNCTIONS_SYSTEM skip the allowlist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private suspend fun readClearListEnabled(manager: AppFunctionManager, context: Context): Boolean? =
    runCatching {
        manager.getAppFunctionStates(
            listOf(AppFunctionName(context.packageName, BaseShoppingFunctionService.CLEAR_LIST_ID))
        ).singleOrNull()?.isEnabled
    }.getOrNull()

/** Packages that hold either execute permission, as "package  PERMISSION". Demo only. */
private fun packagesHoldingAppFunctionPermissions(context: Context): List<String> {
    val permissions = arrayOf(
        "android.permission.EXECUTE_APP_FUNCTIONS",
        "android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM",
    )
    return context.packageManager
        .getPackagesHoldingPermissions(permissions, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        .flatMap { info ->
            val requested = info.requestedPermissions.orEmpty()
            val flags = info.requestedPermissionsFlags ?: IntArray(0)
            requested.indices
                .filter { requested[it] in permissions && flags[it] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }
                .map { "${info.packageName}  ${requested[it].substringAfterLast('.')}" }
        }
        .sorted()
}

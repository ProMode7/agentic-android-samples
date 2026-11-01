package com.promode.agenticandroid.shopping

import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Runs the same search an agent runs, scoped to this app's own package.
 *
 * An app can always search its own functions without any permission, so this screen shows
 * exactly what the system indexed from the APK: function IDs, the KDoc-derived descriptions,
 * and the parameters. Nothing here calls into BaseShoppingFunctionService; the results come
 * from the AppSearch index the system built at install time.
 */
@Composable
fun AgentViewScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<AppFunctionMetadata>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "This runs searchAppFunctions() for this app's package, the same query an agent " +
                    "runs. The results come from the system's index, not from this app's code.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            Button(onClick = {
                scope.launch {
                    error = null
                    val manager = AppFunctionManager.getInstance(context)
                    if (manager == null) {
                        error = "AppFunctions isn't available on this device."
                        return@launch
                    }
                    results = runCatching {
                        manager.searchAppFunctions(
                            AppFunctionSearchSpec(packageNames = setOf(context.packageName))
                        )
                    }.onFailure { error = it.toString() }.getOrNull()
                }
            }) { Text("Search my app functions") }
        }
        error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        results?.let { list ->
            item { Text("${list.size} function(s) indexed", style = MaterialTheme.typography.titleMedium) }
            items(list, key = { it.id }) { FunctionCard(it) }
        }
    }
}

@Composable
private fun FunctionCard(metadata: AppFunctionMetadata) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(metadata.id, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text(
                metadata.description.ifEmpty { "(no description: isDescribedByKDoc is false)" },
                style = MaterialTheme.typography.bodyLarge,
            )
            metadata.parameters.forEach { param ->
                Text(
                    "param ${param.name}${if (param.isRequired) " (required)" else ""}" +
                        param.description.takeIf { it.isNotEmpty() }?.let { ": $it" }.orEmpty(),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            metadata.response.description.takeIf { it.isNotEmpty() }?.let {
                Text("returns: $it", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

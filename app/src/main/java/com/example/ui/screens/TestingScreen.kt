package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.InstalledSandboxAppEntity
import com.example.data.SandboxLogEntity
import com.example.engine.SandboxExecutionResult
import com.example.ui.components.AppMonogramIcon
import com.example.ui.components.MetricBadgeCard
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TestingScreen(
    installedApps: List<InstalledSandboxAppEntity>,
    selectedAppId: Long?,
    lastExecutionResult: SandboxExecutionResult?,
    isRunningContainer: Boolean,
    isolatedFiles: List<Pair<String, Long>>,
    isolatedPrefs: Map<String, String>,
    logs: List<SandboxLogEntity>,
    onSelectApp: (InstalledSandboxAppEntity) -> Unit,
    onRunContainer: (InstalledSandboxAppEntity, String?, String?, Boolean) -> Unit,
    onClearAppData: (InstalledSandboxAppEntity) -> Unit,
    onClearProfileLogs: () -> Unit,
    onNavigateToImport: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val selectedApp = remember(installedApps, selectedAppId) {
        installedApps.find { it.id == selectedAppId } ?: installedApps.firstOrNull()
    }

    var customPrefKey by rememberSaveable { mutableStateOf("qa_env_flag") }
    var customPrefVal by rememberSaveable { mutableStateOf("staging_isolated_true") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("testing_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (installedApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Sandbox App Selected for Testing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Install an APK into this profile first to run its isolated container, benchmark startup latency & RAM usage, and inspect logs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateToImport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("testing_empty_import_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go to Import APK")
                        }
                    }
                }
            }
        } else {
            // App selector strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target Sandbox Package (Profile: ${selectedApp?.profileId ?: ""})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        installedApps.forEach { app ->
                            val selected = app.id == selectedApp?.id
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectApp(app) },
                                label = {
                                    Text("${app.appName} v${app.versionName} [${app.profileId.removePrefix("profile_")}]")
                                },
                                modifier = Modifier.testTag("testing_app_chip_${app.id}")
                            )
                        }
                    }
                }
            }

            selectedApp?.let { app ->
                // Interactive Sandbox Execution Console Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sandbox_runner_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AppMonogramIcon(appName = app.appName, behaviorType = app.sampleBehaviorType)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${app.appName} • v${app.versionName} (${app.versionCode})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Entry: ${app.entryClassName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Container: /files/sandbox/profiles/${app.profileId}/apps/${app.packageName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isRunningContainer) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            // Action Toolbar: Launch, Inject Crash Fault, Clear Data
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRunContainer(app, null, null, false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("run_sandbox_container_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Launch")
                                }

                                FilledTonalButton(
                                    onClick = { onRunContainer(app, null, null, true) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("inject_crash_fault_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Crash")
                                }
                            }

                            OutlinedButton(
                                onClick = { onClearAppData(app) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("clear_isolated_data_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset Isolated App Data (${formatBytes(app.dataSizeBytes)})")
                            }
                        }
                    }
                }

                // Real-time Measured Performance Benchmark Strip
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Measured Runtime Telemetry (Actual Host Device Probe)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricBadgeCard(
                                label = "Startup Latency",
                                value = "${lastExecutionResult?.startupTimeMs ?: app.lastStartupMs} ms",
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBadgeCard(
                                label = "CPU Thread Time",
                                value = "${lastExecutionResult?.cpuTimeMs ?: app.lastCpuTimeMs} ms",
                                accentColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricBadgeCard(
                                label = "Heap Delta",
                                value = "+${lastExecutionResult?.ramDeltaKb ?: app.lastRamDeltaKb} KB",
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBadgeCard(
                                label = "Isolated Storage",
                                value = formatBytes(app.dataSizeBytes),
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Live Container Console Output & Trapped Stack Trace
                if (lastExecutionResult != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("execution_console_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (lastExecutionResult.success) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (lastExecutionResult.success) {
                                        "Container Execution Verified (Process PSS: ${lastExecutionResult.totalProcessPssKb / 1024} MB)"
                                    } else {
                                        "Sandbox Crash Trapped Safely (Host Unharmed)"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (lastExecutionResult.success) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.error
                                )
                                lastExecutionResult.consoleOutputLines.forEach { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (!lastExecutionResult.stackTrace.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = lastExecutionResult.stackTrace.lines().take(8).joinToString("\n"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Isolated State & SharedPreferences Inspector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Isolated Profile State (${app.profileId})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Write a custom key-value preference into this profile's isolated container to verify that cloned profiles or host storage remain completely untouched.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customPrefKey,
                                    onValueChange = { customPrefKey = it },
                                    label = { Text("Key") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("isolated_pref_key_input")
                                )
                                OutlinedTextField(
                                    value = customPrefVal,
                                    onValueChange = { customPrefVal = it },
                                    label = { Text("Value") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("isolated_pref_val_input")
                                )
                            }
                            OutlinedButton(
                                onClick = { onRunContainer(app, customPrefKey, customPrefVal, false) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("write_isolated_pref_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Write Key-Value & Execute in Sandbox")
                            }

                            // Current Isolated Preferences Map
                            if (isolatedPrefs.isNotEmpty()) {
                                Text(
                                    text = "Isolated SharedPreferences (${isolatedPrefs.size} entries):",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        isolatedPrefs.forEach { (k, v) ->
                                            Text(
                                                text = "$k = $v",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // Current Isolated Files List
                            if (isolatedFiles.isNotEmpty()) {
                                Text(
                                    text = "Isolated Container Files (${isolatedFiles.size} files):",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                isolatedFiles.forEach { (path, size) ->
                                    Text(
                                        text = "• /data/files/$path (${formatBytes(size)})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Persistent Sandbox Logs & Crash History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sandbox Logcat & Crash History (${logs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (logs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearProfileLogs,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clear_logs_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs")
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Text(
                    text = "No runtime or installation events logged in this profile yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logs, key = { it.id }) { logEntry ->
                SandboxLogItemCard(logEntry)
            }
        }
    }
}

@Composable
private fun SandboxLogItemCard(log: SandboxLogEntity) {
    val timeStr = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(log.timestamp))
    }
    val badgeColor = when (log.level) {
        "CRASH", "ERROR" -> MaterialTheme.colorScheme.error
        "WARN" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.level,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = log.tag,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!log.stackTrace.isNullOrBlank()) {
                Text(
                    text = log.stackTrace.lines().take(4).joinToString("\n"),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

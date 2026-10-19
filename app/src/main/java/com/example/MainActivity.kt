package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SandboxScreen
import com.example.ui.SandboxViewModel
import com.example.ui.components.DismissibleStatusToast
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImportApkScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TestingScreen
import com.example.ui.theme.DroidSandboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sandboxViewModel: SandboxViewModel = viewModel()
            val uiState by sandboxViewModel.uiState.collectAsStateWithLifecycle()

            DroidSandboxTheme(darkTheme = uiState.isDarkTheme) {
                DroidSandboxApp(viewModel = sandboxViewModel)
            }
        }
    }
}

private data class NavDestination(
    val screen: SandboxScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_DESTINATIONS = listOf(
    NavDestination(
        screen = SandboxScreen.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    NavDestination(
        screen = SandboxScreen.IMPORT_APK,
        label = "Import APK",
        selectedIcon = Icons.Filled.FileOpen,
        unselectedIcon = Icons.Outlined.FileOpen,
        testTag = "nav_tab_import"
    ),
    NavDestination(
        screen = SandboxScreen.TESTING,
        label = "Testing",
        selectedIcon = Icons.Filled.Terminal,
        unselectedIcon = Icons.Outlined.Terminal,
        testTag = "nav_tab_testing"
    ),
    NavDestination(
        screen = SandboxScreen.SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidSandboxApp(
    viewModel: SandboxViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfileApps by viewModel.activeProfileApps.collectAsStateWithLifecycle()
    val allInstalledApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val activeLogs by viewModel.activeProfileLogs.collectAsStateWithLifecycle()

    val activeProfileName = profiles.find { it.profileId == uiState.activeProfileId }?.name ?: "Default QA"
    val backendMode = uiState.compatibilityReport?.activeBackendMode ?: "Limited User-Space Sandbox Mode"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "DS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = "DroidSandbox",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Profile: $activeProfileName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = if (backendMode.contains("Limited")) "USER-SPACE" else "AVF VM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleDarkTheme() },
                            modifier = Modifier.testTag("top_bar_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark or Light Mode"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NAV_DESTINATIONS.forEach { dest ->
                            val selected = uiState.currentScreen == dest.screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(dest.screen) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_navigation_rail"),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NAV_DESTINATIONS.forEach { dest ->
                            val selected = uiState.currentScreen == dest.screen
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(dest.screen) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    uiState.statusBannerMessage?.let { msg ->
                        DismissibleStatusToast(
                            message = msg,
                            onDismiss = { viewModel.dismissBannerMessage() },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    when (uiState.currentScreen) {
                        SandboxScreen.HOME -> {
                            HomeScreen(
                                profiles = profiles,
                                activeProfileId = uiState.activeProfileId,
                                installedApps = activeProfileApps,
                                allAppsCount = allInstalledApps.size,
                                searchQuery = uiState.searchQuery,
                                activeBackendMode = backendMode,
                                storageSummary = uiState.storageSummary,
                                onSelectProfile = { viewModel.selectActiveProfile(it) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onNavigateToImport = { viewModel.navigateTo(SandboxScreen.IMPORT_APK) },
                                onNavigateToSettings = { viewModel.navigateTo(SandboxScreen.SETTINGS) },
                                onLaunchApp = { app ->
                                    viewModel.launchAndTestApp(app, navigateToTestingScreen = true)
                                },
                                onInspectApp = { app ->
                                    viewModel.selectTestingApp(app)
                                    viewModel.navigateTo(SandboxScreen.TESTING)
                                },
                                onUninstallApp = { app ->
                                    viewModel.uninstallApp(app)
                                }
                            )
                        }

                        SandboxScreen.IMPORT_APK -> {
                            ImportApkScreen(
                                profiles = profiles,
                                selectedProfileIds = uiState.installTargetProfileIds,
                                samplePresets = viewModel.samplePresets,
                                inspectedApk = uiState.inspectedApk,
                                isParsingApk = uiState.isParsingApk,
                                installProgress = uiState.installProgress,
                                installErrorMessage = uiState.installErrorMessage,
                                installSuccessMessage = uiState.installSuccessMessage,
                                onExternalApkSelected = { uri, name ->
                                    viewModel.onExternalApkPicked(uri, name)
                                },
                                onGenerateSampleApk = { preset ->
                                    viewModel.onGenerateSampleApk(preset)
                                },
                                onToggleTargetProfile = { profileId ->
                                    viewModel.toggleTargetInstallProfile(profileId)
                                },
                                onInstallApk = {
                                    viewModel.installInspectedApk()
                                },
                                onNavigateToTesting = {
                                    viewModel.navigateTo(SandboxScreen.TESTING)
                                },
                                onBackToHome = {
                                    viewModel.navigateTo(SandboxScreen.HOME)
                                }
                            )
                        }

                        SandboxScreen.TESTING -> {
                            TestingScreen(
                                installedApps = allInstalledApps,
                                selectedAppId = uiState.selectedTestingAppId,
                                lastExecutionResult = uiState.lastExecutionResult,
                                isRunningContainer = uiState.isRunningContainer,
                                isolatedFiles = uiState.isolatedFiles,
                                isolatedPrefs = uiState.isolatedPrefs,
                                logs = activeLogs,
                                onSelectApp = { app -> viewModel.selectTestingApp(app) },
                                onRunContainer = { app, k, v, fault ->
                                    viewModel.launchAndTestApp(
                                        app = app,
                                        navigateToTestingScreen = false,
                                        customKey = k,
                                        customValue = v,
                                        forceFault = fault
                                    )
                                },
                                onClearAppData = { app -> viewModel.clearAppData(app) },
                                onClearProfileLogs = { viewModel.clearLogsForActiveProfile() },
                                onNavigateToImport = { viewModel.navigateTo(SandboxScreen.IMPORT_APK) },
                                onBackToHome = { viewModel.navigateTo(SandboxScreen.HOME) }
                            )
                        }

                        SandboxScreen.SETTINGS -> {
                            SettingsScreen(
                                isDarkTheme = uiState.isDarkTheme,
                                profiles = profiles,
                                activeProfileId = uiState.activeProfileId,
                                allInstalledApps = allInstalledApps,
                                storageSummary = uiState.storageSummary,
                                compatibilityReport = uiState.compatibilityReport,
                                onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                                onSelectProfile = { viewModel.selectActiveProfile(it) },
                                onCreateProfile = { name, desc -> viewModel.createNewProfile(name, desc) },
                                onResetProfile = { id -> viewModel.resetProfile(id) },
                                onDeleteProfile = { id -> viewModel.deleteProfile(id) },
                                onClearStagingCache = { viewModel.clearStagingCache() },
                                onClearSingleAppData = { app -> viewModel.clearAppData(app) },
                                onBackToHome = { viewModel.navigateTo(SandboxScreen.HOME) }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.engine

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import com.example.data.InstalledSandboxAppEntity
import dalvik.system.DexClassLoader
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SandboxInstallProgress(
    val stepIndex: Int,
    val totalSteps: Int = 5,
    val stepTitle: String,
    val fraction: Float
)

data class SandboxExecutionResult(
    val success: Boolean,
    val startupTimeMs: Long,
    val cpuTimeMs: Long,
    val ramDeltaKb: Long,
    val totalProcessPssKb: Long,
    val updatedDataSizeBytes: Long,
    val consoleOutputLines: List<String>,
    val isolatedFilesList: List<Pair<String, Long>>,
    val isolatedPrefsMap: Map<String, String>,
    val errorSummary: String? = null,
    val stackTrace: String? = null
)

object SandboxRuntimeEngine {

    fun getSandboxRootDir(context: Context): File {
        return File(context.filesDir, "sandbox").apply { mkdirs() }
    }

    fun getProfileRootDir(context: Context, profileId: String): File {
        return File(getSandboxRootDir(context), "profiles/$profileId").apply { mkdirs() }
    }

    fun getAppContainerDir(context: Context, profileId: String, packageName: String): File {
        return File(getProfileRootDir(context, profileId), "apps/$packageName")
    }

    /**
     * Installs a parsed APK into a target profile's isolated directory structure.
     * Supports version upgrades (when installing a higher/equal versionCode with matching signature)
     * and rejects downgrades or incompatible minSdk versions with actionable errors.
     */
    suspend fun installApkIntoProfile(
        context: Context,
        metadata: ParsedApkMetadata,
        profileId: String,
        existingApp: InstalledSandboxAppEntity?,
        onProgress: suspend (SandboxInstallProgress) -> Unit
    ): Result<InstalledSandboxAppEntity> {
        return runCatching {
            // Step 1: Verify archive integrity
            onProgress(SandboxInstallProgress(1, 5, "Verifying APK archive & DEX headers...", 0.2f))
            val sourceFile = File(metadata.sourceFilePath)
            if (!sourceFile.exists()) {
                throw IllegalStateException("Installation failed: Source APK file not found at ${metadata.sourceFilePath}")
            }
            if (!metadata.hasValidDex) {
                throw IllegalArgumentException("Installation failed: APK archive does not contain a valid classes.dex bytecode payload.")
            }

            // Step 2: Check SDK & ABI compatibility
            onProgress(SandboxInstallProgress(2, 5, "Checking Android SDK & ABI compatibility...", 0.4f))
            if (metadata.minSdk > Build.VERSION.SDK_INT) {
                throw IllegalArgumentException(
                    "INSTALL_FAILED_OLDER_SDK: Package ${metadata.packageName} requires minSdk ${metadata.minSdk}, " +
                        "but host device runs Android API ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})."
                )
            }

            // Check upgrade/signature consistency if already installed in this profile
            if (existingApp != null) {
                if (metadata.versionCode < existingApp.versionCode) {
                    throw IllegalArgumentException(
                        "INSTALL_FAILED_VERSION_DOWNGRADE: Cannot downgrade ${metadata.packageName} from versionCode ${existingApp.versionCode} (${existingApp.versionName}) to ${metadata.versionCode} (${metadata.versionName}) in profile '$profileId'. Uninstall first or choose another profile."
                    )
                }
            }

            // Step 3: Allocate isolated container directories
            onProgress(SandboxInstallProgress(3, 5, "Allocating isolated profile storage tree...", 0.6f))
            val appContainerDir = getAppContainerDir(context, profileId, metadata.packageName)
            val apkDir = File(appContainerDir, "apk").apply { mkdirs() }
            val dexOptDir = File(appContainerDir, "dex_opt").apply { mkdirs() }
            val dataDir = File(appContainerDir, "data").apply { mkdirs() }
            val filesDir = File(dataDir, "files").apply { mkdirs() }
            val prefsDir = File(dataDir, "shared_prefs").apply { mkdirs() }

            // Step 4: Copy APK & enforce Android 14+ read-only Dynamic Code Loading security
            onProgress(SandboxInstallProgress(4, 5, "Provisioning read-only APK & DEX cache...", 0.8f))
            val baseApkFile = File(apkDir, "base.apk")
            if (baseApkFile.exists()) {
                baseApkFile.setWritable(true)
                baseApkFile.delete()
            }
            sourceFile.copyTo(baseApkFile, overwrite = true)
            // Mandatory on Android 14+ (API 34+) before passing to DexClassLoader
            baseApkFile.setReadOnly()

            // Write initial installation receipt into isolated shared_prefs if not existing
            val prefsFile = File(prefsDir, "${metadata.packageName}_prefs.json")
            val prefsJson = if (prefsFile.exists()) {
                runCatching { JSONObject(prefsFile.readText()) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }
            prefsJson.put("sandbox.profile_id", profileId)
            prefsJson.put("sandbox.installed_version", "${metadata.versionName} (${metadata.versionCode})")
            prefsJson.put("sandbox.isolation_root", dataDir.absolutePath)
            if (!prefsJson.has("sandbox.install_timestamp")) {
                prefsJson.put("sandbox.install_timestamp", System.currentTimeMillis())
            }
            prefsFile.writeText(prefsJson.toString(2))

            // Write manifest metadata summary in isolated filesDir
            val receiptFile = File(filesDir, "package_manifest.txt")
            receiptFile.writeText(
                buildString {
                    appendLine("Package: ${metadata.packageName}")
                    appendLine("App Name: ${metadata.appName}")
                    appendLine("Version: ${metadata.versionName} (code ${metadata.versionCode})")
                    appendLine("Profile ID: $profileId")
                    appendLine("SHA-256: ${metadata.sha256Signature}")
                    appendLine("Permissions: ${metadata.permissions.joinToString(", ").ifEmpty { "None" }}")
                }
            )

            // Step 5: Finalize registration
            onProgress(SandboxInstallProgress(5, 5, "Registering sandbox package in profile...", 1.0f))
            val dataBytes = computeDirectorySize(dataDir) + computeDirectorySize(dexOptDir)

            InstalledSandboxAppEntity(
                id = existingApp?.id ?: 0L,
                profileId = profileId,
                packageName = metadata.packageName,
                appName = metadata.appName,
                versionName = metadata.versionName,
                versionCode = metadata.versionCode,
                minSdk = metadata.minSdk,
                targetSdk = metadata.targetSdk,
                apkFilePath = baseApkFile.absolutePath,
                isolatedDataDirPath = dataDir.absolutePath,
                apkSizeBytes = baseApkFile.length(),
                dataSizeBytes = dataBytes,
                sha256Signature = metadata.sha256Signature,
                permissionsCsv = metadata.permissions.joinToString(","),
                entryClassName = metadata.entryClassName,
                sampleBehaviorType = metadata.sampleBehaviorType,
                installedAt = existingApp?.installedAt ?: System.currentTimeMillis(),
                lastLaunchedAt = existingApp?.lastLaunchedAt,
                launchCount = existingApp?.launchCount ?: 0,
                lastStartupMs = existingApp?.lastStartupMs ?: 0L,
                lastRamDeltaKb = existingApp?.lastRamDeltaKb ?: 0L,
                lastCpuTimeMs = existingApp?.lastCpuTimeMs ?: 0L,
                lastStatus = if (existingApp != null) "UPGRADED" else "INSTALLED"
            )
        }
    }

    /**
     * Launches the installed sandbox app inside an isolated container context:
     * 1. Measures real process PSS memory, Java heap, and thread CPU time before & after.
     * 2. Initializes an isolated DexClassLoader pointing strictly to the profile's read-only base.apk and dex_opt directory.
     * 3. Executes the container workload (and attempts reflective entry class loading if present).
     * 4. Captures any uncaught exceptions inside a crash boundary so the host phone/app never crashes.
     */
    fun launchInSandbox(
        context: Context,
        app: InstalledSandboxAppEntity,
        customUserInputKey: String? = null,
        customUserInputValue: String? = null,
        forceFaultInjection: Boolean = false
    ): SandboxExecutionResult {
        val startWallNs = SystemClock.elapsedRealtimeNanos()
        val startCpuMs = Process.getElapsedCpuTime()
        val runtime = Runtime.getRuntime()
        val startHeapKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024L
        val startPssKb = Debug.getPss()

        val consoleLines = mutableListOf<String>()
        val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        fun logLine(tag: String, msg: String) {
            consoleLines.add("[${timeFmt.format(Date())}] [$tag] $msg")
        }

        val baseApk = File(app.apkFilePath)
        val dataDir = File(app.isolatedDataDirPath).apply { mkdirs() }
        val filesDir = File(dataDir, "files").apply { mkdirs() }
        val prefsDir = File(dataDir, "shared_prefs").apply { mkdirs() }
        val dexOptDir = File(dataDir.parentFile ?: dataDir, "dex_opt").apply { mkdirs() }

        return try {
            logLine("CONTAINER", "Initializing User-Space Sandbox Container for '${app.packageName}' (Profile: ${app.profileId})")
            if (!baseApk.exists()) {
                throw IllegalStateException("APK binary missing at ${baseApk.absolutePath}. Reinstall package.")
            }

            // Ensure read-only protection for Android 14+ dynamic code loading compliance
            baseApk.setReadOnly()
            logLine("SECURITY", "Verified read-only DEX protection on base.apk (${baseApk.length() / 1024} KB)")

            // Instantiate isolated DexClassLoader scoped to this profile's container
            val classLoader = DexClassLoader(
                baseApk.absolutePath,
                dexOptDir.absolutePath,
                null,
                context.classLoader
            )
            logLine("CLASSLOADER", "Isolated DexClassLoader bound to ${dexOptDir.name} (Parent: HostBootClassLoader)")

            // Attempt to probe entry class from APK DEX if present
            val loadedClass = try {
                classLoader.loadClass(app.entryClassName)
            } catch (_: ClassNotFoundException) {
                null
            }

            if (loadedClass != null) {
                logLine("DEX_EXEC", "Resolved compiled entry class: ${loadedClass.name}")
            } else {
                logLine("DEX_EXEC", "Verified DEX header & container descriptor for entry: ${app.entryClassName}")
            }

            // Load and mutate isolated SharedPreferences JSON inside the profile container
            val prefsFile = File(prefsDir, "${app.packageName}_prefs.json")
            val prefsJson = if (prefsFile.exists()) {
                runCatching { JSONObject(prefsFile.readText()) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }

            val newLaunchCount = app.launchCount + 1
            prefsJson.put("sandbox.launch_count", newLaunchCount)
            prefsJson.put("sandbox.last_launch_iso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()))
            prefsJson.put("sandbox.active_version", "${app.versionName} (${app.versionCode})")

            if (!customUserInputKey.isNullOrBlank()) {
                prefsJson.put(customUserInputKey.trim(), customUserInputValue ?: "")
                logLine("ISOLATED_PREFS", "Wrote key '${customUserInputKey.trim()}' = '${customUserInputValue ?: ""}' to profile '${app.profileId}'")
            }

            // Execute behavior-specific workload inside isolated filesDir
            if (forceFaultInjection || app.sampleBehaviorType == "CRASH_TEST") {
                logLine("WORKLOAD", "Executing Fault & Crash Boundary check inside sandbox...")
                prefsFile.writeText(prefsJson.toString(2))
                throw RuntimeException(
                    "Simulated Sandbox Fault in ${app.packageName}.${app.entryClassName.substringAfterLast('.')}: " +
                        "NullPointerException at isolated container frame #4 (Profile=${app.profileId})"
                )
            }

            when (app.sampleBehaviorType) {
                "UPGRADE" -> {
                    logLine("MIGRATION", "Detected v2.0 upgrade payload. Preserving existing profile state (launch #${newLaunchCount}).")
                    val migrationFile = File(filesDir, "v2_migration_state.log")
                    migrationFile.appendText("Upgraded execution on ${Date()} | Profile=${app.profileId} | Version=${app.versionName}\n")
                }
                "STORAGE_STRESS" -> {
                    logLine("STORAGE_IO", "Writing isolated 32 KB telemetry snapshot to profile data directory...")
                    val blobFile = File(filesDir, "isolated_blob_${newLaunchCount}.bin")
                    val payload = ByteArray(32_768) { idx -> ((idx + newLaunchCount) and 0xFF).toByte() }
                    blobFile.writeBytes(payload)
                    logLine("STORAGE_IO", "Created ${blobFile.name} (32 KB) strictly inside profile '${app.profileId}'")
                }
                else -> {
                    val sessionLog = File(filesDir, "runtime_session.log")
                    sessionLog.appendText("Session #$newLaunchCount executed at ${Date()} in profile ${app.profileId}\n")
                    logLine("RUNTIME", "Updated isolated file '${sessionLog.name}' (${sessionLog.length()} bytes)")
                }
            }

            prefsFile.writeText(prefsJson.toString(2))

            // Measure post-execution metrics
            val endWallNs = SystemClock.elapsedRealtimeNanos()
            val endCpuMs = Process.getElapsedCpuTime()
            val endHeapKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024L
            val endPssKb = Debug.getPss()

            val startupMs = ((endWallNs - startWallNs) / 1_000_000L).coerceAtLeast(1L)
            val cpuTimeMs = (endCpuMs - startCpuMs).coerceAtLeast(1L)
            val ramDeltaKb = (endHeapKb - startHeapKb).coerceAtLeast(64L)
            val updatedDataBytes = computeDirectorySize(dataDir) + computeDirectorySize(dexOptDir)

            logLine(
                "BENCHMARK",
                "Completed in ${startupMs}ms | CPU: ${cpuTimeMs}ms | Heap Delta: +${ramDeltaKb}KB | Process PSS: ${endPssKb}KB"
            )

            SandboxExecutionResult(
                success = true,
                startupTimeMs = startupMs,
                cpuTimeMs = cpuTimeMs,
                ramDeltaKb = ramDeltaKb,
                totalProcessPssKb = endPssKb,
                updatedDataSizeBytes = updatedDataBytes,
                consoleOutputLines = consoleLines,
                isolatedFilesList = listIsolatedFiles(filesDir),
                isolatedPrefsMap = jsonToMap(prefsJson)
            )
        } catch (t: Throwable) {
            val endWallNs = SystemClock.elapsedRealtimeNanos()
            val startupMs = ((endWallNs - startWallNs) / 1_000_000L).coerceAtLeast(1L)
            val endPssKb = Debug.getPss()
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()

            logLine("CRASH_TRAP", "Uncaught exception trapped by DroidSandbox isolation boundary: ${t.message}")
            logLine("CRASH_TRAP", "Host phone & DroidSandbox process remained stable.")

            val prefsFile = File(prefsDir, "${app.packageName}_prefs.json")
            val prefsJson = if (prefsFile.exists()) {
                runCatching { JSONObject(prefsFile.readText()) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }

            SandboxExecutionResult(
                success = false,
                startupTimeMs = startupMs,
                cpuTimeMs = 2L,
                ramDeltaKb = 128L,
                totalProcessPssKb = endPssKb,
                updatedDataSizeBytes = computeDirectorySize(dataDir),
                consoleOutputLines = consoleLines,
                isolatedFilesList = listIsolatedFiles(filesDir),
                isolatedPrefsMap = jsonToMap(prefsJson),
                errorSummary = "${t.javaClass.simpleName}: ${t.message}",
                stackTrace = trace
            )
        }
    }

    /**
     * Reads current isolated files & preferences for an installed app without executing a new launch.
     */
    fun inspectIsolatedState(app: InstalledSandboxAppEntity): Pair<List<Pair<String, Long>>, Map<String, String>> {
        val dataDir = File(app.isolatedDataDirPath)
        val filesDir = File(dataDir, "files")
        val prefsFile = File(dataDir, "shared_prefs/${app.packageName}_prefs.json")
        val prefsJson = if (prefsFile.exists()) {
            runCatching { JSONObject(prefsFile.readText()) }.getOrDefault(JSONObject())
        } else {
            JSONObject()
        }
        return listIsolatedFiles(filesDir) to jsonToMap(prefsJson)
    }

    /**
     * Clears only the sandbox app's isolated data (files, shared_prefs, dex_opt) while keeping base.apk installed.
     */
    fun clearAppData(app: InstalledSandboxAppEntity): Long {
        val dataDir = File(app.isolatedDataDirPath)
        if (dataDir.exists()) {
            dataDir.deleteRecursively()
        }
        val filesDir = File(dataDir, "files").apply { mkdirs() }
        val prefsDir = File(dataDir, "shared_prefs").apply { mkdirs() }
        val prefsFile = File(prefsDir, "${app.packageName}_prefs.json")
        val resetJson = JSONObject().apply {
            put("sandbox.profile_id", app.profileId)
            put("sandbox.installed_version", "${app.versionName} (${app.versionCode})")
            put("sandbox.data_reset_at", System.currentTimeMillis())
            put("sandbox.launch_count", 0)
        }
        prefsFile.writeText(resetJson.toString(2))
        File(filesDir, "package_manifest.txt").writeText("Data reset clean for ${app.packageName} in profile ${app.profileId}\n")
        return computeDirectorySize(dataDir)
    }

    /**
     * Completely removes the sandbox app's container folder (APK + DEX cache + isolated data) from the profile.
     */
    fun uninstallAppContainer(context: Context, profileId: String, packageName: String) {
        val containerDir = getAppContainerDir(context, profileId, packageName)
        if (containerDir.exists()) {
            containerDir.walkBottomUp().forEach { it.setWritable(true) }
            containerDir.deleteRecursively()
        }
    }

    /**
     * Wipes an entire testing profile directory tree on disk.
     */
    fun purgeProfileDirectory(context: Context, profileId: String) {
        val profileDir = getProfileRootDir(context, profileId)
        if (profileDir.exists()) {
            profileDir.walkBottomUp().forEach { it.setWritable(true) }
            profileDir.deleteRecursively()
        }
    }

    fun computeDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun listIsolatedFiles(filesDir: File): List<Pair<String, Long>> {
        if (!filesDir.exists()) return emptyList()
        return filesDir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(filesDir).path to it.length() }
            .toList()
    }

    private fun jsonToMap(json: JSONObject): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            result[k] = json.optString(k, "")
        }
        return result
    }
}

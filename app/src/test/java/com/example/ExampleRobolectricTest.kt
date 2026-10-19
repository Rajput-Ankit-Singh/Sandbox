package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ApkParserEngine
import com.example.engine.AvfCompatDetector
import com.example.engine.SandboxRuntimeEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `verify app_name resource is DroidSandbox`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DroidSandbox", appName)
    }

    @Test
    fun `verify AVF compatibility detector reports honest sandbox mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = AvfCompatDetector.evaluate(context)
        assertTrue(report.activeBackendMode.isNotBlank())
        assertTrue(report.limitationsList.isNotEmpty())
        assertTrue(report.backendSummary.contains("Sandbox", ignoreCase = true))
    }

    @Test
    fun `verify sample APK generation, binary parsing, profile isolation, version upgrade, and crash trapping`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Generate and parse v1.0 Benchmark APK
        val presetV1 = ApkParserEngine.SAMPLE_PRESETS.first { it.id == "sample_bench_v1" }
        val parsedV1 = ApkParserEngine.generateSampleApk(context, presetV1).getOrThrow()
        assertEquals("com.droidsandbox.sample.benchmark", parsedV1.packageName)
        assertEquals(1L, parsedV1.versionCode)
        assertTrue(parsedV1.hasValidDex)
        assertTrue(parsedV1.sha256Signature.length == 64)

        // 2. Install into Profile A and Profile B (App Cloning across profiles)
        val installedProfileA = SandboxRuntimeEngine.installApkIntoProfile(
            context = context,
            metadata = parsedV1,
            profileId = "profile_qa_a",
            existingApp = null,
            onProgress = {}
        ).getOrThrow()

        val installedProfileB = SandboxRuntimeEngine.installApkIntoProfile(
            context = context,
            metadata = parsedV1,
            profileId = "profile_qa_b",
            existingApp = null,
            onProgress = {}
        ).getOrThrow()

        // Launch in Profile A and write a custom isolated preference
        val execA = SandboxRuntimeEngine.launchInSandbox(
            context = context,
            app = installedProfileA,
            customUserInputKey = "secret_token",
            customUserInputValue = "alpha_only_value",
            forceFaultInjection = false
        )
        assertTrue(execA.success)
        assertEquals("alpha_only_value", execA.isolatedPrefsMap["secret_token"])

        // Verify Profile B does NOT see Profile A's isolated preference
        val (_, prefsB) = SandboxRuntimeEngine.inspectIsolatedState(installedProfileB)
        assertFalse(prefsB.containsKey("secret_token"))

        // 3. Test Version Upgrade in Profile A (v1.0 -> v2.0) preserves isolated preference
        val presetV2 = ApkParserEngine.SAMPLE_PRESETS.first { it.id == "sample_bench_v2" }
        val parsedV2 = ApkParserEngine.generateSampleApk(context, presetV2).getOrThrow()
        val upgradedProfileA = SandboxRuntimeEngine.installApkIntoProfile(
            context = context,
            metadata = parsedV2,
            profileId = "profile_qa_a",
            existingApp = installedProfileA,
            onProgress = {}
        ).getOrThrow()
        assertEquals(2L, upgradedProfileA.versionCode)
        assertEquals("UPGRADED", upgradedProfileA.lastStatus)
        val (_, prefsAfterUpgrade) = SandboxRuntimeEngine.inspectIsolatedState(upgradedProfileA)
        assertEquals("alpha_only_value", prefsAfterUpgrade["secret_token"])

        // 4. Test Crash Boundary trapping does not crash host
        val crashExec = SandboxRuntimeEngine.launchInSandbox(
            context = context,
            app = upgradedProfileA,
            forceFaultInjection = true
        )
        assertFalse(crashExec.success)
        assertNotNull(crashExec.stackTrace)

        // 5. Test Clear App Data resets preferences while keeping APK installed
        SandboxRuntimeEngine.clearAppData(upgradedProfileA)
        val (_, prefsAfterClear) = SandboxRuntimeEngine.inspectIsolatedState(upgradedProfileA)
        assertFalse(prefsAfterClear.containsKey("secret_token"))
        assertTrue(File(upgradedProfileA.apkFilePath).exists())
    }
}

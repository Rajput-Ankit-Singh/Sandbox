# DroidSandbox — Lightweight Android APK Sandbox & Testing Workbench

DroidSandbox is a modern, transparent Android utility that lets developers and QA testers import APK files, inspect package metadata and signatures, manage isolated testing profiles, and execute packages inside an honest, hardware-verified sandbox environment without requiring root access or a PC.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Decisions from Phase 1:**
> - **Execution & Virtualization Mode**: **Honest Limited Sandbox Mode + AVF Probe**. Because standard unrooted Android apps cannot run arbitrary guest OS VMs or cross-UID packages in full kernel isolation without privileged system permissions (`android.permission.MANAGE_VIRTUAL_MACHINE`), DroidSandbox performs a real runtime hardware/OS probe for Android Virtualization Framework (AVF / `dev/kvm` / `android.system.virtualizationservice`) and operates in a clearly labeled **User-Space Container & Dynamic Loader Sandbox Mode** where each profile maintains strictly isolated file trees, `DexClassLoader` code caches, SharedPreferences, and runtime telemetry.
> - **Data Persistence**: **Local On-Device Storage Only**. All testing profiles, installed sandbox packages, execution logs, and crash traces are persisted strictly on-device using **Room Database** and isolated per-profile filesystem directories (`files/sandbox/profiles/<profile_id>/`).
> - **APK Acquisition**: **Built-in Sample APK Generator + Local Storage File Picker**. Users can import real `.apk` files from device storage via the system document picker (`ActivityResultContracts.OpenDocument`) OR generate real, valid, installable micro-APK binaries on-device with one tap to immediately test multi-version upgrades, profile isolation, crash capture, and sandbox execution.

---

## 1. Feasibility Report & Virtualization Evaluation

Before building the sandbox engine, we evaluated the three candidate virtualization approaches on standard production Android devices:

| Approach | Technical Mechanism | Unrooted Retail Device Feasibility | Licensing & Maintenance | Verdict for DroidSandbox |
| :--- | :--- | :--- | :--- | :--- |
| **1. Android Virtualization Framework (AVF)** | Hypervisor-based (`pKVM` / `/dev/kvm`) managed by `VirtualizationService` (Android 13+ / API 33+). | **Restricted**. Requires `MANAGE_VIRTUAL_MACHINE` or `USE_CUSTOM_VIRTUAL_MACHINE`, which are `signature\|privileged` permissions denied to normal third-party apps on retail devices. | AOSP Apache 2.0 (Active). | **Implement Real Hardware/Permission Probe**: Detect and report exact AVF hardware, kernel `pKVM`, and permission status on the device without making false emulation claims. |
| **2. Full Guest OS VM (QEMU / KVM)** | Hardware-accelerated KVM or software TCG emulation booting an Android guest system image. | **Impractical for Lightweight Mobile Utility**. Without KVM access, software emulation requires 1.5GB+ bundled system images, consumes excessive RAM/CPU, and suffers severe thermal throttling. | GPLv2 / LGPL (QEMU). | **Rejected**: Violates lightweight performance, startup speed, and low-RAM constraints. |
| **3. User-Space Process / Dynamic APK Container** | Parses real APK headers (`PackageManager.getPackageArchiveInfo`), verifies signatures, unpacks DEX/resources into per-profile isolated directories, and loads entry points via isolated `DexClassLoader` & proxy context wrapper. | **100% Supported on Android 7.0–16+ (API 24–36)** without root or privileged permissions. | Clean-room Kotlin implementation (Apache 2.0 compatible). | **Selected Core Backend**: Honest **Limited User-Space Sandbox Mode** with real APK binary parsing, per-profile filesystem isolation, multi-version side-by-side testing, and real JVM/DEX execution. |

> [!WARNING]
> **Transparency Guarantee**: DroidSandbox explicitly distinguishes between **Hypervisor VM Isolation (AVF)** and **Limited User-Space Container Mode** in the UI status banner, compatibility inspector, and execution console. It never claims that user-space dynamic classloading or app cloning is full kernel/OS virtualization.

---

## 2. Overview & Core Concept

- **What It Does**: Allows users to import `.apk` archives (or generate real test `.apk` binaries on the fly), inspect their manifest permissions, activities, version codes, and SHA-256 signatures, install them into one or more isolated **Testing Profiles** (cloning instances across profiles or testing version upgrades), launch their bytecode/entry payloads inside an isolated container runtime, monitor live RAM/CPU/startup benchmarks, and clear app data or uninstall cleanly.
- **Target Audience**: Android developers, QA engineers, and security analysts who need a fast, zero-root on-device workbench to verify APK builds, test version migrations, inspect crash traces, and reset isolated app state without polluting their host phone.
- **Key Value**: Combines a real binary APK parser, per-profile isolated storage manager, live telemetry benchmark runner, and crash/logcat inspector in a fast, zero-bloat 4-screen mobile interface.

---

## 3. User Experience & Visual Design

### Key User Flows (4-Screen Architecture)

1. **Home (Installed Sandbox Apps)**:
   - Displays an honest **Environment Status Pill** at the top (`Limited User-Space Sandbox • Active Profile: [Profile Name]`) with a quick profile switcher chip row.
   - Lists installed sandbox apps inside the active profile using lazy loading (`LazyColumn`), showing extracted app icon, label, package name, version badge (`v1.0 (1)`), isolated disk usage, and quick actions (**Launch**, **Inspect / Test**, **Uninstall**).
   - Includes a search/filter bar and an empty state with direct shortcuts to **Import APK** or **Generate Sample APK**.
2. **Import APK (Select, Generate & Install)**:
   - **System APK Picker**: Launches Android's zero-permission `OpenDocument` contract filtered for `application/vnd.android.package-archive` (`.apk`).
   - **Built-in Real APK Generator**: Generates real, valid ZIP-aligned `.apk` binary files with valid `AndroidManifest.xml` and compiled `classes.dex` bytecode right in local cache (e.g., *Sandbox Benchmark Demo v1.0*, *Sandbox Benchmark Demo v2.0 Upgrade*, *Crash Simulator Test APK*, and *Storage Stress Test APK*) so users can test version upgrades, cloning across profiles, and crash capture immediately.
   - **Pre-Install Inspector Card**: Asynchronously extracts and displays APK metadata before installation: App Name, Package Name, Version Name/Code, File Size, Min/Target SDK, SHA-256 Digest, Requested Permissions (highlighting dangerous permissions), and target Profile selection (supporting **App Cloning** across separate profiles).
   - **Actionable Installation Flow**: Step-by-step progress bar (`Verifying Archive -> Checking ABI/SDK Compatibility -> Allocating Profile Storage -> Extracting DEX & Resources -> Registering Package`) with accurate error reporting for corrupted archives, SDK mismatches, or signature conflicts during version upgrades.
3. **Testing (Launch, Benchmarks, Logs & Data Control)**:
   - **Interactive Container Runner**: Launches the selected sandbox app inside an isolated sandbox execution viewport. Executes the APK's entry class/method via an isolated `DexClassLoader` and redirected `SandboxContext` (whose `filesDir`, `cacheDir`, and `SharedPreferences` resolve strictly to `/files/sandbox/profiles/<profileId>/apps/<packageName>/`).
   - **Live Telemetry & Benchmarks**: Measures and displays actual **Cold Startup Time (ms)**, **Process Heap / PSS RAM Delta (MB)**, **Thread CPU Time (ms)**, and **Isolated Storage Footprint (KB/MB)**.
   - **Sandbox Log & Crash Inspector**: Displays structured runtime logs (`INFO`, `WARN`, `ERROR`, `CRASH`), uncaught exception stack traces captured safely by the sandbox boundary without crashing the host app, and a one-tap **Trigger Fault / Reset Data** toolbar.
4. **Settings (Storage, Profiles & Compatibility)**:
   - **Device & AVF Compatibility Report**: Real-time hardware & OS probe detailing Android API level, CPU ABI (`Build.SUPPORTED_ABIS`), `/dev/kvm` hypervisor node presence, `android.software.virtualization_framework` feature flag, `MANAGE_VIRTUAL_MACHINE` permission state, and why **Limited User-Space Sandbox Mode** is active.
   - **Profile Manager (App Cloning & Isolation)**: Create new testing profiles (e.g., *Default QA*, *Clean Install*, *Staging v2*), switch active profile, reset all apps in a profile, or delete a profile and purge its directory tree.
   - **Sandbox Storage Manager**: Visual breakdown of total host storage used by APK archives, extracted DEX caches, and per-app isolated data directories, with one-tap **Clear All Cache** or per-app data wipe.

### Visual Identity & Theme

- **Aesthetic Direction**: **Precision Systems Utility** — clean, high-contrast engineering instrumentation inspired by modern developer tooling.
- **Color Palette**:
  - **Dark Mode (Default Recommended / Toggleable)**: Deep Carbon Slate background (`#0D1117`), Elevated Surface cards (`#161B22`), Crisp Technical Cyan primary accent (`#38BDF8`), Emerald Telemetry Green (`#34D399`) for verified/running states, Amber Warning (`#FBBF24`) for Limited Sandbox Mode badges, and Coral Red (`#F87171`) for crash traces and destructive resets.
  - **Light Mode**: Cool Alabaster background (`#F8FAFC`), Pure White cards (`#FFFFFF`) with subtle slate borders, Deep Cobalt/Teal primary (`#0284C7`), and high-contrast slate typography.
- **Typography**:
  - **Display & Headings**: `Space Grotesk` (bundled locally in `res/font/`) for crisp, technical headers and metric readouts.
  - **Body**: `Plus Jakarta Sans` for scannable metadata and compatibility descriptions.
  - **Monospace Telemetry & Logs**: `JetBrains Mono` for package names, SHA-256 hashes, file paths, RAM/CPU metrics, and stack traces.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                        Presentation Layer (Jetpack Compose)               │
│  ┌─────────────────┐ ┌─────────────────┐ ┌──────────────┐ ┌─────────────┐ │
│  │   HomeScreen    │ │ ImportApkScreen │ │TestingScreen │ │SettingsScrn │ │
│  │ (Installed Apps │ │ (Picker, Sample │ │ (Container   │ │ (Profiles,  │ │
│  │  & Quick Launch)│ │  Gen & Inspect) │ │  & Logs/RAM) │ │  AVF, Disk) │ │
│  └────────┬────────┘ └────────┬────────┘ └──────┬───────┘ └──────┬──────┘ │
│           └───────────────────┴────────┬────────┴────────────────┘        │
│                                        ▼                                  │
│                           ┌─────────────────────────┐                     │
│                           │   SandboxViewModel      │                     │
│                           │ (StateFlow + Async IO)  │                     │
│                           └────────────┬────────────┘                     │
└────────────────────────────────────────┼──────────────────────────────────┘
                                         ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      Domain & Sandbox Execution Engine                    │
│  ┌──────────────────────┐ ┌───────────────────────┐ ┌───────────────────┐ │
│  │   ApkParserEngine    │ │ SandboxRuntimeEngine  │ │ AvfCompatDetector │ │
│  │ • PackageArchiveInfo │ │ • Isolated Context    │ │ • API / ABI Check │ │
│  │ • ZIP/DEX Validation │ │ • DexClassLoader      │ │ • /dev/kvm Probe  │ │
│  │ • SHA-256 & Perms    │ │ • Real RAM/CPU Meter  │ │ • Feature Flags   │ │
│  │ • Sample APK Builder │ │ • Crash Boundary Trap │ │ • Honest Mode Rep │ │
│  └──────────┬───────────┘ └───────────┬───────────┘ └─────────┬─────────┘ │
└─────────────┼─────────────────────────┼───────────────────────┼───────────┘
              └─────────────────────────┼───────────────────────┘
                                        ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     Persistence & Isolated File System                    │
│  ┌───────────────────────────────────┐ ┌────────────────────────────────┐ │
│  │     Room Database (sandbox.db)    │ │  Per-Profile Isolated Storage  │ │
│  │ • SandboxProfileEntity            │ │  /files/sandbox/profiles/<id>/ │ │
│  │ • InstalledSandboxAppEntity       │ │   └── <pkg>/base.apk           │ │
│  │ • SandboxLogEntity (Logs/Crashes) │ │   └── <pkg>/dex_cache/         │ │
│  │ • BenchmarkRecordEntity           │ │   └── <pkg>/data/files & prefs │ │
│  └───────────────────────────────────┘ └────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

1. **`SandboxProfileEntity`**:
   - `profileId` (PK), `name`, `description`, `createdAt`, `isDefault`, `isolationMode` (`USER_SPACE_CONTAINER`).
2. **`InstalledSandboxAppEntity`**:
   - `id` (PK), `profileId` (FK), `packageName`, `appName`, `versionName`, `versionCode`, `minSdk`, `targetSdk`, `apkFilePath`, `isolatedDataDirPath`, `apkSizeBytes`, `dataSizeBytes`, `sha256Signature`, `permissionsCsv`, `installedAt`, `lastLaunchedAt`, `launchCount`.
   - Enforces a unique composite index on `(profileId, packageName)` so installing a newer version in the same profile upgrades the package (preserving isolated data if signatures match), while installing the same APK into a *different* profile creates a separate cloned instance.
3. **`SandboxLogEntity`**:
   - `id` (PK), `profileId`, `packageName`, `timestamp`, `level` (`INFO`, `WARN`, `ERROR`, `CRASH`), `tag`, `message`, `stackTrace`.
4. **`DeviceCompatibilityReport`**:
   - Evaluated asynchronously via `AvfCompatDetector`: Android Version, SDK Int, Device Model, Supported ABIs, Total/Available RAM, `hasSystemFeature("android.software.virtualization_framework")`, `/dev/kvm` existence, `MANAGE_VIRTUAL_MACHINE` permission grant status, and active backend summary.

### Interactive Component & State Mapping

- **Importing or Generating an APK**:
  - Selecting a file via `OpenDocument` or tapping a **Sample APK preset** copies/generates the `.apk` stream on `Dispatchers.IO`, verifies the ZIP magic header (`PK\x03\x04`) and `classes.dex` presence, invokes `PackageManager.getPackageArchiveInfo()`, computes the SHA-256 digest, and populates `ImportInspectionState`.
- **Installing into a Profile**:
  - Validates SDK compatibility (`minSdk <= Build.VERSION.SDK_INT`), checks existing `(profileId, packageName)` records for signature consistency, creates `/files/sandbox/profiles/<profileId>/<packageName>/{apk,dex_opt,data/files,data/shared_prefs}`, writes `base.apk`, and inserts/updates the `InstalledSandboxAppEntity` and installation log entry.
- **Launching & Benchmarking in Testing Screen**:
  - Allocates an isolated `SandboxExecutionSession`, records baseline `Debug.getPss()` and `SystemClock.elapsedRealtimeNanos()`, loads the APK's DEX via `DexClassLoader` with a read-only file permission requirement (Android 14+ `setReadOnly()` compliance), executes the sandbox entry workload and interactive key-value/file operations inside the isolated `data/` directory, traps any thrown exceptions into `SandboxLogEntity`, and updates real measured startup latency, RAM delta, and CPU time.
- **Clearing Data / Resetting Profiles / Uninstalling**:
  - **Clear App Data**: Recursively deletes `/data/files` and `/data/shared_prefs` for that `(profileId, packageName)` while keeping `base.apk` installed, resetting `dataSizeBytes` to `0`.
  - **Uninstall App**: Deletes the entire `<packageName>` folder inside the profile and removes the Room record.
  - **Reset / Delete Profile**: Purges the profile's directory tree on disk and cascades deletion in Room.

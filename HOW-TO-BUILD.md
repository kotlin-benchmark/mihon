# How to Build Mihon

This document explains how to build the Mihon Android app from source on a clean machine
(Linux, macOS or Windows). It is written for people who have never built this project
before, and it does not assume any particular username, home directory, or filesystem
layout: everywhere a path appears, substitute your own.

> TL;DR
> ```bash
> # JDK 21 is mandatory (see section 1.1)
> export JAVA_HOME=/path/to/jdk-21
> export ANDROID_HOME=/path/to/Android/Sdk
> ./gradlew :app:assembleDebug
> ```
> Output APKs: `app/build/outputs/apk/debug/`

---

## 1. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | **21** | Mandatory. The project's CI builds on Temurin 21, and the Kotlin/AGP versions in use expect it. |
| Android SDK | Platform **37**, Build-Tools **36.x+** | `compileSdk = 37`, `minSdk = 26`, `targetSdk = 36`, from `gradle/mihon.versions.toml`. Platform 37 is recent - see 6.3. |
| Gradle | **9.6.1** | Do **not** install manually. The bundled wrapper (`./gradlew`) downloads the correct version on first run (~150 MB). |
| Disk space | ~6 GB free | 15 Gradle modules, an included build, plus the dependency cache. |
| RAM | 12 GB+ recommended | `gradle.properties` sets `-Xmx4g` and enables parallel execution; CI raises the heap to 6 GB. |

You do **not** need Android Studio, the NDK, or any native toolchain. No credentials,
tokens, or private registries are required: everything resolves from Maven Central and
Google's Maven repository.

Telemetry (Firebase/Crashlytics) is **off by default** and is opt-in via a Gradle property,
so a plain build needs no `google-services.json`. See 4.5.

### 1.1 Install a JDK 21

JDK 21 is not optional here. The version matters more than the vendor: Temurin, Zulu,
Corretto, Liberica, Microsoft Build of OpenJDK and the JetBrains Runtime all work.

- Linux (Debian/Ubuntu): `sudo apt install openjdk-21-jdk`
- Linux (Fedora/RHEL): `sudo dnf install java-21-openjdk-devel`
- Linux (Arch): `sudo pacman -S jdk21-openjdk`
- macOS (Homebrew): `brew install openjdk@21`
- Windows: install "Eclipse Temurin 21" (Adoptium) via the MSI, or `winget install EclipseAdoptium.Temurin.21.JDK`
- Any OS, no admin rights: download a JDK 21 archive from <https://adoptium.net/> and unpack it anywhere you can write to.

If Android Studio is already installed, it ships a JetBrains Runtime 21 you can point at
instead of installing anything:

| OS | Bundled JBR path |
|----|------------------|
| Linux | `<android-studio-dir>/jbr` (often `/opt/android-studio/jbr` or `/usr/local/android-studio/jbr`) |
| macOS | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` |
| Windows | `C:\Program Files\Android\Android Studio\jbr` |

Point `JAVA_HOME` at whichever JDK 21 you chose:

```bash
# Linux / macOS (bash or zsh) - substitute your own path
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
```
```powershell
# Windows (PowerShell) - current session only
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```
```bat
:: Windows (cmd.exe) - current session only
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
```

Verify before continuing. It must print `21.x`:

```bash
"$JAVA_HOME/bin/java" -version
```

To avoid exporting the variable on every shell, write it into `gradle.properties` in your
**home** Gradle directory (`~/.gradle/gradle.properties`, or
`%USERPROFILE%\.gradle\gradle.properties` on Windows) - that file is outside the repo, so
it never gets committed:

```properties
org.gradle.java.home=/absolute/path/to/jdk-21
```

> Note: Mihon uses an **included build** (`gradle/build-logic`). Gradle properties are not
> inherited by included builds, which is why `gradle/build-logic/gradle.properties`
> duplicates the memory settings. If you change heap settings, change both files, or pass
> the value on the command line instead.

### 1.2 Install the Android SDK

If you have Android Studio, the SDK is already installed and you can skip to `ANDROID_HOME`
below. Otherwise install the **command-line tools** from
<https://developer.android.com/studio#command-line-tools-only>, unpack them, and then:

```bash
sdkmanager "platforms;android-37" "build-tools;36.0.0" "platform-tools"
sdkmanager --licenses      # accept once
```

If `platforms;android-37` is not offered by your `sdkmanager`, see 6.3.

Tell the build where the SDK lives, using **either** an environment variable:

```bash
# Linux (default Studio location)
export ANDROID_HOME="$HOME/Android/Sdk"
# macOS (default Studio location)
export ANDROID_HOME="$HOME/Library/Android/sdk"
```
```powershell
# Windows (default Studio location)
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
```

**or** a `local.properties` file in the project root (git-ignored, machine-specific):

```properties
# Linux / macOS
sdk.dir=/absolute/path/to/Android/Sdk
```
```properties
# Windows - use forward slashes or escaped backslashes
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
```

`local.properties` wins over the environment variable, which is handy when several
checkouts on the same machine need different SDKs.

---

## 2. Getting the code

```bash
git clone https://github.com/kotlin-benchmark/mihon.git
cd mihon
```

The build reads the Git history to stamp `COMMIT_COUNT` and `COMMIT_SHA` into
`BuildConfig`, so build from a real clone, not from an extracted zip with no `.git`
directory. A shallow clone works, but the commit count will be wrong.

Windows note: if you hit `Filename too long`, enable long paths once with
`git config --global core.longpaths true`.

---

## 3. Project layout

Mihon is multi-module. The pieces you are most likely to touch:

| Path | What it is |
|------|------------|
| `app/` | The Android application: activities, UI screens, data layer glue. |
| `core/common/` | Shared infrastructure, including the OkHttp `NetworkHelper`. |
| `core/archive/`, `core/viewmodel/` | Focused support libraries. |
| `data/` | SQLDelight database definitions and repository implementations. |
| `domain/` | Business logic and interactors, no Android dependencies. |
| `i18n/` | Localized strings (Moko resources). |
| `presentation-core/`, `presentation-widget/` | Compose UI building blocks and the home-screen widget. |
| `source-api/`, `source-local/` | Extension API and the local-files source. |
| `telemetry/` | Firebase wrapper, no-op unless telemetry is enabled. |
| `gradle/build-logic/` | Included build with the convention plugins that configure every module. |
| `gradle/libs.versions.toml` | Dependency version catalog. |
| `gradle/mihon.versions.toml` | Project version catalog (`mihonx`), including the SDK levels. |

---

## 4. Building

### 4.1 Understand the variants first

Mihon has **no product flavors**, so the plain task names work. What it does have is five
build types:

| Task | Produces |
|------|----------|
| `:app:assembleDebug` | Debug APKs. Application ID gets a `.dev` suffix. The normal choice. |
| `:app:assembleRelease` | Minified release APKs (R8 + resource shrinking). |
| `:app:assemblePreview` | Release-like build of the preview channel (`.debug` suffix). |
| `:app:assembleFoss` | Release-like build with no proprietary bits (`.foss` suffix). |
| `:app:assembleBenchmark` | Profileable build used for baseline profiles (`.benchmark` suffix). |

### 4.2 Debug build (typical)

```bash
./gradlew :app:assembleDebug
```
```powershell
.\gradlew.bat :app:assembleDebug
```

On Windows always use `gradlew.bat` (or `.\gradlew.bat`); the extensionless `gradlew` is the
POSIX shell script and will not run in cmd.exe or PowerShell.

The command CI uses, which is also the most reliable one on a memory-constrained machine:

```bash
./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.jvmargs=-Xmx6g
```

### 4.3 Compile-only check (faster)

To type-check without packaging APKs:

```bash
./gradlew :app:compileDebugKotlin
```

To type-check a single module, e.g. after editing `core/common`:

```bash
./gradlew :core:common:compileDebugKotlin
```

### 4.4 Release build

```bash
./gradlew :app:assembleRelease
```

Signing is **optional**. The release build reuses the debug signing config, and that config
is only customized when either:

- a `keystore.properties` file exists in the project root, or
- the `MIHON_GITHUB_RELEASE` environment variable is set (the CI release path, which also
  expects `storeFileBase64`, `storePassword`, `keyAlias`, `keyPassword` and `RUNNER_TEMP`).

For a local signed build, create `keystore.properties` in the repo root (git-ignored):

```properties
storeFile=/absolute/path/to/your.keystore
storePassword=<store password>
keyAlias=<key alias>
keyPassword=<key password>
```

With neither present, the build falls back to the default Android debug keystore, which is
fine for local installs.

### 4.5 Optional Gradle properties

| Property | Effect |
|----------|--------|
| `-Pinclude-telemetry` | Applies the Google Services and Firebase Crashlytics plugins. **Requires** a valid `app/google-services.json`, or configuration fails. Off by default. |
| `-Penable-updater` | Enables the in-app updater code path. Off by default. |

### 4.6 ABI splits

The build produces per-ABI APKs for `armeabi-v7a`, `arm64-v8a`, `x86` and `x86_64`, plus a
universal APK. That means a single `assembleDebug` writes several files - pick the one
matching your device, or use the universal APK.

### 4.7 Fully clean, reproducible build

```bash
./gradlew clean :app:assembleDebug --no-build-cache --rerun-tasks
```

---

## 5. Output and installing

```
app/build/outputs/apk/debug/
```

Several APKs land there (one per ABI plus a universal one). To list them without guessing:

```bash
find app/build/outputs/apk -name '*.apk'
```
```powershell
Get-ChildItem -Recurse app\build\outputs\apk -Filter *.apk
```

Install on a connected device or emulator, letting `adb` choose nothing for you - install
the ABI that matches, or the universal APK:

```bash
adb install -r app/build/outputs/apk/debug/<the-apk-you-picked>.apk
```

The debug build's application ID is `app.mihon.dev`, so it installs side-by-side with a
release Mihon rather than replacing it.

---

## 6. Troubleshooting

### 6.1 Wrong JDK: toolchain errors, `Unsupported class file major version`, or Kotlin/AGP complaints

Build with JDK 21. Check what Gradle actually resolved, and reset the daemon afterwards -
a daemon started under a different JDK gets reused and keeps failing:

```bash
./gradlew --version     # look at the "JVM:" line
./gradlew --stop
```

If `JAVA_HOME` is awkward to manage on your machine, set `org.gradle.java.home` in
`~/.gradle/gradle.properties` instead (see 1.1).

### 6.2 `SDK location not found`

Neither `ANDROID_HOME` nor `local.properties` is set for this shell/checkout. See 1.2.
Remember that an environment variable exported in one terminal does not exist in another,
and that IDEs do not inherit your shell profile - `local.properties` is the more reliable
option for a machine you return to.

### 6.3 `Failed to find target with hash string 'android-37'`

`compileSdk` is 37, which is newer than many installed SDKs. Options, in order of
preference:

```bash
# 1. update the SDK tooling, then install the platform
sdkmanager --update
sdkmanager "platforms;android-37" "build-tools;36.0.0"
sdkmanager --licenses
```

If your `sdkmanager` still does not offer platform 37, update the command-line tools
package itself (`sdkmanager "cmdline-tools;latest"`) and retry. As a last resort you can
lower `android-sdk-compile` in `gradle/mihon.versions.toml`, but that is a source change
and may break code that relies on newer APIs - prefer installing the platform.

### 6.4 Configuration fails only with `-Pinclude-telemetry`

The Google Services plugin needs `app/google-services.json`, which is not in the repo.
Either drop the flag (telemetry is off by default) or supply your own Firebase config file.

### 6.5 `Cannot lock ... has already been locked by this process` / stuck build

A previous run was killed and left a stale daemon or lock file:

```bash
./gradlew --stop
```
```bash
# Linux / macOS, if it persists
pkill -f GradleDaemon
```
```powershell
# Windows, if it persists
Get-Process java | Where-Object { $_.Path -like '*gradle*' } | Stop-Process
```

### 6.6 Out-of-memory: `Java heap space`, `GC overhead limit exceeded`, or a killed daemon

This is the most common failure on this project - it is a large multi-module Compose build
with `org.gradle.parallel=true`, so several Kotlin compilations can run at once.

```bash
# more heap
./gradlew :app:assembleDebug -Dorg.gradle.jvmargs=-Xmx6g

# or trade speed for a smaller footprint
./gradlew :app:assembleDebug --no-parallel --no-daemon -Dorg.gradle.jvmargs=-Xmx4g
```

Remember the included build has its own `gradle/build-logic/gradle.properties`; a heap
setting edited only in the root file does not reach it.

On Linux, a daemon that dies with no error message is usually the OOM killer - confirm with
`dmesg | tail`.

### 6.7 Configuration cache or build cache errors after changing build files

`org.gradle.caching=true` and `org.gradle.configureondemand=true` are set. After editing
`.gradle.kts` files or the version catalogs:

```bash
./gradlew :app:assembleDebug --no-configuration-cache
```

If results look stale rather than broken:

```bash
./gradlew clean :app:assembleDebug --no-build-cache
```

### 6.8 `COMMIT_COUNT` / `COMMIT_SHA` failures, or a build that cannot read Git

The build shells out to Git to stamp `BuildConfig`. It needs a real repository and `git` on
`PATH`. Building from a zip download, or inside a container without `git` installed, fails
here. Clone properly, or install `git`.

### 6.9 First build is very slow

Expected. The first run downloads Gradle 9.6.1, the Android Gradle Plugin 9.3.0, Kotlin
2.4.0, Compose, SQLDelight and the rest, then configures 15 modules and an included build.
Later builds reuse the cache in your home directory (`~/.gradle`, or
`%USERPROFILE%\.gradle`) and are far faster.

### 6.10 Corporate proxy or offline machine

Gradle honors standard proxy properties. Put them in `~/.gradle/gradle.properties`:

```properties
systemProp.http.proxyHost=proxy.example.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.example.com
systemProp.https.proxyPort=8080
```

For a fully offline build the dependency cache must already be populated; then add
`--offline`.

### 6.11 Line endings on Windows

If Git is configured with `core.autocrlf=true`, shell scripts in the repo may end up with
CRLF. That does not affect `gradlew.bat`, but if you build from Git Bash or WSL and see
`bad interpreter`, re-checkout with `core.autocrlf=input` or run the build from cmd.exe /
PowerShell using `gradlew.bat`.

---

## 7. Verifying a checkout builds at all

The shortest end-to-end check, from a clean clone:

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew --version                # confirms JVM 21 and Gradle 9.6.1
./gradlew :app:compileDebugKotlin  # type-checks the app module and its dependencies
./gradlew :app:assembleDebug       # produces the APKs
```

If step 2 passes but step 3 fails, the problem is packaging or SDK setup, not the source
code.

# AGENTS.md

Guidance for AI coding agents (Bits Dev, Claude Code, Codex, …) working in this repository.

## Repository overview

Datadog Gradle Plugin for Android (Kotlin, Gradle Kotlin DSL).

- `dd-sdk-android-gradle-plugin/`: the Gradle plugin (mapping file upload, Compose instrumentation setup).
- `dd-sdk-android-gradle-plugin-kcp-*`: the Kotlin Compiler Plugin (KCP) for Jetpack Compose
  instrumentation, one module per Kotlin line (see below), plus shared code in `kcp-common`.
- `samples/`, `instrumented/`: sample and instrumented-test apps of the main build.
- `kotlin-compat-app/`: standalone test app used by the scheduled Kotlin compatibility check. It is
  a separate Gradle build on purpose: never add it to the root `settings.gradle.kts` and never make
  it reference files outside of its folder.
- `tools/build-config/`: build logic; the plugin version is `MavenConfig.VERSION`.

## Build and checks

```bash
./gradlew :dd-sdk-android-gradle-plugin:assemble
./gradlew :dd-sdk-android-gradle-plugin:test
./gradlew :dd-sdk-android-gradle-plugin-kcp-kotlin24:test   # any kcp-* module
./gradlew ktlintCheck :build-config:ktlintCheck
./gradlew detekt
```

## Conventions

- Branch names must be `<name>/<description>`; PRs target `develop`.
- Every source file starts with the Apache 2.0 license header used across the repo.
- When bumping AGP or Gradle in the plugin build, bump them in `kotlin-compat-app` too, so the
  Kotlin compatibility check keeps testing a supported toolchain.

## Kotlin compatibility fixes

Use this when a new Kotlin release (stable or Beta/RC) breaks the Compose KCP, typically from a
`kotlin-compat` alert in `#dd-sdk-android-ops`. Input: the breaking Kotlin version and, if
available, the errors from the alert.

### Module map

| Kotlin versions | Module |
|---|---|
| 1.9.23+ – 2.0.x | `dd-sdk-android-gradle-plugin-kcp-kotlin20` |
| 2.1.x | `dd-sdk-android-gradle-plugin-kcp-kotlin21` |
| 2.2.x – 2.3.x | `dd-sdk-android-gradle-plugin-kcp-kotlin22` |
| 2.4.x and newer | `dd-sdk-android-gradle-plugin-kcp-kotlin24` |

The Gradle plugin picks the module from the Kotlin version in
`dd-sdk-android-gradle-plugin/src/main/kotlin/com/datadog/gradle/plugin/kcp/KotlinVersion.kt` and
`DatadogKotlinCompilerPluginSupport.kt`. Each module compiles against the matching `kotlinXX`
entry of `gradle/libs.versions.toml`.

### Reproduce

```bash
./gradlew publishToMavenLocal -Pdd-skip-signing
cd kotlin-compat-app
./gradlew assembleDebug verifyKotlinCompat -PkotlinVersion=<breaking version> -PddPluginVersion=<MavenConfig.VERSION, e.g. 1.32.0-SNAPSHOT>
```

`verifyKotlinCompat` also fails if the plugin compiled but did not instrument the app.

### Fix shapes

- **Patch release breaks an existing line** (e.g. 2.4.21 on `kcp-kotlin24`): usually a small IR
  API change. Examples: `c39c456` (2.2.20), `b3683a9` (2.3.20). Keep the fix working on the
  earlier versions of the same line. Bump the matching `kotlinXX` in `gradle/libs.versions.toml`
  if the module must compile against the new compiler, and add or adjust linkage tests like
  `CommandLineProcessorLinkage24Test`.
- **New minor line** (e.g. 2.5.0 while the newest module is `kcp-kotlin24`): follow the 2.4
  sequence: `8b04302` (module setup) → `66b3904` (implementation) → `26deff2` (routing, tests).
  Decide with the team whether a new module is needed or the newest one can be extended.
- **Pre-release** (Beta/RC): same shapes, but the break may still change before the stable
  release; prefer a minimal fix and mention the pre-release in the PR.

### Toolchain updates

A `toolchain-fail` alert means the test app fails to build with the new Kotlin version even
without the Datadog plugin: its pinned toolchain is too old, the KCP is not at fault (yet).

1. Check what the new Kotlin version requires: the Kotlin Gradle plugin compatibility table
   (minimum Gradle and AGP versions) and the error in the alert.
2. Bump the smallest set of versions that fixes the build:
   - AGP and Compose in `kotlin-compat-app/gradle/libs.versions.toml`;
   - Gradle with `cd kotlin-compat-app && ./gradlew wrapper --gradle-version <version>` (updates
     the wrapper properties and scripts).
3. Prefer versions the plugin build already uses (root `gradle/libs.versions.toml` and wrapper),
   and don't go beyond what customers can use.
4. Re-run the reproduce commands with the new Kotlin version. If the build now fails only with
   the Datadog plugin, continue with the fix shapes above in the same PR or a follow-up.

### Rules

- Never lower Kotlin versions, delete or skip tests to get the build green.
- Run `./gradlew detekt ktlintCheck` and the tests of the modules you touched.
- Re-run the reproduce commands with the breaking version; they must pass.
- Open the PR against `develop` with a `<name>/<description>` branch.

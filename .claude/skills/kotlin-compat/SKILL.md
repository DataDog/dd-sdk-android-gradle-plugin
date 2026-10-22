---
name: kotlin-compat
description: Use when a new Kotlin release breaks the Datadog Compose compiler plugin (KCP), e.g. from a #dd-sdk-android-ops kotlin-compat alert, to reproduce and fix it.
---

# Fix a Kotlin compatibility break in the Compose KCP

Input: the Kotlin version that breaks (e.g. `2.5.0`) and, if available, the errors from the
`kotlin-compat` alert.

## Module map

| Kotlin versions | Module |
|---|---|
| 1.9.23+ – 2.0.x | `dd-sdk-android-gradle-plugin-kcp-kotlin20` |
| 2.1.x | `dd-sdk-android-gradle-plugin-kcp-kotlin21` |
| 2.2.x – 2.3.x | `dd-sdk-android-gradle-plugin-kcp-kotlin22` |
| 2.4.x and newer | `dd-sdk-android-gradle-plugin-kcp-kotlin24` |

Shared code lives in `dd-sdk-android-gradle-plugin-kcp-common`. The Gradle plugin picks the module
from the Kotlin version in
`dd-sdk-android-gradle-plugin/src/main/kotlin/com/datadog/gradle/plugin/kcp/KotlinVersion.kt` and
`DatadogKotlinCompilerPluginSupport.kt`. Each module compiles against the matching `kotlinXX`
entry of `gradle/libs.versions.toml`.

## Reproduce

```bash
./gradlew publishToMavenLocal -Pdd-skip-signing
cd kotlin-compat-app
./gradlew assembleDebug verifyKotlinCompat -PkotlinVersion=<broken version> -PddPluginVersion=<version from tools/build-config/.../MavenConfig.kt>
```

`verifyKotlinCompat` also fails if the plugin compiled but did not instrument the app.

## Fix shapes

- **Patch release breaks an existing line** (e.g. 2.4.21 on `kcp-kotlin24`): usually a small IR
  API change. Examples: `c39c456` (2.2.20), `b3683a9` (2.3.20). Bump the matching `kotlinXX` in
  `gradle/libs.versions.toml` if the module must compile against the new compiler, and add or
  adjust linkage tests like `CommandLineProcessorLinkage24Test`.
- **New minor line** (e.g. 2.5.0 while the newest module is `kcp-kotlin24`): follow the 2.4
  sequence: `8b04302` (module setup) → `66b3904` (implementation) → `26deff2` (routing, tests).
  Decide with the team whether a new module is needed or the newest one can be extended.

## Rules

- Never lower Kotlin versions, delete or skip tests to get the build green.
- Run `./gradlew detekt ktlintCheck` and the tests of the modules you touched.
- Re-run the reproduce commands above with the broken version; it must pass.
- Branch name `<name>/<description>`, open the PR against `develop`.

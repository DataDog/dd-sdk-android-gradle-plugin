# Kotlin compatibility test app

Minimal Compose app used to check that the Datadog compiler plugin (KCP) works with a given
Kotlin version. It is run by the scheduled `kotlin-compat` GitLab pipeline.

```bash
./gradlew publishToMavenLocal -Pdd-skip-signing   # from the repo root
cd kotlin-compat-app
./gradlew assembleDebug verifyKotlinCompat -PkotlinVersion=<kotlin> -PddPluginVersion=<plugin>-SNAPSHOT
```

Locally, `-PkotlinVersion` and `-PddPluginVersion` are optional: defaults live in
`gradle.properties`, so the project can also be opened directly in Android Studio (after
publishing the plugin to mavenLocal). Keep `ddPluginVersion` in sync with the plugin version.

This project is intentionally isolated from the plugin build: own wrapper, catalog and properties,
and no file here may reference a path outside of this folder.

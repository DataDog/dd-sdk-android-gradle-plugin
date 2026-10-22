# Kotlin compatibility test app

Minimal Compose app used to check that the Datadog compiler plugin (KCP) works with a given
Kotlin version. It is run by the scheduled `kotlin-compat` GitLab pipeline.

```bash
./gradlew publishToMavenLocal -Pdd-skip-signing   # from the repo root
cd kotlin-compat-app
./gradlew assembleDebug verifyKotlinCompat -PkotlinVersion=<kotlin> -PddPluginVersion=<plugin>-SNAPSHOT
```

This project is intentionally isolated from the plugin build: own wrapper, catalog and properties,
and no file here may reference a path outside of this folder.

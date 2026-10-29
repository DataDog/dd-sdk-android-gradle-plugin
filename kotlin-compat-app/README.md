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

## Scheduling

The check runs from a GitLab pipeline schedule (CI/CD → Schedules) on `develop`, daily, with the
variable `CI_PIPELINE_KEY=kotlin-compat` (see `ci/pipelines/kotlin-compat-pipeline.yml`). Results
are posted to `#dd-sdk-android-ops` (`$SLACK_OPS_CHANNEL`) by the slack-notifier bot. On a KCP
failure, the message proposes asking Bits Dev to fix it following the "Kotlin compatibility fixes"
section of the root `AGENTS.md`.

Already reported results are kept in the project's package registry (generic package
`kotlin-compat-state`), so each Kotlin version is posted once per outcome. Delete that package to
reset it.

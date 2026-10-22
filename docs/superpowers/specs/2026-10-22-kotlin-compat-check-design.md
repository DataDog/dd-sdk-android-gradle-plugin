# Automated Kotlin compatibility check for the Compose KCP

## Intent

Every Kotlin release can break the Datadog Kotlin Compiler Plugin (KCP) used for Compose
instrumentation. Today the loop is manual: a Datadog workflow posts a Slack heads-up, then a
developer opens a local test app (`~/workspace/HelloworldApplication`), bumps Kotlin by hand,
publishes the plugin to mavenLocal, compiles, and — if it breaks — asks an AI agent to fix it and
opens a PR.

We want the *detection and verification* part to run without a human: a scheduled CI job finds
new Kotlin releases, builds a real app against them using the plugin from source, and posts the
result to Slack. Fixing stays human-initiated: the interruption handler (IH) sees the failure
message and asks Bits Dev to fix it.

## Goals

- Detect a new stable Kotlin release within a day, with no human involvement.
- Verify it against the plugin exactly as a customer uses it: an Android Compose app applying
  `com.datadoghq.dd-sdk-android-gradle-plugin` with `composeInstrumentation = AUTO`, so the
  plugin's Kotlin-version routing (`DatadogKotlinCompilerPluginSupport.kt`) is exercised.
- On failure, post a message to `#dd-sdk-android-ops` (`$SLACK_OPS_CHANNEL`) with everything IH needs to act, including a
  ready-to-copy prompt for Bits Dev.
- Give Bits Dev (and developers running Claude Code locally) a checked-in skill describing how
  Kotlin-compat fixes are done in this repo.

## Non-goals

- No autonomous fixing in CI. The pipeline never runs an AI agent, never creates branches or
  PRs. (Explicit user decision.)
- No mention / paging of a specific person or group. The message goes to the channel; it is IH's
  responsibility to see it.
- No changes to the existing Datadog workflow Slack heads-up. Whether to keep or retire it is
  decided later.
- Not checking Beta / RC / dev Kotlin builds (builder's call, see below).

## Anti-goals (would make this a failure even if it "works")

- Daily repeat of the same Slack message for a version that was already reported.
- A green result that doesn't actually exercise the KCP path (e.g. a build where the Datadog
  plugin silently skips instrumentation, or the Kotlin override silently doesn't apply).
- A check that only works on someone's laptop.

## Current state (from repo recon)

- KCP is split per Kotlin line: `dd-sdk-android-gradle-plugin-kcp-{common,kotlin20,kotlin21,kotlin22,kotlin24}`.
  The Gradle plugin picks the module by Kotlin version (`DatadogKotlinCompilerPluginSupport.kt:97`).
- `gradle/libs.versions.toml` holds `kotlin20..kotlin24`; newest is `kotlin24 = "2.4.20"`.
- The existing `instrumented` module is **not** a valid harness: it wires the KCP directly via
  `kotlinCompilerPluginClasspath(project(":dd-sdk-android-gradle-plugin-kcp-kotlin22"))` and uses the
  repo's Kotlin plugin (2.3.20), bypassing the Gradle plugin's routing.
- `HelloworldApplication` (local only, no git remote) is the proven manual harness: AGP + Kotlin
  Compose plugin + Datadog Gradle plugin `*-SNAPSHOT` from `mavenLocal()`, Kotlin version in
  `libs.versions.toml`. It has stale Kotlin pins (`2.1.0`) in `settings.gradle.kts` and root
  `buildscript` that must not be copied.
- CI is GitLab. `ci/pipelines/check-release-pipeline.yml` is the template to copy: it is selected
  by `$CI_PIPELINE_KEY`, runs on a schedule, and posts to Slack with the
  `slack-notifier/v3-sdm` template's `postmessage`.
- Past fixes come in two shapes:
  - **Patch release breaks an existing line**: small IR fix inside an existing module
    (`c39c456` for 2.2.20, `b3683a9` for 2.3.20).
  - **New minor line**: new `kcp-kotlinXX` module + routing + tests
    (`8b04302`, `66b3904`, `26deff2` for 2.4.x).

## Design

### 1. Compat sample project (new)

A new, purpose-built, minimal Gradle build checked into this repo (suggested:
`samples/kotlin-compat/`, **not** included in the root `settings.gradle.kts`). It replaces
HelloworldApplication entirely (user decision: drop the local app, nothing is ported from it).
Keep it as small as possible; it exists only to answer "does the KCP compile with Kotlin X":

- Android application, Compose enabled, Kotlin Compose compiler plugin, Datadog Gradle plugin
  with `composeInstrumentation = InstrumentationMode.AUTO`.
- Compose code covering what the KCP transforms: composables with modifiers (tag injection) and
  a `NavHost` (nav-host transformer).
- Kotlin version comes from a single Gradle property (e.g. `-PkotlinVersion=2.5.0`), used for
  both `org.jetbrains.kotlin.android` and `org.jetbrains.kotlin.plugin.compose`. No other Kotlin
  pins anywhere in the sample.
- Plugin consumed as `<current version>-SNAPSHOT` from `mavenLocal()` after
  `./gradlew publishToMavenLocal` on the root build — mirrors the proven manual flow and
  guarantees the KCP artifacts resolve the same way they do for customers. *(Builder's call: if
  `includeBuild` with dependency substitution works reliably for the plugin + KCP artifacts,
  that's acceptable instead.)*
- The build must fail loudly if instrumentation didn't run (e.g. assert the plugin logged/applied
  the expected `kcp-kotlinXX` artifact, or inspect compiled bytecode for the injected call).
  How exactly is the builder's call; the requirement is that a "skip" can't look like a pass.

Check command (what "compatible" means): `assembleDebug` of the sample succeeds with the given
Kotlin version. *(Builder's call: also running the KCP module unit tests against the new version
is nice-to-have, not required.)*

### 2. Version detection

- Read `https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml`,
  take the highest **stable** version (no `-Beta`, `-RC`, `-dev`, etc.).
- If that version was already reported (pass or fail), exit quietly.
- State for "already reported" is the builder's call (GitLab CI cache, a project-level CI
  variable, or a marker file in a dedicated branch). Requirement: at most one Slack message per
  Kotlin version per outcome; a later pass for a previously failing version (after a fix is
  merged) may post one ✅ follow-up.
- Classify the version as **patch** or **new minor line** *(builder's call, settled in review)*.
  Note that `KotlinVersion.from()` routes every version ≥ 2.4 (including 2.5, 3.0) to
  `KOTLIN24`, so routing can't be the signal. Instead: a version is a **new minor line** when its
  `major.minor` is higher than the highest `major.minor` among the `kotlinXX` entries in
  `gradle/libs.versions.toml` (today `kotlin24 = "2.4.20"` → 2.4). This stays correct
  automatically because each new-line PR adds a `kotlinXX` entry.
- A new minor line that builds fine posts ✅ with a caveat line: "routed to `kcp-kotlin24`; a
  dedicated module may be needed — please review." It is not treated as a failure.

### 2b. Telling KCP failures apart from everything else *(builder's call, settled in review)*

A new Kotlin release can break the sample for reasons unrelated to the KCP (KGP/AGP mismatch,
Compose plugin mismatch, network flakes). To avoid spending the single failure message on the
wrong cause:

- **Infra failures** (`publishToMavenLocal` fails, dependency resolution / network errors): the
  job fails without recording the version as reported, so it retries on the next scheduled run.
  If this persists, the normal CI failure path applies.
- **Control build**: when the sample fails with the Datadog plugin, build it again with the
  Datadog plugin disabled (e.g. `-PdisableDatadog=true`).
  - Control passes → the failure is the KCP: post the failure message with the Bits Dev prompt.
  - Control also fails → post a different message: "Kotlin <v> fails to build the sample even
    without the Datadog plugin (toolchain incompatibility, likely AGP/Compose); KCP status
    unknown." No Bits Dev prompt. This is recorded as an outcome; a later pass or KCP failure
    for the same version can still post.
- The sample's AGP version is pinned to the repo's `androidToolsPlugin` from
  `gradle/libs.versions.toml`. It does not track the newest AGP, so we're testing the setup we
  support.

Outcome states tracked per version: `pass`, `pass-new-line`, `kcp-fail`, `toolchain-fail`.

### 3. GitLab pipeline

New file `ci/pipelines/kotlin-compat-pipeline.yml`, included from `.gitlab-ci.yml` with
`rules: - if: '$CI_PIPELINE_KEY == "kotlin-compat"'`, mirroring `check-release`.

Jobs:
1. `kotlin-compat:check` — detect version (exit 0 with "nothing new" when applicable), publish
   plugin to mavenLocal, build the sample with `-PkotlinVersion=<v>`, save the build log as an
   artifact, write a small result file (version, classification, pass/fail, first errors).
2. `kotlin-compat:notify` — extends `.slack-notifier-base`, posts to `$SLACK_OPS_CHANNEL` (`#dd-sdk-android-ops`) based on
   the result file.

A GitLab pipeline schedule (daily, on `develop`, `CI_PIPELINE_KEY=kotlin-compat`) is created
manually in the GitLab UI; document the steps in the PR.

### 4. Slack messages

On failure:

```
:warning: Kotlin <version> breaks the Datadog Compose compiler plugin
Type: patch on 2.4 line | new minor line (needs new kcp-kotlinXX module)
Failed task: <task>
<first ~20 lines of compiler errors, in a code block>
Job: <GitLab job URL>

To fix, reply in thread:
@Bits Dev fix Kotlin <version> compatibility in DataDog/dd-sdk-android-gradle-plugin.
Errors and job link are above. Follow .claude/skills/kotlin-compat/SKILL.md.
```

On pass (once per version): a short `:white_check_mark: Kotlin <version> is compatible with the
Datadog Compose compiler plugin (<job URL>)`.

No user or group mentions.

### 5. `kotlin-compat` skill

`.claude/skills/kotlin-compat/SKILL.md`, used by Bits Dev and by developers locally
(`/kotlin-compat 2.5.0`). Content:

- Module map: which `kcp-kotlinXX` module serves which Kotlin range and where routing lives.
- How to reproduce: the exact sample build command with `-PkotlinVersion`.
- The two fix shapes, pointing at the reference commits above.
- For a patch fix: bump the matching `kotlinXX` entry in `libs.versions.toml` if the module's
  compiler dependency needs the new version; add/adjust tests like
  `CommandLineProcessorLinkage24Test`.
- For a new minor line: follow the 2.4 sequence (module setup → implementation → routing → tests).
- Rules: never lower Kotlin versions or delete/skip tests to get green; run detekt + ktlint; branch
  name `<name>/<description>`; open a PR against `develop`.

## Decided by the user

- Detection via a scheduled GitLab pipeline in this repo (not triggered from the Datadog workflow).
- CI does not fix anything; on failure it posts with version and error
  details and proposes calling Bits Dev.
- No IH identification or mention in the message.
- Drop HelloworldApplication; build a simple, dedicated test app for this check.

## Builder's calls (marked as such above)

- Sample location/name, exact Compose content, and how "instrumentation ran" is asserted.
- mavenLocal vs `includeBuild` for consuming the plugin.
- Storage for "already reported" state.
- Stable-only detection (RC excluded).
- Whether KCP unit tests also run against the new version.
- Exact Slack wording.
- New-minor-line detection based on `kotlinXX` entries in `libs.versions.toml`; a passing new
  line posts ✅ with a caveat.
- A control build without the Datadog plugin to separate KCP failures from toolchain failures;
  infra failures retry silently; AGP pinned to `androidToolsPlugin`.

## Slack channel

User decision: post to `#dd-sdk-android-ops` via the existing `$SLACK_OPS_CHANNEL` variable for
now, like the other CI notifications. Moving to `#dd-sdk-android` may come later. Keep the channel
configurable through a variable so that move doesn't need a pipeline change.

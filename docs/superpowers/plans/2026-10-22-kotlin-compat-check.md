# Kotlin Compatibility Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A daily GitLab pipeline that detects a new stable Kotlin release, builds a minimal Compose app against it with the Datadog Gradle plugin from source, and posts the result to `#dd-sdk-android-ops` — with a Bits Dev prompt on KCP failures.

**Architecture:** A fully self-contained Gradle project (`kotlin-compat-app/`, own wrapper, catalog and properties, consuming the plugin only from mavenLocal) takes the Kotlin version as a property and verifies the KCP actually instrumented the bytecode. A stdlib-only Python script (`ci/scripts/kotlin_compat.py`) does detection, runs the builds, classifies the outcome, and writes the Slack message plus the next state file. A new GitLab pipeline (selected by `CI_PIPELINE_KEY == "kotlin-compat"`) runs the script, then a slack-notifier job posts the message and republishes the state as its artifact (state is read back from the latest successful notify job's artifacts via `CI_JOB_TOKEN`).

**Tech Stack:** Gradle Kotlin DSL, AGP (`androidToolsPlugin` from the repo catalog), Jetpack Compose, Python 3 stdlib (`unittest`, `urllib`, `xml.etree`), GitLab CI, `slack-notifier/v3-sdm` template.

**Spec:** `docs/superpowers/specs/2026-10-22-kotlin-compat-check-design.md`

## Global Constraints

- Slack channel: `$SLACK_OPS_CHANNEL` (`#dd-sdk-android-ops`, defined in `.gitlab-ci.yml`). No user or group mentions.
- CI never runs an AI agent, never creates branches, commits, or PRs.
- Only **stable** Kotlin versions are checked (reject any version containing `-`, e.g. `-Beta1`, `-RC2`, `-dev-123`).
- At most one Slack message per (Kotlin version, outcome). Outcomes: `pass`, `pass-new-line`, `kcp-fail`, `toolchain-fail`.
- Infra failures (plugin publish fails, dependency resolution / network errors) record nothing and exit non-zero, so the next scheduled run retries.
- `kotlin-compat-app/` is fully self-contained: own `settings.gradle.kts`, wrapper, `gradle/libs.versions.toml`, `gradle.properties`. No file in it may contain `../`. Never add it to the root `settings.gradle.kts`; always run it as `cd kotlin-compat-app && ./gradlew ...`.
- No Kotlin version is pinned anywhere in the test app except via `-PkotlinVersion`. AGP, Compose BOM, navigation, and Datadog SDK versions are pinned in the test app's own catalog (initial values = the root catalog's current `androidToolsPlugin`, `composeBom`, `androidXComposeNavigation`, `activityCompose`, `datadogSdk`).
- Python: stdlib only (no `requests`), so it runs in the CI image without pip installs.
- License header on every new source file, copied from existing files (`ci/scripts/dogfood.py` for Python, any `.kt`/`.kts` for Kotlin).
- Branch naming for any human/Bits Dev fix: `<name>/<description>`.

## Review Focus

1. **The `-PkotlinVersion` override silently not applying** (e.g. Gradle picks a Kotlin plugin from elsewhere) → the build must fail. Pinned by Task 1 Step 4 (KGP version mismatch check).
2. **Datadog plugin applied but KCP skipped** (unsupported-version early return, `composeInstrumentation` not set) → the build must fail, not pass. Pinned by Task 1 Step 5 (instrumentation off → `verifyKotlinCompat` fails).
3. **First run with no previous state artifact (HTTP 404)** → treated as empty state, not a crash. Pinned by Task 2 test `test_load_state_missing_is_empty`.
4. **Latest Kotlin is already covered by `libs.versions.toml`** (e.g. 2.4.20 today) → quiet exit, no Slack message. Pinned by Task 2 test `test_classify_known`.
5. **Huge or error-free failure logs** (Gradle failed without `e: ` lines, e.g. AGP error) → message still contains a bounded excerpt (≤ 20 lines). Pinned by Task 2 test `test_extract_errors_falls_back_to_what_went_wrong`.

---

### Task 1: Self-contained compat test app

**Files:**
- Create: `kotlin-compat-app/settings.gradle.kts`
- Create: `kotlin-compat-app/build.gradle.kts`
- Create: `kotlin-compat-app/gradle.properties`
- Create: `kotlin-compat-app/gradle/libs.versions.toml`
- Create: `kotlin-compat-app/gradlew`, `kotlin-compat-app/gradlew.bat`, `kotlin-compat-app/gradle/wrapper/gradle-wrapper.{jar,properties}` (copy the root wrapper files; same Gradle distribution URL)
- Create: `kotlin-compat-app/src/main/AndroidManifest.xml`
- Create: `kotlin-compat-app/src/main/kotlin/com/datadog/kotlincompat/MainActivity.kt`
- Create: `kotlin-compat-app/README.md`

**Interfaces:**
- Produces: invoked as
  `cd kotlin-compat-app && ./gradlew assembleDebug verifyKotlinCompat -PkotlinVersion=<v> -PddPluginVersion=<p> [-PdisableDatadog=true] --no-daemon --stacktrace`
  - `kotlinVersion` (required): used for both `org.jetbrains.kotlin.android` and `org.jetbrains.kotlin.plugin.compose`.
  - `ddPluginVersion` (required): version of `com.datadoghq.dd-sdk-android-gradle-plugin`, resolved from `mavenLocal()`.
  - `disableDatadog` (optional, default `false`): when `true`, the Datadog plugin is not applied.
  - Task `verifyKotlinCompat` (depends on `compileDebugKotlin`): fails if `getKotlinPluginVersion()` ≠ `kotlinVersion`; when Datadog is enabled, also fails unless the compiled classes under the debug Kotlin output dir contain both strings `instrumentedDatadog` and `InstrumentedNavigationViewTrackingEffect` (raw byte search of `.class` files — they appear in the constant pool).

- [ ] **Step 1: Write the settings file**

`settings.gradle.kts`:
- `pluginManagement.repositories`: the same proxy block as the root `settings.gradle.kts` (`gradlePluginProxy`, `mavenRepositoryProxy`), then `mavenLocal()`, `google()`, `gradlePluginPortal()`, `mavenCentral()`.
- `pluginManagement.plugins`: `org.jetbrains.kotlin.android` and `org.jetbrains.kotlin.plugin.compose` at `kotlinVersion`, `com.datadoghq.dd-sdk-android-gradle-plugin` at `ddPluginVersion`. Read both with `providers.gradleProperty(...).orNull ?: error("Pass -P<name>=...")`.
- `dependencyResolutionManagement`: same proxy repos, `mavenLocal()`, `google()`, `mavenCentral()`. The default `gradle/libs.versions.toml` in the folder is picked up automatically as `libs` — do not configure any other catalog.
- `rootProject.name = "kotlin-compat-app"`; single project, no subprojects.

- [ ] **Step 2: Write the catalog, build file, properties, manifest, and app code**

`gradle/libs.versions.toml` (own copy, no Kotlin entries): versions `agp`, `composeBom`, `activityCompose`, `navigationCompose`, `datadogSdk` with the initial values from Global Constraints; libraries `androidx-compose-bom`, `androidx-ui`, `androidx-material3`, `androidx-activity-compose`, `androidx-navigation-compose`, `datadog-rum`, `datadog-compose`; plugin `android-application`.

`gradle.properties`: `android.useAndroidX=true`, `android.builtInKotlin=false`, `android.newDsl=false`, `org.gradle.jvmargs=-Xmx4g`.

`build.gradle.kts`:
- `plugins`: `alias(libs.plugins.android.application)`, `id("org.jetbrains.kotlin.android")`, `id("org.jetbrains.kotlin.plugin.compose")`, `id("com.datadoghq.dd-sdk-android-gradle-plugin") apply false`.
- If `disableDatadog` is not `"true"`: `apply(plugin = "com.datadoghq.dd-sdk-android-gradle-plugin")` and configure `DdExtension` with `composeInstrumentation = InstrumentationMode.AUTO`.
- `android`: namespace/applicationId `com.datadog.kotlincompat`, compileSdk/minSdk/targetSdk `36`, Java 17, `buildFeatures.compose = true`.
- Dependencies from the catalog: `platform(libs.androidx.compose.bom)`, `libs.androidx.ui`, `libs.androidx.material3`, `libs.androidx.activity.compose`, `libs.androidx.navigation.compose`, `libs.datadog.rum`, `libs.datadog.compose`.
- Register `verifyKotlinCompat` as specified in Interfaces.

`MainActivity.kt`: a `ComponentActivity` whose content has a composable taking a `Modifier` (e.g. `Text("hello", modifier = Modifier.padding(8.dp))`) and a `NavHost` built from `rememberNavController()` with two `composable(...)` destinations. Nothing else — no Datadog SDK initialization needed for compilation.

`README.md`: 10 lines max — what the app is for, the command from Interfaces, and that it is intentionally isolated from the plugin build (no shared config, never reference `../`).

- [ ] **Step 3: Publish the plugin and run the happy path**

Run:
```bash
./gradlew publishToMavenLocal -Pdd-skip-signing --no-daemon
cd kotlin-compat-app && ./gradlew assembleDebug verifyKotlinCompat -PkotlinVersion=2.4.20 -PddPluginVersion=1.32.0-SNAPSHOT --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3b: Prove isolation**

Run: `grep -rn '\.\./' kotlin-compat-app --exclude-dir=build --exclude-dir=.gradle`
Expected: no output.

- [ ] **Step 4: Prove the version check bites**

Temporarily change the `kotlin.android` plugin version in `pluginManagement.plugins` to a literal `"2.3.20"` and run with `-PkotlinVersion=2.4.20`.
Expected: `verifyKotlinCompat` FAILS with a message naming both versions. Revert.

- [ ] **Step 5: Prove the instrumentation check bites**

Temporarily set `composeInstrumentation = InstrumentationMode.DISABLE` and rerun Step 3's test app command.
Expected: `verifyKotlinCompat` FAILS naming the missing symbol(s). Revert.

- [ ] **Step 6: Control build passes**

Run Step 3's test app command with `-PdisableDatadog=true`.
Expected: `BUILD SUCCESSFUL` (instrumentation check skipped, version check still runs).

- [ ] **Step 7: Commit**

```bash
git add kotlin-compat-app
git commit -m "Add self-contained Kotlin compatibility test app for the Compose KCP"
```

---

### Task 2: Detection, classification, and message logic (pure functions)

**Files:**
- Create: `ci/scripts/kotlin_compat.py`
- Test: `ci/scripts/test_kotlin_compat.py`

**Interfaces:**
- Produces (all in `kotlin_compat.py`):
  - `parse_version(v: str) -> tuple[int, int, int]`
  - `latest_stable(metadata_xml: str) -> str` — highest version under `<versions>` with no `-`.
  - `@dataclass(frozen=True) class Supported: key: str; version: str` — e.g. `Supported("kotlin24", "2.4.20")`.
  - `highest_supported(toml_text: str) -> Supported` — max over lines matching `^kotlin(\d{2})\s*=\s*"([^"]+)"` (ignores `kotlinComposePlugin` etc.).
  - `classify(version: str, supported: Supported) -> str` — `"known"` if `parse_version(version) <= parse_version(supported.version)`; `"new-line"` if its `(major, minor)` > supported's; else `"patch"`.
  - `decide_outcome(datadog_ok: bool, control_ok: bool | None, kind: str) -> str` — `datadog_ok` → `"pass-new-line"` if `kind == "new-line"` else `"pass"`; not ok and `control_ok` → `"kcp-fail"`; not ok and not `control_ok` → `"toolchain-fail"`.
  - `is_infra_failure(log: str) -> bool` — true if log contains any of: `Could not resolve`, `Could not GET`, `Could not HEAD`, `Connection reset`, `Connect timed out`, `Read timed out`, `Received status code 429`, `Received status code 5`.
  - `failed_task(log: str) -> str | None` — first match of `Execution failed for task '([^']+)'`.
  - `extract_errors(log: str, max_lines: int = 20) -> str` — Kotlin compiler error lines (start with `e: `); if none, the lines after `* What went wrong:` up to the next `* ` header; if neither, the last `max_lines` lines. Always truncated to `max_lines`.
  - `parse_plugin_version(maven_config_kt: str) -> str` — from `Version(1, 32, 0, Version.Type.Snapshot)` → `"1.32.0-SNAPSHOT"`; `Version.Type.Release` → `"1.32.0"`.
  - `should_notify(state: dict[str, str], version: str, outcome: str) -> bool` — `state.get(version) != outcome`.
  - `find_outside_references(root: Path) -> list[str]` — `"relative/path:line"` for every line containing `../` in text files under `root`, skipping `build/`, `.gradle/` and `.kotlin/` dirs and files that aren't valid UTF-8 (e.g. `gradle-wrapper.jar`).
  - `build_message(version: str, outcome: str, supported: Supported, task: str | None, errors: str, job_url: str) -> str`

- [ ] **Step 1: Write the failing tests**

`test_kotlin_compat.py` (`unittest`, imports `kotlin_compat` from the same directory via `sys.path`):

```python
def test_latest_stable_skips_prereleases(self):
    xml = "<metadata><versioning><versions><version>2.4.20</version><version>2.5.0-RC1</version><version>2.4.21</version><version>2.5.0-dev-1</version></versions></versioning></metadata>"
    self.assertEqual(latest_stable(xml), "2.4.21")

def test_latest_stable_orders_numerically(self):  # "2.10.0" > "2.9.0"
    ...assertEqual(latest_stable(<2.9.0, 2.10.0>), "2.10.0")

def test_highest_supported_ignores_other_kotlin_keys(self):
    toml = 'kotlin20 = "2.0.21"\nkotlin24 = "2.4.20"\nkotlinComposePlugin = "2.1.21"\nkotlin23 = "2.3.20"\n'
    self.assertEqual(highest_supported(toml), Supported("kotlin24", "2.4.20"))

def test_classify_known(self):     classify("2.4.20", Supported("kotlin24", "2.4.20")) == "known"
def test_classify_patch(self):     classify("2.4.21", Supported("kotlin24", "2.4.20")) == "patch"
def test_classify_new_line(self):  classify("2.5.0",  Supported("kotlin24", "2.4.20")) == "new-line"
def test_classify_new_major(self): classify("3.0.0",  Supported("kotlin24", "2.4.20")) == "new-line"

def test_decide_outcome(self):
    decide_outcome(True, None, "patch") == "pass"
    decide_outcome(True, None, "new-line") == "pass-new-line"
    decide_outcome(False, True, "patch") == "kcp-fail"
    decide_outcome(False, False, "new-line") == "toolchain-fail"

def test_is_infra_failure(self):
    is_infra_failure("> Could not resolve com.datadoghq:dd-sdk-android-rum:3.14.0.") is True
    is_infra_failure("e: file.kt:3:5 Unresolved reference 'IrFoo'.") is False

def test_failed_task(self):
    failed_task("* What went wrong:\nExecution failed for task ':compileDebugKotlin'.") == ":compileDebugKotlin"

def test_extract_errors_prefers_compiler_errors(self):
    log of 30 "e: ..." lines plus noise → result has exactly 20 lines, all starting with "e: "

def test_extract_errors_falls_back_to_what_went_wrong(self):
    log = "noise\n* What went wrong:\nAGP requires Kotlin 2.x\n> details\n\n* Try:\n..."  → result contains "AGP requires Kotlin 2.x", not "* Try:"

def test_parse_plugin_version(self):
    parse_plugin_version("val VERSION = Version(1, 32, 0, Version.Type.Snapshot)") == "1.32.0-SNAPSHOT"
    parse_plugin_version("val VERSION = Version(1, 32, 0, Version.Type.Release)") == "1.32.0"

def test_find_outside_references(self):
    # in a TemporaryDirectory: "settings.gradle.kts" containing 'from(files("../../gradle/libs.versions.toml"))' on line 2,
    # "build/tmp/x.txt" containing "../", "gradle/wrapper/gradle-wrapper.jar" with bytes b"\xff\xfe../"
    self.assertEqual(find_outside_references(tmp), ["settings.gradle.kts:2"])

def test_should_notify(self):
    should_notify({}, "2.5.0", "kcp-fail") is True
    should_notify({"2.5.0": "kcp-fail"}, "2.5.0", "kcp-fail") is False
    should_notify({"2.5.0": "kcp-fail"}, "2.5.0", "pass") is True

def test_message_kcp_fail(self):
    msg = build_message("2.5.0", "kcp-fail", Supported("kotlin24", "2.4.20"), ":compileDebugKotlin", "e: boom", "https://gitlab/job/1")
    for s in [":warning: Kotlin 2.5.0 breaks the Datadog Compose compiler plugin",
              "new minor line (needs new kcp-kotlinXX module)", ":compileDebugKotlin",
              "```\ne: boom\n```", "https://gitlab/job/1",
              "@Bits Dev fix Kotlin 2.5.0 compatibility in DataDog/dd-sdk-android-gradle-plugin.",
              "Follow .claude/skills/kotlin-compat/SKILL.md."]:
        self.assertIn(s, msg)
    self.assertNotIn("<!", msg)  # no Slack mentions

def test_message_kcp_fail_patch_type(self):  # 2.4.21 → contains "Type: patch on 2.4 line"
def test_message_toolchain_fail(self):       # contains "fails to build the test app even without the Datadog plugin", "KCP status unknown", NOT "@Bits Dev"
def test_message_pass(self):                 # == ":white_check_mark: Kotlin 2.4.21 is compatible with the Datadog Compose compiler plugin (https://gitlab/job/1)"
def test_message_pass_new_line(self):        # pass text for 2.5.0 + "routed to `kcp-kotlin24`; a dedicated module may be needed — please review."
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `python3 -m unittest ci/scripts/test_kotlin_compat.py -v`
Expected: FAIL / ERROR — `ModuleNotFoundError: No module named 'kotlin_compat'`.

- [ ] **Step 3: Implement the functions listed in Interfaces in `ci/scripts/kotlin_compat.py`**

Message copy (exact, from the spec):

```
kcp-fail:
:warning: Kotlin {v} breaks the Datadog Compose compiler plugin
Type: {"patch on {major}.{minor} line" | "new minor line (needs new kcp-kotlinXX module)"}
Failed task: {task or "unknown"}
```{errors}```   (fenced, on its own lines)
Job: {job_url}

To fix, reply in thread:
@Bits Dev fix Kotlin {v} compatibility in DataDog/dd-sdk-android-gradle-plugin. Errors and job link are above. Follow .claude/skills/kotlin-compat/SKILL.md.

toolchain-fail:
:warning: Kotlin {v} fails to build the test app even without the Datadog plugin (toolchain incompatibility, likely AGP/Compose); KCP status unknown.
Failed task / errors block / Job lines as above, no Bits Dev lines.

pass:          :white_check_mark: Kotlin {v} is compatible with the Datadog Compose compiler plugin ({job_url})
pass-new-line: pass line + "\nRouted to `kcp-{supported.key}`; a dedicated module may be needed — please review."
```

(The `{major}.{minor}` in the patch line is the checked version's.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `python3 -m unittest ci/scripts/test_kotlin_compat.py -v`
Expected: all tests `ok`.

- [ ] **Step 5: Commit**

```bash
git add ci/scripts/kotlin_compat.py ci/scripts/test_kotlin_compat.py
git commit -m "Add Kotlin compatibility detection and message logic"
```

---

### Task 3: Orchestration entry point

**Files:**
- Modify: `ci/scripts/kotlin_compat.py` (add I/O functions and `main`)
- Test: `ci/scripts/test_kotlin_compat.py`

**Interfaces:**
- Consumes: everything from Task 2; the test app command from Task 1.
- Produces: `python3 ci/scripts/kotlin_compat.py --out-dir kotlin-compat` run from the repo root. Writes into `--out-dir`:
  - `state.json` — always written on exit code 0: previous state, updated with `{version: outcome}` when a build ran.
  - `message.txt` — written only when `should_notify(...)` is true; absent otherwise.
  - `build-datadog.log`, `build-control.log` — when those builds ran.
  - Exit codes: `0` = done (nothing new, or outcome recorded); `1` = infra failure (nothing recorded).
- New functions:
  - `load_state(fetch: Callable[[], bytes]) -> dict[str, str]` — `fetch` raises `urllib.error.HTTPError`; a 404 → `{}`. Other errors propagate.
  - `fetch_previous_state() -> bytes` — GET `$CI_API_V4_URL/projects/$CI_PROJECT_ID/jobs/artifacts/$CI_DEFAULT_BRANCH/raw/kotlin-compat/state.json?job=kotlin-compat:notify` with header `JOB-TOKEN: $CI_JOB_TOKEN`. If any of those env vars is unset or empty (local run), raise an `HTTPError` with code 404 so `load_state` returns `{}`.
  - `run_gradle(args: list[str], log_path: Path, cwd: Path) -> bool` — runs `./gradlew` inside `cwd` (repo root for publishing, `kotlin-compat-app/` for the test app) with `args`, tees combined output to `log_path` and stdout, returns success.

- [ ] **Step 1: Write the failing tests**

```python
def test_load_state_missing_is_empty(self):
    def fetch(): raise urllib.error.HTTPError("u", 404, "Not Found", {}, None)
    self.assertEqual(load_state(fetch), {})

def test_load_state_parses_json(self):
    self.assertEqual(load_state(lambda: b'{"2.5.0": "kcp-fail"}'), {"2.5.0": "kcp-fail"})

def test_load_state_other_http_errors_propagate(self):
    500 → assertRaises(urllib.error.HTTPError)
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `python3 -m unittest ci/scripts/test_kotlin_compat.py -v`
Expected: the three new tests ERROR with `NameError`/`ImportError` for `load_state`.

- [ ] **Step 3: Implement `load_state`, `fetch_previous_state`, `run_gradle`, and `main`**

`main` flow:
0. `find_outside_references(Path("kotlin-compat-app"))` non-empty → print the offending lines, exit 1 (isolation broken; nothing recorded).
1. Load state; fetch Maven Central metadata (`https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml`); `version = latest_stable(...)`; `supported = highest_supported(gradle/libs.versions.toml)`; `kind = classify(...)`.
2. `kind == "known"` → write `state.json` unchanged, print "nothing new", exit 0.
3. `run_gradle(["publishToMavenLocal", "-Pdd-skip-signing", "--no-daemon"], out/"publish.log", cwd=repo_root)`; failure → exit 1.
4. Plugin version from `parse_plugin_version(tools/build-config/.../MavenConfig.kt)`.
5. Datadog build (Task 1 command, `cwd=kotlin-compat-app`); on failure, `is_infra_failure(log)` → exit 1; else control build with `-PdisableDatadog=true`; control failure that `is_infra_failure` → exit 1.
6. `outcome = decide_outcome(...)`; if `should_notify` write `message.txt` with `build_message(..., job_url=$CI_JOB_URL)` using the failing log (Datadog log for `kcp-fail`, control log for `toolchain-fail`).
7. `state[version] = outcome`; write `state.json`; exit 0.

- [ ] **Step 4: Run tests to verify they pass**

Run: `python3 -m unittest ci/scripts/test_kotlin_compat.py -v`
Expected: all `ok`.

- [ ] **Step 5: Local end-to-end smoke run**

Temporarily lower `kotlin24` in `gradle/libs.versions.toml` to `"2.4.0"` (so the current 2.4.x release is "new"), then run with no CI env vars:
`python3 ci/scripts/kotlin_compat.py --out-dir /tmp/kc`
Expected: exit 0; `/tmp/kc/state.json` contains the version with `"pass"`; `/tmp/kc/message.txt` is the `:white_check_mark:` line. Revert the TOML change.

- [ ] **Step 6: Commit**

```bash
git add ci/scripts/kotlin_compat.py ci/scripts/test_kotlin_compat.py
git commit -m "Add Kotlin compatibility check orchestration"
```

---

### Task 4: GitLab pipeline

**Files:**
- Create: `ci/pipelines/kotlin-compat-pipeline.yml`
- Modify: `.gitlab-ci.yml` (`include:` section, after the `check-release` entry)

**Interfaces:**
- Consumes: `python3 ci/scripts/kotlin_compat.py --out-dir kotlin-compat` (Task 3).

- [ ] **Step 1: Add the include**

```yaml
  - local: 'ci/pipelines/kotlin-compat-pipeline.yml'
    rules:
      - if: '$CI_PIPELINE_KEY == "kotlin-compat"'
```

- [ ] **Step 2: Write the pipeline, mirroring `ci/pipelines/check-release-pipeline.yml`**

- Include `slack-notifier/v3-sdm/template.yml`; stages `check`, `notify`.
- `kotlin-compat:check`: `tags: ["arch:amd64"]`, `image: $CI_IMAGE_DOCKER`, `timeout: 1h`, `KUBERNETES_MEMORY_LIMIT: "20Gi"`; script runs `python3 -m unittest ci/scripts/test_kotlin_compat.py` then the entry point; artifacts `paths: [kotlin-compat/]`, `when: always`, `expire_in: 1 week`.
- `kotlin-compat:notify`: `extends: .slack-notifier-base`, `needs: [kotlin-compat:check]`, `when: on_success`; script: if `kotlin-compat/message.txt` exists, `postmessage "$SLACK_OPS_CHANNEL" "$(cat kotlin-compat/message.txt)"`; artifacts `paths: [kotlin-compat/state.json]`, `expire_in: 1 year`. (This job's artifact is the state `fetch_previous_state` reads — it only exists when the post succeeded.)
- `notify:report-failure-to-slack`: copy of the one in `check-release-pipeline.yml`, `when: on_failure`, text `":status_alert: Kotlin compatibility check pipeline failed (infra?). $BUILD_URL"`.

- [ ] **Step 3: Lint the YAML**

Run: `python3 -c "import yaml,sys; [yaml.safe_load(open(f)) for f in sys.argv[1:]]" .gitlab-ci.yml ci/pipelines/kotlin-compat-pipeline.yml`
Expected: no output, exit 0. (If PyYAML is missing locally, use GitLab's CI Lint page on the pushed branch instead.)

- [ ] **Step 4: Commit**

```bash
git add .gitlab-ci.yml ci/pipelines/kotlin-compat-pipeline.yml
git commit -m "Add scheduled Kotlin compatibility pipeline"
```

---

### Task 5: `kotlin-compat` skill and rollout notes

**Files:**
- Create: `.claude/skills/kotlin-compat/SKILL.md` (add only this file; `.claude/` has untracked local files)
- Modify: `kotlin-compat-app/README.md` (rollout section)

- [ ] **Step 1: Write the skill**

Frontmatter: `name: kotlin-compat`, `description: Use when a new Kotlin release breaks the Datadog Compose compiler plugin (KCP), e.g. from a #dd-sdk-android-ops kotlin-compat alert, to reproduce and fix it.`

Body (content from the spec §5):
- Module map: `kcp-kotlin20` (1.9.23+–2.0), `kcp-kotlin21` (2.1), `kcp-kotlin22` (2.2–2.3), `kcp-kotlin24` (2.4+); routing in `dd-sdk-android-gradle-plugin/src/main/kotlin/com/datadog/gradle/plugin/kcp/KotlinVersion.kt` and `DatadogKotlinCompilerPluginSupport.kt`.
- Reproduce: the publish + test app commands from Task 1.
- Patch-fix shape: reference `c39c456`, `b3683a9`; bump the matching `kotlinXX` in `gradle/libs.versions.toml` when the module's compiler dependency needs it; add/adjust linkage tests like `CommandLineProcessorLinkage24Test`.
- New-minor-line shape: follow `8b04302` → `66b3904` → `26deff2` (module setup → implementation → routing → tests).
- Rules: never lower Kotlin versions, delete or skip tests to get green; run `./gradlew detekt ktlintCheck` and the module tests; branch `<name>/<description>`; PR against `develop`.

- [ ] **Step 2: Add rollout notes to the test app README**

Section "Scheduling": create a GitLab pipeline schedule on `develop`, daily, variable `CI_PIPELINE_KEY=kotlin-compat`; note the slack-notifier bot already posts to `#dd-sdk-android-ops`.

- [ ] **Step 3: Verify the skill loads**

Run: `claude -p "/kotlin-compat 2.4.21 — only describe the steps you would take, do not edit files"` from the repo root.
Expected: the answer references the module map and the test app command.

- [ ] **Step 4: Commit**

```bash
git add .claude/skills/kotlin-compat/SKILL.md kotlin-compat-app/README.md
git commit -m "Add kotlin-compat skill and scheduling notes"
```

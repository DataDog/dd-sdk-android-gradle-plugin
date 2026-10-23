#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2019-Present Datadog, Inc

"""
Checks whether the latest stable Kotlin release is compatible with the Datadog Compose
compiler plugin (KCP), by building kotlin-compat-app against it. Writes the Slack message to
post (if any) and the updated state, which are consumed by the kotlin-compat GitLab pipeline.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ElementTree
from argparse import ArgumentParser
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Dict, List, Optional, Tuple

INFRA_FAILURE_MARKERS = [
    "Could not resolve",
    "Could not GET",
    "Could not HEAD",
    "Connection reset",
    "Connect timed out",
    "Read timed out",
    "Received status code 429",
    "Received status code 5",
    # Network errors before Gradle starts, e.g. while the wrapper downloads the distribution
    "UnknownHostException",
    "Connection refused",
    "Server returned HTTP response code: 5",
]

IGNORED_DIRS = {"build", ".gradle", ".kotlin"}

KOTLIN_METADATA_URL = (
    "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml"
)

REPO_ROOT = Path(__file__).resolve().parents[2]
TEST_APP_DIR = REPO_ROOT / "kotlin-compat-app"
VERSIONS_TOML = REPO_ROOT / "gradle" / "libs.versions.toml"
MAVEN_CONFIG_KT = REPO_ROOT / "tools/build-config/src/main/kotlin/com/datadog/gradle/config/MavenConfig.kt"

STATE_FILE = "state.json"
PRERELEASE_STAGES = {"Beta": 1, "RC": 2}
STABLE_STAGE = 3
# State is kept in this project's generic package registry, written only by this pipeline
# (CI_JOB_TOKEN can read and write it); a GET returns the most recently uploaded file.
# TEMP(e2e): separate test state; revert before merging
STATE_PACKAGE_PATH = "packages/generic/kotlin-compat-state-test/1.0.0/state.json"

MAX_ERROR_LINES = 20


@dataclass(frozen=True)
class Supported:
    """Newest Kotlin version known by the plugin build, e.g. key `kotlin24`, version `2.4.20`."""
    key: str
    version: str


def parse_version(version: str) -> Tuple[int, int, int]:
    """Numeric part of a version, ignoring any pre-release suffix: `2.5.0-Beta2` -> (2, 5, 0)."""
    major, minor, patch = version.split("-")[0].split(".")[:3]
    return int(major), int(minor), int(patch)


def is_prerelease(version: str) -> bool:
    return "-" in version


def version_key(version: str) -> Tuple[int, int, int, int, int]:
    """Sort key ordering `2.5.0-Beta1 < 2.5.0-Beta2 < 2.5.0-RC < 2.5.0-RC2 < 2.5.0`."""
    match = re.fullmatch(r"(\d+\.\d+\.\d+)(?:-(Beta|RC)(\d*))?", version)
    if match is None:
        raise ValueError(f"Unsupported Kotlin version format: {version}")
    numbers, stage, stage_number = match.groups()
    if stage is None:
        return parse_version(numbers) + (STABLE_STAGE, 0)
    return parse_version(numbers) + (PRERELEASE_STAGES[stage], int(stage_number or 1))


def latest_stable(metadata_xml: str) -> str:
    versions = [
        node.text.strip()
        for node in ElementTree.fromstring(metadata_xml).iter("version")
        if node.text and "-" not in node.text
    ]
    return max(versions, key=parse_version)


def latest_prerelease(metadata_xml: str, stable: str) -> Optional[str]:
    """Latest Beta or RC newer than the latest stable version, if any. Dev builds are ignored."""
    prereleases = [
        node.text.strip()
        for node in ElementTree.fromstring(metadata_xml).iter("version")
        if node.text and re.fullmatch(r"\d+\.\d+\.\d+-(Beta|RC)\d*", node.text.strip())
    ]
    newer = [version for version in prereleases if version_key(version) > version_key(stable)]
    return max(newer, key=version_key) if newer else None


def highest_supported(toml_text: str) -> Supported:
    entries = [
        Supported(f"kotlin{match.group(1)}", match.group(2))
        for match in re.finditer(r'^kotlin(\d{2})\s*=\s*"([^"]+)"', toml_text, re.MULTILINE)
    ]
    return max(entries, key=lambda entry: parse_version(entry.version))


def classify(version: str, supported: Supported) -> str:
    if version_key(version) <= version_key(supported.version):
        return "known"
    if parse_version(version)[:2] > parse_version(supported.version)[:2]:
        return "new-line"
    return "patch"


def decide_outcome(datadog_ok: bool, control_ok: Optional[bool], kind: str) -> str:
    if datadog_ok:
        return "pass-new-line" if kind == "new-line" else "pass"
    return "kcp-fail" if control_ok else "toolchain-fail"


def is_infra_failure(log: str) -> bool:
    return any(marker in log for marker in INFRA_FAILURE_MARKERS)


def failed_task(log: str) -> Optional[str]:
    match = re.search(r"Execution failed for task '([^']+)'", log)
    return match.group(1) if match else None


def extract_errors(log: str, max_lines: int = MAX_ERROR_LINES) -> str:
    lines = log.splitlines()
    compiler_errors = [line for line in lines if line.startswith("e: ")]
    if compiler_errors:
        return "\n".join(compiler_errors[:max_lines])
    if "* What went wrong:" in lines:
        section = []
        for line in lines[lines.index("* What went wrong:") + 1:]:
            if line.startswith("* "):
                break
            section.append(line)
        section_lines = "\n".join(section).strip("\n").split("\n")
        return "\n".join(section_lines[:max_lines])
    return "\n".join(lines[-max_lines:])


def parse_plugin_version(maven_config_kt: str) -> str:
    match = re.search(r"Version\((\d+),\s*(\d+),\s*(\d+),\s*Version\.Type\.(\w+)\)", maven_config_kt)
    if match is None:
        raise ValueError("Cannot find the plugin version in MavenConfig.kt")
    major, minor, patch, version_type = match.groups()
    suffix = "-SNAPSHOT" if version_type == "Snapshot" else ""
    return f"{major}.{minor}.{patch}{suffix}"


def should_notify(state: Dict[str, str], version: str, outcome: str) -> bool:
    return state.get(version) != outcome


def find_outside_references(root: Path) -> List[str]:
    references = []
    for path in sorted(root.rglob("*")):
        relative = path.relative_to(root)
        if not path.is_file() or IGNORED_DIRS.intersection(relative.parts):
            continue
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for number, line in enumerate(content.splitlines(), start=1):
            if "../" in line:
                references.append(f"{relative.as_posix()}:{number}")
    return references


def message_file_name(version: str) -> str:
    return f"message-{version}.txt"


def build_message(
    version: str,
    outcome: str,
    supported: Supported,
    task: Optional[str],
    errors: str,
    job_url: str
) -> str:
    label = f"Kotlin {version}" + (" (pre-release)" if is_prerelease(version) else "")
    if outcome in ("pass", "pass-new-line"):
        message = f":white_check_mark: {label} is compatible with the Datadog Compose compiler plugin ({job_url})"
        if outcome == "pass-new-line":
            message += (
                f"\nCaveat: routed to `kcp-{supported.key}`; a dedicated module may be needed — please review."
            )
        return message

    details = f"Failed task: {task or 'unknown'}\n```\n{errors}\n```\nJob: {job_url}"
    if outcome == "toolchain-fail":
        return (
            f":warning: {label} fails to build the test app even without the Datadog plugin "
            f"(toolchain incompatibility, likely AGP/Compose); KCP status unknown.\n{details}"
        )

    major, minor, _ = parse_version(version)
    if classify(version, supported) == "new-line":
        kind = "new minor line (needs new kcp-kotlinXX module)"
    else:
        kind = f"patch on {major}.{minor} line"
    # A broken stable release is an error; a broken pre-release is only a heads-up.
    emoji = ":warning:" if is_prerelease(version) else ":x:"
    return (
        f"{emoji} {label} breaks the Datadog Compose compiler plugin\n"
        f"Type: {kind}\n"
        f"{details}\n\n"
        "To fix, reply in thread:\n"
        f"@Bits Dev fix Kotlin {version} compatibility in DataDog/dd-sdk-android-gradle-plugin. "
        "Errors and job link are above. Follow .claude/skills/kotlin-compat/SKILL.md."
    )


def load_state(fetch: Callable[[], bytes]) -> Dict[str, str]:
    try:
        return json.loads(fetch())
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return {}
        raise


def state_url(env: Dict[str, str]) -> Optional[str]:
    if not all(env.get(name) for name in ("CI_API_V4_URL", "CI_PROJECT_ID", "CI_JOB_TOKEN")):
        return None
    return f"{env['CI_API_V4_URL']}/projects/{env['CI_PROJECT_ID']}/{STATE_PACKAGE_PATH}"


def fetch_previous_state() -> bytes:
    url = state_url(dict(os.environ))
    if url is None:
        raise urllib.error.HTTPError("", 404, "Not running in GitLab CI, no previous state", {}, None)
    request = urllib.request.Request(url, headers={"JOB-TOKEN": os.environ["CI_JOB_TOKEN"]})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read()


def save_state(state_file: Path) -> None:
    url = state_url(dict(os.environ))
    if url is None:
        raise RuntimeError("Saving the state requires CI_API_V4_URL, CI_PROJECT_ID and CI_JOB_TOKEN")
    request = urllib.request.Request(
        url, data=state_file.read_bytes(), method="PUT", headers={"JOB-TOKEN": os.environ["CI_JOB_TOKEN"]}
    )
    with urllib.request.urlopen(request, timeout=30):
        print(f"Saved {state_file} to the package registry")


def run_gradle(args: List[str], log_path: Path, cwd: Path) -> bool:
    print(f"$ ./gradlew {' '.join(args)}  (in {cwd})", flush=True)
    with open(log_path, "w") as log:
        process = subprocess.Popen(
            ["./gradlew"] + args, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True
        )
        for line in process.stdout:
            sys.stdout.write(line)
            log.write(line)
        return process.wait() == 0


def check_version(
    version: str,
    kind: str,
    plugin_version: str,
    out_dir: Path
) -> Optional[Tuple[str, str]]:
    """Builds the test app with this Kotlin version. Returns (outcome, failing log), None on infra failure."""
    build_args = [
        "assembleDebug", "verifyKotlinCompat",
        f"-PkotlinVersion={version}", f"-PddPluginVersion={plugin_version}",
        "--no-daemon", "--stacktrace",
    ]
    datadog_log = out_dir / f"build-datadog-{version}.log"
    control_log = out_dir / f"build-control-{version}.log"
    if run_gradle(build_args, datadog_log, TEST_APP_DIR):
        return decide_outcome(True, None, kind), ""
    if is_infra_failure(datadog_log.read_text()):
        print(f"Infra failure while building Kotlin {version} with the Datadog plugin, will retry next run")
        return None
    control_ok = run_gradle(build_args + ["-PdisableDatadog=true"], control_log, TEST_APP_DIR)
    if not control_ok and is_infra_failure(control_log.read_text()):
        print(f"Infra failure while building Kotlin {version} without the Datadog plugin, will retry next run")
        return None
    outcome = decide_outcome(False, control_ok, kind)
    return outcome, (datadog_log if outcome == "kcp-fail" else control_log).read_text()


def main() -> int:
    parser = ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--out-dir", type=Path, help="run the check, writing state, message and logs here")
    mode.add_argument("--save-state", type=Path, help="upload this state file, after the message was posted")
    args = parser.parse_args()
    if args.save_state:
        save_state(args.save_state)
        return 0
    out_dir = args.out_dir.resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    outside_references = find_outside_references(TEST_APP_DIR)
    if outside_references:
        print("kotlin-compat-app must not reference files outside of its folder:")
        print("\n".join(outside_references))
        return 1

    state = load_state(fetch_previous_state)
    with urllib.request.urlopen(KOTLIN_METADATA_URL, timeout=30) as response:
        metadata_xml = response.read().decode("utf-8")
    stable = latest_stable(metadata_xml)
    prerelease = latest_prerelease(metadata_xml, stable)
    supported = highest_supported(VERSIONS_TOML.read_text())
    print(f"Latest stable Kotlin: {stable}, latest pre-release: {prerelease or 'none newer'}, "
          f"newest supported: {supported.version} ({supported.key})")

    versions = [version for version in (stable, prerelease)
                if version and classify(version, supported) != "known"]
    if not versions:
        print("Nothing new to check")
    elif not run_gradle(["publishToMavenLocal", "-Pdd-skip-signing", "--no-daemon"],
                        out_dir / "publish.log", REPO_ROOT):
        print("Publishing the plugin to mavenLocal failed")
        return 1

    plugin_version = parse_plugin_version(MAVEN_CONFIG_KT.read_text())
    for version in versions:
        result = check_version(version, classify(version, supported), plugin_version, out_dir)
        if result is None:
            return 1
        outcome, log = result
        print(f"Outcome for Kotlin {version}: {outcome}")
        if should_notify(state, version, outcome):
            message = build_message(
                version, outcome, supported, failed_task(log), extract_errors(log) if log else "",
                os.environ.get("CI_JOB_URL", "(local run)")
            )
            (out_dir / message_file_name(version)).write_text(message)
        state[version] = outcome

    (out_dir / STATE_FILE).write_text(json.dumps(state, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())

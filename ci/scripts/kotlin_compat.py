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

import re
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

INFRA_FAILURE_MARKERS = [
    "Could not resolve",
    "Could not GET",
    "Could not HEAD",
    "Connection reset",
    "Connect timed out",
    "Read timed out",
    "Received status code 429",
    "Received status code 5",
]

IGNORED_DIRS = {"build", ".gradle", ".kotlin"}

MAX_ERROR_LINES = 20


@dataclass(frozen=True)
class Supported:
    """Newest Kotlin version known by the plugin build, e.g. key `kotlin24`, version `2.4.20`."""
    key: str
    version: str


def parse_version(version: str) -> Tuple[int, int, int]:
    major, minor, patch = version.split(".")[:3]
    return int(major), int(minor), int(patch)


def latest_stable(metadata_xml: str) -> str:
    versions = [
        node.text.strip()
        for node in ElementTree.fromstring(metadata_xml).iter("version")
        if node.text and "-" not in node.text
    ]
    return max(versions, key=parse_version)


def highest_supported(toml_text: str) -> Supported:
    entries = [
        Supported(f"kotlin{match.group(1)}", match.group(2))
        for match in re.finditer(r'^kotlin(\d{2})\s*=\s*"([^"]+)"', toml_text, re.MULTILINE)
    ]
    return max(entries, key=lambda entry: parse_version(entry.version))


def classify(version: str, supported: Supported) -> str:
    checked = parse_version(version)
    known = parse_version(supported.version)
    if checked <= known:
        return "known"
    if checked[:2] > known[:2]:
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


def build_message(
    version: str,
    outcome: str,
    supported: Supported,
    task: Optional[str],
    errors: str,
    job_url: str
) -> str:
    if outcome in ("pass", "pass-new-line"):
        message = f":white_check_mark: Kotlin {version} is compatible with the Datadog Compose compiler plugin ({job_url})"
        if outcome == "pass-new-line":
            message += (
                f"\nCaveat: routed to `kcp-{supported.key}`; a dedicated module may be needed — please review."
            )
        return message

    details = f"Failed task: {task or 'unknown'}\n```\n{errors}\n```\nJob: {job_url}"
    if outcome == "toolchain-fail":
        return (
            f":warning: Kotlin {version} fails to build the test app even without the Datadog plugin "
            f"(toolchain incompatibility, likely AGP/Compose); KCP status unknown.\n{details}"
        )

    major, minor, _ = parse_version(version)
    if classify(version, supported) == "new-line":
        kind = "new minor line (needs new kcp-kotlinXX module)"
    else:
        kind = f"patch on {major}.{minor} line"
    return (
        f":warning: Kotlin {version} breaks the Datadog Compose compiler plugin\n"
        f"Type: {kind}\n"
        f"{details}\n\n"
        "To fix, reply in thread:\n"
        f"@Bits Dev fix Kotlin {version} compatibility in DataDog/dd-sdk-android-gradle-plugin. "
        "Errors and job link are above. Follow .claude/skills/kotlin-compat/SKILL.md."
    )

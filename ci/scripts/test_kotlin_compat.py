#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2019-Present Datadog, Inc

import os
import sys
import unittest
import urllib.error
from pathlib import Path
from tempfile import TemporaryDirectory

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from kotlin_compat import (  # noqa: E402
    Supported,
    build_message,
    classify,
    decide_outcome,
    extract_errors,
    failed_task,
    find_outside_references,
    highest_supported,
    is_infra_failure,
    latest_stable,
    load_state,
    parse_plugin_version,
    should_notify,
    state_url,
)

SUPPORTED = Supported("kotlin24", "2.4.20")
JOB_URL = "https://gitlab/job/1"


def metadata(*versions: str) -> str:
    tags = "".join(f"<version>{v}</version>" for v in versions)
    return f"<metadata><versioning><versions>{tags}</versions></versioning></metadata>"


class DetectionTest(unittest.TestCase):

    def test_latest_stable_skips_prereleases(self):
        xml = metadata("2.4.20", "2.5.0-RC1", "2.4.21", "2.5.0-dev-1")
        self.assertEqual(latest_stable(xml), "2.4.21")

    def test_latest_stable_orders_numerically(self):
        self.assertEqual(latest_stable(metadata("2.10.0", "2.9.0")), "2.10.0")

    def test_highest_supported_ignores_other_kotlin_keys(self):
        toml = 'kotlin20 = "2.0.21"\nkotlin24 = "2.4.20"\nkotlinComposePlugin = "2.1.21"\nkotlin23 = "2.3.20"\n'
        self.assertEqual(highest_supported(toml), Supported("kotlin24", "2.4.20"))

    def test_classify_known(self):
        self.assertEqual(classify("2.4.20", SUPPORTED), "known")

    def test_classify_patch(self):
        self.assertEqual(classify("2.4.21", SUPPORTED), "patch")

    def test_classify_new_line(self):
        self.assertEqual(classify("2.5.0", SUPPORTED), "new-line")

    def test_classify_new_major(self):
        self.assertEqual(classify("3.0.0", SUPPORTED), "new-line")

    def test_parse_plugin_version(self):
        self.assertEqual(
            parse_plugin_version("val VERSION = Version(1, 32, 0, Version.Type.Snapshot)"),
            "1.32.0-SNAPSHOT"
        )
        self.assertEqual(
            parse_plugin_version("val VERSION = Version(1, 32, 0, Version.Type.Release)"),
            "1.32.0"
        )

    def test_find_outside_references(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "settings.gradle.kts").write_text(
                'rootProject.name = "app"\nfrom(files("../../gradle/libs.versions.toml"))\n'
            )
            (root / "build" / "tmp").mkdir(parents=True)
            (root / "build" / "tmp" / "x.txt").write_text("../")
            (root / "gradle" / "wrapper").mkdir(parents=True)
            (root / "gradle" / "wrapper" / "gradle-wrapper.jar").write_bytes(b"\xff\xfe../")
            self.assertEqual(find_outside_references(root), ["settings.gradle.kts:2"])


class OutcomeTest(unittest.TestCase):

    def test_decide_outcome(self):
        self.assertEqual(decide_outcome(True, None, "patch"), "pass")
        self.assertEqual(decide_outcome(True, None, "new-line"), "pass-new-line")
        self.assertEqual(decide_outcome(False, True, "patch"), "kcp-fail")
        self.assertEqual(decide_outcome(False, False, "new-line"), "toolchain-fail")

    def test_is_infra_failure(self):
        self.assertTrue(is_infra_failure("> Could not resolve com.datadoghq:dd-sdk-android-rum:3.14.0."))
        self.assertFalse(is_infra_failure("e: file.kt:3:5 Unresolved reference 'IrFoo'."))

    def test_is_infra_failure_before_gradle_starts(self):
        self.assertTrue(is_infra_failure('Exception in thread "main" java.net.UnknownHostException: services.gradle.org'))
        self.assertTrue(is_infra_failure("java.net.ConnectException: Connection refused"))
        self.assertTrue(is_infra_failure("Server returned HTTP response code: 503 for URL: https://services.gradle.org"))

    def test_should_notify(self):
        self.assertTrue(should_notify({}, "2.5.0", "kcp-fail"))
        self.assertFalse(should_notify({"2.5.0": "kcp-fail"}, "2.5.0", "kcp-fail"))
        self.assertTrue(should_notify({"2.5.0": "kcp-fail"}, "2.5.0", "pass"))


class LogParsingTest(unittest.TestCase):

    def test_failed_task(self):
        log = "* What went wrong:\nExecution failed for task ':compileDebugKotlin'."
        self.assertEqual(failed_task(log), ":compileDebugKotlin")

    def test_extract_errors_prefers_compiler_errors(self):
        log = "noise\n" + "\n".join(f"e: file.kt:{i}:1 boom" for i in range(30)) + "\nmore noise\n"
        lines = extract_errors(log).splitlines()
        self.assertEqual(len(lines), 20)
        self.assertTrue(all(line.startswith("e: ") for line in lines))

    def test_extract_errors_falls_back_to_what_went_wrong(self):
        log = "noise\n* What went wrong:\nAGP requires Kotlin 2.x\n> details\n\n* Try:\n> Run with --stacktrace\n"
        errors = extract_errors(log)
        self.assertIn("AGP requires Kotlin 2.x", errors)
        self.assertNotIn("* Try:", errors)


class MessageTest(unittest.TestCase):

    def test_message_kcp_fail(self):
        msg = build_message("2.5.0", "kcp-fail", SUPPORTED, ":compileDebugKotlin", "e: boom", JOB_URL)
        for expected in [
            ":warning: Kotlin 2.5.0 breaks the Datadog Compose compiler plugin",
            "new minor line (needs new kcp-kotlinXX module)",
            ":compileDebugKotlin",
            "```\ne: boom\n```",
            JOB_URL,
            "@Bits Dev fix Kotlin 2.5.0 compatibility in DataDog/dd-sdk-android-gradle-plugin.",
            "Follow .claude/skills/kotlin-compat/SKILL.md.",
        ]:
            self.assertIn(expected, msg)
        self.assertNotIn("<!", msg)

    def test_message_kcp_fail_patch_type(self):
        msg = build_message("2.4.21", "kcp-fail", SUPPORTED, ":compileDebugKotlin", "e: boom", JOB_URL)
        self.assertIn("Type: patch on 2.4 line", msg)

    def test_message_toolchain_fail(self):
        msg = build_message("2.5.0", "toolchain-fail", SUPPORTED, ":compileDebugKotlin", "e: boom", JOB_URL)
        self.assertIn("fails to build the test app even without the Datadog plugin", msg)
        self.assertIn("KCP status unknown", msg)
        self.assertNotIn("@Bits Dev", msg)

    def test_message_pass(self):
        msg = build_message("2.4.21", "pass", SUPPORTED, None, "", JOB_URL)
        self.assertEqual(
            msg,
            ":white_check_mark: Kotlin 2.4.21 is compatible with the Datadog Compose compiler plugin ({})".format(JOB_URL)
        )

    def test_message_pass_new_line(self):
        msg = build_message("2.5.0", "pass-new-line", SUPPORTED, None, "", JOB_URL)
        self.assertIn(":white_check_mark: Kotlin 2.5.0 is compatible with the Datadog Compose compiler plugin", msg)
        self.assertIn("routed to `kcp-kotlin24`; a dedicated module may be needed — please review.", msg)


class StateTest(unittest.TestCase):

    def test_state_url_uses_project_package_registry(self):
        env = {"CI_API_V4_URL": "https://gitlab/api/v4", "CI_PROJECT_ID": "42", "CI_JOB_TOKEN": "t"}
        self.assertEqual(
            state_url(env),
            "https://gitlab/api/v4/projects/42/packages/generic/kotlin-compat-state-test/1.0.0/state.json"
        )

    def test_state_url_outside_ci_is_none(self):
        self.assertIsNone(state_url({"CI_API_V4_URL": "", "CI_PROJECT_ID": "42", "CI_JOB_TOKEN": "t"}))

    def test_load_state_missing_is_empty(self):
        def fetch():
            raise urllib.error.HTTPError("u", 404, "Not Found", {}, None)
        self.assertEqual(load_state(fetch), {})

    def test_load_state_parses_json(self):
        self.assertEqual(load_state(lambda: b'{"2.5.0": "kcp-fail"}'), {"2.5.0": "kcp-fail"})

    def test_load_state_other_http_errors_propagate(self):
        def fetch():
            raise urllib.error.HTTPError("u", 500, "Server Error", {}, None)
        with self.assertRaises(urllib.error.HTTPError):
            load_state(fetch)


if __name__ == "__main__":
    unittest.main()

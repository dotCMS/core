#!/usr/bin/env python3
"""Safety tests for the temporary, branch-only live LICENSE workflow sandbox."""

import os
from pathlib import Path
import runpy
import subprocess
import unittest

HERE = Path(__file__).resolve().parent
WORKFLOW = HERE.parent / "test_license-date-sandbox.yml"
extract = runpy.run_path(str(HERE / "update-license-date.test.py"))["step_script"]
extract.__globals__["WORKFLOW"] = WORKFLOW


class SandboxSafetyTests(unittest.TestCase):
    def guard(self, base, head, mode="direct", ref="test-38014-license-date-workflow"):
        return subprocess.run(
            ["bash", "-euo", "pipefail", "-c", extract("validate-sandbox")],
            env=dict(os.environ, DEFAULT_BRANCH=base, PR_BRANCH=head, MODE=mode,
                     GITHUB_REF_NAME=ref, CI_MACHINE_TOKEN="placeholder"),
            text=True, capture_output=True,
        )

    def test_permits_only_sandbox_branches(self):
        for mode in ("direct", "pr"):
            result = self.guard(f"sandbox/license-date-38014-123-{mode}",
                                f"sandbox/license-date-38014-123-{mode}-update", mode)
            self.assertEqual(result.returncode, 0, result.stderr)

    def test_refuses_main_and_production_automation_branch(self):
        for base, head in (("main", "sandbox/license-date-38014-123-direct-update"),
                           ("sandbox/license-date-38014-123-direct", "automation/update-license-date")):
            self.assertNotEqual(self.guard(base, head).returncode, 0)

    def test_refuses_other_trigger_branch_or_mode(self):
        base = "sandbox/license-date-38014-123-direct"
        head = base + "-update"
        self.assertNotEqual(self.guard(base, head, ref="main").returncode, 0)
        self.assertNotEqual(self.guard(base, head, mode="unexpected").returncode, 0)

    def test_only_push_trigger_and_explicit_sandbox_pr_targets(self):
        workflow = WORKFLOW.read_text()
        self.assertIn("branches: [test-38014-license-date-workflow]", workflow)
        self.assertNotIn("workflow_dispatch:", workflow)
        self.assertNotIn("pull_request:", workflow)
        self.assertNotIn("schedule:", workflow)
        self.assertIn("ref: ${{ github.sha }}", workflow)
        self.assertIn("base: ${{ env.DEFAULT_BRANCH }}", workflow)
        self.assertIn("branch: ${{ env.PR_BRANCH }}", workflow)
        self.assertIn("mode: [direct, pr]", workflow)
        self.assertIn('step_script("publish")', workflow)
        self.assertIn('step_script("update-license")', workflow)


if __name__ == "__main__":
    unittest.main()

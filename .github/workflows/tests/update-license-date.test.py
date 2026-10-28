#!/usr/bin/env python3
"""Offline workflow tests. Run: python3 .github/workflows/tests/update-license-date.test.py"""

import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
WORKFLOW = ROOT / ".github/workflows/cicd_scheduled_update-license-date.yml"


def step_script(step_id):
    """Extract a workflow run block so tests execute the actual shell code."""
    workflow = WORKFLOW.read_text()
    step = re.search(
        rf"^      - .*\n(?:(?!      - ).*\n)*?        id: {re.escape(step_id)}\n"
        r"(?:(?!      - ).*\n)*?        run: \|\n((?:          .*\n|\n)+)",
        workflow,
        re.MULTILINE,
    )
    if not step:
        raise AssertionError(f"Missing run block for {step_id}")
    return "\n".join(line[10:] for line in step[1].splitlines())


class LicenseWorkflowTests(unittest.TestCase):
    def test_schedule_and_default_branch(self):
        workflow = WORKFLOW.read_text()
        self.assertIn("cron: '0 23 * * 6'", workflow)
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn("ref: ${{ github.event.repository.default_branch }}", workflow)
        self.assertIn("cancel-in-progress: false", workflow)

    def test_direct_push_uses_job_token_not_machine_token(self):
        workflow = WORKFLOW.read_text()
        checkout = workflow.split("      - name: Checkout default branch\n", 1)[1].split("      - name:", 1)[0]
        self.assertIn("token: ${{ github.token }}", checkout)
        self.assertNotIn("CI_MACHINE_TOKEN", checkout)
        self.assertNotIn("pull-requests: write", workflow)
        self.assertIn("token: ${{ secrets.CI_MACHINE_TOKEN }}", workflow)

    def test_update_only_changes_date_and_is_idempotent(self):
        script = step_script("update-license")
        with tempfile.TemporaryDirectory() as directory:
            repo = Path(directory)
            subprocess.run(["git", "init", "-q", directory], check=True)
            destination = repo / ".github/actions/update-license-date.sh"
            destination.parent.mkdir(parents=True)
            destination.write_bytes((ROOT / ".github/actions/update-license-date.sh").read_bytes())
            license_file = repo / "LICENSE"
            original = (ROOT / "LICENSE").read_bytes()
            license_file.write_bytes(original)
            # Freeze only this test's clock; the real script inherits the exported
            # bash function. This also exercises leap-day formatting without racing
            # a second date call at UTC midnight or altering the production script.
            today = "February 29, 2024"
            env = dict(os.environ, TZ="UTC", LC_ALL="C", FIXED_DATE=today)
            clock_stub = 'date() { printf "%s\\n" "$FIXED_DATE"; }; export -f date;\n'
            expected = re.sub(
                rb"(?m)^Change Date:.*$",
                f"Change Date:     Four years from {today}".encode(),
                original,
            )
            for _ in range(2):
                subprocess.run(["bash", "-euo", "pipefail", "-c", clock_stub + script], cwd=repo, env=env, check=True)
                self.assertEqual(license_file.read_bytes(), expected)

    def run_publish(self, changed=True, push_succeeds=True):
        """Stub git; neither this test nor its shell code can publish remotely."""
        script = step_script("publish")
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "output"
            calls = Path(directory) / "calls"
            env = dict(
                os.environ,
                GITHUB_OUTPUT=str(output),
                CALLS=str(calls),
                DEFAULT_BRANCH="main",
                CI_MACHINE_USER="dotcmsmachineuser",
                DIFF_STATUS="1" if changed else "0",
                PUSH_STATUS="0" if push_succeeds else "1",
            )
            stub = '''git() {
  printf '%s\\n' "$*" >> "$CALLS"
  case "$1" in
    diff) return "$DIFF_STATUS" ;;
    push) return "$PUSH_STATUS" ;;
    *) return 0 ;;
  esac
}
'''
            subprocess.run(["bash", "-euo", "pipefail", "-c", stub + script], env=env, check=True)
            return output.read_text(), calls.read_text()

    def test_unchanged_license_does_not_commit_or_push(self):
        output, calls = self.run_publish(changed=False)
        self.assertIn("create_pr=false", output)
        self.assertNotIn("commit", calls)
        self.assertNotIn("push", calls)

    def test_successful_direct_push_does_not_create_pr(self):
        output, calls = self.run_publish()
        self.assertIn("create_pr=false", output)
        self.assertIn("add -- LICENSE", calls)
        self.assertIn("config user.name dotcmsmachineuser", calls)
        self.assertIn("push origin HEAD:refs/heads/main", calls)
        self.assertNotIn("--force", calls)
        self.assertNotIn("reset", calls)

    def test_rejected_push_restores_changes_for_pr_action(self):
        output, calls = self.run_publish(push_succeeds=False)
        self.assertIn("create_pr=true", output)
        self.assertIn("reset --soft HEAD^", calls)
        self.assertNotIn("reset --hard", calls)

    def test_pr_fallback_uses_machine_credentials_and_stable_branch(self):
        workflow = WORKFLOW.read_text()
        self.assertIn("if: steps.publish.outputs.create_pr == 'true'", workflow)
        self.assertIn("uses: peter-evans/create-pull-request@", workflow)
        self.assertIn("token: ${{ secrets.CI_MACHINE_TOKEN }}", workflow)
        self.assertIn("branch: automation/update-license-date", workflow)
        self.assertIn("add-paths: LICENSE", workflow)
        self.assertIn("base: ${{ github.event.repository.default_branch }}", workflow)
        self.assertIn("secrets.CI_MACHINE_USER || 'dotcmsmachineuser'", workflow)

    def test_generated_pr_references_license_tracking_issue(self):
        workflow = WORKFLOW.read_text()
        pr_body = workflow.split("          body: |\n", 1)[1]
        self.assertIn("Refs #31844", pr_body)
        self.assertNotIn("Closes #31844", pr_body)

    def test_missing_fallback_token_fails_with_clear_error(self):
        result = subprocess.run(
            ["bash", "-euo", "pipefail", "-c", step_script("check-pr-token")],
            env=dict(os.environ, CI_MACHINE_TOKEN=""), text=True, capture_output=True,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("CI_MACHINE_TOKEN", result.stdout + result.stderr)

    def test_available_fallback_token_passes(self):
        subprocess.run(
            ["bash", "-euo", "pipefail", "-c", step_script("check-pr-token")],
            env=dict(os.environ, CI_MACHINE_TOKEN="test-placeholder"), check=True,
        )


if __name__ == "__main__":
    unittest.main()

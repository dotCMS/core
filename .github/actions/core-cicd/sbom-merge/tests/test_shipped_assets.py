"""US1 — FR-005: the guard for "what ships" vs "what the manifest calls production".

This test does not exercise the merge. It pins an assumption about the repository that
the design leans on, so the assumption fails loudly instead of silently.

The assumption: core-web's Angular build copies exactly two packages out of node_modules
verbatim, and both arrive in the image with their package.json intact, so the Syft scan
already inventories them. That is what lets the frontend inventory use a production-only
filter without losing a shipped component — even though monaco-editor is declared under
devDependencies.

It is a coincidence, not a design guarantee. A third copied asset, or one the bundler
inlines instead of copying, would break it. See research.md R6.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[5]
PROJECT_JSON = REPO_ROOT / "core-web" / "apps" / "dotcms-ui" / "project.json"

EXPECTED_COPIED_FROM_NODE_MODULES = {
    "node_modules/tinymce",
    "node_modules/monaco-editor",
}


def _copied_asset_inputs() -> set[str]:
    assets = json.loads(PROJECT_JSON.read_text())["targets"]["build"]["options"]["assets"]
    return {
        a["input"]
        for a in assets
        if isinstance(a, dict) and (a.get("input") or "").startswith("node_modules/")
    }


def test_project_json_is_where_we_think_it_is():
    assert PROJECT_JSON.is_file(), f"expected the dotcms-ui project at {PROJECT_JSON}"


def test_shipped_node_modules_assets_are_unchanged():
    """If this fails, do not just update the constant — decide whether the new asset is a
    production dependency. If it is a devDependency, the production-only inventory will
    miss a component that ships, and FR-005 is violated."""
    assert _copied_asset_inputs() == EXPECTED_COPIED_FROM_NODE_MODULES


@pytest.mark.parametrize("package", sorted(EXPECTED_COPIED_FROM_NODE_MODULES))
def test_each_copied_asset_is_declared_somewhere(package):
    """A copied asset that is in no manifest at all would be invisible to both sources."""
    name = package.removeprefix("node_modules/")
    manifest = json.loads((REPO_ROOT / "core-web" / "package.json").read_text())
    declared = set(manifest.get("dependencies", {})) | set(manifest.get("devDependencies", {}))
    assert name in declared, f"{name} is copied into the build but declared nowhere"

"""Regressions from the review on PR #37655.

Grouped in one file because they share a theme: each is a case where the safety net had a
hole rather than the happy path being wrong.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest

from sbom_merge.cli import EXIT_INVALID_INPUT, EXIT_OK, COVERAGE_MARKER, main
from sbom_merge.merge import merge

FIXTURES = Path(__file__).parent / "fixtures"


# --- Non-object JSON must not crash the process -----------------------------------------

@pytest.mark.parametrize("payload", ["[]", "null", '"a string"', "42"])
def test_non_object_json_exits_with_the_documented_code(tmp_path, payload):
    """Valid JSON that is not an object used to reach `.get()` and raise AttributeError,
    which escaped the _InputError handler and exited 1 — outside the contract, with a
    traceback. A truncated or replaced artifact is exactly how this arrives."""
    broken = tmp_path / "broken.json"
    broken.write_text(payload)

    assert main([
        "--image", str(broken),
        "--frontend", str(FIXTURES / "pnpm-sample.json"),
        "--output", str(tmp_path / "out.json"),
    ]) == EXIT_INVALID_INPUT


# --- A corrupt frontend must degrade, not take the release down -------------------------

def test_malformed_frontend_degrades_rather_than_aborting(tmp_path, capsys):
    """`pnpm sbom > file` creates the file before the command runs, so a mid-stream failure
    leaves partial JSON behind — non-empty and unparseable. Treating that as fatal loses the
    Java/OS inventory too, which is precisely what FR-009 forbids."""
    partial = tmp_path / "partial.json"
    partial.write_text('{"bomFormat": "CycloneDX", "specVersion": "1.6", "compo')

    assert main([
        "--image", str(FIXTURES / "syft-sample.json"),
        "--frontend", str(partial),
        "--output", str(tmp_path / "out.json"),
    ]) == EXIT_OK

    captured = capsys.readouterr()
    assert f"{COVERAGE_MARKER}false" in captured.out
    # "Unreadable" and "absent" are different diagnoses and must read differently.
    assert "unreadable" in captured.err.lower()
    assert json.loads((tmp_path / "out.json").read_text())["components"]


def test_malformed_frontend_still_fails_when_asked_to(tmp_path):
    """--fail-on-missing-frontend must cover corrupt as well as absent, or it is not the
    strict mode it claims to be."""
    partial = tmp_path / "partial.json"
    partial.write_text("{oh no")

    assert main([
        "--image", str(FIXTURES / "syft-sample.json"),
        "--frontend", str(partial),
        "--output", str(tmp_path / "out.json"),
        "--fail-on-missing-frontend",
    ]) == EXIT_INVALID_INPUT


# --- The loss guard has to count, not just check membership -----------------------------

def test_loss_guard_detects_a_dropped_duplicate(image_doc, frontend_doc):
    """SC-009's guard keyed on a SET of (name, version), so losing one of two identical
    components left both tuples present and the check passed. The merge preserves duplicates
    today, so this was a hole in the net rather than a live bug — but a net with a hole in it
    is what lets the next regression through."""
    from sbom_merge.cli import _components_lost

    merged = merge(image_doc, frontend_doc)

    duplicated = None
    for component in merged["components"]:
        key = (component["name"], component["version"])
        if sum(1 for c in merged["components"] if (c["name"], c["version"]) == key) > 1:
            duplicated = key
            break
    assert duplicated, "fixture problem: no duplicate pair to drop"

    # Drop exactly one of the pair, leaving the other behind.
    lossy = dict(merged)
    removed = False
    kept = []
    for component in merged["components"]:
        if not removed and (component["name"], component["version"]) == duplicated:
            removed = True
            continue
        kept.append(component)
    lossy["components"] = kept

    assert _components_lost(image_doc, frontend_doc, lossy), (
        "a dropped duplicate went undetected: the guard is counting membership, not multiplicity"
    )


# --- Intra-source duplicates collapse on the frontend side too --------------------------

def test_duplicates_within_the_frontend_document_are_preserved(image_doc, frontend_doc):
    """The stated rule is that reconciliation happens BETWEEN sources, never within one.
    That was enforced for the image side but not the frontend side, where a second component
    with the same identity matched the first and merged into it.

    A pnpm lockfile lists each name@version once, so this has no practical impact today —
    but a rule enforced on one side only is not the rule the model claims."""
    doubled = dict(frontend_doc)
    doubled["components"] = frontend_doc["components"] + [dict(frontend_doc["components"][0])]

    merged = merge(image_doc, doubled)

    target = frontend_doc["components"][0]
    count = sum(
        1 for c in merged["components"]
        if (c["name"], c["version"]) == (target["name"], target["version"])
    )
    assert count == 2, f"intra-frontend duplicate collapsed: expected 2 entries, got {count}"

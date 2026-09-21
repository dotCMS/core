"""US2 — licence coverage (FR-002, SC-003).

Note on what is and is not asserted here. An earlier draft of these tests checked "at least
95% of components carry a licence" against the trimmed fixture. That is theatre: the fixture
is ten hand-picked components, so the ratio measures the trimming, not the pipeline. The
threshold is a property of the real 2,365-component tree and is verified by T027 / quickstart
step 2 against actual output.

What IS testable here, and what actually protects the story:
  - the merge never drops licence data it was given;
  - a component whose licence is unknown stays in the document;
  - the generation step is not configured in the one way that silently zeroes licences.
"""
from __future__ import annotations

import re
from pathlib import Path

import pytest
import yaml

from sbom_merge.merge import merge

ACTION_YML = (
    Path(__file__).resolve().parents[5]
    / ".github" / "actions" / "legacy-release" / "sbom-generator" / "action.yml"
)


def _run_scripts() -> str:
    """Every `run:` body in the action, concatenated.

    Deliberately not a grep over the raw file: the action *documents* why --lockfile-only
    is wrong, and a raw search matches that comment. Asserting against comments would make
    these tests fail for explaining themselves.
    """
    action = yaml.safe_load(ACTION_YML.read_text())
    lines = [
        line
        for step in action["runs"]["steps"]
        if isinstance(step.get("run"), str)
        for line in step["run"].splitlines()
        # Shell comments live inside the run block, and the action documents why
        # --lockfile-only is wrong. Matching prose would fail the action for explaining itself.
        if not line.lstrip().startswith("#")
    ]
    return "\n".join(lines)


def _licensed(doc: dict) -> dict[tuple[str, str], list]:
    return {
        (c["name"], c["version"]): c["licenses"]
        for c in doc.get("components", [])
        if c.get("licenses")
    }


def test_merge_preserves_every_licence_it_was_given(image_doc, licensed_frontend_doc):
    """SC-003 depends on licences surviving the merge, not just being generated."""
    merged = merge(image_doc, licensed_frontend_doc)
    after = _licensed(merged)

    before = _licensed(licensed_frontend_doc)
    assert before, "fixture problem: no licensed components to assert on"

    for key, licences in before.items():
        assert key in after, f"{key} lost its licence entirely"
        assert after[key] == licences, f"{key} licence changed: {licences} -> {after[key]}"


def test_licence_from_frontend_fills_a_gap_in_the_image_scan(image_doc, licensed_frontend_doc):
    """The image scan often knows a package exists without resolving its licence. When both
    sources describe the same component, the merge must take the licence rather than keep
    the emptier entry — that is half the point of merging instead of publishing two files."""
    unlicensed = dict(licensed_frontend_doc["components"][0])
    unlicensed.pop("licenses", None)

    image_with_gap = dict(image_doc)
    image_with_gap["components"] = image_doc["components"] + [unlicensed]

    merged = merge(image_with_gap, licensed_frontend_doc)
    entry = next(
        c for c in merged["components"]
        if (c["name"], c["version"]) == (unlicensed["name"], unlicensed["version"])
    )
    assert entry.get("licenses"), "the frontend licence did not fill the image scan's gap"


def test_component_without_a_licence_is_still_present(licensed_frontend_doc, image_doc):
    """US2 acceptance scenario 2 — an undetermined licence is a missing FIELD on a known
    component, never a missing component. Dropping it would understate the inventory."""
    unlicensed = [
        c for c in licensed_frontend_doc["components"] if not c.get("licenses")
    ]
    assert unlicensed, "fixture problem: no unlicensed component to assert on"

    merged = merge(image_doc, licensed_frontend_doc)
    present = {(c["name"], c["version"]) for c in merged["components"]}

    for component in unlicensed:
        key = (component["name"], component["version"])
        assert key in present, f"{key} was dropped for having no licence"
        entry = next(c for c in merged["components"] if (c["name"], c["version"]) == key)
        assert entry.get("purl"), f"{key} survived but lost its purl, so it cannot be looked up"


def test_generation_step_does_not_use_lockfile_only():
    """The regression guard for this whole story.

    `pnpm sbom --lockfile-only` returns the correct component count with ZERO licences,
    because licence text is read from the store. Measured: 2,365 components / 0 licences
    from the lockfile, versus 132 of 134 with a populated store. It looks right in every
    count-based check and silently destroys the licence audit.
    """
    assert "--lockfile-only" not in _run_scripts(), (
        "the frontend SBOM is generated with --lockfile-only, which yields zero licences"
    )


def test_generation_step_populates_the_store_before_generating():
    """The positive half of the previous test: `pnpm fetch` must precede `pnpm sbom`."""
    scripts = _run_scripts()
    fetch = scripts.find("pnpm fetch")
    sbom = scripts.find("pnpm sbom")

    assert fetch != -1, "no `pnpm fetch` step: the store will be empty and licences will be missing"
    assert sbom != -1, "no `pnpm sbom` step"
    assert fetch < sbom, "`pnpm fetch` must run before `pnpm sbom`"


@pytest.mark.parametrize("flag", ["--prod", "--sbom-spec-version 1.6"])
def test_generation_step_carries_required_flags(flag):
    """--prod bounds the inventory to what ships; the spec version is an external contract."""
    assert re.search(re.escape(flag), _run_scripts()), f"missing {flag}"

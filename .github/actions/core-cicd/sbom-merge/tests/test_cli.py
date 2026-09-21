"""US3 — CLI contract: exit codes, degraded mode, determinism.

Contract: specs/37575-core-web-npm-sbom/contracts/sbom-merge-cli.md
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest

from sbom_merge.cli import (
    COVERAGE_MARKER,
    EXIT_INVALID_INPUT,
    EXIT_OK,
    EXIT_SCHEMA_INVALID,
    main,
)

FIXTURES = Path(__file__).parent / "fixtures"


def _argv(tmp_path: Path, *, image="syft-sample.json", frontend="pnpm-sample.json", extra=()):
    return [
        "--image", str(FIXTURES / image) if image else "/nonexistent-image.json",
        "--frontend", str(FIXTURES / frontend) if frontend else "/nonexistent-frontend.json",
        "--output", str(tmp_path / "merged.json"),
        *extra,
    ]


def test_successful_merge_writes_output_and_reports_coverage(tmp_path, capsys):
    assert main(_argv(tmp_path)) == EXIT_OK

    assert f"{COVERAGE_MARKER}true" in capsys.readouterr().out
    written = json.loads((tmp_path / "merged.json").read_text())
    assert written["components"]


def test_missing_frontend_degrades_rather_than_failing(tmp_path, capsys):
    """FR-009 / SC-007 — a release from a branch without this change, or a frontend step
    that failed, must still publish the Java/OS inventory."""
    assert main(_argv(tmp_path, frontend=None)) == EXIT_OK

    captured = capsys.readouterr()
    assert f"{COVERAGE_MARKER}false" in captured.out
    assert "does not cover" in captured.err, "a degraded release must say so"

    written = json.loads((tmp_path / "merged.json").read_text())
    assert written["components"], "the image inventory must still be published"


def test_fail_on_missing_frontend_turns_degradation_into_an_error(tmp_path):
    assert main(
        _argv(tmp_path, frontend=None, extra=("--fail-on-missing-frontend",))
    ) == EXIT_INVALID_INPUT


def test_missing_image_is_an_error_not_a_degradation(tmp_path):
    """The image scan is not optional: without it there is no document at all."""
    assert main(_argv(tmp_path, image=None)) == EXIT_INVALID_INPUT


@pytest.mark.parametrize("bad", ["not json at all", '{"bomFormat": "SPDX"}'])
def test_malformed_input_is_rejected(tmp_path, bad):
    broken = tmp_path / "broken.json"
    broken.write_text(bad)

    assert main([
        "--image", str(broken),
        "--frontend", str(FIXTURES / "pnpm-sample.json"),
        "--output", str(tmp_path / "out.json"),
    ]) == EXIT_INVALID_INPUT


def test_mismatched_spec_version_is_rejected(tmp_path):
    """pnpm defaults to 1.7 while Syft emits 1.6. Merging them silently would change the
    published document's spec version, which downstream consumers rely on."""
    doc = json.loads((FIXTURES / "pnpm-sample.json").read_text())
    doc["specVersion"] = "1.7"
    wrong = tmp_path / "wrong.json"
    wrong.write_text(json.dumps(doc))

    assert main([
        "--image", str(FIXTURES / "syft-sample.json"),
        "--frontend", str(wrong),
        "--output", str(tmp_path / "out.json"),
    ]) == EXIT_INVALID_INPUT


def test_output_is_byte_identical_across_runs(tmp_path):
    """Determinism makes two releases' documents diffable, and makes a re-run safe."""
    first, second = tmp_path / "a.json", tmp_path / "b.json"

    for out in (first, second):
        assert main([
            "--image", str(FIXTURES / "syft-sample.json"),
            "--frontend", str(FIXTURES / "pnpm-sample.json"),
            "--output", str(out),
        ]) == EXIT_OK

    assert first.read_bytes() == second.read_bytes()


def test_empty_frontend_document_is_treated_as_absent(tmp_path, capsys):
    """An empty file is what a failed `pnpm sbom` leaves behind — the shell redirect
    creates the file before the command fails. Treating it as valid would publish a
    document claiming zero frontend components rather than admitting the gap."""
    empty = tmp_path / "empty.json"
    empty.write_text("")

    assert main([
        "--image", str(FIXTURES / "syft-sample.json"),
        "--frontend", str(empty),
        "--output", str(tmp_path / "out.json"),
    ]) == EXIT_OK
    assert f"{COVERAGE_MARKER}false" in capsys.readouterr().out


def test_schema_invalid_output_is_not_written(tmp_path):
    """SC-005 — a document that does not validate must never reach the output path.

    Exit 3 is distinct from exit 2 so the caller can tell "you gave me bad input" from
    "the merge produced something unpublishable".
    """
    doc = json.loads((FIXTURES / "syft-sample.json").read_text())
    doc.setdefault("metadata", {}).setdefault("component", {})["type"] = "not-a-real-type"
    broken = tmp_path / "broken-meta.json"
    broken.write_text(json.dumps(doc))

    out = tmp_path / "out.json"
    assert main([
        "--image", str(broken),
        "--frontend", str(FIXTURES / "pnpm-sample.json"),
        "--output", str(out),
    ]) == EXIT_SCHEMA_INVALID
    assert not out.exists(), "an unpublishable document was written anyway"

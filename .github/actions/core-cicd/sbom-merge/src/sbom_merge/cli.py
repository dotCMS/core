"""Command line entry point for sbom-merge.

Argument surface and exit codes are fixed by
specs/37575-core-web-npm-sbom/contracts/sbom-merge-cli.md.
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Sequence

from sbom_merge.merge import SPEC_VERSION, merge
from sbom_merge.schema import errors as schema_errors

# Distinct by contract: FR-009 requires a frontend-inventory failure to be distinguishable,
# which a single generic 1 would not satisfy.
EXIT_OK = 0
EXIT_INVALID_INPUT = 2
EXIT_SCHEMA_INVALID = 3
EXIT_INVARIANT_VIOLATED = 4

# Read by the calling workflow step so it can branch on a degraded release without parsing
# JSON in bash. Mirrors changelog-publisher's `::changelog-skip::`.
COVERAGE_MARKER = "::frontend-covered::"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="sbom-merge",
        description="Merge a frontend npm CycloneDX inventory into a Syft image-scan SBOM.",
    )
    parser.add_argument(
        "--image", required=True, help="CycloneDX JSON from the Syft image scan"
    )
    parser.add_argument(
        "--frontend", required=True, help="CycloneDX JSON from `pnpm sbom`"
    )
    parser.add_argument(
        "--output", required=True, help="Destination for the merged document"
    )
    parser.add_argument(
        "--fail-on-missing-frontend",
        action="store_true",
        help="Exit non-zero if --frontend is absent or empty instead of degrading",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    args = build_parser().parse_args(argv)

    try:
        image_doc = _load_required(Path(args.image), "image")
    except _InputError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return EXIT_INVALID_INPUT

    try:
        frontend_doc = _load_optional(
            Path(args.frontend), strict=args.fail_on_missing_frontend
        )
    except _InputError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return EXIT_INVALID_INPUT

    if frontend_doc is None and args.fail_on_missing_frontend:
        print(
            f"error: no frontend inventory at {args.frontend} "
            "and --fail-on-missing-frontend was set",
            file=sys.stderr,
        )
        return EXIT_INVALID_INPUT

    merged = merge(image_doc, frontend_doc)

    lost = _components_lost(image_doc, frontend_doc, merged)
    if lost:
        # SC-009. Publishing a document that quietly dropped components is worse than
        # publishing nothing, because it still looks authoritative.
        print(
            f"error: {len(lost)} component(s) present in an input are missing from the "
            f"merged document, e.g. {sorted(lost)[:5]}",
            file=sys.stderr,
        )
        return EXIT_INVARIANT_VIOLATED

    invalid = schema_errors(merged)
    if invalid:
        # SC-005: never publish a document that does not validate. Writing it and letting a
        # downstream consumer discover the problem turns one failure into several.
        print(
            "error: merged document does not validate against CycloneDX "
            f"{SPEC_VERSION}:",
            file=sys.stderr,
        )
        for problem in invalid:
            print(f"  {problem}", file=sys.stderr)
        return EXIT_SCHEMA_INVALID

    Path(args.output).write_text(json.dumps(merged, indent=2, sort_keys=True) + "\n")

    covered = frontend_doc is not None
    print(f"{COVERAGE_MARKER}{'true' if covered else 'false'}")
    if not covered:
        print(
            "warning: no frontend inventory was merged; this release's SBOM does not "
            "cover core-web's npm dependencies",
            file=sys.stderr,
        )
    return EXIT_OK


class _InputError(Exception):
    """A document cannot be used and the caller must be told."""


class _UnreadableError(_InputError):
    """The bytes are not a usable document at all — unparseable, or not a JSON object.

    Split from _InputError because the two have different causes and deserve different
    handling. Unreadable is what a truncated write looks like (`pnpm sbom > file` creates
    the file before the command runs), so for the optional frontend input it degrades.
    A well-formed document declaring the wrong bomFormat or specVersion is a configuration
    error — someone changed a flag — and must stay loud, or the published document's spec
    version could drift without anyone noticing.
    """


def _load_required(path: Path, label: str) -> dict:
    if not path.is_file():
        raise _InputError(f"{label} document not found: {path}")
    return _validate(_parse(path), path)


def _load_optional(path: Path, *, strict: bool) -> dict | None:
    """Absent, empty or unreadable frontend inventory is the degraded path (FR-009).

    Unreadable counts because `pnpm sbom > file` creates the file before the command runs:
    a mid-stream failure leaves partial JSON behind, non-empty and unparseable. Treating
    that as fatal would lose the Java/OS inventory too — the exact outcome FR-009 exists to
    prevent. Under --fail-on-missing-frontend it still raises, so strict mode stays strict.

    A readable document declaring the wrong bomFormat or specVersion is NOT degraded: that
    is a configuration error, and silently dropping it would let the published document's
    spec version drift unnoticed.
    """
    if not path.is_file() or path.stat().st_size == 0:
        return None
    try:
        doc = _validate(_parse(path), path)
    except _UnreadableError:
        if strict:
            raise
        print(
            f"warning: the frontend inventory at {path} is unreadable and was skipped; "
            "publishing image coverage only",
            file=sys.stderr,
        )
        return None
    return doc if doc.get("components") else None


def _parse(path: Path) -> dict:
    try:
        return json.loads(path.read_text())
    except json.JSONDecodeError as exc:
        raise _UnreadableError(f"{path} is not valid JSON: {exc}") from exc


def _validate(doc: object, path: Path) -> dict:
    # Valid JSON is not necessarily an object. A truncated or replaced artifact can be `[]`
    # or `null`, which used to reach .get() and raise AttributeError past the _InputError
    # handler — a traceback and exit 1, outside the documented contract.
    if not isinstance(doc, dict):
        raise _UnreadableError(
            f"{path} is not a JSON object (got {type(doc).__name__})"
        )
    if doc.get("bomFormat") != "CycloneDX":
        raise _InputError(f"{path} is not a CycloneDX document")
    if doc.get("specVersion") != SPEC_VERSION:
        # Not merged silently: a 1.7 input carries fields a 1.6 consumer may not handle,
        # and the published document's spec version is an external contract.
        raise _InputError(
            f"{path} declares specVersion {doc.get('specVersion')!r}, expected {SPEC_VERSION!r}"
        )
    return doc


def _components_lost(image_doc: dict, frontend_doc: dict | None, merged: dict) -> set:
    """Components present in an input but missing from the output (SC-009).

    Counts rather than checks membership. Keying on a set could not detect losing one of two
    identical components — both tuples still appeared — which is precisely the Syft dual-path
    case this tool exists to preserve.

    The expected multiplicity is the per-source maximum, not the sum: a component found by
    both sources collapses to one entry by design (rule 2), while two entries within one
    source must both survive (rule 7).
    """

    def counts(doc: dict | None) -> Counter:
        return Counter(
            (c.get("name"), c.get("version")) for c in (doc or {}).get("components", [])
        )

    image, frontend, out = counts(image_doc), counts(frontend_doc), counts(merged)

    expected = {
        key: max(image[key], frontend[key]) for key in set(image) | set(frontend)
    }
    return {key for key, n in expected.items() if out[key] < n}


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())

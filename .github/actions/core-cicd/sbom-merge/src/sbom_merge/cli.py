"""Command line entry point for sbom-merge.

Argument surface and exit codes are fixed by
specs/37575-core-web-npm-sbom/contracts/sbom-merge-cli.md.
"""
from __future__ import annotations

import argparse
import json
import sys
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
    parser.add_argument("--image", required=True, help="CycloneDX JSON from the Syft image scan")
    parser.add_argument("--frontend", required=True, help="CycloneDX JSON from `pnpm sbom`")
    parser.add_argument("--output", required=True, help="Destination for the merged document")
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
        frontend_doc = _load_optional(Path(args.frontend))
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
        print("error: merged document does not validate against CycloneDX "
              f"{SPEC_VERSION}:", file=sys.stderr)
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
    pass


def _load_required(path: Path, label: str) -> dict:
    if not path.is_file():
        raise _InputError(f"{label} document not found: {path}")
    return _validate(_parse(path), path)


def _load_optional(path: Path) -> dict | None:
    """Absent or empty frontend inventory is the degraded path, not an error (FR-009)."""
    if not path.is_file() or path.stat().st_size == 0:
        return None
    doc = _validate(_parse(path), path)
    return doc if doc.get("components") else None


def _parse(path: Path) -> dict:
    try:
        return json.loads(path.read_text())
    except json.JSONDecodeError as exc:
        raise _InputError(f"{path} is not valid JSON: {exc}") from exc


def _validate(doc: dict, path: Path) -> dict:
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
    def identities(doc: dict | None) -> set:
        return {
            (c.get("name"), c.get("version"))
            for c in (doc or {}).get("components", [])
        }

    return (identities(image_doc) | identities(frontend_doc)) - identities(merged)


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())

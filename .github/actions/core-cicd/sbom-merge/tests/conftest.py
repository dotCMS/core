"""Shared fixtures.

Every JSON fixture here is REAL captured data, not hand-written:

- `syft-sample.json`   — trimmed from artifact `sbom-dotcms-25.07.10_lts_v15` (run 29338983716).
                         Retains all 7 npm components the real scan found, plus maven and deb.
- `pnpm-sample.json`   — trimmed from `pnpm sbom --prod` over core-web. No licences, because it
                         was generated with `--lockfile-only`.
- `pnpm-licensed-sample.json`
                       — trimmed from `pnpm sbom` over dotcms-postman with a populated store, so
                         it carries real licence data. It comes from postman rather than core-web
                         because populating core-web's store means downloading all 2,365 packages;
                         only the presence and shape of licence fields matter to these tests.

This matters: invented fixtures would not have caught the tinymce three-copy case or the
monaco-editor devDependency case, both of which contradicted the spec's first draft.
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

FIXTURES = Path(__file__).parent / "fixtures"


def load(name: str) -> dict:
    return json.loads((FIXTURES / name).read_text())


def purls(doc: dict) -> set[str]:
    return {c["purl"] for c in doc.get("components", []) if c.get("purl")}


def names_versions(doc: dict) -> set[tuple[str, str]]:
    return {(c["name"], c["version"]) for c in doc.get("components", [])}


@pytest.fixture
def image_doc() -> dict:
    return load("syft-sample.json")


@pytest.fixture
def frontend_doc() -> dict:
    return load("pnpm-sample.json")


@pytest.fixture
def licensed_frontend_doc() -> dict:
    return load("pnpm-licensed-sample.json")

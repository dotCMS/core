"""US1 — FR-003 / SC-004: package URLs are the CVE-correlation handle."""

from __future__ import annotations

import re

from sbom_merge.merge import merge

NPM_PURL = re.compile(r"^pkg:npm/")


def test_every_npm_component_carries_a_purl(image_doc, frontend_doc):
    """SC-004 — 100% purl coverage for npm. A component without one cannot be
    correlated against an advisory, which is the document's whole purpose."""
    merged = merge(image_doc, frontend_doc)

    npm_like = [
        c
        for c in merged["components"]
        if NPM_PURL.match(c.get("purl") or "") or _looks_like_npm(c)
    ]
    assert npm_like, "fixture problem: no npm components present"

    missing = [c["name"] for c in npm_like if not c.get("purl")]
    assert not missing, f"npm components without a purl: {missing}"


def test_purl_matches_name_and_version(image_doc, frontend_doc):
    """A purl that disagrees with the component's own name/version silently
    misdirects every lookup made against it."""
    merged = merge(image_doc, frontend_doc)

    for c in merged["components"]:
        purl = c.get("purl") or ""
        if not NPM_PURL.match(purl):
            continue
        assert purl == f"pkg:npm/{c['name']}@{c['version']}", (
            f"purl {purl} disagrees with {c['name']}@{c['version']}"
        )


def _looks_like_npm(component: dict) -> bool:
    return any(
        (p.get("value") or "").endswith("package.json")
        for p in component.get("properties", [])
    )

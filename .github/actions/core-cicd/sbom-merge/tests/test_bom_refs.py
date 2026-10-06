"""US1 — reconciliation rule 5: bom-ref uniqueness."""

from __future__ import annotations

from collections import Counter

from sbom_merge.merge import merge


def test_bom_refs_are_unique(image_doc, frontend_doc):
    """A CycloneDX document with duplicate bom-refs is ambiguous for any consumer
    resolving a dependency graph."""
    merged = merge(image_doc, frontend_doc)
    refs = [c["bom-ref"] for c in merged["components"] if "bom-ref" in c]

    duplicated = [ref for ref, n in Counter(refs).items() if n > 1]
    assert not duplicated, f"duplicate bom-refs: {duplicated}"


def test_bom_ref_collision_rewrites_rather_than_drops(image_doc, frontend_doc):
    """Rule 5 — the resolution for a collision is to rewrite a ref, never to drop a
    component. Two DIFFERENT components sharing a bom-ref must both survive."""
    collided = dict(image_doc)
    clashing = [
        dict(c, **{"bom-ref": "shared-ref"}) for c in image_doc["components"][:1]
    ]
    collided["components"] = clashing + image_doc["components"][1:]

    frontend_clashing = dict(frontend_doc)
    frontend_clashing["components"] = [
        dict(c, **{"bom-ref": "shared-ref"}) for c in frontend_doc["components"][:1]
    ] + frontend_doc["components"][1:]

    merged = merge(collided, frontend_clashing)

    refs = [c["bom-ref"] for c in merged["components"] if "bom-ref" in c]
    assert len(refs) == len(set(refs)), "collision was not resolved"

    expected = {(c["name"], c["version"]) for c in collided["components"]} | {
        (c["name"], c["version"]) for c in frontend_clashing["components"]
    }
    got = {(c["name"], c["version"]) for c in merged["components"]}
    assert got == expected, "a component was dropped to resolve a bom-ref collision"

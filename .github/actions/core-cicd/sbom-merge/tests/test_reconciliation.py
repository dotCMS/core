"""US1 — reconciliation rules 1 and 2 (FR-013, FR-014).

The governing rule: components are keyed on (name, version), never on name alone.
"""

from __future__ import annotations

from conftest import names_versions

from sbom_merge.merge import merge


def test_components_from_both_sources_are_present(image_doc, frontend_doc):
    """Rule 1 — the merged document is the union of both inputs."""
    merged = merge(image_doc, frontend_doc)
    got = names_versions(merged)

    assert names_versions(image_doc) <= got, "image components went missing"
    assert names_versions(frontend_doc) <= got, "frontend components went missing"


def test_same_name_different_versions_all_retained(image_doc, frontend_doc):
    """Rule 1 / FR-013 — the tinymce case, the regression this rule exists for.

    The image ships 6.8.6 (dotAdmin, from npm) and 7.2.1 (ext/tinymcev7, vendored for the
    legacy Dojo editor). The frontend tree resolves 6.8.3. A name-keyed dedupe collapses
    these and misreports the CVE surface for two different editors.
    """
    merged = merge(image_doc, frontend_doc)
    tinymce_versions = {
        c["version"] for c in merged["components"] if c["name"] == "tinymce"
    }

    assert tinymce_versions == {"6.8.3", "6.8.6", "7.2.1"}, (
        f"expected all three tinymce copies, got {sorted(tinymce_versions)}"
    )


def test_multiple_versions_within_one_source_retained(frontend_doc, image_doc):
    """Rule 1 — the same trap inside a single input.

    core-web's own tree resolves tslib at three versions simultaneously. Deduplicating
    within a source is as wrong as deduplicating across sources.
    """
    merged = merge(image_doc, frontend_doc)
    tslib_versions = {
        c["version"] for c in merged["components"] if c["name"] == "tslib"
    }

    assert tslib_versions == {"1.14.1", "2.3.0", "2.8.1"}


def test_exact_duplicate_appears_once_with_both_sources_recorded(
    image_doc, frontend_doc
):
    """Rule 2 / FR-014 — same name AND version from both sources collapses to one entry,
    but the fact that both found it must survive."""
    # tinymce@6.8.3 exists only in the frontend fixture; inject it into the image side so
    # the fixtures present a genuine (name, version) collision.
    collided = dict(image_doc)
    collided["components"] = image_doc["components"] + [
        c for c in frontend_doc["components"] if c["name"] == "tinymce"
    ]

    merged = merge(collided, frontend_doc)
    entries = [
        c
        for c in merged["components"]
        if (c["name"], c["version"]) == ("tinymce", "6.8.3")
    ]

    assert len(entries) == 1, f"expected exactly one entry, got {len(entries)}"
    assert _sources_of(entries[0]) >= {"image", "frontend"}, (
        "provenance from both sources must be preserved, not silently discarded"
    )


def _sources_of(component: dict) -> set[str]:
    """Read back whichever provenance marker the implementation records."""
    return {
        p.get("value")
        for p in component.get("properties", [])
        if p.get("name", "").endswith("source")
    }

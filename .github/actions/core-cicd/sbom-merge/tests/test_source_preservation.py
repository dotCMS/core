"""US1 — reconciliation rules 3 and 4, and the SC-009 invariant."""
from __future__ import annotations

from conftest import names_versions

from sbom_merge.merge import merge


def test_image_only_components_are_retained(image_doc, frontend_doc):
    """Rule 3 / FR-016 — the frontend inventory is additive, never a replacement.

    ext/tinymcev7 and the Dojo tree are vendored into the repository and appear in no
    lockfile, so the image scan is the ONLY source that can see them. An implementation
    that treats the pnpm document as the authority on npm would erase them.
    """
    merged = merge(image_doc, frontend_doc)
    merged_nv = names_versions(merged)

    for name, version in [("tinymce", "7.2.1"), ("dojo", "1.17.2"), ("dijit", "1.17.2"),
                          ("dojox", "1.17.2"), ("edit-content-bridge", "1.0.0")]:
        assert (name, version) in merged_nv, f"{name}@{version} is image-only and was dropped"


def test_frontend_only_components_are_added(image_doc, frontend_doc):
    """Rule 4 / FR-001 — the point of the whole feature."""
    merged = merge(image_doc, frontend_doc)
    merged_nv = names_versions(merged)

    frontend_only = names_versions(frontend_doc) - names_versions(image_doc)
    assert frontend_only, "fixture problem: no frontend-only components to assert on"
    assert frontend_only <= merged_nv


def test_no_component_is_lost(image_doc, frontend_doc):
    """SC-009 — the invariant. No reconciliation step may shrink the union."""
    merged = merge(image_doc, frontend_doc)
    expected = names_versions(image_doc) | names_versions(frontend_doc)

    assert names_versions(merged) == expected


def test_non_npm_components_pass_through_untouched(image_doc, frontend_doc):
    """Rule 7 / FR-010 / SC-006 — Java and OS coverage must not regress.

    Asserted here rather than only in US3 because it is the thing most likely to break
    silently while the npm numbers look impressive.
    """
    merged = merge(image_doc, frontend_doc)

    for ecosystem in ("pkg:maven/", "pkg:deb/"):
        before = [c for c in image_doc["components"] if (c.get("purl") or "").startswith(ecosystem)]
        after = [c for c in merged["components"] if (c.get("purl") or "").startswith(ecosystem)]
        assert len(after) == len(before), f"{ecosystem} count changed: {len(before)} -> {len(after)}"


def test_duplicate_entries_within_the_image_document_are_preserved(image_doc, frontend_doc):
    """Rule 7 / SC-006 — reconciliation happens BETWEEN sources, never within one.

    Syft legitimately emits the same purl twice when it finds the same jar at two paths in
    the image (e.g. bundled in the WAR and again in a plugin). Those are real, distinct
    findings about the deployment, and collapsing them changes the maven count — which
    SC-006 forbids.

    Caught on real data: an earlier implementation keyed the whole merge on identity and
    silently took maven from 571 to 555. The trimmed fixture had no duplicates, so only
    the full document exposed it.
    """
    merged = merge(image_doc, frontend_doc)

    for ecosystem in ("pkg:maven/", "pkg:deb/"):
        before = [c for c in image_doc["components"] if (c.get("purl") or "").startswith(ecosystem)]
        after = [c for c in merged["components"] if (c.get("purl") or "").startswith(ecosystem)]
        assert len(after) == len(before), (
            f"{ecosystem}: {len(before)} -> {len(after)}; intra-source duplicates were collapsed"
        )

    duplicated_purls = {
        c["purl"] for c in image_doc["components"]
        if c.get("purl") and sum(1 for o in image_doc["components"] if o.get("purl") == c["purl"]) > 1
    }
    assert duplicated_purls, "fixture problem: no intra-source duplicate to assert on"

    for purl in duplicated_purls:
        before = sum(1 for c in image_doc["components"] if c.get("purl") == purl)
        after = sum(1 for c in merged["components"] if c.get("purl") == purl)
        assert after == before, f"{purl}: {before} entries became {after}"

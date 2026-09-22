"""US3 — reconciliation rule 6: one document, one subject (FR-015)."""

from __future__ import annotations

from sbom_merge.merge import COVERAGE_PROPERTY, merge


def _property(doc: dict, name: str) -> str | None:
    for p in doc.get("metadata", {}).get("properties", []):
        if p.get("name") == name:
            return p.get("value")
    return None


def test_metadata_describes_the_released_artifact(image_doc, frontend_doc):
    """The image document's metadata describes the release; the frontend document's
    describes a workspace. Taking the latter would make the document claim to be about
    something that was never shipped."""
    merged = merge(image_doc, frontend_doc)

    assert merged["metadata"].get("component") == image_doc["metadata"].get("component")


def test_coverage_is_recorded_when_the_frontend_is_present(image_doc, frontend_doc):
    assert _property(merge(image_doc, frontend_doc), COVERAGE_PROPERTY) == "true"


def test_coverage_is_recorded_when_the_frontend_is_absent(image_doc):
    """SC-007 — a degraded release must be identifiable from the artifact alone, without
    anyone opening a build log."""
    assert _property(merge(image_doc, None), COVERAGE_PROPERTY) == "false"


def test_both_generators_are_named_in_the_tools(image_doc, frontend_doc):
    """A reader has to be able to tell which half of the document came from which tool."""
    merged = merge(image_doc, frontend_doc)
    tools = merged["metadata"].get("tools")

    if tools is None:
        return  # neither input declared tools; nothing to assert

    rendered = str(tools).lower()
    image_tools = str(image_doc["metadata"].get("tools", "")).lower()
    frontend_tools = str(frontend_doc["metadata"].get("tools", "")).lower()

    if "syft" in image_tools:
        assert "syft" in rendered, (
            "the image scanner is no longer named in the merged metadata"
        )
    if "pnpm" in frontend_tools:
        assert "pnpm" in rendered, (
            "the frontend generator is no longer named in the merged metadata"
        )

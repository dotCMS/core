"""US3 — the published document must stay readable by existing consumers (FR-006, SC-005)."""

from __future__ import annotations

import pytest

from sbom_merge.merge import SPEC_VERSION, merge
from sbom_merge.schema import validator as _validator


@pytest.fixture(scope="module")
def validator():
    """The SAME validator the CLI uses, deliberately: testing a separately-constructed one
    would leave the shipped validation path untested."""
    return _validator()


def test_merged_document_validates_against_the_schema(
    validator, image_doc, frontend_doc
):
    """SC-005 — a document that does not validate is not publishable, no matter how
    complete its inventory is."""
    merged = merge(image_doc, frontend_doc)
    errors = sorted(validator.iter_errors(merged), key=lambda e: e.path)

    assert not errors, "\n".join(f"{list(e.path)}: {e.message}" for e in errors[:5])


def test_degraded_document_also_validates(validator, image_doc):
    """A degraded release still publishes; publishing an invalid document instead would
    turn one problem into two."""
    merged = merge(image_doc, None)
    errors = list(validator.iter_errors(merged))
    assert not errors, errors[0].message if errors else ""


def test_spec_version_is_held_at_the_published_value(image_doc, frontend_doc):
    """FR-006 — moving the published document to 1.7 is a consumer-visible decision and
    does not belong to this change."""
    merged = merge(image_doc, frontend_doc)
    assert merged["specVersion"] == SPEC_VERSION == "1.6"
    assert merged["bomFormat"] == "CycloneDX"


def test_the_validator_actually_rejects_an_invalid_document(
    validator, image_doc, frontend_doc
):
    """Guards the three tests above.

    A misconfigured validator — unresolved $refs, a schema that failed to load — reports
    zero errors for everything, so the passing assertions above would prove nothing. This
    corrupts a required field and asserts the validator notices.
    """
    merged = merge(image_doc, frontend_doc)
    merged["components"][0]["type"] = "not-a-valid-component-type"

    assert list(validator.iter_errors(merged)), (
        "the validator accepted an invalid document; it is not actually validating"
    )

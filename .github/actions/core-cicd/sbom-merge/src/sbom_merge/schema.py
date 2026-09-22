"""CycloneDX schema validation.

The schemas are vendored rather than fetched: a release must not depend on network access
to an external schema host, and a silently-skipped validation is worse than none.
"""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path

from jsonschema import Draft7Validator
from jsonschema.protocols import Validator
from referencing import Registry, Resource
from referencing.jsonschema import DRAFT7

SCHEMAS = Path(__file__).parent / "schemas"
SPEC_SCHEMA = "cyclonedx-1.6.schema.json"

# CycloneDX 1.6 $refs these two by bare filename.
REFERENCED = ("spdx.schema.json", "jsf-0.82.schema.json")


@lru_cache(maxsize=1)
def validator() -> Validator:
    schema = json.loads((SCHEMAS / SPEC_SCHEMA).read_text())
    registry = Registry().with_resources(
        [
            (
                name,
                Resource.from_contents(
                    json.loads((SCHEMAS / name).read_text()),
                    default_specification=DRAFT7,
                ),
            )
            for name in REFERENCED
        ]
    )
    return Draft7Validator(schema, registry=registry)


def errors(document: dict, limit: int = 5) -> list[str]:
    """Schema errors as readable strings, capped so a broken document does not flood the log."""
    found = sorted(validator().iter_errors(document), key=lambda e: list(e.path))
    return [f"{list(e.path) or '<root>'}: {e.message}" for e in found[:limit]]

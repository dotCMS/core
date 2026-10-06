"""Reconciliation of two CycloneDX 1.6 documents into one.

Rules are specified in specs/37575-core-web-npm-sbom/data-model.md#reconciliation-rules.

The rule worth stating up front, because getting it wrong is silent and expensive: components
are keyed on IDENTITY (purl, or name+version when there is no purl), never on name alone.
dotCMS ships tinymce three times — 6.8.3 from core-web's tree, 6.8.6 copied into dotAdmin, and
7.2.1 vendored under the legacy webapp for the Dojo editor. A name-keyed dedupe collapses those
into one and misreports the CVE surface for two different editors.
"""

from __future__ import annotations

from typing import Iterable

SPEC_VERSION = "1.6"

SOURCE_PROPERTY = "dotcms:sbom:source"
SOURCE_IMAGE = "image"
SOURCE_FRONTEND = "frontend"

COVERAGE_PROPERTY = "dotcms:sbom:frontend-covered"


def merge(image_doc: dict, frontend_doc: dict | None) -> dict:
    """Merge a frontend npm inventory into a Syft image-scan document.

    `frontend_doc` may be None, which is the degraded path (FR-009): the image document
    passes through and the result records that frontend coverage is absent, so a reader can
    tell a degraded release from a complete one without opening build logs.
    """
    merged = _merge_components(
        _tagged(image_doc, SOURCE_IMAGE),
        _tagged(frontend_doc, SOURCE_FRONTEND) if frontend_doc else [],
    )

    document = dict(image_doc)
    document["specVersion"] = SPEC_VERSION
    document["components"] = _assign_unique_refs(merged)
    document["metadata"] = _merge_metadata(
        image_doc.get("metadata", {}),
        (frontend_doc or {}).get("metadata", {}),
        frontend_covered=frontend_doc is not None,
    )
    return document


def _tagged(doc: dict | None, source: str) -> list[tuple[dict, str]]:
    return [(component, source) for component in (doc or {}).get("components", [])]


def _identity(component: dict) -> tuple:
    """The reconciliation key.

    The purl is preferred because it carries the ecosystem: a maven `tinymce` and an npm
    `tinymce` at the same version are different components and must not be collapsed.
    Components without a purl fall back to name+version.
    """
    purl = component.get("purl")
    if purl:
        return ("purl", purl)
    return ("nv", component.get("name"), component.get("version"))


def _merge_components(
    image: Iterable[tuple[dict, str]],
    frontend: Iterable[tuple[dict, str]],
) -> list[dict]:
    """Rules 1–4 and 7: the image document passes through intact; the frontend inventory is
    additive.

    Reconciliation happens BETWEEN sources, never within one. Syft legitimately emits the
    same purl twice when it finds the same jar at two paths in the image — those are
    distinct findings about the deployment, and collapsing them would change the maven
    count, which SC-006 forbids. An earlier version keyed the whole merge on identity and
    quietly took maven from 571 to 555 on the real document.
    """
    # dict(...) so the caller's input documents are never mutated.
    kept = [dict(component) for component, _ in image]

    by_identity: dict[tuple, list[dict]] = {}
    for component in kept:
        by_identity.setdefault(_identity(component), []).append(component)

    sources: dict[int, list[str]] = {
        id(component): [SOURCE_IMAGE] for component in kept
    }

    seen_in_frontend: set[tuple] = set()

    for component, _ in frontend:
        key = _identity(component)
        # Reconciliation is BETWEEN sources, never within one — on this side too. A second
        # frontend component with the same identity is a duplicate within its own source and
        # must survive, exactly as an image-side duplicate does. A pnpm lockfile lists each
        # name@version once so this does not arise today, but a rule enforced on one side
        # only is not the rule the data model states.
        existing = None if key in seen_in_frontend else by_identity.get(key)
        seen_in_frontend.add(key)
        if existing:
            # Rule 2: both sources found it. No new entry, but the fact that the frontend
            # inventory also saw it survives — and it may carry a licence the scan lacked.
            for match in existing:
                if SOURCE_FRONTEND not in sources[id(match)]:
                    sources[id(match)].append(SOURCE_FRONTEND)
                _fill_missing_fields(match, component)
        else:
            # Rule 4: frontend-only component. This is the bulk of the feature.
            added = dict(component)
            kept.append(added)
            by_identity.setdefault(key, []).append(added)
            sources[id(added)] = [SOURCE_FRONTEND]

    ordered = sorted(kept, key=_sort_key)
    return [_with_sources(component, sources[id(component)]) for component in ordered]


def _sort_key(component: dict) -> tuple:
    """Deterministic ordering, so the same inputs always produce byte-identical output and
    two releases' documents are diffable."""
    return (
        component.get("name") or "",
        component.get("version") or "",
        component.get("purl") or "",
        # Distinguishes intra-source duplicates, which share name, version and purl and are
        # told apart only by where they were found.
        str(component.get("properties") or ""),
    )


def _fill_missing_fields(target: dict, other: dict) -> None:
    """Take fields the first source did not supply.

    Typically licences: the image scan often knows a package exists without resolving its
    licence, while the frontend inventory reads it from the populated store.
    """
    for field, value in other.items():
        if field in ("properties", "bom-ref"):
            continue
        if not target.get(field) and value:
            target[field] = value


def _with_sources(component: dict, sources: list[str]) -> dict:
    existing = [
        p for p in component.get("properties", []) if p.get("name") != SOURCE_PROPERTY
    ]
    component["properties"] = existing + [
        {"name": SOURCE_PROPERTY, "value": source} for source in sources
    ]
    return component


def _assign_unique_refs(components: list[dict]) -> list[dict]:
    """Rule 5: bom-refs are unique in the output.

    A collision between two DIFFERENT components is resolved by rewriting one ref — never
    by dropping a component. Duplicate refs make a dependency graph ambiguous for any
    consumer that resolves one.
    """
    used: set[str] = set()

    for component in components:
        candidates = [
            component.get("bom-ref"),
            component.get("purl"),
            f"{component.get('name')}@{component.get('version')}",
        ]
        ref = next((c for c in candidates if c and c not in used), None)

        if ref is None:
            base = (
                component.get("purl")
                or f"{component.get('name')}@{component.get('version')}"
            )
            suffix = 2
            while f"{base}#{suffix}" in used:
                suffix += 1
            ref = f"{base}#{suffix}"

        component["bom-ref"] = ref
        used.add(ref)

    return components


def _merge_metadata(
    image_meta: dict, frontend_meta: dict, *, frontend_covered: bool
) -> dict:
    """Rule 6: one coherent header describing one product release.

    The image document's metadata describes the released artifact, so it is the base. The
    frontend document's metadata describes a workspace, not a release, so only its tooling
    is carried across — appending its `component` would claim the document is about two
    different subjects.
    """
    metadata = dict(image_meta)

    tools = _combined_tools(image_meta.get("tools"), frontend_meta.get("tools"))
    if tools is not None:
        metadata["tools"] = tools

    properties = [
        p for p in metadata.get("properties", []) if p.get("name") != COVERAGE_PROPERTY
    ]
    properties.append(
        {"name": COVERAGE_PROPERTY, "value": "true" if frontend_covered else "false"}
    )
    metadata["properties"] = properties

    return metadata


def _combined_tools(image_tools, frontend_tools):
    """CycloneDX 1.6 allows `tools` as either a list or an object with `components`.

    Both generators must be named: a reader has to be able to tell which half of the
    document came from which tool.
    """
    if image_tools is None and frontend_tools is None:
        return None
    if isinstance(image_tools, list) and isinstance(frontend_tools, list):
        return image_tools + frontend_tools
    if isinstance(image_tools, dict) and isinstance(frontend_tools, dict):
        combined = dict(image_tools)
        combined["components"] = image_tools.get("components", []) + frontend_tools.get(
            "components", []
        )
        return combined
    return image_tools if image_tools is not None else frontend_tools

# Phase 1 Data Model: Product SBOM coverage for core-web's npm dependencies

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-09-21

There is no database and no persisted state. The "data model" here is the shape of the CycloneDX
documents the merge tool consumes and produces, and the reconciliation rules that relate them.
Field names below are CycloneDX 1.6.

---

## Entities

### CycloneDX Document

The unit of input and output. Two come in, one goes out.

| Field | Notes |
|---|---|
| `bomFormat` | Always `CycloneDX` |
| `specVersion` | Always `1.6`. Both inputs are forced to it — Syft emits it natively, pnpm is pinned down from its 1.7 default ([R7](./research.md#r7--which-cyclonedx-specification-version)) |
| `metadata` | Describes the subject of the document. Reconciled, not concatenated — see FR-015 |
| `components[]` | The inventory. See below |

Three instances exist at runtime:

| Instance | Produced by | Approx. size | Ecosystems |
|---|---:|---|---|
| **Image inventory** | Syft scanning `dotcms/dotcms:<version>` | ~726 | maven, deb, npm (incidental), generic |
| **Frontend inventory** | `pnpm sbom` over `core-web` | ~2,365 | npm only |
| **Merged document** | The merge tool | ~3,080 | all of the above |

### Component

One third-party package. The merge operates entirely on this collection.

| Field | Required | Notes |
|---|---|---|
| `name` | yes | Not a unique key on its own — see the `tinymce` case below |
| `version` | yes | |
| `purl` | yes for npm (SC-004) | `pkg:npm/<name>@<version>`. The CVE-correlation handle |
| `bom-ref` | yes | Must be unique within the merged document; collisions across sources must be resolved without dropping a component |
| `type` | yes | `library` for everything in scope |
| `licenses[]` | best-effort | Present for ~98.5% of frontend components when the store is populated; **absent entirely** if generated from the lockfile alone ([R2](./research.md#r2--where-in-the-pipeline-can-the-inventory-be-generated)) |
| `externalReferences[]` | no | pnpm puts the tarball URL plus a SHA-512 here. Note this is the *distribution* hash, not a component-level `hashes[]` entry — the issue's "no integrity hashes" claim is right at the component level only |
| `properties[]` | no | Syft records discovery paths here (`syft:location:0:path`), which is how the three `tinymce` copies were told apart |

### Component Identity

**The key is `(name, version)`, never `name`.** This is the single most important rule in the model,
and it is not theoretical — `tinymce` is the counterexample the repository already contains:

| Copy | Version | Origin | In lockfile? | Found by |
|---|---|---|---|---|
| `dotAdmin/tinymce/` | 6.8.3 (`main`) | `node_modules/tinymce`, copied as a build asset | yes | both sources |
| `ext/tinymcev7/` | 7.2.1 | vendored under the legacy webapp, Dojo/JSP editor | **no** | image scan only |
| `html/js/tinymce/` | unversioned | older vendored copy, ships no manifest | **no** | **neither** |

A name-keyed dedupe collapses the first two and misreports the CVE surface for both editors.

### Provenance

Not a CycloneDX entity — a merge-tool concern that must survive into the output (FR-011, FR-014).
Each component in the merged document must be attributable to the source(s) that found it, so a
reader can tell image-scan coverage from lockfile coverage. Recorded as a property on the component;
exact key is an implementation decision for the contract, not fixed here.

---

## Reconciliation Rules

Applied when combining the image inventory with the frontend inventory. Each maps to a spec
requirement and to a test row in the plan's Test Strategy.

| # | Rule | Requirement |
|---|---|---|
| 1 | Match components on `(name, version)`. Differing versions are distinct components; retain all. | FR-013 |
| 2 | A component found by **both** sources appears **once**, with provenance from both preserved — neither source's entry silently discarded. | FR-014 |
| 3 | A component found **only** by the image scan is retained. The frontend inventory is additive, never a replacement. | FR-016 |
| 4 | A component found **only** by the frontend inventory is added. | FR-001 |
| 5 | `bom-ref` values are unique in the output; a collision is resolved by rewriting a ref, never by dropping a component. | FR-014 |
| 6 | Top-level `metadata` describes one product release — not one input's metadata with the other's components appended. | FR-015 |
| 7 | Non-npm components (maven, deb, generic) pass through untouched, in count and content. | FR-010, SC-006 |
| 8 | The output validates against the CycloneDX 1.6 schema. | FR-006, SC-005 |

**Invariant (SC-009)**: every component present in either input is present in the output. Formally,
`|output| = |image ∪ frontend|` keyed on `(name, version, ecosystem)`. No reconciliation step may
reduce the count except by collapsing an exact `(name, version)` match under rule 2.

---

## Scope Boundaries

What enters the frontend inventory in the first place, before any merging:

| Category | Included? | Why |
|---|---|---|
| `core-web` production dependencies | **yes** | Ships (FR-004) |
| `core-web` dev dependencies copied verbatim as build assets | **yes** | Ships despite the declaration — `monaco-editor` is the live case (FR-005) |
| `core-web` dev/build-only dependencies | no | Does not ship (FR-004) |
| `dotcms-postman` dependencies | no | Test tooling, does not ship (FR-017) |
| Vendored copies under `dotCMS/src/main/webapp/` | not by this source | In no lockfile; covered by the image scan under rule 3 |

The second row is the subtle one. The build copies exactly two packages out of `node_modules`
verbatim (`tinymce`, `monaco-editor`, per `apps/dotcms-ui/project.json`), and `monaco-editor` is a
`devDependency`. Both happen to arrive with their manifest intact, so the image scan already covers
them and rule 3 retains them — the production-only filter loses nothing **today**. That is
coincidence, not design, and [R6](./research.md#r6--what-counts-as-shipped-fr-004--fr-005) proposes
a guard test so a third copied asset cannot slip through silently.

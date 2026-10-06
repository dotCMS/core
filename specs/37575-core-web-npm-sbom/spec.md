# Feature Specification: Product SBOM coverage for core-web's npm dependencies

**Feature Branch**: `nicobytes/build-the-product-sbom-does-not-cover-core-webs`

**Created**: 2026-09-18

**Status**: Draft

**Input**: GitHub issue [#37575](https://github.com/dotCMS/core/issues/37575) — "Build: the product SBOM does not cover core-web's npm dependencies"

## Context

dotCMS publishes a CycloneDX SBOM with every release. It is produced by scanning the published
`dotcms/dotcms:<version>` Docker image with Syft. Java dependencies ship as jars carrying their own
metadata, so Syft inventories them well. The frontend does not: `core-web`'s WAR is packaged from
built output only, so what reaches the image is bundled, minified JavaScript with no package
manifests. The npm packages that produced that bundle are invisible to an image scan.

### The gap is confirmed, not assumed

The issue asked for this to be verified before any work started. It has been, against the SBOM
artifact from the most recent release run whose SBOM job succeeded (run `29338983716`,
`sbom-dotcms-25.07.10_lts_v15`, CycloneDX 1.6):

| Ecosystem | Components |
|-----------|-----------:|
| maven     |        571 |
| deb       |        145 |
| npm       |          7 |
| generic   |          2 |
| no purl   |          1 |
| **Total** |    **726** |

All 7 npm components are directories that happen to ship a `package.json` inside the image —
`dojo`, `dijit`, `dojox`, `monaco-editor`, `tinymce` (×2), and a legacy custom-field bridge. Only
**2** of them (`monaco-editor`, and the `tinymce` under `dotAdmin/`) originate in `core-web`'s
dependency tree, and they are visible only because the Angular build copies them out of
`node_modules` verbatim as static assets, manifest included. Everything that went through the
bundler — the other ~2,363 packages — is invisible.

For scale, `pnpm sbom --prod` over `core-web` yields **1,981 production components** with a
populated store, every one carrying a package URL. That is the inventory currently missing from
the published document.

(A lockfile-only run reports 2,365. The extra 384 are platform-specific optional binaries —
`@esbuild/aix-ppc64`, `@esbuild/android-arm64`, `@emnapi/*` and similar — that the lockfile lists
for every platform but that are never installed or shipped. Lockfile-only over-reports; the
store-backed figure is the accurate one.)

### A second, separate problem found while confirming this

The **Generate SBOM job failed in 3 of the last 4 releases** (runs `33520792625`, `31558819567`,
`30867435272`; last success `29338983716`, 2026-07-14). The job is `continue-on-error: true`, so
those releases were published and reported green with **no SBOM at all**.

The failure is `'anchore-syft' executable script not found in package 'anchore-syft'` — the exact
breakage #36755/#36756 fixed. The fix landed on `main` on 2026-07-27 but is not present on the
release line those runs executed from, so it never reached them.

This is a defect in the Syft half of the pipeline and is **out of scope here** (see Out of Scope),
but it bounds the value of this work: an npm inventory added to a document that is not produced is
worth nothing. It should be tracked as its own issue.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Answer "are we affected?" for an npm advisory (Priority: P1)

A security engineer receives a CVE for an npm package. They open the SBOM published with the
dotCMS release under review and search for the package by name or package URL. Today that search
returns nothing for any frontend dependency, and they cannot tell whether the absence means "not
affected" or "not inventoried" — so they must fall back on reading lockfiles per release branch by
hand. With frontend components present, the SBOM answers the question directly.

**Why this priority**: This is the entire purpose of shipping an SBOM. Every other benefit is
secondary to the document being able to answer the question it exists to answer, and npm is where
the supply-chain attacks of recent years have actually occurred.

**Independent Test**: Take a package known to be in `core-web`'s production tree, look it up in the
SBOM produced by a release build, and confirm it appears at the version the lockfile resolves.

**Acceptance Scenarios**:

1. **Given** a release build has completed, **When** the published SBOM is searched for a package
   that `core-web`'s lockfile resolves as a production dependency, **Then** a component is found
   with that package's name and resolved version.
2. **Given** the published SBOM, **When** its components are grouped by package-URL ecosystem,
   **Then** the npm count reflects `core-web`'s full production tree (thousands), not the handful
   of vendored asset directories present today.
3. **Given** a component sourced from the frontend tree, **When** it is inspected, **Then** it
   carries a package URL in `pkg:npm/<name>@<version>` form usable for CVE correlation.
4. **Given** a package used only to build or test, and never copied into the shipped output,
   **When** the SBOM is searched, **Then** it is absent — the document describes what ships, not
   what built it.
5. **Given** a package declared as a development dependency but copied verbatim into the shipped
   admin UI as a static asset, **When** the SBOM is searched, **Then** it is present — declaration
   does not decide the matter, shipping does.

---

### User Story 2 - Audit the licenses of what ships (Priority: P2)

Legal or compliance review needs the license of every third-party component in a release. The Java
side is covered. The frontend side is not, so the licence position of the shipped admin UI is
currently undocumented.

**Why this priority**: Real and recurring, but it is a periodic audit rather than an incident
response, so it can tolerate a slower answer than P1 can. It is also the part of the value that
depends on *where* in the pipeline the inventory is generated (see Assumptions), so it is worth
stating separately rather than folding into P1.

**Independent Test**: Count how many frontend components in a release SBOM carry a license
identifier, and confirm the proportion is high enough for the document to be usable for audit.

**Acceptance Scenarios**:

1. **Given** the published SBOM, **When** the frontend components are inspected, **Then** the large
   majority carry a license identifier.
2. **Given** a component whose license cannot be determined, **When** the SBOM is inspected,
   **Then** that component is still present with its package URL, so the gap is a missing field on
   a known component rather than a missing component.

---

### User Story 3 - Consume the SBOM with existing tooling (Priority: P3)

A downstream consumer — a scanner, a customer's compliance pipeline, an internal dashboard — ingests
the SBOM dotCMS publishes. Adding frontend coverage must not force that consumer to learn a second
document shape or a second format.

**Why this priority**: It protects the value delivered by P1 and P2 rather than adding new value.
If consumers cannot read the result, the coverage is not actually delivered.

**Independent Test**: Validate the published output against the CycloneDX schema at the declared
specification version, and confirm the format and version match what is published today.

**Acceptance Scenarios**:

1. **Given** a release build, **When** the SBOM output is validated against the CycloneDX JSON
   schema, **Then** it validates cleanly.
2. **Given** the SBOM published today and the SBOM published after this change, **When** their
   format and specification version are compared, **Then** they match.
3. **Given** a release, **When** its published artifacts are listed, **Then** the SBOM is
   discoverable in the same place and under the same naming convention as before.

---

### Edge Cases

- **The release runs from a branch where the frontend SBOM step does not exist.** Older release
  lines will not carry this change. The release must still publish the Java/OS inventory it
  publishes today rather than failing.
- **The frontend SBOM step itself fails.** A release must not silently publish a document that
  claims completeness it does not have. Either the failure is loud, or the document records that
  frontend coverage is absent — it must not look identical to a successful run.
- **A component resolves from a source other than the public registry** (git dependency, workspace
  link, tarball, patched package). Package-URL form may differ or be absent; the component must
  still appear rather than being dropped.
- **The same package appears at two versions** in the tree. Both must be inventoried; deduplicating
  to one would under-report the CVE surface.
- **A package name collides between the Java and npm halves** of a merged document. Components must
  remain distinguishable by ecosystem.
- **The same package ships more than once, from different origins, at different versions.** This is
  not hypothetical — `tinymce` ships three times, for two different editors:

  | Copy in the image | Version | Origin | In the lockfile? |
  |---|---|---|---|
  | `dotAdmin/tinymce/` | 6.8.3 on `main` (6.8.6 on the scanned LTS image) | `node_modules/tinymce`, copied as a build asset by the Angular app | **Yes** |
  | `ext/tinymcev7/` | 7.2.1 | vendored and committed under the legacy webapp, for the Dojo/JSP editor rendered by Java | **No** |
  | `html/js/tinymce/` | unversioned | older vendored copy, ships no manifest | **No** |

  The two editors are deliberate: the legacy Dojo/JSP surface and the Angular surface use different
  TinyMCE majors. Only the Angular one comes from npm. The merge must therefore keep components the
  image scan finds that the lockfile cannot know about — see FR-016.
- **The pinned tool version stops resolving**, exactly as the Syft pin did in the failures described
  above. The failure must be visible rather than producing a silently thinner document.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The release build MUST produce an inventory of the npm dependencies that contribute
  to the shipped frontend, generated from the resolved dependency graph rather than from the built
  bundle.
- **FR-002**: The inventory MUST be generated at a point in the release pipeline where the
  dependency metadata needed for licensing is available, not merely the lockfile.
- **FR-003**: Each inventoried component MUST carry its name, resolved version, and a package URL
  suitable for CVE correlation.
- **FR-004**: The inventory MUST cover what ships and MUST exclude packages used only to build or
  test. The manifest's production/development split is a good approximation of that boundary but is
  **not** equivalent to it in this repository (see FR-005), and where the two disagree, what ships
  wins.
- **FR-005**: Packages that the build copies verbatim into the shipped output as static assets MUST
  be inventoried even when the manifest declares them as development dependencies. This is not
  hypothetical: `monaco-editor` is declared under `devDependencies` yet is copied into the shipped
  admin UI and is present in the image today. A production-only filter would drop it and report a
  shipped component as absent.
- **FR-006**: The frontend inventory MUST be published in the same format and at the same CycloneDX
  specification version as the existing Syft output, so downstream consumers handle one shape.
- **FR-007**: The frontend inventory MUST be published as part of the same release artifact set as
  the existing SBOM, discoverable alongside it.
- **FR-008**: The version of the tool generating the frontend inventory MUST be pinned, in the same
  spirit as the existing Syft pin, so a future upstream packaging change cannot silently alter or
  break the output.
- **FR-009**: Failure to generate the frontend inventory MUST be surfaced as a distinguishable
  failure, and MUST NOT result in a published document that is indistinguishable from a complete
  one.
- **FR-010**: The change MUST NOT reduce or alter the Java and OS coverage the SBOM provides today.
- **FR-011**: The published result MUST record which portion of the inventory came from which
  source, so a reader can tell frontend coverage apart from image-scan coverage.
- **FR-012**: The frontend inventory MUST be merged into the single CycloneDX document published
  today, so that one artifact answers a "are we affected?" query across every ecosystem. A release
  MUST continue to publish exactly one SBOM.
- **FR-013**: Components MUST be identified by name **and resolved version** when reconciling the
  two inventories. Entries that share a name but differ in version are distinct components and MUST
  both be retained.
- **FR-014**: A component discovered by both sources at the same name and version MUST appear once,
  with its provenance from both sources preserved rather than one source's entry silently
  discarded.
- **FR-015**: The merged document MUST carry coherent top-level metadata describing a single
  product release, not the metadata of one input document with the other's components appended.
- **FR-016**: npm components that the image scan discovers but the lockfile cannot describe —
  third-party libraries vendored into the repository rather than installed as dependencies — MUST be
  retained in the merged document. The frontend inventory is additive to the image scan's npm
  findings, never a replacement for them.
- **FR-017**: Scope is limited to the workspace whose build output ships in the product image.
  Dependencies of test and build tooling MUST NOT be inventoried, so the document describes what
  ships and cannot produce advisory matches against packages no customer runs.

### Key Entities

- **Product SBOM**: The CycloneDX JSON document published with each release. Today it contains
  Java, OS-package, and incidental-npm components discovered by scanning the product image.
- **Frontend component inventory**: The set of npm packages that contribute to the shipped admin UI,
  derived from the frontend's resolved dependency graph. Each entry has a name, a resolved version,
  a package URL, and where available a license.
- **Release artifact set**: The collection published with a release, within which the SBOM is
  discoverable by downstream consumers.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A release SBOM contains the whole of `core-web`'s shipped production tree — on the
  order of 2,000 npm components, up from the 7 incidental ones measured today. Verified by
  comparing the published count against `pnpm sbom --prod` run on the release ref, not against a
  fixed number: the exact figure is platform-dependent (see Assumptions).
- **SC-002**: For a package known to be in the shipped frontend, a reviewer can determine from the
  SBOM alone whether a given release is affected by an advisory against it, in under 2 minutes and
  without consulting a lockfile or the source repository.
- **SC-003**: At least 95% of the frontend components in a published SBOM carry a license
  identifier.
- **SC-004**: 100% of frontend components carry a package URL.
- **SC-005**: The published output validates against the CycloneDX JSON schema at the same
  specification version published today, with zero schema errors.
- **SC-006**: Java and OS component counts in the SBOM are unchanged by this work, within the
  variation normally seen between releases.
- **SC-007**: A release in which the frontend inventory could not be generated is identifiable as
  such by a reader of the published artifacts, with no manual investigation of build logs.
- **SC-008**: A release publishes exactly one SBOM artifact, as it does today — consumers fetch and
  search one file.
- **SC-009**: Every component present in either input inventory is present in the merged document;
  no component is lost to reconciliation. Verified by comparing component counts before and after
  the merge.

## Assumptions

- **Generation point.** The frontend inventory is generated during the release build while the
  frontend's dependencies are installed, as the issue proposes. This is a deliberate choice over
  generating from the lockfile alone: a lockfile-only inventory was measured to produce the same
  2,365 components with **zero** license identifiers, because license text is read from the
  installed packages. Lockfile-only would satisfy P1 but not P2 (FR-002, SC-003).
- **Platform-dependent count.** The inventory is generated on the release runner, so
  platform-specific optional dependencies resolve for *that* platform. The image is multi-arch,
  so the count is an approximation of any single architecture's tree rather than an exact match.
  This is why SC-001 is expressed as "the whole production tree, on the order of 2,000" and
  verified by comparison against a `pnpm sbom --prod` run, not against a fixed number.
- **Production scope, with a known exception.** "What ships" is approximated by the production
  dependency tree — 1,981 components measured with a populated store — because the full tree including
  development dependencies is substantially larger and would over-report the shipped surface
  (FR-004). The approximation is imperfect and knowingly so: the Angular build copies
  `node_modules/tinymce` and `node_modules/monaco-editor` into the shipped output verbatim, and
  `monaco-editor` is declared under `devDependencies`. A production-only filter therefore misses at
  least one genuinely shipped component (FR-005).

  Both of those two happen to be caught today by the image scan, because they ship with their
  manifest intact — so FR-016 covers them without extra work. That is luck, not design: it holds
  only for packages copied whole, and would not survive someone adding a dev-declared package that
  the bundler inlines. Planning should decide whether to rely on that coincidence or to derive the
  shipped set from the build's asset configuration.
- **Tooling.** The frontend package manager's own SBOM generation is assumed to be the mechanism, as
  the issue proposes — it reads the same lockfile the install uses and adds no tool to maintain. The
  repository already pins this package manager at a version that provides it.
- **Merge over two artifacts.** Decided during specification: one document, because an SBOM's job is
  to answer one query across every ecosystem, and two files push that reconciliation onto every
  consumer. The cost is accepted — a merge step that must reconcile top-level metadata (FR-015) and
  match components by name *and* version (FR-013).
- **Specification version.** The existing Syft output declares CycloneDX **1.6**; the frontend
  tooling defaults to **1.7**. FR-006 is read as "match what is published today", i.e. the frontend
  output is pinned down to 1.6 rather than the Syft output being moved up.
- **Pipeline coverage.** Both the deprecated legacy release workflow and the current release-phase
  workflow invoke the *same* composite SBOM action, so work placed in that action reaches both
  without duplication. No change to either workflow's trigger or structure is assumed.
- **Integrity hashes.** The issue notes the frontend tooling emits no integrity hashes. Confirmed at
  the component level, with one correction: each component's distribution reference *does* carry a
  SHA-512 of the published tarball. The result is an inventory suitable for CVE correlation and
  license audit, not cryptographic verification of the shipped bundle — no requirement here assumes
  otherwise.
- **Release lines.** Only release lines that carry this change will produce frontend coverage.
  Backporting to active LTS lines is not assumed and is not specified here.

## Out of Scope

- **Fixing the failing Syft step.** The `'anchore-syft' executable script not found` failure
  described in Context has broken 3 of the last 4 release SBOMs. It is a defect in the existing
  Syft invocation on release lines that lack #36756, not in frontend coverage, and it deserves its
  own issue. It is recorded here because it bounds the value of this work.
- **Removing `continue-on-error` from the SBOM job.** Whether a missing SBOM should fail a release
  is a release-policy decision beyond this issue. FR-009 constrains only the visibility of a
  frontend-inventory failure.
- **Inventorying `dotcms-postman` or any other test/build tooling.** Its dependencies do not ship in
  the product image, and listing them in a *product* SBOM would over-report the shipped surface and
  generate advisory matches against packages no customer runs. The issue mentions it
  parenthetically; it is deliberately excluded (FR-017). If supply-chain visibility into test
  tooling is wanted, it belongs in its own artifact under its own issue.
- **Backporting to existing LTS release lines.**
- **Adding integrity hashes or attestation** for the shipped frontend bundle.
- **Vulnerability scanning or license-policy enforcement.** This work produces the inventory; acting
  on it is separate.
- **SBOMs for any artifact other than the product release**, such as per-PR or nightly builds.
- **The npm install-time hardening from #37553** (`trustPolicy`, `blockExoticSubdeps`,
  `minimumReleaseAge`), which is already in place and unaffected.

## Dependencies

- The existing composite SBOM action and the two release workflows that invoke it.
- The frontend package manager version pinned in the repository, which must provide SBOM generation.
- A release build stage at which the frontend's dependencies are installed (FR-002).

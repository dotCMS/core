# Feature Specification: Release SAML with dotCMS

**Feature Branch**: `issue-38104-saml-release-spec`

**Created**: 2026-10-19

**Status**: Draft — awaiting developer review

**Type**: New Feature

**Issue**: https://github.com/dotCMS/core/issues/38104

**Input**: Build SAML with every core release, like Tika: build from source, publish the matching artifact, and reference and include it automatically.

## Problem and Scope

Tika is built and published with the core release version, but SAML requires a separate manual release. The published dotCMS 26.10.19-01 system-bundles archive contains Tika 26.10.19-01 alongside SAML 26.08.21. Core can therefore ship an older SAML bundle even after fixes have been reviewed upstream.

Bring SAML source and its existing tests into core's release lifecycle. A release must build SAML, publish its JAR and POM, and include that same artifact without a separately maintained version pin or an external repository dispatch. Preserve existing artifact coordinates and OSGi identity.

This applies to releases built from the lineage containing this change. It does not republish 26.10.19-01, modify historical tags, automatically backport to LTS branches, or change releases built from older source lineages. Authentication redesign, unrelated dependency upgrades, retiring the upstream repository, and fixing the separate plugin-examples dispatch are out of scope.

## User Scenarios & Testing

### User Story 1 — Ship the matching SAML build automatically (Priority: P1)

As a release maintainer, I want the normal core release to build, publish, and bundle SAML so that no separate SAML release or manual version bump is needed.

**Why this priority**: Independently publishing a plugin version is insufficient if core still ships an older one.

**Independent Test**: Build a new release version from the integrated source, retrieve its published SAML JAR and POM, and compare the JAR with the one in the released system-bundles archive.

**Acceptance Scenarios**:

1. **Given** a release version that has never been published, **when** the normal core release succeeds, **then** the repository contains a SAML JAR and POM for that version and the published system-bundles archive contains a byte-identical SAML JAR.
2. **Given** a subsequent release with no changes to SAML source, **when** that release runs, **then** it produces and references its own matching SAML version without editing a separate SAML version constant.
3. **Given** a supported Java-variant release, **when** it runs, **then** SAML follows the same resolved release-coordinate suffix convention as the other release-built artifacts and the archive references that matching artifact.
4. **Given** a build where SAML is missing, has the wrong version, or cannot be compiled, **when** release verification runs, **then** the release fails before publishing images or Maven artifacts that would claim the incomplete build is a valid release.

### User Story 2 — Keep existing SAML installations compatible (Priority: P1)

As a customer using SAML, I want the newly bundled plugin to remain compatible with supported identity-provider configurations and retain the reviewed authentication protections.

**Why this priority**: Moving a security-sensitive bundle into the release build must not silently change its identity, omit resources, or drop tests and validation.

**Independent Test**: Run the imported SAML regression tests and verify the packaged bundle identity, required classes, resources, and loading in dotCMS; exercise valid and invalid login responses in a configured SAML environment.

**Acceptance Scenarios**:

1. **Given** the reviewed upstream source selected for import, **when** it is built in core, **then** its production code, resources, existing tests, and response-validation protections are retained without unrelated authentication changes.
2. **Given** an existing supported SAML configuration, **when** the matching plugin is loaded, **then** its existing bundle identity and configuration contract remain usable without customer configuration migration.
3. **Given** valid signed responses and assertions, **when** authentication runs, **then** login succeeds; wrong-audience, wrong-destination, expired, and replayed responses remain rejected according to the imported regression coverage.

### Edge Cases

- A warm Maven cache must not allow the previous standalone SAML version to satisfy verification for a new release.
- Restore-and-publish release paths must validate the matching SAML artifact rather than silently omit it.
- Maven and OSGi express versions differently; normalized OSGi metadata must still identify the same release.
- A fresh checkout must build without an already-published SAML artifact at the new version or a build-order cycle involving core and its bundled plugin.
- Missing publication credentials or failed uploads must not be reported as successful SAML publication.
- Existing immutable artifacts must not be overwritten to retrofit this change into a completed release.

## Requirements

### Functional Requirements

- **FR-001**: Core MUST build SAML from source versioned with core, without fetching mutable upstream source during the release.
- **FR-002**: The SAML artifact MUST use the resolved core build version, including supported release suffixes, without an independent release-version pin.
- **FR-003**: The normal release publication MUST upload the matching SAML JAR and POM under the existing `com.dotcms.samlbundle:com.dotcms.samlbundle` coordinates, using the same publisher, metadata/checksum handling, and existing CI credentials (`MAVEN_BUNNY_RW_USERNAME` / `MAVEN_BUNNY_RW_PASSWORD`) as the other release-built plugins. It MUST NOT require new publication secrets, operator-local credentials, a separate SAML release workflow, or a manual upload.
- **FR-004**: The published system-bundles dependency declaration and archive MUST reference and contain the matching SAML JAR; the packaged and published JARs MUST have identical checksums.
- **FR-005**: Build verification MUST reject missing or incorrectly versioned SAML outputs before release image or Maven publication begins. Upload failures MUST fail the publication job.
- **FR-006**: Integration MUST retain the upstream plugin's production classes, required resources, OSGi symbolic name and activator, external configuration contract, and existing regression tests. It MUST NOT introduce unrelated authentication or dependency changes.
- **FR-007**: A clean, uncached core build MUST resolve its build order without circular dependencies or a prepublished SAML artifact at the new release version.
- **FR-008**: Existing publication and packaging behavior for Tika and the other bundled artifacts MUST remain intact.
- **FR-009**: Developer-approved tests MUST demonstrate failure for absent SAML build/version/publication wiring before implementation; existing SAML regressions MUST run in the integrated module rather than merely compile.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Two distinct previously unpublished core build versions produce and reference their respective SAML versions with zero manual SAML version edits between builds.
- **SC-002**: For a controlled release validation, the published SAML JAR and the JAR included in the published system-bundles archive have identical checksums, and their version metadata identifies the same release.
- **SC-003**: Missing and mismatched SAML artifact cases are both rejected before publication; simulated upload failure cannot produce a successful publication result.
- **SC-004**: All imported automated SAML regression tests execute and pass with a nonzero reported test count. Existing Tika packaging checks still pass.
- **SC-005**: A configured SAML environment loads the new bundle and accepts a valid login while rejecting the invalid-response cases covered above, without changing the customer's existing supported configuration.

## Legacy Considerations

- **Existing behavior touched**: Bundled SAML authentication, OSGi plugin loading, and the existing release artifact lifecycle. This crosses older but active authentication and packaging surfaces.
- **Backward-compatibility expectations**: Preserve Maven coordinates, OSGi identity, customer configuration, and supported authentication flows. Existing released artifacts and tags remain unchanged; rollback to a previous image remains possible without a configuration migration introduced by this integration.
- **Known related decisions**: Tika already follows the core build/version/publication lifecycle. Relevant architecture decisions must be consulted during planning; this spec does not authorize changing established authentication or release contracts.

## Assumptions

- The import baseline is the reviewed upstream `dotCMS/com.dotcms.dotsaml` main commit `2006c0fa021f21dd78db9c9d467bac6cb43e7aff`, including the response validation merged in upstream PR #25. A different baseline requires explicit review.
- The pending standalone SAML 26.10.17 release and core version-pin PR are related work, not prerequisites or permission to merge them (dotCMS/com.dotcms.dotsaml#26 and dotCMS/core#38083).
- No build, publication, or authentication test has been performed for this integration yet. Spec approval precedes implementation planning; test approval and confirmed failing tests precede implementation.

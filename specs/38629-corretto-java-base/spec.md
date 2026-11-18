# Feature Specification: Corretto Java 25 and Java-base Preparation

**Feature Branch**: `issue-38629-corretto-java-base-spec`

**Created**: 2026-11-18

**Status**: Draft — awaiting approval before implementation

**Type**: New Feature

**Issue**: [Adopt Corretto Java 25 and prepare missing Java-base images (#38629)](https://github.com/dotCMS/core/issues/38629)

**Input**: Update dotCMS to the latest Corretto Java 25. When a PR changes `.sdkmanrc`, check for the corresponding Java-base image and build/push it if needed before building the test image. Create the issue/spec first. Do not add tests; the existing CI suite validates the result.

## User Scenarios & Testing

### User Story 1 - Build with Corretto Java 25 (Priority: P1)

As a maintainer, I can build and run dotCMS with Amazon Corretto
`25.0.5.1.2-amzn`, without mismatched defaults selecting an older runtime image.

**Why this priority**: The requested Java update must reach both the build and
its resulting runtime image.

**Independent Validation**: The existing CI pipeline builds the Corretto-based
test image and runs its existing suites against that image. Build/runtime
version output identifies Corretto Java 25.

**Acceptance Scenarios**:

1. **Given** the updated default Java selection, **when** CI builds dotCMS,
   **then** the build JDK and resulting test image use Corretto `25.0.5.1.2`.
2. **Given** the standard local or CI image build, **when** defaults are used,
   **then** Java-base and Maven runtime-image defaults agree with `.sdkmanrc`.

### User Story 2 - Prepare a changed Java version automatically (Priority: P1)

As a maintainer changing `.sdkmanrc`, I do not need to publish a Java-base image
manually before CI can build the test image.

**Why this priority**: The CI build already creates a test image before the
optional labeled PR-image publisher runs. Preparation after that build is too late.

**Independent Validation**: CI logs show change detection, the requested image,
its architecture availability, reuse or publication, and successful preparation
before the first dependent test-image build. Published manifests confirm both
required architectures.

**Acceptance Scenarios**:

1. **Given** a PR changing `.sdkmanrc` and no corresponding image, **when** CI
   prepares the build, **then** it publishes a base containing both required
   architectures before the first test-image build uses it.
2. **Given** a matching image containing both architectures, **when** preparation
   runs, **then** it reuses that image without rebuilding or pushing it.
3. **Given** a matching image lacking a required architecture, **when** preparation
   runs, **then** it publishes a complete image before dependent builds proceed.
4. **Given** an unchanged `.sdkmanrc`, **when** a PR builds, **then** this preparation
   does not introduce a registry check or base-image publication.
5. **Given** a registry, authentication, build or push failure, **when** preparation
   cannot establish a usable image, **then** dependent builds stop with a useful error.

### Edge Cases

- Detect `.sdkmanrc` changes across the complete PR diff, including changes made
  before its latest commit. A comment-only file change still triggers the check.
- An image is usable only when both `linux/amd64` and `linux/arm64` are present.
- Lookup failures such as authentication, rate limits or network errors are not
  evidence that an image is absent and must not trigger publication.
- Concurrent requests for one Java version must reuse an image completed by
  another run rather than unnecessarily overwrite it.
- Invalid or ambiguous Java entries fail before any publication; never execute
  `.sdkmanrc` as a script or interpret its value as shell code.
- Fork PRs cannot publish. If their requested base is missing or incomplete,
  report that a maintainer must prepare it; never expose publishing credentials.
- Runs must use the Java selection for the actual build checkout, including PR
  merge refs, rather than silently fall back to the default branch's Java version.

## Requirements

### Functional Requirements

- **FR-001**: Default build/runtime Java MUST be pinned to SDKMAN identifier
  `25.0.5.1.2-amzn`; Java-base and Maven runtime-image defaults MUST agree.
- **FR-002**: For a PR changing `.sdkmanrc`, CI MUST complete Java-base preparation
  before its first dependent test-image build, not only before optional PR-image publication.
- **FR-003**: Change detection MUST cover the complete PR diff. PRs not changing
  `.sdkmanrc` MUST retain their existing build path without this preparation.
- **FR-004**: Preparation MUST read the build checkout's Java entry as validated
  data and derive the existing `dotcms/java-base` tag convention, replacing `+`
  with `-` only in the Docker tag, not the SDKMAN installation identifier.
- **FR-005**: A matching image containing both required Linux architectures MUST
  be reused without rebuilding or publication.
- **FR-006**: If the image or either required architecture is absent, an authorized
  run MUST build/push a complete image and confirm availability before continuing.
- **FR-007**: Publication MUST use trusted base/default-branch workflow code and
  Java-base build context, never PR-supplied scripts, Dockerfiles or artifacts.
  Publishing credentials MUST NOT be exposed to PR-controlled execution or forks.
- **FR-008**: Invalid input, inspection errors other than confirmed absence, and
  build/push failures MUST stop dependent builds with an actionable explanation.
- **FR-009**: Concurrent preparation for the same tag MUST recheck availability
  before publication and reuse an image already completed by another run.
- **FR-010**: The manual Java-base workflow MUST remain available with its existing
  version, image-suffix, architecture and push controls.
- **FR-011**: Implementation MUST NOT add or modify test files. Validation MUST
  use the existing CI suites against the resulting image and CI/registry evidence
  for preparation order, reuse and architecture availability.

## Success Criteria

### Measurable Outcomes

- **SC-001**: The standard build and resulting test image report the requested
  Corretto Java 25 version, and the existing CI suites pass against that image.
- **SC-002**: A Java-selection change requiring a new base reaches a successful
  test-image build without a separate manual base-publication step.
- **SC-003**: An existing complete base causes zero rebuilds and zero pushes during
  preparation; an unchanged Java-selection file causes zero preparation lookups.
- **SC-004**: Every automatically prepared image supports both required architectures
  before dependent test-image builds start.
- **SC-005**: Failed preparation never starts the dependent test-image build, and
  unauthorized/fork requests perform zero base-image publications.

## Legacy Considerations

- **Existing behavior touched**: Java selection, Java-base publication and CI image
  build ordering. No legacy application code, content, schema or API changes.
- **Backward-compatibility expectations**: Keep Java 25 and existing compiler targets,
  image-tag conventions, runtime behavior and manual publication controls. Vendor
  compatibility is validated by the existing suite; it is not assumed from the major version.
- **Known related decisions**: The previous Java update is tracked in #38572.
  Immutable base references and lifecycle management remain separate work (#34782).
  Relevant architectural decisions must be consulted during planning, after approval.

## Assumptions

- `25.0.5.1.2-amzn` is the requested pinned release, not a moving "latest" selector
  ([Corretto release](https://github.com/corretto/corretto-25/releases/tag/25.0.5.1.2)).
- `.sdkmanrc` is the intended file; no `.sdkman` file is introduced.
- Existing image consumers remain on Ubuntu 24.04 LTS and Linux amd64/arm64.
- Trusted CI has Docker Hub publication credentials; fork PRs need a maintainer to
  supply a missing base through the trusted/manual path.
- **Developer validation decision**: The developer explicitly requested no tests
  because the existing suite runs the result. Do not add, modify or locally run
  test files for this work. Passing application suites alone does not establish
  lookup-error handling, reuse, ordering or publication permissions; retain CI
  logs and registry-manifest evidence for review.
- Scheduled SDKMAN upgrades, scheduled base rebuilds, digest pinning, base-content
  rebuilds unrelated to `.sdkmanrc`, Java-version override redesign, and unrelated
  Docker/security changes are out of scope.
- This PR contains only the specification. Implementation waits for its approval;
  first-rollout readiness must be established before relying on the new automation.

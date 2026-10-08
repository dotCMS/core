# Feature Specification: Remove License Levels

**Feature Branch**: `issue-37944-remove-license-levels`

**Created**: 2026-10-07

**Status**: Draft

**Type**: New Feature (cleanup of vestigial behavior)

**Input**: User description: "Remove license levels from dotCMS. Under BSL licensing, license levels are vestigial: every install runs at the top level (Platform), so level checks always pass and the 'is this a community install' check is always false. The dead branches confuse developers and AI tools. Delete the level checks and the never-taken community branches across the backend, the enterprise-feature gating mechanism, the legacy admin pages, and the modern admin UI, plus the tests that assert community-mode behavior. Keep the license-level fields that APIs return, with fixed values and a deprecation notice. Keep the public level lookup that plugins may call, deprecated and always reporting Platform. License-file plumbing (serial, server ID, heartbeat, license pool) is out of scope. Deliver as stacked PRs, with removal of the deprecated pieces in a later release."

## Background

dotCMS moved to the Business Source License (BSL). Feature tiers (Community, Standard,
Professional, Platform) no longer exist as a commercial concept. The code still carries them.
Every running install reports the Platform level. That happens because the active license is
always created with the Platform default and is never replaced, and the routine that once
dropped an install back to Community does nothing. So every "is the level high enough" check
passes, and every "is this Community" check fails, on every install.

The result is a large amount of code that looks meaningful and never runs. It includes
roughly 130 backend source files, about 40 legacy admin page and script files, and about 40
modern admin UI files. Developers and AI coding tools keep reading these branches as real
constraints. They propose license guards, write tests for "community edition" behavior, and
file security findings about guards that can be bypassed but protect nothing.

This feature removes the concept of license levels from product behavior. It keeps the few
outward-facing surfaces that external callers might depend on, and marks them for removal.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Product behaves exactly as it does today, without level gating in the code (Priority: P1)

A dotCMS administrator or content author uses the product after the change and sees no
difference. Every feature that is available today stays available. Nothing new appears or
disappears. A developer reading any area of the codebase no longer finds branches that
depend on a license tier.

**Why this priority**: This is the whole point of the feature. Behavior parity is what makes
the cleanup safe, and removing the dead branches is the value delivered.

**Independent Test**: Run the existing automated test suites, then exercise a sample of
previously gated features from the admin UI (for example push publishing, site search, the
rules engine, workflow features, multiple sites, and time machine). All of them behave
exactly as before. Search the codebase for license-level comparisons outside the deprecated
compatibility surface, and find none.

**Acceptance Scenarios**:

1. **Given** a fresh install with no license file, **When** an administrator opens any
   feature that was formerly gated by tier, **Then** the feature is available and works as it
   did before the change.
2. **Given** an install that still has a legacy license file uploaded with a lower tier
   recorded in it, **When** an administrator uses any feature, **Then** the feature is
   available, just as it is today.
3. **Given** the codebase after the change, **When** a developer searches for license-level
   comparisons or community-mode checks, **Then** the only matches are the deprecated
   compatibility pieces described in User Stories 2 and 3.
4. **Given** the admin UI after the change, **When** any user navigates the portal, **Then**
   no "upgrade your license" or "enterprise only" message appears anywhere it does not appear
   today.

---

### User Story 2 - Integrations that read license information keep working (Priority: P2)

An external integration, monitoring script, or customer tool calls the system configuration
or license information endpoints and reads the license tier fields. After the change it still
receives those fields with the same names, and the values match what every install returns
today: not Community, at the Platform level, with the Platform level name. The API
documentation marks those fields as deprecated and says they will be removed in a future
release.

**Why this priority**: Removing the fields outright would be an API contract break, and that
kind of change is unsafe to roll back. Keeping them is cheap and protects unknown consumers.

**Independent Test**: Call the configuration and license information endpoints before and
after the change, and compare the responses. The tier fields are present with identical
values. The published API description shows them as deprecated.

**Acceptance Scenarios**:

1. **Given** an authenticated caller, **When** they request the system configuration,
   **Then** the response still contains the community flag (false), the numeric level
   (Platform), and the level name (Platform).
2. **Given** an authenticated administrator, **When** they request license information,
   **Then** the response still contains the level fields it contains today, unchanged.
3. **Given** the published API documentation, **When** a reader looks at those fields,
   **Then** each one is marked deprecated with a note that it will be removed in a future
   release.

---

### User Story 3 - Plugins that query the license tier keep loading and working (Priority: P3)

A customer or partner plugin that asks the platform for the current license tier, or
compares against the named tiers, keeps compiling against the platform and keeps running. It
always gets the Platform answer, which is what it gets today. The plugin author sees a
deprecation warning at build time that tells them the lookup is going away.

**Why this priority**: Plugins are built outside this repository, so their usage cannot be
found by searching. Keeping the public lookup for one release cycle avoids breaking them
without warning.

**Independent Test**: Load a sample plugin that reads the license tier and branches on it.
Confirm it loads, gets the Platform answer, and takes the same path as before the change.

**Acceptance Scenarios**:

1. **Given** a deployed plugin that asks for the current license tier, **When** it runs,
   **Then** it gets the Platform level.
2. **Given** a plugin compiled against the new platform version, **When** its author builds
   it, **Then** the tier lookup and tier names produce deprecation warnings rather than
   compile errors.

---

### User Story 4 - The test suites stop describing behavior that cannot happen (Priority: P4)

A developer reading or maintaining the automated tests no longer finds tests that switch an
install into Community mode and assert that features refuse to work. Tests that only existed
to prove tier gating are removed. Tests that switched tiers as setup for some other purpose
keep testing that purpose without the tier switch.

**Why this priority**: The tests that simulate Community mode are the main thing that teaches
developers and AI tools that tiers still matter. They are lower priority than the product
code because they do not affect customers.

**Independent Test**: Search the test sources for tier switching or Community-mode setup and
find none. The suites that still exist pass.

**Acceptance Scenarios**:

1. **Given** the test sources after the change, **When** a developer searches for tests that
   put the install into a lower tier, **Then** none remain.
2. **Given** a test that previously downgraded the tier only as setup, **When** it runs after
   the change, **Then** it still checks the behavior it was written for.

### Edge Cases

- **An install has a legacy tiered license file uploaded.** Today the uploaded file does not
  change the active tier, which stays Platform. After the change that stays true. The license
  administration screen may still show the tier recorded in the file, because that display is
  part of the license-file plumbing that is out of scope.
- **A previously gated feature had a different code path for Community that did something
  rather than nothing** (for example a reduced feature set instead of a refusal). The
  Community path is never taken today, so it is removed, and the full path stays.
- **A tier check sits inside an older admin page or template** rather than in backend code.
  The same rule applies: keep the content that showed for licensed installs, and remove the
  alternative.
- **The modern admin UI reads the community flag from the configuration API.** It stops
  reading it and behaves as if the install is fully licensed. The API keeps sending the flag
  for other consumers (User Story 2).
- **A cluster node joins with a different stored license.** Clustering and license pool
  behavior is unchanged, because it is out of scope.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every feature available on an install today MUST remain available after the
  change, with the same behavior, for every user who can use it today.
- **FR-002**: Product behavior MUST NOT depend on a license tier anywhere outside the
  deprecated compatibility pieces listed in FR-005 and FR-006.
- **FR-003**: The mechanism that marks individual operations as "enterprise only" and blocks
  them below a tier MUST be removed, along with every place it is applied. The operations it
  guarded MUST keep running as they do today.
- **FR-004**: The legacy admin pages, templates and scripts, and the modern admin UI, MUST
  NOT show, hide, or change content based on license tier or community status.
- **FR-005**: The system configuration endpoint and the license information endpoint MUST keep
  returning their tier fields under the same names, with the values every install returns
  today (community false, level Platform, level name Platform). The API documentation MUST
  mark these fields deprecated.
- **FR-006**: The public lookups that plugins can call to read the license tier or compare
  against the named tiers MUST keep existing, MUST always report Platform, and MUST be marked
  deprecated.
- **FR-007**: Tests whose only purpose is to verify tier gating or Community-mode behavior
  MUST be removed. Tests that changed the tier only as setup MUST be updated to keep testing
  their real subject.
- **FR-008**: License-file handling MUST be unchanged. This covers uploading and deleting
  license files, the license pool for clusters, serial numbers, server identifiers, the
  heartbeat, and expiry messaging.
- **FR-009**: The work MUST be deliverable in independent, reviewable stages: backend
  together with the enterprise-only mechanism, then the legacy admin pages, then the modern
  admin UI. Each stage on its own MUST leave the product working and its tests passing.

### Key Entities

- **License tier**: The named feature level (Community, Standard, Professional, Platform)
  that the product used to grant features by. After this change it survives only as a
  deprecated, fixed Platform value for compatibility.
- **License file**: The uploaded license record with its serial number, client name, expiry,
  and the tier written into it. It is unchanged by this feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero product source files outside the deprecated compatibility pieces contain a
  license-tier comparison or a community-mode check after all stages ship. The starting point
  is about 210 files.
- **SC-002**: Zero test files switch the install to a lower tier or assert Community-mode
  behavior after the change.
- **SC-003**: All existing automated suites that run in CI pass after each stage, apart from
  tests the change deliberately removes. Every removed test has a recorded reason.
- **SC-004**: Responses from the system configuration and license information endpoints are
  identical before and after the change, field for field.
- **SC-005**: A manual spot check of at least eight formerly gated features finds each one
  available and working the same as before.
- **SC-006**: AI coding tools and new developers stop proposing tier guards or filing
  "license guard bypass" findings. This is observed qualitatively over the following
  quarter.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: This is a wide but shallow change across most of the
  product, because tier checks are scattered through both the modern and the legacy code.
  The legacy areas include the older admin pages and templates, publishing, site search, the
  rules engine, workflow, content types, and storage. Each change removes a branch that never
  runs. No working branch is altered.
- **Backward-compatibility expectations**: Content, admin workflows and stored data are
  unaffected. The API fields for license tier stay in place, deprecated (FR-005). The plugin
  tier lookups stay in place, deprecated (FR-006). Removing those deprecated pieces is a
  separate, later change, scheduled after at least one release has shipped with the
  deprecation notices. That later change is an API contract change and must be labeled as
  rollback-unsafe when it happens. This feature's own stages contain no rollback-unsafe
  change.
- **Known related decisions**: The move to BSL licensing is the decision that makes tiers
  obsolete. Long-standing behavior to respect: the active license is always created at the
  Platform default, and the downgrade-to-Community routine is already disabled. The plan
  phase consults `dotCMS/platform-adrs` for any recorded decision about licensing, feature
  gating, or plugin API stability.

## Assumptions

- Every install's active tier is Platform today, whatever license file is uploaded. This
  was confirmed by reading how the active license is created, and the plan should re-verify
  it before deleting anything.
- No commercial or legal requirement still needs a tier-gated feature. The project owner
  confirmed that license levels no longer exist as a concept.
- Removing the deprecated API fields and plugin lookups is a follow-up feature, not part of
  this spec. It needs its own release timing.
- License-file plumbing (serials, server identifiers, heartbeat, license pool, upload and
  delete) may still feed cluster node identity or usage reporting. It has not been traced, so
  it is left untouched and can be looked at separately.
- The file counts in this spec come from a text search on 2026-10-07. They are estimates, and
  the plan will produce the exact inventory.

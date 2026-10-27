# Feature Specification: Remove License Levels

**Feature Branch**: `issue-37944-remove-license-levels`

**Created**: 2026-10-07

**Status**: Draft

**Type**: New Feature (cleanup of vestigial behavior)

**Input**: User description: "Remove license levels from dotCMS. Under BSL licensing, license levels are gone: every install runs at the top level (Platform), so level checks always pass and the 'is this a community install' check is always false. The dead branches confuse developers and AI tools. Delete the level checks and the never-taken community branches across the backend, both feature-gating mechanisms, the legacy admin pages, and the modern admin UI, plus the tests that assert community-mode behavior. Remove the tier outright everywhere, with no deprecation period, because licensing is not something customers have to worry about. License-file handling (serial, server ID, heartbeat, license pool, upload and delete) is a separate follow-up. Deliver as stacked PRs."

## Background

dotCMS moved to the Business Source License (BSL). Feature tiers (Community, Standard,
Professional, Prime, Platform) no longer exist. The code still carries them, but every
answer is already fixed. The tier lookup always reports Platform. The checks that ask
"is this install authorized for this tier," "is it enterprise," and "is it Platform" all
answer yes. The check that asks "is it Community" answers no. Nothing a customer does,
including uploading an old license file, changes those answers.

The result is a large amount of code that looks meaningful and never changes behavior. It
covers roughly 135 backend source files, about 40 legacy admin page and script files, and
about 40 modern admin UI files. Developers and AI coding tools keep reading these branches as
real constraints. They propose license guards, write tests for "community edition" behavior,
and file security findings about guards that can be bypassed but protect nothing.

This feature removes license tiers from dotCMS completely. There is no deprecation period,
because tiers carry no meaning that any customer, integration or plugin should rely on.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Product behaves exactly as it does today, without tier gating in the code (Priority: P1)

A dotCMS administrator or content author uses the product after the change and sees no
difference in what they can do. Every feature that is available today stays available.
A developer reading any area of the codebase no longer finds code that depends on a
license tier.

**Why this priority**: This is the whole point of the feature. Behavior parity is what makes
the cleanup safe, and removing the dead code is the value delivered.

**Independent Test**: Run the existing automated test suites. Exercise a sample of formerly
gated features from the admin UI, for example push publishing, static publishing, site
search, the rules engine, workflow, multiple sites, time machine, LDAP login, and cache
directives in templates. All of them behave exactly as before. Search the codebase for tier
lookups or tier comparisons and find none.

**Acceptance Scenarios**:

1. **Given** a fresh install with no license file, **When** an administrator uses any
   feature that was formerly gated by tier, **Then** the feature works as it did before the
   change.
2. **Given** an install that has a legacy license file uploaded with a lower tier recorded
   in it, **When** an administrator uses any feature, **Then** the feature works, just as it
   does today.
3. **Given** the codebase after the change, **When** a developer searches for license tier
   lookups, tier comparisons, or community-mode checks, **Then** there are no matches.

---

### User Story 2 - Nothing presents a license tier anymore (Priority: P2)

An administrator browsing the admin UI, or an integration reading the system configuration
or license information, no longer sees a license tier. The admin UI shows no "upgrade your
license," "enterprise only" or tier label anywhere. The system configuration and license
information responses no longer contain tier fields.

**Why this priority**: Showing a tier that means nothing invites the same confusion this
feature exists to remove. It is second because it changes outward-facing responses, which
needs more care than deleting dead branches.

**Independent Test**: Browse the admin UI, including the license administration screen, and
find no tier name or upgrade prompt. Call the system configuration and license information
endpoints and confirm that the tier fields are gone and every other field is unchanged.

**Acceptance Scenarios**:

1. **Given** an authenticated caller, **When** they request the system configuration,
   **Then** the response has no community flag, numeric level or level name, and every other
   field is unchanged.
2. **Given** an authenticated administrator, **When** they request license information,
   **Then** each license entry has no tier field, and every other field (serial, server,
   expiry, last ping and so on) is unchanged.
3. **Given** any admin screen, **When** an administrator views it, **Then** no tier label,
   "enterprise only" marker or upgrade prompt appears.

---

### User Story 3 - The test suites stop describing behavior that cannot happen (Priority: P3)

A developer reading or maintaining the automated tests no longer finds tests that switch an
install into a lower tier and assert that features refuse to work. Tests that only existed
to prove tier gating are removed. Tests that switched tiers only as setup for some other
purpose keep testing that purpose without the tier switch.

**Why this priority**: The tests that simulate Community mode are the main thing that teaches
developers and AI tools that tiers still matter. They come last because they do not affect
customers.

**Independent Test**: Search the test sources for tier switching or Community-mode setup and
find none. The suites that still exist pass.

**Acceptance Scenarios**:

1. **Given** the test sources after the change, **When** a developer searches for tests that
   put the install into a lower tier, **Then** none remain.
2. **Given** a test that previously changed the tier only as setup, **When** it runs after
   the change, **Then** it still checks the behavior it was written for.

### Edge Cases

- **An install has a legacy tiered license file uploaded.** The tier recorded in that file
  already has no effect, and it still has none after the change. The file itself and the
  rest of license-file handling stay as they are (FR-008). The tier is simply no longer read
  or shown.
- **A plugin calls the removed tier lookup.** The plugin fails when it reaches that call.
  This is accepted, because tiers have no meaning a plugin should rely on. The release notes
  must say the tier lookup was removed and that callers should drop the check.
- **An integration reads the removed tier fields.** It finds them missing. This is accepted
  on the same grounds and goes in the same release note.
- **A formerly gated feature had a Community code path that did something rather than
  nothing**, for example a reduced feature set instead of a refusal. That path is never
  taken today, so it is removed and the full path stays.
- **A tier check sits in an older admin page or template** rather than in backend code. The
  same rule applies: keep what a licensed install shows today, and remove the alternative.
- **The rollback case.** If an install rolls back to a version from before this change, that
  version's admin UI reads the tier fields from its own backend, so it is unaffected. A
  mixed-version cluster during a rolling upgrade keeps each session on one node, because
  clusters already require sticky sessions. An old UI therefore talks to an old backend and
  still gets the tier fields it expects.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every feature available on an install today MUST remain available after the
  change, with the same behavior, for every user who can use it today.
- **FR-002**: The concept of a license tier MUST be removed from product code. That covers
  the tier names, the tier lookup, the "is it community / enterprise / Platform" checks, and
  every comparison against a tier.
- **FR-003**: Both feature-gating mechanisms MUST be removed: the marker that blocks
  individual operations below a tier, and the per-feature lists of allowed tiers. The
  operations they guarded MUST keep running as they do today.
- **FR-004**: The legacy admin pages, templates and scripts, and the modern admin UI, MUST
  NOT show, hide, or change content based on license tier or community status, and MUST NOT
  display a tier.
- **FR-005**: The system configuration response and the license information response MUST
  stop returning tier fields. All of their other fields MUST stay unchanged.
- **FR-006**: The release notes for the version that ships this change MUST state that the
  license tier fields and the tier lookup were removed, and that integrations and plugins
  that read them should drop those checks.
- **FR-007**: Tests whose only purpose is to verify tier gating or Community-mode behavior
  MUST be removed. Tests that changed the tier only as setup MUST be updated to keep testing
  their real subject.
- **FR-008**: License-file handling MUST otherwise be unchanged. That covers uploading and
  deleting license files, the license pool for clusters, serial numbers, server identifiers,
  the heartbeat, and expiry messaging. Removing that machinery is a separate follow-up.
- **FR-009**: The work MUST be deliverable in independent, reviewable stages: the backend
  with both gating mechanisms, then the legacy admin pages, then the modern admin UI. Each
  stage MUST leave the product working and its tests passing. Removing the tier fields from
  responses MUST NOT ship before the admin UI stops reading them.

### Key Entities

- **License tier**: The named feature level (Community, Standard, Professional, Prime,
  Platform) that the product used to grant features by. This feature removes it.
- **License file**: The uploaded license record with its serial number, client name and
  expiry. It may contain a recorded tier, which nothing reads after this change. Otherwise
  it is unchanged by this feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero product source files contain a license-tier lookup, tier comparison or
  community-mode check after all stages ship. The starting point is about 215 files.
- **SC-002**: Zero test files switch the install to a lower tier or assert Community-mode
  behavior after the change.
- **SC-003**: All existing automated suites that run in CI pass after each stage, apart from
  tests the change deliberately removes. Every removed test has a recorded reason.
- **SC-004**: The system configuration and license information responses differ from
  today's only by the missing tier fields.
- **SC-005**: A manual spot check of at least eight formerly gated features finds each one
  available and working the same as before.
- **SC-006**: AI coding tools and new developers stop proposing tier guards or filing
  "license guard bypass" findings. This is observed qualitatively over the following
  quarter.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: This is a wide but shallow change across most of the
  product, because tier checks are scattered through both the modern and the legacy code.
  The legacy areas include the older admin pages and templates, publishing, site search, the
  rules engine, workflow, LDAP, dashboards, Velocity cache directives, content types,
  clustering checks, and storage. Each change removes a check whose answer is already fixed.
  No working behavior is altered.
- **Backward-compatibility expectations**: Content, admin workflows and stored data are
  unaffected. Two outward surfaces change on purpose. Tier fields disappear from two API
  responses, and the public tier lookup disappears for plugins. Both are removed with no
  deprecation period, by decision, and are covered by a release note (FR-006). Removing
  fields from API responses is an API contract change, so the stage that does it must carry
  the rollback-unsafe label even though the rollback risk is low (see Edge Cases).
- **Known related decisions**: The move to BSL licensing is the decision that makes tiers
  obsolete. Long-standing behavior to respect: every tier answer is already fixed at the
  most permissive value. ADR-0020, which deprecated the folder `byPath` endpoint, is the
  usual pattern for retiring an API surface with a deprecation window. This feature departs
  from it on purpose, because the tier fields carry no information. The plan records that
  departure.

## Assumptions

- Every tier answer is fixed today, whatever license file is uploaded. This was confirmed by
  reading the license code. The plan should re-verify it before deleting anything.
- No commercial or legal requirement still needs a tier-gated feature. The project owner
  confirmed that license levels no longer exist as a concept.
- License-file handling may still feed cluster node identity or usage reporting. It has not
  been traced, so it is left in place and handled as a separate follow-up.
- The file counts in this spec come from a text search on 2026-10-07. They are estimates, and
  the plan will produce the exact inventory.

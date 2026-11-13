# Issue Resolution Specification: Allow long cluster identifiers

**Feature Branch**: `issue-36175-cluster-id-spec`

**Created**: 2026-11-12

**Status**: Draft — awaiting approval by another developer

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: https://github.com/dotCMS/core/issues/36175

**Input**: Support longer Cloud Control environment names; let the database accept cluster identifiers up to its own storage and indexing limits rather than imposing 36- or 255-character cutoffs. Increase every affected cluster-ID field, not unrelated identifiers.

## Problem Statement *(mandatory)*

A valid Cloud Control environment name can prevent dotCMS from becoming ready because the corresponding cluster identifier is longer than its database columns allow. Retrying provisioning does not repair the schema. The earlier fix widened only one column to 255 characters and did not cover fresh installations.

**Severity / Impact**: Affected instances remain unavailable. The observed environment repeatedly timed out waiting for readiness while dotCMS logged `ERROR: value too long for type character varying(36)`.

## Reproduction *(mandatory)*

**Environment**: PostgreSQL-backed dotCMS provisioned by Cloud Control, including environment `dotpr/38557-issue-38556-preserve-legac`.

**Steps to Reproduce**:

1. Start dotCMS with cluster identifier `dotpr-38557-issue-38556-preserve-legac` (38 characters) on a fresh database using the current schema.
2. Allow startup, cluster registration, and application health checks to run.
3. Repeat with a database that already recorded the earlier cluster-ID widening migration, with one cluster-ID column at 255 characters and the other at 36.

**Expected Behavior**: Valid identifiers fitting PostgreSQL's storage and primary-key index limits can be persisted unchanged in both cluster metadata and server registrations, without these character-length failures.

**Actual Behavior**: A cluster metadata insert fails against a 36-character column; widening that column alone leaves server registration subject to the same problem.

**Reproducibility**: Deterministic when the supplied identifier exceeds either column's declared character limit.

## Scope of Investigation *(mandatory)*

- **Affected area**: Cluster identity persistence, fresh-install database definitions, and startup upgrades.
- **Suspected surface**: Legacy startup and database code under `com.dotmarketing.*`, with cluster registration consumers under `com.dotcms.*`.
- **Related known decisions**: Preserve the existing cluster identity and relationships. Consult applicable database and upgrade ADRs during planning; this specification does not introduce a new identity scheme.

## Root-Cause Hypothesis

The cluster metadata and server-registration columns were originally sized for UUIDs. The earlier migration broadened only the metadata column, while new installations still used the original schema. An older table-creation path also initializes clustering before later widening migrations can run.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Remove fixed character-length limits from every PostgreSQL column storing this cluster identifier, including both metadata and server registration.
- Cover fresh installations, legacy cluster-table creation, and upgrades from 36-character or partially widened schemas.
- Repair databases that already recorded the earlier widening migration.
- Preserve existing identifiers, rows, primary keys, foreign keys, and nullability constraints; repeated upgrade execution must be safe.

**Explicitly out of scope / non-goals**:

- Changing UUID fields such as server IDs, unrelated database columns, or unsupported legacy database engines.
- Renaming Cloud Control environments or changing Kubernetes, DNS, PostgreSQL database-name, or Docker-tag limits.
- Truncating, hashing, regenerating, or migrating the values of existing cluster identifiers.
- Bypassing PostgreSQL's native storage or index-entry limits.
- Applying manual production DDL, changing image tags, or deleting/recreating an environment as part of spec review.

## Regression Risk *(mandatory)*

- **Blast radius**: Cluster startup and server registration share the affected schema. Review all creation and upgrade paths, not only the failing insert.
- **Backward compatibility**: Broaden storage without transforming data or removing constraints. No REST, GraphQL, or search-index contract changes are intended. Application rollback must retain the widened schema; narrowing it after long IDs have been stored is unsafe.
- **Data considerations**: Existing data and references must remain byte-for-byte unchanged. Schema alteration takes PostgreSQL locks and belongs in the normal controlled upgrade path.

## Acceptance & Verification *(mandatory)*

- **AC-001**: A fresh database accepts the observed 38-character identifier in both cluster metadata and server registrations.
- **AC-002**: Both columns also accept identifiers longer than 255 characters, without application-defined character cutoffs or value truncation, subject to PostgreSQL's native limits.
- **AC-003**: Databases with 36-character columns and databases with a 255-character metadata column plus a 36-character server column are repaired without losing existing rows or relationships.
- **AC-004**: The repair is registered as a new startup upgrade, so an earlier recorded migration does not prevent it from running. It detects a still-bounded server column even when metadata is already unbounded.
- **AC-005**: Repeating the upgrade is safe; once both columns are unbounded it reports no further repair needed.
- **AC-006**: Legacy PostgreSQL table creation supports long identifiers before that task initializes clustering.
- **AC-007**: Regression coverage restores altered schemas and removes inserted fixture rows, leaving other tests unaffected.
- **Verification method**: Run the fresh-schema and legacy-creation unit tests (`ClusterIdSchemaTest`, `ClusterConfigSchemaTest`) and the existing migration integration class (`Task260615AlterClusterIdLengthTest`) with the Maven build cache disabled. Verify nonzero test counts in reports. After an approved implementation passes CI and is deployed through the controlled rollout, monitor the affected environment for the absence of these column-length errors and successful readiness; investigate unrelated remaining failures separately.

## Assumptions

- PostgreSQL is the supported database for this rollout.
- Cloud Control legitimately supplies a human-readable cluster identifier; its value must not be shortened to hide a database defect.
- Fixing these column limits does not guarantee that unrelated infrastructure or application failures are absent.

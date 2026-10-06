# Restore publication of nested Maven groups

Issue: [#37854](https://github.com/dotCMS/core/issues/37854).
Implementation: [#37855](https://github.com/dotCMS/core/pull/37855).

## Problem statement

The Artifactory-to-Bunny migration replaced Maven deployment with a publisher
that scans only `com/dotcms/<artifactId>/<version>`. Release modules whose group
IDs extend `com.dotcms` are built locally but omitted from the public repository.
Customer plugin builds against newer core releases cannot resolve the required
`com.dotcms.core.plugins:com.dotcms.tika-api` compile dependency.

This is a customer-blocking hotfix. The developer approved the written failing
regression tests and the fix-first, separate-defect-spec-PR route. This spec is
prepared for that separate follow-up PR, not the implementation PR.

## Reproduction and evidence

Observed against the public Bunny CDN on October 1, 2026:

- Tika API `26.09.14-01` JAR and POM resolve.
- Core POMs for `26.09.17-02` and `26.09.28-02` declare matching-version Tika API
  compile dependencies; those matching Tika JARs/POMs do not resolve.
- The nested core-plugin parent and newer core/system bundle POMs are also missing.
- Eight offline regression test methods failed before the fix because nested
  paths were not discovered; four existing-behavior tests passed.

## Root cause

`cmd_maven` used a single-depth glob and reconstructed destinations from
artifactId alone. Metadata generation also hardcoded `com.dotcms` as groupId.
Actual Tika artifacts live at
`com/dotcms/core/plugins/com.dotcms.tika-api/<version>`.

## Fix scope and non-goals

- Recursively discover matching version directories under `com/dotcms`, retaining
  complete Maven repository-relative paths for upload and metadata.
- Derive metadata groupId from the group path, with artifactId kept separate.
- Apply artifactId-based module filters at all group depths. Preserve distinct
  paths when the same artifactId appears in multiple groups.
- Retain version selection, checksum generation, snapshot refusal, dry-run
  behavior, existing flat-group uploads, and metadata-listing failure protection.
- Add offline publisher tests and a scoped PR regression-test workflow.
- No changes to Java APIs, dependency versions, or customer-facing coordinates.
- Live repository backfill is a separate authorized operational step, not part
  of implementation CI. Copy each release's exact original GitHub Actions
  `maven-repo` files; do not rebuild JARs or rename older release artifacts.
- Fail-closed handling of directory-scan errors is explicitly deferred by the
  developer to keep this hotfix narrow.

## Acceptance and verification

1. Flat and nested group JAR/POM/ZIP artifacts publish to their original Maven
   paths, including nested POM-only parent modules.
2. Metadata is written beside each artifact with its actual groupId, artifactId,
   prior version history, and correct unqualified latest/release values.
3. Artifact and metadata SHA-1/MD5 sidecars match the uploaded bytes.
4. Module filters find nested artifacts without flattening or mixing groups.
5. Unrelated versions/groups and configured local-file exclusions are not uploaded.
6. Offline tests, Bash syntax validation, and ShellCheck pass; CI runs the tests
   whenever the publisher, action, or test workflow changes.
7. Operational completion additionally requires restoring omitted matching-version
   artifacts from original release builds, verifying public CDN objects and
   checksums, and a cold consumer resolution against an affected core release.
   Record backfill and consumer-verification evidence on the issue separately
   from code CI; copying binaries must not involve rebuilding them.

## Regression and operational risks

- Directory-scan failure can still produce a successful partial publish; compare
  dry-run output against an expected release-module inventory before backfilling.
- Metadata and checksum failures remain best-effort warnings.
- Publishing overwrites existing destination keys. Use only original release
  build artifacts, verify their provenance, and leave unrelated existing files
  untouched. If original sources are unavailable, report rather than reconstruct.
- Serialize backfills with normal publications of the same artifact, and handle
  cached CDN misses before declaring customer resolution restored.

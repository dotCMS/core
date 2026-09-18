# Binary S3 storage checks

## Content-addressed S3 storage

New binary and rendition writes now keep one immutable copy of their exact bytes at
`asset-blobs/sha256/<chars-1-2>/<chars-3-4>/<chars-5-6>/<chars-7-8>/<sha256>`.
Existing one-level blobs remain readable through a fallback when the four-level key is absent;
new writes and backfills use four levels and retain existing blobs. The content-owned
keys hold small reference objects marked by the S3 user-metadata header
`dotcms-blob-sha256`. The database still owns the filename, field and version reference;
permissions continue to be enforced by dotCMS. A private upload snapshot prevents
hashing one version of a file and uploading another. Existing blobs are compared by
bytes, and cold downloads verify SHA-256 before entering the local cache. Existing
raw S3 objects remain readable. Backfill verifies a raw object's bytes and replaces
it with a hash reference only if its S3 ETag still matches the version that was read.
A concurrent replacement or deletion causes migration to fail and retain local
sources; it cannot overwrite the replacement or resurrect the deleted object.

Tika's byte-stream extraction is cached separately at
`extracted-metadata/<source-sha256>/<configuration-hash>.json`. Configuration includes
the actual parser bundle version, binary metadata schema version and extracted-text
limit. Filenames, local paths, fallback titles and modification times are added by
the existing per-use metadata layer afterwards; custom attributes and focal points
remain in their existing content-owned snapshots. Unknown parser versions and
flag-off mode retain direct extraction. Empty/failed extraction is not published.

Ordinary content/field cleanup deletes owned references, not shared blobs. Shared
blobs and extracted metadata are currently retained: safe reclamation across active
references, in-flight uploads and independent nodes remains required work. Rendition
bytes deduplicate, but avoiding duplicate computation through a shared transformation
recipe index is also still pending. The backfill worker now migrates referenced raw
originals (including retired binary definitions) and recognized completed renditions.
The demo now runs `legacy-20260915`. Its administrator migration job verified 545
originals, including both legacy Image filename objects, and migrated recognized
renditions. The same job recovered automatically after an actual process kill,
using its persisted cursor/count, and completed with one retry. Two legacy images
passed cold HTTP retrieval with SHA blob GETs captured by MinIO. Evidence and a
standard-library verifier are in `dotCMS/target/s3-poc/proof-restart-20260915/`.
Orphan inventories and full independent CMS node acceptance remain open.
These limits must not
be presented as completed lifecycle support.

All of this is gated by `FEATURE_FLAG_S3_ASSET_STORAGE`. The filesystem/NFS path is
unchanged when disabled. The running demo now uses this layout. Its original GIF,
migrated/new renditions and a fresh mixed-case upload passed cold HTTP retrieval
with actual SHA-blob GETs recorded by MinIO. Duplicate uploads shared one blob and
one extraction; deleting one owner preserved cold retrieval of the other. GIF
frame count and timing were preserved. Evidence is in
`dotCMS/target/s3-poc/proof-sha-20260915/proof-summary.json`.

## Metadata generation from evicted originals

With S3 enabled, `FileStorageAPI` restores the exact supplied binary path only when
metadata generation needs its bytes, then retains a cache lease through basic
inspection, hashing and Tika parsing. Raw basic/full callers use the same path.
Restore failures propagate; shared-extraction storage/I/O failures also propagate
through full generation instead of becoming a successful partial metadata result.
Disabled mode retains its existing local generation and error fallback.

JSON metadata hydration reads the linked image's owner key, not the parent content's
binary key. Complete stored hydration metadata avoids normalizing or downloading the
original. Missing metadata can be regenerated from a cold legacy or immutable revision
path, including a path exported from a different installation root. The normalization
retains mixed-case names and revision identity. The CMS/MinIO regression cases for
these paths passed on 2026-09-16; see the latest completion checkpoint.

## Sharing one bucket across installations

With the S3 feature enabled, set `storage.file-metadata.s3.namespace` (Docker:
`DOT_STORAGE_FILE_METADATA_S3_NAMESPACE`) before the installation first stores
assets. Use the same stable value for nodes in one CMS cluster and distinct values
for independent installations, such as `Customer-Production` and `Customer-Staging`.
Values contain 1–64 letters, digits, underscores or hyphens and start with a letter
or digit; case is preserved. Empty is the default and preserves the existing layout.
The setting is ignored when the feature flag is off.

Owned keys become `asset-namespaces/<namespace>/<group>/<existing-key>`. This covers
binary/rendition references, editorial metadata, temporary uploads, WebDAV records,
publishing archives and recovery ZIPs. Shared bytes remain under `asset-blobs/sha256/`
and byte-derived extraction remains under `extracted-metadata/`, allowing duplicate
content to be shared without combining ownership or editorial values. Missing
namespaced records do not fall back to root-layout records or another namespace.

This separates application keys; it does not restrict what bucket credentials can
access. Separate installations need separate local asset/cache roots as well.
Changing a namespace on an existing installation does not migrate its records;
keep its original value until a verified migration is available. Shared blob
reclamation and full independent-CMS-node acceptance remain open.

Validation: 66 core tests passed (two fixture skips), including real MinIO owner
isolation with separate clients. Twenty CMS tests passed with namespace `Acceptance`,
including cold binary/metadata restore and recovery archives. With the flag off,
51 CMS/metadata tests passed (fifteen expected skips) despite an invalid namespace
and unavailable S3 endpoint. See the latest completion checkpoint for logs. The
running demo predates this optional namespace setting and retains its root layout.

## Legacy Image/File values and retired binaries

Migration, starter asset export and full-content recovery now use the same persisted
reference inventory. Typed `Binary` entries remain included even when their field
definitions have been retired. An `Image` or `File` entry with a plain filename is
included only if the exact `<inode-shards>/<inode>/<field>/<filename>` object exists
in the combined filesystem/S3 inventory. Ordinary linked asset identifiers, external
URLs and unrelated text fields do not become binaries. Listing failures propagate;
they are not interpreted as absence.

This preserves the original JSON types and values, carries existing raw metadata,
and uses the same conditional SHA backfill as other binaries. Export restores
missing local files before checking size filters and writing the ZIP. Temporary
metadata lookup values are applied to separate content snapshots, preserving cached
Image/File strings. Main's Contentlet and JSON behavior is unchanged.

The demo's two older Spanish Image-field assets are still readable through the
registered `image` exporter. An exploratory `raw` request returned 404 because that
exporter is not registered; it was not a storage failure. The migration/export/backup
omission is fixed and deployed. Both images were migrated and passed cold SHA
retrieval on the updated demo. Migration-worker process recovery also passed;
cleanup-worker restart remains in the completion checklist.

## Binary field trash

With the S3 feature enabled, deleting a binary field records a `binaryFieldCleanup`
request in the same transaction as the field deletion. The existing queue retries
failures. Requests cover historical and working content versions up to the deletion
timestamp; nonbinary values belonging to a later field incarnation are retained.

Cleanup locks each content row, uploads and verifies a recovery ZIP, then clears the
old field reference and records the exact cleanup inventory transactionally. A
separate cleanup job checks that the archived files are no longer referenced and
that the recovery ZIP remains available before deleting metadata, original files
and disposable renditions. It never deletes an entire field prefix, so uploads made
after the inventory was captured survive a retry. Direct submissions through the
public job endpoint are rejected; the authorized field deletion API creates this
internal work. Flag-off scheduling and local trash behavior are unchanged.

Field recovery ZIPs use the same `deleted-content-backups` group and owner/key layout
as deleted-content archives below. They contain `contentlet.json`, `binary-assets/`
for the field's physical file inventory, and `dotmetadata/` for logical metadata.
These namespaces are distinct because local metadata can live beside original files.
The inventory includes unreferenced revisions and local-only files. Recovery and
retention are manual/operator-controlled; automatic database restoration and
migration of pre-existing local trash remain separate work.

## Deleted-content recovery archives

When both `FEATURE_FLAG_S3_ASSET_STORAGE` and the existing
`BACKUP_DELETED_CONTENTLETS_TO_DISK` option are enabled, deletion writes a verified
S3 recovery ZIP before removing content. Archives live in the separate
`deleted-content-backups` group at `<identifier>/<inode>/<uuid>.zip`. Full destruction
and all-version deletion archive each distinct version; single-version deletion
archives only that version. Failed backup publication aborts deletion. Source files
are copied rather than moved, and binary cleanup does not delete recovery archives.

Each ZIP contains `contentlet.json` (the persisted row's complete typed field data),
`contentlet.xml`, and `assets/` entries using the original binary and metadata paths.
Field directories keep equal filenames distinct. Persisted binary fields whose
definitions were removed are included. Backup locks the content row while reading
its snapshot, and holds the existing cache lease while consuming binary files.

Operators can download these standard ZIPs directly from S3 for manual recovery.
Internal callers can use `ContentletBackupStorage.list(identifier)` and `open(key)`;
closing the returned stream removes its private download. These are recovery
artifacts, not whole-site starters or an automatic database restore API. Archives
may remain after a later transaction rollback and are retained until explicitly
removed or expired by an operator's bucket policy. Existing local backup migration,
field trash and broader restore/retention acceptance remain open.

The public `deleteAllVersionsandBackup` interceptor was previously a no-op. With the
S3 feature enabled it now invokes its existing implementation and the existing
all-version deletion hooks; disabled mode retains the legacy no-op. Other disabled
backup behavior remains unchanged. `ContentletBackupStorageTest` exercises real
CMS/MinIO destroy, all-version and single-version backup/cleanup, retired fields,
equal mixed-case filenames, and backup-outage rollback. The core gate test checks
disabled isolation, hook veto and failure propagation.

## WebDAV staging checkpoint

Enabled WebDAV temporary uploads now persist completed payloads and discoverable path
records in the `webdav-temporary` S3 group. The focused real MinIO test covers cold
reads across independent local roots, mixed-case names, failed replacement, copy and
delete. `DotWebdavHelperTest` covers the actual temporary resource handlers and normal
URL compatibility in enabled and disabled modes, including mixed-case CMS-folder
routing and remote-only site/language-root temporary listings. The latest results and log paths are
in the completion checklist. Remote cleanup now protects completed and pending payload
references and conditionally tombstones expired path records. MinIO ignored conditional
DELETE in acceptance, so physical tombstone reclamation remains open.

Actual kernel NFS 4.1 checks passed: 30 focused tests and five provider phases across two
independent client mounts/JVMs with the S3 flag disabled. The reproducible fixture is
`docker/docker-compose-examples/s3-asset-storage-poc/nfs-acceptance/README.md`.
This does not establish full CMS cluster or NFS failover behavior. Historical results
below describe earlier checkpoints; use the completion checklist for current status.

## Feature flag and current fix status

### Credentials and region

With the feature enabled, leave both `storage.file-metadata.s3.access-key` and
`storage.file-metadata.s3.secret-access-key` unset to use the AWS SDK credential
provider chain. The configured bucket region and endpoint are preserved in this
mode. A partial explicit key pair fails initialization. The matching SDK STS module
is packaged for role-based providers, including web identity; deployment-specific
AWS IAM trust policies and OIDC exchange still need acceptance in their environment.

Without a custom endpoint, a configured region selects the regional AWS endpoint;
an absent region uses the SDK region provider chain. A custom endpoint must be an
absolute HTTP(S) URL and requires `storage.file-metadata.s3.bucket-region` for
signing. Invalid endpoint configuration fails instead of silently selecting AWS.
Static publishing keeps its own endpoint and region settings. Feature-disabled
configuration retains its previous behavior.

The isolated temporary-credential fixture is documented in
`docker/docker-compose-examples/s3-asset-storage-poc/sts-acceptance/README.md`.

### Existing behavior

S3 asset behavior now requires `FEATURE_FLAG_S3_ASSET_STORAGE=true` (Docker:
`DOT_FEATURE_FLAG_S3_ASSET_STORAGE=true`). It defaults to false; the old
`BINARY_ASSET_STORAGE_TYPE=BINARY_CHAIN` setting alone does not opt in. Restart
when changing provider configuration. Disabled callers retain main's filesystem/NFS
paths. The running demo opts in and runs the tested `legacy-20260915` worktree image,
including the metadata, export, backfill, starter-cleanup and SHA changes.

Warm rendition hits no longer hash/contact S3 or upload a local copy. This prevents
an outage from breaking an otherwise cached response and prevents warm reads from
republishing invalidated objects. Existing assets can be copied and verified with
`BinaryAssetBackfill.runBatch(afterInode, limit)` or the administrator job API
documented below. Broader legacy inventory remains open. The prior warm-backfill behavior described
in historical validation below has been superseded.

See [the completion checklist](S3_ASSET_STORAGE_COMPLETION.md) for remaining work.
The full regression previously passed 88 reported cases (87 passed; one existing
native libvips availability skip), including seven real MinIO cases. After closing
remaining default-off filesystem and exporter differences, the focused rerun
compiled core and passed 82 tests with one native libvips skip (83 reported).
That rerun did not include MinIO. New disabled-mode checks cover legacy provider
path normalization, missing-file behavior, and exporter parameters without storage
calls. Actual NFS provider validation has since passed as described above; the full
implementation goal remains open.


Run from the repository root. The S3 checks use the actual filesystem provider,
chain, AWS adapter, and a disposable MinIO bucket. Only the chain's 404 cache and
the local asset-directory configuration are replaced in the test. No storage
provider is mocked.

```sh
docker run -d --rm --name binary-s3-test \
  -p 127.0.0.1:19002:9000 \
  -e MINIO_ROOT_USER=binary-storage-test \
  -e MINIO_ROOT_PASSWORD=binary-storage-test \
  minio/minio:latest server /data

./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
  -Dtest=AssetStorageFeatureTest,BinaryAssetStorageAPIImplTest,BinaryCacheEvictionJobTest,BinaryS3StorageTest,BinaryFileSystemStorageTest,ImageFilterExporterTest,ImageFilterExporterSharedStoreTest,ImageFilterExporterEngineSelectionTest \
  -Ds3.test.endpoint=http://127.0.0.1:19002 \
  -DskipTests=false -Dskip.surefire.tests=false

docker stop binary-s3-test
```

`BinaryS3StorageTest` is explicitly skipped without `s3.test.endpoint`; the other
checks always run. CI needs to supply this service and property to run the S3
checks. The credentials above are disposable local test credentials.

Coverage includes a differently named upload source, mixed-case fields and
filenames, verified eviction, cold-cache filename discovery, synchronous local
restoration, version copying, field and inode deletion, remote-only siblings,
prefix isolation, more than 1,000 objects, and slash-prefixed hashed metadata keys.
Eviction checks cover local-only assets, filesystem fallback, missing or stale S3
copies, matching prefixes, S3 errors, changed local files, age, and cache size.

The existing contentlet integration checks are registered in `Junit5Suite1`:

```sh
./mvnw install -pl :dotcms-core --am -DskipTests -Ddocker.skip
./mvnw verify -pl :dotcms-integration -Dmaven.build.cache.enabled=false -Dcoreit.test.skip=false \
  -Dit.test=BinaryAssetStorageIntegrationTest,StoragePersistenceAPITest
```

Generated renditions use the `generated-assets` group, mapped to the configured
`dotGenerated` root. Completed new filter outputs are uploaded synchronously when
the feature is enabled. Warm reads do not backfill existing local renditions;
legacy backfill remains pending. Cold reads restore them atomically
where supported, and the binary cache budget now includes completed renditions.
Incomplete filter outputs are excluded. Enabled renditions use
`generated-assets/{first}/{second}/{inode}/dotGenerated_...`, with the same relative
layout locally. Thumbnail invalidation and source deletion target that inode's
directory. Disabled mode retains the existing two-character layout. Legacy
rendition objects are not migrated into the new layout and need later reclamation.

Regular compiled Sass outputs also use that generated group under the feature flag.
Their keys include the source/dependency bytes, site, live/working mode, compiler
options and application build. A cold compilation stages its S3-backed inputs, then
restores a matching CSS output without invoking Sass. Source maps keep main's private,
uncached behavior. `CSSAssetStorageTest` verifies real Sass/MinIO compilation,
publication failure and retry, cold servlet serving, changed imports, live/working
versions and deletion. Markdown direct reads use the protected FileAsset stream API;
`AssetTemplateStorageTest` verifies cold mixed-case Markdown. These changes have not
yet been deployed to the demo.

## Current limits

The scanner and direct eviction API first require an owned binary layout: matching
inode shards, a field/file pair or a UUID revision, or a completed rendition under
the generated root. Operational files, legacy inode metadata, hidden staging,
malformed revisions, generated temporary files and symlinked descendants do not
enter the eviction budget or reach the remote durability check. A configured cache
root may itself be an alias; canonical provider paths remain supported. A user
upload such as `dotGenerated_UserUpload.PNG` remains an ordinary original.

Eviction requires an exact S3 key and verified matching contents. A matching MD5
ETag is the fast path; other ETags use a streamed byte comparison. Real MinIO
coverage includes a 40 MiB multipart object. Actual KMS remains unverified. Legacy
assets without a verified S3 copy remain local; eviction does not backfill them.
The cache can therefore exceed its configured size limit safely.

NFS-backed asset directories continue to use `FILE_SYSTEM`, which never authorizes
cache eviction. Regular binary reads and writes remain concurrent. Enabled HTTP
responses, image-filter execution and starter export hold a cache lease through
consumption; FileAsset streams hold it through resolution/open. Eviction tries
exclusive access and skips when a lease or storage operation is active, avoiding a
queued eviction writer that could delay new readers. Sustained activity can defer
all eviction on that API instance. Other callers retaining a `File` must acquire
`acquireCacheLease()` before resolution and close it on the same thread after use.
Metadata reads retain main's direct-open behavior without an extra existence check.

This does not coordinate external filesystem writers or multiple processes sharing
the same asset directory. Enabling `BINARY_CHAIN` eviction on a shared NFS directory
needs separate coordination and validation. No actual NFS mount was tested here.

These tests do not validate AWS IAM, KMS, bucket versioning, or production-scale
throughput. The contentlet integration checks default to filesystem mode; the
explicit S3 configuration below exercises the real CMS lifecycle through MinIO.

## Validation on 2026-09-11

- Core build/install passed, including the in-project dependencies.
- All 55 focused checks passed, including all four real MinIO tests; none skipped.
- All 22 contentlet and shared-storage integration checks passed against PostgreSQL
  and OpenSearch; none skipped.
- `git diff --check` passed. The preserved SearchAPI registration, Contentlet type
  guard, and JSON stored-filename fast path were unchanged from the rebased tip.
- Runs used installed Java 25.0.2. The exact pinned Java 25.0.4+1-ms was not tested.
- The disposable MinIO and integration containers were removed after testing.
- NFS compatibility follow-up: removed serialization of normal binary I/O and
  added a regression check for a stalled filesystem read alongside an unrelated
  upload/read. Core build and all 52 filesystem/binary checks passed.
- The full `FileMetadataAPITest` run passed: 46 executed, two existing skips,
  including the regression for reading persisted metadata without accessing the
  binary. This is source-level and filesystem regression coverage, not a test on
  an actual NFS mount.
- Final rerun after the NFS concurrency change: all 56 focused checks passed,
  including four real MinIO tests, with no skips. The test endpoint now reads the
  same JVM property used by its opt-in condition, allowing a separate test port.

## Demo POC

The [local MinIO demo](../../docker/docker-compose-examples/s3-asset-storage-poc/README.md)
runs the worktree image with the TravelLux starter. Its 540 starter binaries were
seeded and verified in MinIO. A new mixed-case filename uploaded through the
workflow API was verified in S3, removed from the local cache, and retrieved over
HTTP with the original checksum; the local cache file was restored. The demo and
test MinIO instances use separate ports and buckets.


## Generated rendition validation on 2026-09-11

- Core build/install and the updated demo image build passed.
- The final regression run reported 72 tests: 71 passed, one existing native-libvips
  availability skip. All seven real MinIO tests ran and passed.
- PNG and GIF exporter checks prove an evicted rendition returns from S3 without
  running the resize filter again. Source deletion and remote-only invalidation,
  warm-cache backfill, safe eviction, temporary-file exclusion, and path-boundary
  checks passed. The filesystem/NFS and shared-completed exporter checks passed.
- The running demo's native image engine resized `happy-friday.gif` to 139 × 208.
  After clearing both local files, MinIO traced GETs for the original and completed
  rendition; the returned bytes matched. A new upload also wrote its metadata to S3.
- The preserved main fixes remain intact. CRLF-aware diff validation passed
  (`git -c core.whitespace=cr-at-eol diff --check`).
- No new actual-NFS, multi-node invalidation, native fallback, or full contentlet
  integration run was performed for the rendition extension. Earlier contentlet
  and metadata integration results above predate this extension.

## Durable deletion checks

Whole-inode CMS deletion and old-version maintenance record `binaryAssetCleanup`
jobs in the same database transaction as the content deletion. The worker checks
that the version is absent, then removes its binaries and caches. S3 failures use
the existing job queue retry policy; after retry exhaustion the job remains failed
and can be retried through the existing job management API. Disabling the feature
prevents workers from touching storage; it does not silently complete pending jobs.
Whole-inode metadata deletion now runs in the same durable cleanup processor,
after the deleted version is confirmed absent. It removes metadata before deleting
source objects, so failures leave source keys available for a retry. The CMS test
verifies deletion rollback preserves cold metadata, committed cleanup removes
current/prior metadata, repeated cleanup is harmless, and remote-only metadata
for a removed field is deleted without touching a neighboring inode. Hashed S3
metadata listings now return logical paths suitable for deletion. Field trash,
explicit version deletion, standalone metadata-edit rollback, and worker restart
tests remain open. Binary replacement rollback is covered below.

Run the transaction proof against disposable PostgreSQL (the test creates and
removes its own schema using the production job-table startup task):

```sh
docker run -d --rm --name binary-cleanup-postgres-test \
  -p 127.0.0.1:19003:5432 \
  -e POSTGRES_USER=binary-storage-test \
  -e POSTGRES_PASSWORD=binary-storage-test \
  -e POSTGRES_DB=binary_storage_test postgres:16-alpine

./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
  -Dtest=BinaryAssetCleanupTransactionTest,BinaryAssetCleanupProcessorTest \
  -Ds3.test.jdbc=jdbc:postgresql://127.0.0.1:19003/binary_storage_test \
  -DskipTests=false -Dskip.surefire.tests=false

docker stop binary-cleanup-postgres-test
```

The PostgreSQL test is explicitly skipped without `s3.test.jdbc`. It exercises the
real queue persistence and two database connections, with the queue manager and
server identity supplied by the test; it does not boot the CMS or a job worker.

## Immutable replacements

With the feature enabled, new CMS binary writes use
`binary-assets/{a}/{b}/{inode}/{field}/.revisions/{revision-id}/{filename}`.
The Binary field JSON retains its original `value` filename and adds `storageKey`.
The field reference changes in the content transaction; an uncommitted replacement
does not overwrite the prior object. Legacy binary JSON and storage keys continue
to be readable. Reconstructed files retain the stored revision path without I/O.
Rendition hashes include the original revision key so replacing bytes under the
same inode and filename cannot select the previous revision's rendition.

Run `BinaryAssetReferenceTest` and `BinaryS3StorageTest` with both
`s3.test.endpoint=http://127.0.0.1:19002` and
`s3.test.jdbc=jdbc:postgresql://127.0.0.1:19003/binary_storage_test` using the
disposable services above. The replacement test uses actual filesystem/S3 providers
and PostgreSQL transactions; it does not invoke full CMS check-in. It verifies
uncommitted-read isolation, rollback, cache eviction and restore, commit, preserved
old snapshots, cleared-field lookup, and deletion of the revision objects.

Full CMS replacement metadata/rollback now passes: metadata files and memory cache
entries identify the exact binary revision when enabled. Custom metadata is copied
to replacements; rollback retains the prior metadata. The test deletes metadata's
local file and memory entry and restores the stored object from MinIO without
regenerating it. Basic S3 check-in, version copying, FileAsset access, and cold
physical-path lookup also passed. Standalone metadata edits, deletion cleanup,
and safe reclamation remain pending. Old and rolled-back revisions are retained
until whole-inode cleanup. Disabling the flag restores legacy behavior; S3-only
revision data requires migration to the legacy layout before operating without
the feature. The running demo has not yet been rebuilt with this storage format.

Latest immutable-reference regression: 101 reported tests, 100 passed, no failures
or errors, one existing native-libvips skip. All eight real MinIO cases and the
PostgreSQL cleanup transaction case passed. This includes a real Contentlet snapshot
restoring its old binary from S3 after a replacement commits, and Binary JSON
round-trip/ownership checks. Both disposable containers were stopped afterward.

## Real CMS feature isolation

`BinaryAssetStarterRestoreTest` is an opt-in starter acceptance test with separate fresh-startup and populated-database restore phases.
Run it alone using the enabled CMS/MinIO configuration below, retaining the same
test bucket between runs. Use a persistent archive path under `dotCMS/target`,
outside the harness's disposable `dotcms-integration/target/testdata` directory:

1. Set `-Dit.test=BinaryAssetStarterRestoreTest`,
   `-Ds3.starter.restore.enabled=true`, `-Ds3.starter.phase=export`, and
   `-Ds3.starter.archive=/absolute/worktree/dotCMS/target/s3-starter-acceptance/starter.zip`.
   Wait for successful completion and harness teardown. This writes the archive
   and a fixture manifest, then removes the fixture's original and metadata from S3.
2. Run the same command with `-Ds3.starter.phase=restore` and
   `-Dstarter.run.path=/absolute/worktree/dotCMS/target/s3-starter-acceptance/starter.zip`.
   This boots a fresh CMS database from the exported ZIP, verifies import to S3,
   clears the restored local original/metadata, and reads them through the CMS.

3. To test replacement of a populated database, repeat the export phase first so
   the new fixture's original and metadata are absent from S3. Then run with
   `-Ds3.starter.phase=populated`, omitting `-Dstarter.run.path` so the harness first
   boots its normal starter. This case invokes the importer against existing
   workflows, templates, categories, rules, variants and experiments. It checks failed/successful cleanup
   rollback, enforced foreign keys, restored deletion hooks, stale-cache removal,
   then the same cold binary/metadata assertions. Run this only
   in the disposable harness: it replaces that database's content.

Each phase contains one active test and two intentionally skipped tests. Fresh
startup and populated PostgreSQL/MinIO replacement passed on 2026-09-15 with
archived/default variants, an experiment, complete rule parameters and cold
binary/metadata retrieval (`/private/tmp/s3-starter-configuration-fresh-restore.log`
and `/private/tmp/s3-starter-configuration-populated.log`). All 27 focused
cleanup/feature-isolation checks also passed. This fixture does not establish
whole-import atomicity or a consistent export during concurrent edits.

Use `-Dmaven.build.cache.enabled=false` when verifying different runtime modes.
The build cache can otherwise reuse an earlier result despite different feature
flags or test selections; `BUILD SUCCESS` alone is not evidence that tests ran.

The integration suite verifies the requested mode at startup. The default run
uses the original filesystem layout; the S3 replacement case is explicitly skipped.
The disabled check-in also checks that no revision reference is persisted and
that eviction leaves the original file in place. An old provider setting must not
bypass the feature flag:

```sh
./mvnw verify -pl :dotcms-integration -Dmaven.build.cache.enabled=false -Dcoreit.test.skip=false \
  -Dit.test=BinaryAssetStorageIntegrationTest -Dit.test.forkcount=1 \
  -Ddocker.run.context=s3-asset-integration -Ddb.port=15437 -Des.port=19207 \
  -DDOT_FEATURE_FLAG_S3_ASSET_STORAGE=false \
  -DDOT_BINARY_ASSET_STORAGE_TYPE=BINARY_CHAIN \
  -DDOT_STORAGE_FILE_METADATA_S3_ENDPOINT=http://127.0.0.1:1
```

The harness starts and removes isolated PostgreSQL/OpenSearch containers. For an
S3 run, first create a disposable MinIO bucket, then replace the last three
properties with `-Ds3.cms.enabled=true`,
`-DDOT_FEATURE_FLAG_S3_ASSET_STORAGE=true`,
`-DDOT_STORAGE_FILE_METADATA_DEFAULT_CHAIN=FILE_SYSTEM,S3`, and the corresponding
`DOT_STORAGE_FILE_METADATA_S3_BUCKET_NAME`, `BUCKET_REGION`, `ACCESS_KEY`,
`SECRET_ACCESS_KEY`, and `ENDPOINT` properties (each with the full
`DOT_STORAGE_FILE_METADATA_S3_` prefix). The latest S3 run passed all five cases,
including cold metadata restoration and rollback. An earlier run exposed stale
replacement metadata; revision-specific metadata keys fixed that failure.

The final S3 test also updates two binary fields using separate temporary uploads
and verifies each retains its uploaded custom attributes. Enabled check-in copies
prior metadata once, then applies upload metadata, preventing cross-field overwrites.
The final uncached flag-off run passed 50 cases with three expected skips across
the binary CMS and existing metadata suites. Core build/install and 17 focused
revision/feature unit checks passed. Actual NFS, standalone metadata edits, field
trash, and the other completion-checklist items remain unverified or open.

The cleanup follow-up passed all five real CMS S3 cases and 27 focused cases,
including seven MinIO storage cases; the separate PostgreSQL-dependent case was
explicitly skipped in that focused run. Failure injection verifies that metadata
cleanup failure leaves originals intact and that enabled content metadata write
failures reach the caller. Disabled writes retain their original error behavior.


Crop-after-resize acceptance (2026-09-14): the uncached core build passed; the
focused suite passed 95 cases with two expected skips, including nine real MinIO
cases. All six real CMS cases passed. The crop regression edits actual focal
metadata, verifies changed crop pixels/cache keys, evicts the local result, and
restores it from S3. Additional checks verify explicit overrides, missing focal
points, revision identity, metadata read errors, reusable parameters, and Java/native
focal-coordinate parity with the feature both enabled and disabled. Native pixel
execution and actual NFS remain outstanding. See the completion ledger for the
remaining transaction, migration, publishing, and additional storage requirements.


Publishing metadata isolation (2026-09-14): core build/install passed. All six CMS
S3 cases passed with additional received-metadata replacement, original metadata
cold retrieval, omitted-field preservation, empty-bundle handling, and rollback
checks. The focused suite passed 96 cases with two expected skips. The uncached
flag-off metadata/CMS suite passed 50 cases with four expected skips, while an old
BINARY_CHAIN setting and unusable S3 endpoint remained configured. The CMS test
calls the same metadata API as the publishing receiver after check-in; it does not
exercise full publishing transport. Same-revision metadata write failures and
transaction isolation remain open. No demo redeployment or push occurred.


Metadata write-failure safety (2026-09-14): core build/install passed. Focused tests
passed 32 cases with one PostgreSQL-dependent skip, including eleven real MinIO
cases. Serialization and rejected-upload failures preserve the prior value;
successful replacements update local and remote metadata. Tests also cover unique
S3 staging paths, staging cleanup, reads during a partial filesystem write, and
temporary-metadata failure propagation behind the flag. The full enabled CMS and
metadata run passed 52 cases with two existing skips; the disabled run passed 50
with four expected skips. Both modes use the production metadata cache-key helper.
The enabled run explicitly verifies that reading persisted metadata does not
restore the original binary. A single-method selection that executed zero tests
was discarded and replaced by a full suite run.

Provider errors are still suppressed by the higher metadata read helper, and
same-revision metadata updates still need transaction isolation and reconciliation
after an uncertain remote commit or local-cache failure. Those are open work, as
are the other completion-checklist requirements. No demo redeployment or push.


Metadata read-error follow-up (2026-09-14): build/install passed; all 52 applicable
enabled CMS/metadata cases and all 50 disabled cases passed (two and four expected
skips respectively). Across the focused runs, 87 cases passed with one separate
PostgreSQL-dependent skip. The final feature-only run passed all 21 cases; the
other run passed 66 storage cases, including twelve real MinIO cases. Those tests
verify metadata read/generation failure during an injected S3 503, no source
binary access during that failure, normal object 404 handling, recovery into a
removed custom cache directory, corruption preservation, and legacy focal lookup
through an explicitly empty memory cache. Database query failures retain their
cause. Disabled behavior is exercised separately. MinIO and harness containers
were stopped; no demo redeployment or push.

The previously recorded metadata read-error gap is fixed for these flows.
Metadata transaction isolation, uncertain remote-write reconciliation, bucket
configuration/readiness handling, actual NFS, and the other completion-checklist
requirements remain open.

Standalone metadata transaction follow-up (2026-09-14): custom-attribute edits and
full metadata replacement publish unique metadata references through the content
transaction. The final build passed; 92 focused cases passed with one separate
PostgreSQL skip. The real CMS run passed 53 applicable cases with two existing
skips, including rollback, cross-request isolation, concurrent merges, cold
metadata/reference restoration, and historical/current focal crops. Logs are
`/private/tmp/s3-metadata-tx-{build-final,unit-final,cms-final}.log`.

Inside an outer transaction, fetch the content again to read its pending metadata
reference. Shared Contentlet objects update only after commit. Generation/copy
paths, legacy focal lookup, uncertain-write failure injection, and orphan cleanup
remain open; see the completion checklist. The disabled rerun passed all 50
applicable cases with five expected skips (`/private/tmp/s3-metadata-tx-off-cms.log`),
with BINARY_CHAIN and an unusable S3 endpoint still configured. All disposable
test containers were stopped. No demo redeployment or push.

The existing demo was subsequently rebuilt and deployed on 2026-09-14 with its
data preserved and backed up. The updated image passed a real HTTP cold-cache
proof for the uploaded happy-friday.gif and a new 141×211 rendition; MinIO traced
both object reads. A new mixed-case upload used an immutable revision and matched
its S3 object and restored HTTP response. The homepage and protected GIF were
verified in the browser after normal admin login. See the POC compose README and
`dotCMS/target/s3-poc/proof-20260914/proof-summary.json`. This completes the current
demo refresh only; the broader completion checklist remains open.

Public custom-metadata copy subsequently gained transactional immutable references
under the flag, preserving destination file details and prior snapshots. The new
CMS/MinIO case verifies rollback, clearing custom attributes, and independent cold
historical reads. The core build passed; focused tests passed 38 cases with one
PostgreSQL-dependent skip. Full CMS runs passed 54 cases in S3 mode (two existing
skips) and 50 in disabled mode (six expected skips), with no failures/errors.
Logs: `/private/tmp/s3-metadata-copy-{build-final,unit,cms,off-cms}.log`.
This copy fix is not yet deployed to the demo. Actual NFS and the remaining
generation/lifecycle checklist are still unverified.

Forced and lazy metadata regeneration subsequently gained immutable publication
under the feature flag. It reads raw stored metadata, retains custom attributes,
and publishes only while the input binary and metadata references remain current.
Historical reindexing does not overwrite current metadata. The real CMS/MinIO
regression verifies rollback, old/new cold reads, stale-snapshot regeneration,
custom-only generation after a UI metadata edit, and warm reuse. Existing strict
basic metadata assertions remain unchanged.

Final core build passed. Focused tests passed 38 cases with one separate
PostgreSQL-dependent skip. Enabled CMS tests passed 55 cases with two existing
skips; disabled CMS tests passed 50 with seven expected skips. No failures/errors
in these final runs. Logs: `/private/tmp/s3-metadata-generation-build-verified.log`,
`/private/tmp/s3-metadata-generation-unit-verified.log`,
`/private/tmp/s3-metadata-generation-cms-verified.log`, and
`/private/tmp/s3-metadata-generation-off-cms.log`. This change is not deployed to
the demo. Actual NFS and the wider lifecycle checklist remain open.

Cold asset export now enumerates persisted binary references under the feature
flag, restores originals and exports raw metadata alongside legacy local files.
The real ZIP regression verifies original bytes, mixed-case names, custom
attributes, duplicate prevention, both asset-version options and unpublished
working drafts. It also verifies that a missing referenced original fails export.
The same case checks warm local ZIP contents with the feature disabled.

Core build passed (`/private/tmp/s3-starter-export-build-final.log`). All ten
enabled binary CMS cases passed without skips
(`/private/tmp/s3-starter-export-cms-verified.log`); all five applicable disabled
cases passed with five expected S3-only skips
(`/private/tmp/s3-starter-export-off-cms.log`). No failures/errors in the final
runs. Import/backfill, complete starter restore, concurrent export consistency,
HTTP partial-stream failure handling and actual NFS remain open. No demo
deployment or push.

## Operational binary backfill

With `FEATURE_FLAG_S3_ASSET_STORAGE=true`, an active administrator can start a
migration through the existing job API. Authentication supplies the submitting
`userId`; the processor checks administrator status when queued and when run.

```http
POST /api/v1/jobs/binaryAssetBackfill
Content-Type: application/json

{"batchSize":250}
```

Follow the returned `statusUrl` (`GET /api/v1/jobs/{jobId}/status`). The job's
`parameters.afterInode` is the last committed batch cursor and
`parameters.verifiedBinaries` counts the binaries verified in completed batches.
The batch size is a number of content inodes, from 1 through 1000; an inode can
have several binary fields. A successful result includes `complete: true`.
Progress reaches 100% only when the scan finishes; no estimated percentage is
reported while the total remains unknown.

Each batch uses conditional, verified original/metadata backfill. Referenced raw S3
objects are converted to SHA-256 references without changing their database paths.
The worker reads persisted binary fields, including retired definitions, and also
migrates recognized completed renditions belonging to each inode. Reported binary
counts cover originals; rendition work must succeed before the inode's batch can
advance. Previously completed jobs need a new run from an empty cursor to include
objects processed before hash migration was added. In versioned S3 buckets, older
raw object versions remain subject to the operator's existing retention policy.
It leaves local sources intact. Only a fully verified batch advances the cursor.
Retries reload the cursor from the job database, including when a checkpoint
committed but its response was lost. Updates compare the previous cursor so an
overlapping worker cannot overwrite newer progress. Job state/progress updates
and requeueing use the existing queue implementation and retain these parameters.

Use `POST /api/v1/jobs/{jobId}/cancel` to stop between batches. The current batch
can finish and save its checkpoint. Existing job retry limits apply; after
cancellation or exhausted retries, fix the reported problem and submit another
job with the last **persisted parameters**, for example:

```json
{"batchSize":250,"afterInode":"<parameters.afterInode>","verifiedBinaries":42}
```

Omit `afterInode` to verify the inventory again from the beginning. Enqueueing
and executing this processor are rejected while the feature is disabled.
This scan covers content-referenced originals and metadata; it does not migrate
operational server files or solve the other legacy/staging paths in the audit.

Validation so far: six processor checks pass, including failed copies, failures
before/after checkpoint persistence, cancellation, stale cursor rejection,
authorization and flag-off behavior. `BinaryAssetBackfillCheckpointTest` needs
`s3.test.jdbc` and the existing disposable PostgreSQL credentials to verify actual
checkpoint visibility, requeueing, reconnection, stale status updates and a
competing cursor. The latest targeted run skipped that separate PostgreSQL fixture;
its earlier acceptance is recorded in the completion checklist. Real administrator
HTTP submission and CMS/MinIO execution passed on the demo: job
`33c3fc11-de1c-4033-86ab-7dedf2378f16` completed 542 originals with no retries and
persisted its cursor/count after batches of 25. A subsequent job
`9ce8717f-6e6f-4d0f-8a73-141e96cd7975` verified 545 originals, including legacy
Image filename objects. It survived an actual SIGKILL after a persisted five-object
checkpoint: the abandoned-job detector retried the same job automatically and it
completed with one retry. Acceptance temporarily used one-minute detection and
staleness settings; the normal five/thirty-minute settings were restored. Evidence
is in `dotCMS/target/s3-poc/proof-restart-20260915/`. Cleanup-worker process restart
and independent-node races remain separate outstanding checks.

# S3 asset storage

S3 asset storage lets dotCMS keep binary assets and their metadata durably in S3, with the
local asset directory acting as a cache. It is opt-in and off by default. With the flag off,
the filesystem/NFS behavior is unchanged.

This page describes what is in the code today. It grows as the remaining parts land.

## Behavior with the flag off

With the flag off, the storage chain, its providers, metadata and static push publishing run
the same code paths as before. Two details are worth knowing:

- This feature packages the AWS STS module (`aws-java-sdk-sts`). Before, the web identity step
  of the SDK's default credential chain always failed for lack of that module, and the chain
  moved on to the profile files and then the container or EC2 instance role. With the flag off,
  every place where dotCMS falls back to the default chain (static push publishing, endpoint
  validation and the S3 metadata provider) uses `NoWebIdentityCredentialsProviderChain`, which
  is that chain without the web identity step, so a pod with a web identity token keeps using
  the identity it used before. Two differences remain because the module is on the classpath:
  a profile in the AWS config files that assumes a role (`role_arn`) now resolves instead of
  failing over to the instance role, and an OSGi plugin that builds its own
  `DefaultAWSCredentialsProviderChain` now gets the web identity step.
- The flag is logged at INFO only when it is on. With it off, the first read logs at debug.

## Feature flag

Set `FEATURE_FLAG_S3_ASSET_STORAGE=true` (Docker: `DOT_FEATURE_FLAG_S3_ASSET_STORAGE=true`)
in the environment or `dotmarketing-config.properties`, then restart every node. The default
is `false`.

The flag is read once per process (`AssetStorageFeature.isEnabled()`) and then kept, because
several storage objects capture the mode when they are built. A value set only in the system
table may not be seen at first read, and a runtime change is ignored until restart. In-memory
overrides through `Config.setProperty` do re-read it, which is how tests switch modes. Tests
that mock `Config` statically must call `AssetStorageFeature.reset()` themselves.

With the flag off, the S3 job queues (`binaryAssetCleanup`, `binaryFieldCleanup`,
`binaryAssetBackfill`, `bundleArchiveCleanup`) are not registered.

### Enabling the flag is not rollback-safe

Enabling the flag is a one-way step for any content written while it is on. Check-in stores the
active binary only under a `.revisions/<uuid>/` key recorded in `contentlet_as_json`
(`storageKey`/`metadataStorageKey`); no legacy flat file is written, and the local copy may later
be evicted to S3. Neither a release without this code nor this release with the flag turned back
off reads those revision keys (the reader falls back to the legacy field folder), so affected
binaries resolve as stale or missing. Leaving the flag off, the default, changes nothing. Keeping
a legacy-path copy for rollback is not implemented; treat enabling the flag as requiring a
forward-only recovery plan.

## Storage layer behavior with the flag on

The storage chain (`ChainableStoragePersistenceAPI`) and its providers change as follows:

- Writes publish to every durable provider before the new local copy becomes visible, and a
  failed remote write keeps the previous local contents.
- Reads restore a missing local copy from S3. A miss is not cached as a 404, because another
  node can upload the same key at any time. Object reads restore under the same per-key lock
  as writes and deletes, so a restore cannot overwrite a newer write on the same chain.
- A zero-length or truncated local metadata file is a read failure, not a miss. When S3 holds a
  readable copy, the chain replaces the local file with it under the per-key lock. Otherwise the
  read fails and the local file is kept, so the metadata is never treated as absent and
  regenerated, which would drop custom attributes such as a focal point.
- Deletes keep the local copy until all durable providers accept the deletion, so an
  in-flight restore on the same chain cannot bring the object back.
- Storage and database query failures propagate instead of being reported as a missing
  object.
- Providers can list objects under a prefix, verify that a durable copy has the same
  contents (`hasDurableCopy`), and copy an existing file or object to durable storage without
  overwriting a conflicting one (`backfillFile`, `backfillObject`).
- S3 existence checks (`existsGroup`, `existsObject`, `hasDurableCopy`) list at most one key
  per lookup. Only listings that need every key (`listObjectPaths`, `listObjectSnapshots`,
  `deleteGroup` and static push) follow every page.


## Binary asset API

`BinaryAssetStorageAPI` (`APILocator.getBinaryAssetStorageAPI()`) manages binary assets and
completed renditions through the storage chain. Binary assets use the `binary-assets` group under
the asset root, laid out as `{inode[0]}/{inode[1]}/{inode}/{field}/{fileName}`. Renditions use the
`generated-assets` group under the `dotGenerated` root. With the flag on, keys in these groups keep
their mixed-case names, as do publishing bundles and temporary uploads (below); other groups are
still lowercased.

With the flag on, S3 keeps one immutable copy of each set of bytes for these two groups, at
`asset-blobs/sha256/<chars-1-2>/<chars-3-4>/<chars-5-6>/<chars-7-8>/<sha256>`
(`S3ContentAddressedStorage`). The owned key holds a small reference object marked by the S3
user-metadata header `dotcms-blob-sha256`, and identical bytes under different names or owners
share one blob. Uploads hash a private snapshot, so the hashed bytes are the uploaded bytes.
Existing blobs are compared by content, and cold downloads verify SHA-256 before entering the
local cache. Older one-level blob keys and raw objects remain readable. Backfill replaces a raw
object with a reference only if its ETag still matches the version that was read, so a concurrent
replacement or deletion makes the migration fail instead of overwriting or resurrecting it.
Deleting an owner removes its reference, not the shared blob; shared blobs are not reclaimed yet.

With the flag on, a lookup by inode and field uses the binary reference recorded in the row's
`contentlet_as_json`. A row without that JSON (`SAVE_CONTENTLET_AS_JSON=false`, or a row not yet
populated) falls back to the older `{inode}/{field}/{fileName}` lookup instead of reporting no binary.

The API rejects an inode that is not at least two letters, digits, underscores or hyphens, and a
field or file name that is blank, contains `/` or `\`, or is `.` or `..`, so a built path cannot
leave its `{inode}/{field}` directory. This applies with the flag on or off.

With the flag off, the API uses the existing filesystem/NFS paths and makes no remote calls.

## Content binaries and immutable revisions

With the flag on, new CMS binary writes use
`binary-assets/{a}/{b}/{inode}/{field}/.revisions/{revision-id}/{filename}`. The Binary field JSON
keeps its original `value` filename and adds `storageKey` (and `metadataStorageKey` for its
metadata). The field reference changes in the content transaction, so an uncommitted replacement
does not overwrite the prior object, and a rollback keeps the previous revision. Legacy binary
JSON and storage keys remain readable. Reconstructed files keep the stored revision path without
I/O. Old revisions are kept until whole-inode cleanup. A check-in that rolls back deletes the
revision and revision metadata it uploaded, through a rollback listener; the key carries a fresh
UUID, so no other version can reference it. If that delete fails it is logged and the object is
left behind. Metadata written under a different key during the rolled-back check-in, and
revisions abandoned by a rollback to a savepoint, are not reclaimed.

Custom metadata is copied to replacements, and metadata files and cache entries identify the exact
binary revision. Binary HTTP responses (`BinaryExporterServlet`) hold a cache lease while they
resolve, export and open the file, and release it before streaming, so a slow client does not
defer eviction. `FileAsset.getInputStream` and `Contentlet.getBinaryStream` also hold it only until
the stream is open, because a lease must be released on the thread that took it and a caller may
read or close the stream elsewhere. On a local disk an open file survives eviction, so this is
safe. Eviction on an NFS asset directory, where deleting an open file can break the read,
has not been validated.

## Metadata from evicted originals

With the flag on, `FileStorageAPI` restores the exact binary only when metadata generation needs
its bytes, and holds a cache lease through basic inspection, hashing and Tika parsing. Restore
failures propagate, and so do shared-extraction storage failures, instead of producing a
successful partial metadata result.

Byte-derived Tika extraction is shared through `SharedExtractedMetadata`, cached at
`extracted-metadata/<source-sha256>/<configuration-hash>.json`. The configuration hash covers the
parser bundle version, the binary metadata schema version and the extracted-text limit. Filenames,
local paths, fallback titles and modification times are added per use afterwards, and custom
attributes and focal points stay in their content-owned snapshots. Unknown parser versions and
flag-off mode extract directly. Empty or failed extractions are not published. Shared extraction
records are not reclaimed yet.

JSON metadata hydration reads the linked image's owner key, not the parent content's binary key.
Complete stored metadata avoids normalizing or downloading the original. Missing metadata can be
regenerated from a cold legacy or revision path.

## Durable deletion

Whole-inode deletion, deletion of one language of multilingual content, and old-version
maintenance record one `binaryAssetCleanup` job per deleted inode in the same database transaction
as the content deletion. Main leaves the files of a deleted language on disk (#9146); with the
flag on, that path now records cleanup jobs and, when `BACKUP_DELETED_CONTENTLETS_TO_DISK` is on,
recovery archives like the other deletion paths.

The job carries the exact binary and metadata paths stored for the inode when it was recorded, so
recording it lists the inode's objects inside the deletion transaction, and a storage listing
failure aborts the deletion. The worker refuses while a content version with that inode exists,
then deletes the recorded metadata before the recorded source objects, so a failure leaves the
sources available for a retry. It never deletes by prefix: an inode can be re-created after the
deletion (push publishing keeps the sender's inodes), and the revisions it uploads survive. Objects
uploaded under the deleted inode by a transaction that overlapped the deletion are therefore not
reclaimed. The inode's completed renditions and legacy image cache are still removed whole, since
they regenerate on demand. S3 failures use the job queue's retry policy; after retries are
exhausted the job stays failed and can be retried through the job management API. Direct
submissions through the public job endpoint are rejected. With the flag off, workers do not touch
storage and pending jobs are not silently completed.

## Binary field trash

With the flag on, deleting a binary field records a `binaryFieldCleanup` request in the same
transaction as the field deletion. Requests cover historical and working versions up to the
deletion time; values from a later field with the same name are kept.

Cleanup runs as a queued job, never inside the caller's request. It handles one content row per
transaction: it locks that row, rechecks that it was not edited after the field was removed,
uploads and verifies a recovery ZIP, clears the old field reference, records the exact cleanup
inventory, and advances the job's saved cursor, all in the row's own commit. Only that row is locked
while its archive uploads, and a retry resumes after the last committed row. A separate step checks that
the archived files are no longer referenced and that the ZIP is still available before deleting
metadata, originals and renditions. It never deletes a whole field prefix, so uploads made after
the inventory was captured survive a retry. Direct submissions through the public job endpoint are
rejected; only field deletion and `ContentletAPI.cleanField` create this work. With the flag off, scheduling and local
trash behave as before.

## Deleted-content recovery archives

When both the flag and the existing `BACKUP_DELETED_CONTENTLETS_TO_DISK` option are on, deletion
writes a verified S3 recovery ZIP before removing content. Archives live in the
`deleted-content-backups` group at `<identifier>/<inode>/<uuid>.zip`. Full destruction and
all-version deletion archive each version, deleting one language archives each version in that
language, and single-version deletion archives only that version. A failed backup aborts the
deletion. Binary cleanup never deletes recovery archives. Field trash ZIPs use the same group and
layout.

Each ZIP contains `contentlet.json` (the row's complete typed field data), `contentlet.xml`, and
`assets/` entries under the original binary and metadata paths, including binary fields whose
definitions were removed. Operators can download the ZIPs from S3 for manual recovery; internal
callers can use `ContentletBackupStorage.list(identifier)` and `open(key)`. There is no automatic
database restore, and archives are kept until removed or expired by the bucket's lifecycle policy.

With the flag on, the `deleteAllVersionsandBackup` interceptor, previously a no-op, calls its
implementation and the all-version deletion hooks. With the flag off it stays a no-op.

## Migrating existing binaries (backfill)

With the flag on, an active administrator can copy existing binaries to S3 through the job API.
The processor checks administrator status when the job is queued and again when it runs.

```http
POST /api/v1/jobs/binaryAssetBackfill
Content-Type: application/json

{"batchSize":250}
```

Follow the returned `statusUrl` (`GET /api/v1/jobs/{jobId}/status`). `parameters.afterInode` is
the last committed batch cursor and `parameters.verifiedBinaries` counts the binaries verified in
completed batches. The batch size is a number of content inodes, from 1 to 1000. A successful
result includes `complete: true`; progress reaches 100% only when the scan finishes.

Each batch uses conditional, verified backfill of originals and metadata, and converts raw S3
objects to SHA-256 references without changing their database paths. It reads persisted binary
fields, including retired field definitions, and migrates recognized completed renditions for each
inode. Local sources are left in place. Only a fully verified batch advances the cursor, retries
reload the cursor from the job database, and a stale worker cannot overwrite newer progress. The
page size is applied in the SQL query, so each batch reads only its own rows. The job sends a
heartbeat after every inode, so a slow batch is not mistaken for an abandoned job.

Problems in the data itself do not stop the scan, because a retry cannot fix them: a referenced
binary or metadata record that exists neither locally nor in S3, a row whose JSON cannot be parsed,
and a legacy row whose content cannot be found. Each one is logged with its inode and field, the
rest of the row is still copied, and the job reports `skippedCount` and `skippedInodes` (the first
1000 inodes) in its parameters and result. Storage and database errors, including an S3 object
that conflicts with the local bytes, still fail the batch so the retry policy applies; the error
message names the inode.

`POST /api/v1/jobs/{jobId}/cancel` stops between batches. After a cancellation or exhausted
retries, submit a new job with the last persisted parameters, for example
`{"batchSize":250,"afterInode":"<parameters.afterInode>","verifiedBinaries":42}`, or omit
`afterInode` to verify everything again. With the flag off, enqueueing and running this job are
rejected. The scan covers content-referenced originals and metadata, not operational server files.

### Legacy Image and File values

Backfill, starter export and recovery archives use the same inventory of persisted references.
Typed `Binary` entries are included even when their field definitions were retired. An `Image` or
`File` entry holding a plain filename is included only when the exact
`<inode-shards>/<inode>/<field>/<filename>` object exists in the combined filesystem and S3
inventory, so linked asset identifiers, external URLs and unrelated text are never treated as
binaries. Listing failures propagate instead of being read as absence.

## Starter export and import

With the flag on, starter asset export lists persisted binary references, restores originals from
S3 and includes raw metadata alongside legacy local files. It holds a cache lease for one binary at
a time, so eviction can keep trimming the files it restores, and it skips a binary whose stored
metadata says it exceeds `maxSize` before restoring it. Restored originals still pass through the
local cache; streaming them from S3 straight into the archive is not implemented. A missing
referenced original or any other export error fails the export without finishing the archive: the
client receives a truncated ZIP with no central directory, which standard ZIP readers reject,
instead of a valid ZIP that silently lacks the remaining entries. Whether the HTTP transfer also
ends with an error depends on the servlet container. With the flag off, export behaves as before,
including logging and skipping a file that cannot be read.

Importing a starter publishes its binaries to S3 before the database commit, and cleanup of the
imported files runs only after both succeed. A referenced binary or metadata record that is in
neither the starter nor S3 is logged and skipped, as before, so starters exported without assets,
with `maxSize` or with `oldAssets=false` still import; a summary count is logged at the end. S3 and
database errors still fail the import, and so does an error importing rules, so a starter is
never left partly imported.

Importing over a populated database with the flag on performs a full replacement: existing
variants, workflows, templates, categories, rules and experiments are cleared before import,
foreign keys stay enforced, and caches are flushed afterwards. With the flag off, import behaves as
before.

A starter exported with the flag on cannot be fully restored into an instance with the flag off.
Content checked in with the flag on has its binary only at its `.revisions/<uuid>/` key, and the
archive stores it there; a flag-off instance resolves binaries by the legacy field path and ignores
`storageKey`, so those binaries are missing after import. Import such a starter only into an
instance with the flag on.

## Integrity checks

With the flag on, the file-asset integrity checker's repair copies stored binaries to the repaired
content before its corrected JSON is published, and removes the old sources in the same
repair transaction. The copies are uploaded before the commit, so a rollback listener deletes
them, and their metadata, if the repair rolls back; a failed deletion is logged and leaves an
unreferenced object behind.

## Publishing bundles

With the flag on, completed push-publishing archives (`<bundle-id>.tar.gz`, including their
manifest) are stored durably in the `publishing-bundles` group, with the local bundle directory as
a cache (`BundleArchiveStorage`). Bundle ids keep their case and are checked so an archive cannot
resolve outside the bundle directory. Generated and received bundles are written to a private
staging file and published to S3 before they replace the last complete local archive, so an
incomplete upload never becomes visible. A received file's bundle id is the part of its name
before the first `.tar.gz`, which is the rule the receiving endpoints and `BundlePublisher` use to
find the archive again. Reading a bundle's manifest or payload restores the archive from S3 only
when its bytes are needed. Storage failures are reported as failures, not as a missing bundle, for
every publishing and retry decision. The bundle pages are the exception: whether to show a
download link or a retry button is checked once per bundle, and a storage failure there is logged
as a warning and shown as "not generated", so an S3 outage does not stop the page from rendering.

With the flag on, static publishing fails the bundle when a File Asset's binary is missing, so it
can be retried. This matches the flag-off behavior, where the copy of the missing file fails.

Deleting a bundle records a `bundleArchiveCleanup` job after the deletion commits. The job deletes
the S3 object and keeps the local bytes if remote deletion fails, so the existing queue can retry.
If a bundle row with the same id exists again when the job runs, for example because a receiver got
the same bundle again, the job keeps the archive and finishes successfully. On one node, storing an
archive and the job's row check and delete hold the same lock, so the job cannot delete an archive
stored between its check and its delete. That lock does not cover other cluster nodes, or a
receiver whose new bundle row is not yet committed when its archive is stored. In those cases the
new archive can still be deleted, and the receive fails with an error and can be retried.

Durable archives are kept for the same time as local ones. When `BinaryCleanupJob` deletes files
older than `CLEANUP_BUNDLES_OLDER_THAN_DAYS` (default 4) from the bundle directory, with the flag on
it also deletes S3 archives whose S3 last-modified time is older than that, together with their
local copy and extraction directory. A bundle older than that can no longer be retried or
downloaded, as with the flag off. A value below 1 turns off both cleanups. An archive that cannot be
deleted is logged and tried again on the next run. If several nodes run the cleanup job, each one
lists the group and deletes the same expired archives, which is harmless because deleting an archive
that is already gone succeeds.

With the flag off, bundles are written, read and deleted in the bundle directory as before.

## Temporary uploads

With the flag on, completed temporary uploads (`TempFileAPI`, including the image editor's output)
are stored in the `temporary-assets` group, with an immutable receipt that records who may use the
upload and when it expires. Access and expiry are checked before any bytes are downloaded, so
another node can serve the upload after restoring it into its own local root. Mixed-case and
nested names are kept. A payload is published only after the upload stream has closed; an
interrupted upload keeps any existing complete file and never exposes a partial one. Managed
uploads without a receipt cannot fall back to legacy local access.

Custom metadata for a temporary upload, such as a focal point set before check-in, is stored with
the upload in S3 and read directly from S3, so an edit made on another node is visible. The image
filter's focal point for existing content uses an id made of `temp_` and the content inode. That id
never gets an upload receipt, so its metadata stays in the local temporary directory, as with the
flag off, where `BinaryCleanupJob` removes it after `CLEANUP_TMP_FILES_OLDER_THAN_HOURS`. It is
therefore visible only on the node that wrote it, which the sticky sessions a cluster already
needs make sufficient. Metadata edits are a read, merge and unconditional write, so two nodes
setting different attributes on the same upload at the same moment can lose one of the two
writes. This is accepted because temporary metadata lives only until check-in.

The scheduled `BinaryCleanupJob` removes the S3 objects of an expired upload (its receipt, payload,
metadata and renditions) once `TEMP_RESOURCE_MAX_AGE_SECONDS` has passed. Local copies are left to
the existing `CLEANUP_TMP_FILES_OLDER_THAN_HOURS` age rule, as with the flag off, so a check-in that
resolved the file just before it expired keeps its source; the local upload marker stops an expired
copy from being served. Each upload is cleaned on its own: a failure is logged, that upload keeps
its receipt and payload, the remaining uploads are still cleaned, and the job reports one combined
failure and retries on the next run.

## WebDAV temporary files

With the flag on, WebDAV temporary files (the scratch files some clients write before the final
upload) keep completed payloads and discoverable path records in the `webdav-temporary` group, so
another node can list and read them. A writer reserves a unique payload, uploads it, and then
publishes the path record with an S3 conditional write, so a stale writer cannot publish after a
deletion. Reads materialize immutable local cache files, so an earlier request keeps its bytes
through a later replacement. Mixed-case temporary names are kept, while normal CMS URLs keep their
case-insensitive handling.

Cleanup uses S3 server timestamps, protects completed and pending payload references, and
conditionally replaces expired path records with tombstones. Tombstones are never deleted, because
some S3-compatible stores (MinIO among them) ignore the condition on a conditional delete, and an
unconditional delete could remove a newer upload that reused the path. The group therefore keeps
one small tombstone record per deleted WebDAV temporary path, and each cleanup run reads them
again, until safe reclamation is added. Normal WebDAV uploads still go through content check-in,
and their reads, copies and overwrites open the file under the cache lease through
`Contentlet.getBinaryStream`. With the flag off, WebDAV and temporary uploads use the local
filesystem as before.

A WebDAV COPY of a CMS file creates an unpublished working version on every mount, with either flag
setting, so it needs only edit permission. A folder listing (PROPFIND) reads only the records that
decide each direct temporary child: the child's own record, and only for a folder without a live
record of its own, its descendants until one live descendant is found. Each file resource is built
from the record the listing read, so a temporary file that a client deletes during the listing is
skipped instead of failing it. If S3 cannot be read, the failure is logged and only the temporary
children are left out, so the CMS files and folders still list. The remaining cost is one S3 read
per direct child record on every listing, and that includes the tombstone of every deleted direct
child, so it grows with the number of temporary names ever written directly in that folder until
tombstones can be reclaimed.

## Renditions and rendering

With the flag on, completed image-filter outputs are uploaded to the `generated-assets` group as
they are produced, at `generated-assets/{first}/{second}/{inode}/dotGenerated_...`, with the same
relative layout under the local `dotGenerated` root. A cold read restores a completed rendition
from S3 without running the filter again. Warm hits do not contact S3, so an S3 outage does not
break a cached response and a warm read cannot republish an invalidated object. Incomplete filter
outputs are never uploaded or evicted. Thumbnail invalidation and source deletion target that
inode's directory. With the flag off, renditions keep the existing two-character layout. Existing
rendition objects are not moved to the new layout.

Rendition keys include the original's revision key, so replacing bytes under the same inode and
filename never selects the previous revision's rendition. Crops read the focal point from the
content's metadata snapshot, including focal points saved on a temporary upload, and the Java and
native crop engines use the same pinned coordinates. Image-filter execution holds a cache lease
while it reads the original.

Compiled Sass output also uses the `generated-assets` group. Its key covers the source and
dependency bytes, site, live or working mode, compiler options and application build, so a cold
node restores a matching CSS output without running Sass. Source maps keep their existing private,
uncached behavior. Markdown files are read through the protected FileAsset stream, and VTL and
included files are opened through the binary asset API, so a file whose local copy was evicted is
restored from S3 before it is rendered.

## Local cache eviction

With the flag on, the local asset directory is a cache that `BinaryCacheEvictionJob` can trim.
The job is scheduled only when `BINARY_CACHE_EVICTION_CRON` is set. Each node has its own cache, so
the job runs on every node from the node-local scheduled thread pool, not from the clustered Quartz
scheduler, which would fire it on only one node per run. The first run is at least ten minutes after
startup, and each run schedules the next one when it finishes, so runs never overlap. A run holds
one thread of the shared scheduled pool (`SCHEDULER_COREPOOLSIZE`, default 5) while it works. It
evicts the oldest files once the cache exceeds `BINARY_CACHE_MAX_SIZE_MB` (default 5000), skipping
files newer than `BINARY_CACHE_EVICTION_MIN_AGE_MINUTES` (default 60).

A file is evicted only when S3 holds a verified copy under the exact key. For `binary-assets` and
`generated-assets`, the key must hold a reference whose SHA-256 matches the local file, and the
shared blob it names is then compared with the local file byte for byte. A raw object at the key,
written before references existed, is also compared byte for byte. For other groups the provider
requires the same size, then accepts a matching MD5 ETag and otherwise compares bytes. The byte comparison is deliberate: a matching
hash proves only what the reference says, not that the blob in the bucket still holds those bytes,
and eviction removes the last local copy. Its cost is one full download of each evicted file.

Only owned layouts are candidates (inode shards, a field/file pair or a UUID revision, or a
completed rendition). Operational files, hidden staging, temporary files and symlinked descendants
never enter the budget. Files without a verified S3 copy stay local, so the cache can exceed its
limit safely; eviction does not backfill.

Callers that hold a `File` across a read must take `acquireCacheLease()` before resolving it and
close the lease on the same thread after use. Verification runs with no lock held, so readers never
wait for it. Only the final check and delete take the eviction lock, and they do not wait for it: if
a lease or storage operation is active at that moment, or the file changed during verification, the
file is kept for a later run. Sustained activity can therefore defer eviction on that node.

The lease is local to one process and does not coordinate external writers or several processes
sharing one asset directory. With the flag on, the binary API always uses the filesystem-plus-S3
chain, so the job evicts from whatever asset directory is configured, including a shared NFS mount.
Do not set `BINARY_CACHE_EVICTION_CRON` on nodes that share an asset directory.

## Credentials and region

With the flag on, leave both `storage.file-metadata.s3.access-key` and
`storage.file-metadata.s3.secret-access-key` unset to use the AWS SDK default credential
provider chain. The configured bucket region and endpoint are kept in that mode. A partial
key pair fails initialization. The AWS STS module is packaged so role-based providers,
including web identity, work with the flag on.

Without a custom endpoint, a configured region selects the regional AWS endpoint. With no
region, the client uses the global endpoint and the SDK looks up the bucket's region on first
use. Requests are signed with SigV4, the SDK's default S3 signer. A custom endpoint
(`storage.file-metadata.s3.endpoint`) must be an absolute HTTP(S) URL and requires
`storage.file-metadata.s3.bucket-region` for request signing. Invalid endpoint configuration
fails instead of silently selecting AWS. With the flag off, configuration behaves as before.

## Sharing one bucket across installations

With the flag on, `storage.file-metadata.s3.namespace` (Docker:
`DOT_STORAGE_FILE_METADATA_S3_NAMESPACE`) separates the keys of installations that share a
bucket. Owned keys become `asset-namespaces/<namespace>/<group>/<existing-key>`. Two prefixes
stay at the bucket root and are shared by every namespace: content-addressed blobs
(`asset-blobs/`), so identical bytes are stored once, and shared extracted metadata
(`extracted-metadata/`), so identical bytes are extracted once. Use the same
value for all nodes in one cluster and distinct values for independent installations. Values
are 1 to 64 letters, digits, underscores or hyphens, starting with a letter or digit. Empty is
the default and keeps the existing layout.

Set the namespace before the installation first stores assets. Changing it later does not
migrate existing records. It separates application keys only; it does not restrict what the
bucket credentials can access.

## Static push publishing

Enabling the flag also changes static push publishing to S3 (`AWSS3Storage`). Clients sign
with SigV4 and list every page of objects instead of only the first 1,000. Without a custom
endpoint, a configured region selects that region's endpoint; with no region, the client uses
the global endpoint and the SDK looks up the bucket's region, so a region is not required.
Endpoints that use the default credential chain are not pinned to a region either. Their
configured endpoint and region are still not passed to the client on that path, as before, so
an S3-compatible custom endpoint needs a key and secret.

## Running the checks

The S3 checks use the real filesystem provider, chain and AWS adapter against a disposable
MinIO bucket, and the transaction checks use a disposable PostgreSQL database; each test creates
and removes its own schema. `BinaryS3StorageTest` is skipped unless `s3.test.endpoint` is set, and
the PostgreSQL cases are skipped unless `s3.test.jdbc` is set. The credentials below are disposable
local test values.

```sh
docker run -d --rm --name binary-s3-test \
  -p 127.0.0.1:19002:9000 \
  -e MINIO_ROOT_USER=binary-storage-test \
  -e MINIO_ROOT_PASSWORD=binary-storage-test \
  minio/minio:latest server /data

docker run -d --rm --name binary-cleanup-postgres-test \
  -p 127.0.0.1:19003:5432 \
  -e POSTGRES_USER=binary-storage-test \
  -e POSTGRES_PASSWORD=binary-storage-test \
  -e POSTGRES_DB=binary_storage_test postgres:16-alpine

./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
  -Dtest=AssetStorageFeatureTest,AssetStorageFeatureLatchTest,S3StorageConfigurationTest,NoWebIdentityCredentialsProviderChainTest,BinaryS3StorageTest,BinaryAssetReferenceTest,BinaryCacheEvictionJobTest,BinaryFileSystemStorageTest,BinaryAssetStorageAPIImplTest,MetadataLocalCacheTest,BinaryAssetCleanupTransactionTest,BinaryAssetCleanupProcessorTest,ContentletBackupStorageGateTest,BinaryFieldCleanupProcessorTest,AssetJobEventSerializationTest,BinaryAssetBackfillCheckpointTest,ImportStarterWorkflowCleanupTest,BinaryAssetBackfillProcessorTest,BinaryAssetBackfillTest,ExportStarterFailureTest,BundleArchiveStorageTest,FileAssetBundlerTest,TemporaryAssetStorageTest,WebdavAssetStorageTest,TemporaryMetadataStorageTest,WebdavTemporaryStorageTest,ImageFilterExporterFocalPointTest,ImageFilterExporterTest,ImageFilterExporterSharedStoreTest,ImageFilterExporterEngineSelectionTest \
  -Ds3.test.endpoint=http://127.0.0.1:19002 \
  -Ds3.test.jdbc=jdbc:postgresql://127.0.0.1:19003/binary_storage_test

docker stop binary-s3-test binary-cleanup-postgres-test
```

The CMS integration checks are registered in `Junit5Suite1` and run against the full integration
stack:

```sh
./mvnw install -pl :dotcms-core --am -DskipTests -Ddocker.skip
./mvnw verify -pl :dotcms-integration -Dmaven.build.cache.enabled=false -Dcoreit.test.skip=false \
  -Dit.test=BinaryAssetStorageIntegrationTest,ContentletBackupStorageTest,SharedAssetStorageIntegrationTest,BinaryAssetStarterRestoreTest,PublishingArchiveStorageTest,DotWebdavHelperTest,AssetTemplateStorageTest,CSSAssetStorageTest
```

These default to flag-off mode, where the S3 cases are skipped. To run the S3 cases, create a
bucket in the disposable MinIO (for example `mc mb local/s3-cms-it` inside the container) and add
`-Dit.test.forkcount=1 -Ds3.cms.enabled=true -DDOT_FEATURE_FLAG_S3_ASSET_STORAGE=true
-DDOT_BINARY_ASSET_STORAGE_TYPE=BINARY_CHAIN -DDOT_STORAGE_FILE_METADATA_DEFAULT_CHAIN=FILE_SYSTEM,S3`
plus the `DOT_STORAGE_FILE_METADATA_S3_BUCKET_NAME`, `_BUCKET_REGION`, `_ACCESS_KEY`,
`_SECRET_ACCESS_KEY` and `_ENDPOINT` properties.

`BinaryAssetStarterRestoreTest` is an opt-in starter acceptance test that runs in phases against
the same bucket. Add `-Ds3.starter.restore.enabled=true` and a persistent
`-Ds3.starter.archive=<absolute path under dotCMS/target>/starter.zip`, then run
`-Ds3.starter.phase=export`, followed by `-Ds3.starter.phase=restore` with
`-Dstarter.run.path` set to the same archive. `-Ds3.starter.phase=populated` (after a fresh
export, without `-Dstarter.run.path`) tests replacing a populated database; run it only in the
disposable harness, because it replaces that database's content.

CI does not yet provide the MinIO service, so `BinaryS3StorageTest` does not run there.

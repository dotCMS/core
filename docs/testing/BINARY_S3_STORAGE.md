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

`SharedExtractedMetadata` caches byte-derived Tika extraction at
`extracted-metadata/<source-sha256>/<configuration-hash>.json`. Nothing calls it yet; metadata
generation starts using it in a later slice.

## Binary asset API

`BinaryAssetStorageAPI` (`APILocator.getBinaryAssetStorageAPI()`) manages binary assets and
completed renditions through the storage chain. Binary assets use the `binary-assets` group under
the asset root, laid out as `{inode[0]}/{inode[1]}/{inode}/{field}/{fileName}`. Renditions use the
`generated-assets` group under the `dotGenerated` root. With the flag on, keys in both groups keep
their mixed-case names; other groups are still lowercased.

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
MinIO bucket. `BinaryS3StorageTest` is skipped unless `s3.test.endpoint` is set; the other
tests always run. The credentials below are disposable local test values.

```sh
docker run -d --rm --name binary-s3-test \
  -p 127.0.0.1:19002:9000 \
  -e MINIO_ROOT_USER=binary-storage-test \
  -e MINIO_ROOT_PASSWORD=binary-storage-test \
  minio/minio:latest server /data

./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
  -Dtest=AssetStorageFeatureTest,AssetStorageFeatureLatchTest,S3StorageConfigurationTest,NoWebIdentityCredentialsProviderChainTest,BinaryS3StorageTest,BinaryAssetReferenceTest,BinaryCacheEvictionJobTest,BinaryFileSystemStorageTest,BinaryAssetStorageAPIImplTest,MetadataLocalCacheTest \
  -Ds3.test.endpoint=http://127.0.0.1:19002

docker stop binary-s3-test
```

CI does not yet provide the MinIO service, so `BinaryS3StorageTest` does not run there.

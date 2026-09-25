# S3 asset storage

S3 asset storage lets dotCMS keep binary assets and their metadata durably in S3, with the
local asset directory acting as a cache. It is opt-in and off by default. With the flag off,
the filesystem/NFS behavior is unchanged.

This page describes what is in the code today. It grows as the remaining parts land.

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
  node can upload the same key at any time.
- Deletes keep the local copy until all durable providers accept the deletion, so an
  in-flight restore on the same chain cannot bring the object back.
- Storage and database query failures propagate instead of being reported as a missing
  object.
- Providers can list objects under a prefix, verify that a durable copy has the same
  contents (`hasDurableCopy`), and copy an existing file or object to durable storage without
  overwriting a conflicting one (`backfillFile`, `backfillObject`).

Two building blocks are included for the slices that follow. `S3ContentAddressedStorage`
stores one immutable copy of each set of bytes at
`asset-blobs/sha256/<chars-1-2>/<chars-3-4>/<chars-5-6>/<chars-7-8>/<sha256>`.
`SharedExtractedMetadata` caches byte-derived Tika extraction at
`extracted-metadata/<source-sha256>/<configuration-hash>.json`. Nothing calls either one yet.

## Credentials and region

With the flag on, leave both `storage.file-metadata.s3.access-key` and
`storage.file-metadata.s3.secret-access-key` unset to use the AWS SDK default credential
provider chain. The configured bucket region and endpoint are kept in that mode. A partial
key pair fails initialization. The AWS STS module is packaged so role-based providers,
including web identity, work.

Without a custom endpoint, a configured region selects the regional AWS endpoint, and an
absent region falls back to the SDK region provider chain. A custom endpoint
(`storage.file-metadata.s3.endpoint`) must be an absolute HTTP(S) URL and requires
`storage.file-metadata.s3.bucket-region` for request signing. Invalid endpoint configuration
fails instead of silently selecting AWS. With the flag off, configuration behaves as before.

## Sharing one bucket across installations

With the flag on, `storage.file-metadata.s3.namespace` (Docker:
`DOT_STORAGE_FILE_METADATA_S3_NAMESPACE`) separates the keys of installations that share a
bucket. Owned keys become `asset-namespaces/<namespace>/<group>/<existing-key>`; shared
extracted metadata stays at the bucket root so identical bytes are extracted once. Use the same
value for all nodes in one cluster and distinct values for independent installations. Values
are 1 to 64 letters, digits, underscores or hyphens, starting with a letter or digit. Empty is
the default and keeps the existing layout.

Set the namespace before the installation first stores assets. Changing it later does not
migrate existing records. It separates application keys only; it does not restrict what the
bucket credentials can access.

## Static push publishing

Enabling the flag also changes static push publishing to S3 (`AWSS3Storage`). Clients sign
with SigV4, resolve the endpoint from the region when no endpoint is configured, and list
every page of objects instead of only the first 1,000.

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
  -Dtest=AssetStorageFeatureTest,AssetStorageFeatureLatchTest,S3StorageConfigurationTest,BinaryS3StorageTest \
  -Ds3.test.endpoint=http://127.0.0.1:19002

docker stop binary-s3-test
```

CI does not yet provide the MinIO service, so `BinaryS3StorageTest` does not run there.

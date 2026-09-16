# Local S3 binary storage POC

This runs the worktree's dotCMS image with the TravelLux demo starter, PostgreSQL,
OpenSearch, and MinIO. The ports bind only to localhost. Credentials are disposable
local demo credentials.

| Service | URL | Login |
| --- | --- | --- |
| Demo site | http://localhost:18080/ | Public |
| dotCMS admin | http://localhost:18080/dotAdmin/ | `admin@dotcms.com` / `admin` |
| MinIO console | http://localhost:19001/ | `s3-poc-admin` / `local-s3-poc-password` |
| S3 endpoint | http://localhost:19000/ | Same MinIO credentials |

The private bucket is `binary-assets-poc`:

- `asset-blobs/sha256/`: immutable original/rendition bytes shared by SHA-256.
- `binary-assets/`: content-owned original references.
- `generated-assets/`: content-owned references for completed image renditions.
- `extracted-metadata/`: shared byte-derived Tika output, keyed by source hash and parser configuration.
- `dotmetadata/`: asset metadata, using the existing metadata S3 adapter's hashed paths.

The compose file opts in with `DOT_FEATURE_FLAG_S3_ASSET_STORAGE=true`. The flag
is off by default; `BINARY_ASSET_STORAGE_TYPE=BINARY_CHAIN` alone no longer enables
S3. Rebuild the worktree image to use this flag and the latest fixes.

With the feature enabled, the chain stores originals and completed renditions in S3, restoring missing
local copies when requested. Responses still pass through dotCMS's existing URLs
and permission checks. Local disk is working space/cache; it is still required by
the image engines. `DOT_DOTGENERATED_DEFAULT_PATH=LOCAL` keeps generation local,
and `DOT_STORAGE_FILE_METADATA_DEFAULT_CHAIN=FILE_SYSTEM,S3` enables metadata backup.
Scheduled eviction is not enabled in this demo. Existing filesystem/NFS modes,
including `SHARED_COMPLETED`, retain their behavior when S3 mode is not selected.

## Current SHA demo acceptance — 2026-09-15

The prepared instance runs `dotcms/s3-assets-poc:legacy-20260915`, image
`sha256:ea1f5a5da350c9f087f2a435047435316ea66fb98c0816f3e79d8a08412b1fd9`.
The database, filesystem volumes and 1,619 S3 objects were backed up under
`dotCMS/target/s3-poc-backups/20260915-legacy` before replacing only the application.

Administrator migration job `9ce8717f-6e6f-4d0f-8a73-141e96cd7975` completed
545 originals, including the two older starter Image filename objects. The app
was deliberately killed after a persisted checkpoint; the existing job detector
automatically retried the same job and it completed with one retry. Normal job
recovery timers were restored after this test. Both legacy images then passed
byte-identical HTTP retrieval after their local cache copies were removed, with
MinIO recording their SHA blob reads. The original and resized GIF remain unchanged.

The preceding SHA acceptance also verified the following:

The original `happy-friday.gif`, its migrated width-143 rendition, a newly generated
width-145 rendition and a fresh mixed-case upload all passed byte-identical HTTP
retrieval after their exact local cache files were cleared. MinIO recorded four
successful reads from the SHA blob keys. All GIFs preserve 22 frames with the same
70 ms frame timing. Readiness and the TravelLux homepage also passed.

Two new uploads with identical bytes and different filenames shared one blob and
one extracted-metadata object. Deleting the first through dotCMS removed its owner
reference; the second then passed cold retrieval with its own filename intact.

- [Resized GIF](http://localhost:18080/contentAsset/image/a06977d1-69bb-4509-a361-b392b49c8ef9/asset/byInode/true/resize_w/145)
- [Surviving shared-blob upload](http://localhost:18080/dA/ec3db33a121a982ef0c668dfc03f2554/asset/SHA-Shared-Proof-B.Txt)

Evidence and a runnable artifact verifier are in
`dotCMS/target/s3-poc/proof-sha-20260915/` (`proof-summary.json`, both MinIO trace
files and `verify-proof.py`; the verifier requires Pillow). This supersedes the
older demo checkpoints below. The latest migration/restart and legacy cold-read
proof is in `dotCMS/target/s3-poc/proof-restart-20260915/`, including
`proof-summary.json` and `verify-proof.py` (standard library only). Shared-blob
reclamation, cleanup-worker restart, full independent CMS node acceptance and the
other completion-checklist gaps remain open. Scheduled eviction remains disabled. Nothing was pushed.

## Start and stop the prepared instance

Run from the repository root:

```sh
docker compose -f docker/docker-compose-examples/s3-asset-storage-poc/docker-compose.yml up -d
docker compose -f docker/docker-compose-examples/s3-asset-storage-poc/docker-compose.yml stop
```

Named volumes preserve the demo database, bucket, and cache between runs.

## Rebuild and prepare a fresh instance

Build the branch before building its image:

```sh
./mvnw install -pl :dotcms-core --am -DskipTests -Ddocker.skip -Dmaven.build.cache.enabled=false
./mvnw initialize docker:build -pl :dotcms-core \
  -Ddotcms.image.name=dotcms/s3-assets-poc:legacy-20260915 \
  -Ddocker.filter=dotcms -Ddocker.skip=false -Dmaven.build.cache.enabled=false
mkdir -p dotCMS/target/s3-poc
curl -fL https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20260630/starter-20260630.zip \
  -o dotCMS/target/s3-poc/demo-starter.zip
```

The starter contains legacy filesystem assets. Seed its binary files before the
first CMS startup; this is POC preparation, not an application migration feature:

```sh
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile

root = Path('dotCMS/target/s3-poc')
count = 0
with ZipFile(root / 'demo-starter.zip') as starter:
    for entry in starter.infolist():
        parts = entry.filename.split('/')
        if entry.is_dir() or len(parts) != 6 or parts[0] != 'assets':
            continue
        assert all(part not in ('', '.', '..') for part in parts)
        target = root / 'seed' / 'binary-assets' / Path(*parts[1:])
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(starter.read(entry))
        count += 1
assert count == 540, f'Unexpected starter binary count: {count}'
PY
docker compose -f docker/docker-compose-examples/s3-asset-storage-poc/docker-compose.yml up -d minio create-bucket
docker run --rm --network dotcms-s3-assets-poc_default \
  -v "$PWD/dotCMS/target/s3-poc/seed:/seed:ro" --entrypoint /bin/sh minio/mc:latest -c \
  'mc alias set poc http://minio:9000 s3-poc-admin local-s3-poc-password && mc mirror --overwrite /seed/binary-assets poc/binary-assets-poc/binary-assets'
docker compose -f docker/docker-compose-examples/s3-asset-storage-poc/docker-compose.yml up -d
```

The `binary-assets-poc.minio` network alias and `MINIO_DOMAIN` support the AWS
adapter's virtual-host bucket addressing. Wait for
`http://localhost:18090/dotmgt/readyz` to return HTTP 200.

## Verified in the running POC

On 2026-09-11, the 540 starter binaries (124,306,258 bytes) were copied into MinIO
and checked against the ZIP contents. The TravelLux homepage rendered with images.
A fresh `S3-Poc-Proof.txt` was uploaded and published through dotCMS's workflow API,
then found at this MinIO key:

```text
binary-assets/f/5/f5d4f5a2-4f3f-4f43-9b2a-1beb30aaab51/asset/S3-Poc-Proof.txt
```

After verifying the remote object, only that binary's local cached file was
removed. An HTTP request restored it from MinIO; both the response and restored
file matched the original SHA-256:
`a86de3712f02fd28162db589fc9e052fb46598c83a80796681bb5a62e3e57cc6`.

The proof file remains available at
http://localhost:18080/dA/307ef27018a7f62a922e0763e4be291c/asset/S3-Poc-Proof.txt
(sign into dotCMS if prompted). The IDs describe this prepared instance and change
when recreated. Automated lifecycle/deletion and safe-eviction coverage is documented
in [the storage test notes](../../../docs/testing/BINARY_S3_STORAGE.md).


## Original + rendition serving proof

The updated image was built and deployed on 2026-09-11 with the existing demo data
preserved. `happy-friday.gif` was resized to 139 × 208. The generated object is:

```text
generated-assets/a/0/dotGenerated_resize_ad1145fd36b98bfe52aec318e5fad20e.gif
```

After verifying the remote copies, both the original and this rendition were
removed from their local caches. A new resize request returned the same bytes.
MinIO's request trace recorded successful `s3.GetObject` calls for both
`binary-assets/.../happy-friday.gif` and the `generated-assets/` object above.
The rendition MD5 was `76709048713e2c284b854942419f6649` before and after retrieval.

The 536 existing metadata files were backfilled for this prepared instance, using
the original field-variable case and the metadata adapter's SHA-256 directory
keys. A fresh `S3-Metadata-Proof.txt` upload then created both its binary and its
metadata object through dotCMS itself. Freshly recreated instances generate
metadata through the configured S3 chain; preserving pre-existing custom metadata
requires backfill before discarding any filesystem data.

The automated checks cover PNG and GIF generation, cold-cache restoration without
pixel work, backfilling a warm rendition, remote invalidation/source deletion,
and safe generated-cache eviction. The demo remains a single-node POC; it does
not establish multi-node invalidation, actual NFS-mount behavior, or AWS IAM/KMS
compatibility. The current build serves warm renditions without a remote durability check or
re-upload; production throughput still needs measurement.

## Updated demo acceptance — 2026-09-14

The prepared instance now runs `dotcms/s3-assets-poc:metadata-tx-20260914`
(image `df20bd1e96107159ba0aef1e017cc0f468d79c33142a090b66135967a10726ff`),
with the feature flag enabled. The deployed feature/reference classes match the
last tested build. The database, local/shared volumes, and MinIO bucket were
backed up first under `dotCMS/target/s3-poc-backups/20260914-metadata-tx`.
The previous image remains tagged `dotcms/s3-assets-poc:before-metadata-tx-20260914`.

The TravelLux homepage renders with images and readiness returns HTTP 200.
The existing `happy-friday.gif` retains its original SHA-256. A new **141 × 211**
GIF rendition was generated at:

```text
generated-assets/a/0/a06977d1-69bb-4509-a361-b392b49c8ef9/dotGenerated_resize_339ab2425087f1fcf184224ab5067613.gif
```

After verifying remote sizes and checksums, only the original and this rendition
were removed from local cache. The next image request fetched **both objects from
MinIO**, as confirmed by two successful `s3.GetObject` trace records. The original
and rendition responses matched their prior bytes exactly. Sign in to the admin
first to view this protected asset:

[View the restored resized GIF](http://localhost:18080/contentAsset/image/a06977d1-69bb-4509-a361-b392b49c8ef9/asset/byInode/true/resize_w/141).

A fresh `S3-Current-Build-Proof.Txt` was uploaded and published through the normal
workflow API. Its `storageKey` points to an immutable revision, and both the
original and case-preserving metadata object were verified in MinIO. Its HTTP
response after clearing the local binary also matched the uploaded bytes.

[View the new proof upload](http://localhost:18080/dA/239e2e5757b83e828498d6aecba25451/asset/S3-Current-Build-Proof.Txt).

Evidence is in `dotCMS/target/s3-poc/proof-20260914/proof-summary.json` and the
adjacent response/checksum/trace files. This updates the running POC; the full
asset-path, migration, metadata lifecycle, multi-node, and NFS checklist remains
open. Scheduled eviction remains disabled in the demo. Nothing was pushed.

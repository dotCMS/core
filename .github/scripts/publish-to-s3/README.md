# publish-to-s3

Helper that publishes dotCMS build artifacts to the BunnyCDN S3-compatible
storage zone. It replaces the former Artifactory (`repo.dotcms.com`)
deployments, so every CI/CD publish path goes through one place instead of
duplicating endpoint/credential wiring in each workflow.

## Usage

```bash
# Publish one version of every module under com/dotcms, including nested groups
.github/scripts/publish-to-s3/publish.sh maven --version 26.09.14-01

# Publish a single file to an explicit key
.github/scripts/publish-to-s3/publish.sh file \
  --source ./starter/20260910.zip \
  --key com/dotcms/starter/20260910/starter-20260910.zip

# Preview without writing
.github/scripts/publish-to-s3/publish.sh maven --version 26.09.14-01 --dry-run
```

Both modes publish `.sha1`/`.md5` sidecars beside the artifacts so consumers
never hit `Checksum validation failed, no checksums available`; pass
`--no-checksums` to skip that. `maven` also (re)generates `maven-metadata.xml`
and its sidecars.

## Configuration

CLI flags take precedence over these environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `MAVEN_BUNNY_RW_USERNAME` | — | Bunny storage-zone name / S3 access key id |
| `MAVEN_BUNNY_RW_PASSWORD` | — | Bunny storage-zone password / S3 secret key |
| `MAVEN_S3_BUCKET` | `$MAVEN_BUNNY_RW_USERNAME` | Bucket (storage zone) |
| `MAVEN_S3_PREFIX` | `libs-release` | Key prefix inside the bucket |
| `MAVEN_S3_ENDPOINT` | `https://ny-s3.storage.bunnycdn.com` | S3 endpoint |
| `MAVEN_S3_REGION` | `ny` | S3 signing region |
| `MAVEN_S3_PUBLIC_URL` | `https://dotcms-repo.b-cdn.net` | Public CDN base used for printed URLs |
| `MAVEN_REPO_DIR` | `$HOME/.m2/repository` | Local Maven repository |
| `MAVEN_S3_EXCLUDE_EXT` | `repositories,excludeext` | Extra extensions to skip |
| `MAVEN_S3_UPDATE_METADATA` | `true` | Regenerate `maven-metadata.xml` |
| `MAVEN_S3_CHECKSUMS` | `true` | Upload `.sha1`/`.md5` checksums |
| `MAVEN_S3_ALLOW_SNAPSHOTS` | `false` | Publish `-SNAPSHOT` versions |

BunnyCDN convention: the S3 access key id **is** the storage-zone name, so the
bucket defaults to `MAVEN_BUNNY_RW_USERNAME`.

## What `maven` does

1. Recursively finds `<version>` directories under `$MAVEN_REPO_DIR/com/dotcms`,
   including nested groups such as `com.dotcms.core.plugins` and `com.dotcms.plugins`.
   Uploads each subtree to `s3://$BUCKET/$PREFIX/<groupPath>/<artifactId>/<version>/`,
   preserving the full repository-relative path. S3 has no real folders; nested
   key prefixes are created implicitly, matching the old Artifactory layout.
   `--modules` filters by artifactId at any group depth; if the same artifactId
   exists in multiple groups, all matching paths are preserved.
2. Uploads `.sha1`/`.md5` checksums for the primary artifacts (`.pom`, `.jar`,
   `.zip`, `.war`, `.aar`, `.module`).
3. Regenerates `<groupPath>/<artifactId>/maven-metadata.xml` from the versions
   already present in the bucket plus the one just uploaded, with the actual
   groupId derived from the group path. Artifactory used to do this automatically
   and consumers (for example the dotCLI action) read it, so a plain file copy is
   not enough.

Step 3 is best-effort: a metadata failure logs a warning but does not fail a
publish whose artifacts are already in place.

### Snapshots

`maven` **refuses to publish `-SNAPSHOT` versions** by default (logs a warning
and exits 0). dotCMS does not consume shared snapshots, and publishing one to
`libs-release` would make the artifact-level `maven-metadata.xml` `<latest>` a
snapshot, breaking the release lookup the CLI action performs. Pass
`--allow-snapshots` to override.

## Regression tests

The tests execute the publisher with an offline AWS CLI stub; no credentials,
network access, or live repository writes are needed. They cover nested group
paths, metadata, checksums, module selection, and existing flat-group behavior.
The PR workflow runs them whenever the publisher or its action changes.

Known limitation: a failed directory scan can yield a partial candidate list
without failing publication. This hardening is deferred from the nested-group
hotfix; compare the dry-run output with the expected release-module inventory
before any backfill.

```bash
python3 -m unittest discover -s .github/scripts/publish-to-s3/tests -v
```

## Backfilling missing releases

Fixing the publisher does not restore artifacts omitted by earlier releases.
Restore the original release's GitHub Actions `maven-repo` build artifact into
an isolated local repository and copy its exact release-versioned files. Do not
regenerate JARs, rename a previous release's JAR/POM to a newer version, or publish
snapshots as releases. If the original build artifact is unavailable, stop and
report the missing source instead of reconstructing it. Audit **all** nested
dotCMS groups, not just the Tika API.

For a targeted Tika API backfill, preview first:

```bash
.github/scripts/publish-to-s3/publish.sh maven \
  --version 26.09.28-02 --repo-dir /path/to/restored-release-repository \
  --modules com.dotcms.tika-api --dry-run
```

After reviewing the paths and release provenance, authorized operators can rerun
without `--dry-run` to publish the artifacts, checksums, and metadata. Compare
existing destination objects first: publication overwrites matching keys. Only
the original release-build bytes are authorized for this recovery.
Serialize backfills with normal releases of the same artifact to avoid metadata
read/modify/write races. Verify JAR/POM/ZIP objects, metadata, and their sidecars
through the public CDN, purge cached misses if necessary, and resolve the
affected core version using an empty local Maven repository. Remove the temporary
customer dependency override once matching versions resolve.

## Resulting URLs

With the defaults, an artifact published for version `26.09.14-01` is served at:

```
https://dotcms-repo.b-cdn.net/libs-release/com/dotcms/dotcms-core/26.09.14-01/dotcms-core-26.09.14-01.jar
```

> **Prefix note:** the request that introduced this migration said the repo
> starts under `/libs-releases`, but the example URLs (and the retired
> Artifactory `libs-release` repo) use `/libs-release`. The script defaults to
> `libs-release`; set `MAVEN_S3_PREFIX` to override if the storage zone really
> uses the plural form.

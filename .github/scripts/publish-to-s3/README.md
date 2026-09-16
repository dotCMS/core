# publish-to-s3

Helper that publishes dotCMS build artifacts to the BunnyCDN S3-compatible
storage zone. It replaces the former Artifactory (`repo.dotcms.com`)
deployments, so every CI/CD publish path goes through one place instead of
duplicating endpoint/credential wiring in each workflow.

## Usage

```bash
# Publish one version of every com/dotcms module installed in ~/.m2/repository
.github/scripts/publish-to-s3/publish.sh maven --version 26.09.14-01

# Publish a single file to an explicit key
.github/scripts/publish-to-s3/publish.sh file \
  --source ./starter/20260910.zip \
  --key com/dotcms/starter/20260910/starter-20260910.zip

# Preview without writing
.github/scripts/publish-to-s3/publish.sh maven --version 26.09.14-01 --dry-run
```

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

1. Walks `$MAVEN_REPO_DIR/com/dotcms/*/<version>` and uploads each subtree to
   `s3://$BUCKET/$PREFIX/com/dotcms/<artifactId>/<version>/`. S3 has no real
   folders; nested key prefixes are created implicitly, so the layout matches
   the old Artifactory `libs-release` layout.
2. Uploads `.sha1`/`.md5` checksums for the primary artifacts (`.pom`, `.jar`,
   `.zip`, `.war`, `.aar`, `.module`).
3. Regenerates `com/dotcms/<artifactId>/maven-metadata.xml` from the versions
   already present in the bucket plus the one just uploaded. Artifactory used to
   do this automatically and consumers (for example the dotCLI action) read it,
   so a plain file copy is not enough.

Step 3 is best-effort: a metadata failure logs a warning but does not fail a
publish whose artifacts are already in place.

### Snapshots

`maven` **refuses to publish `-SNAPSHOT` versions** by default (logs a warning
and exits 0). dotCMS does not consume shared snapshots, and publishing one to
`libs-release` would make the artifact-level `maven-metadata.xml` `<latest>` a
snapshot, breaking the release lookup the CLI action performs. Pass
`--allow-snapshots` to override.

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

# Deploy Maven Artifacts to S3

Publishes the locally-installed dotCMS Maven artifacts to the BunnyCDN
S3-compatible repository. This replaces the former `deploy-jfrog` (Artifactory)
action.

The action restores the `maven-repo` artifact produced by the build phase,
resolves the project version, and delegates the upload to
[`.github/scripts/publish-to-s3/publish.sh`](../../../../scripts/publish-to-s3/README.md).
The script preserves the `com/dotcms/<artifactId>/<version>` layout, writes
`.sha1`/`.md5` checksums, and regenerates the `maven-metadata.xml` files that
Artifactory used to create.

## Inputs

| Input | Required | Default | Description |
| --- | --- | --- | --- |
| `version` | no | project version | Version to publish. Set it when the restored `maven-repo` artifact was built from a different ref than the checked-out POM (e.g. releases). |
| `modules` | no | all modules for the version | Comma-separated artifactIds to restrict the publish to. |
| `exclude-ext` | no | `repositories,excludeext` | Extra file extensions to skip. |
| `dry-run` | no | `false` | Print what would be uploaded without writing. |
| `bucket` | no | `MAVEN_BUNNY_RW_USERNAME` | S3 bucket / Bunny storage zone. |
| `prefix` | no | `libs-release` | Key prefix inside the bucket. |
| `endpoint` | no | `https://ny-s3.storage.bunnycdn.com` | S3 endpoint URL. |
| `region` | no | `ny` | S3 signing region. |
| `access-key-id` | **yes** | — | S3 access key id (Bunny storage-zone name). |
| `secret-access-key` | **yes** | — | S3 secret access key (Bunny storage-zone password). |
| `checksums` | no | `true` | Upload `.sha1`/`.md5` checksums. |
| `github-token` | **yes** | — | Token used to download the `maven-repo` artifact. |
| `artifact-run-id` | no | `${{ github.run_id }}` | Run id that holds the `maven-repo` artifact. |

## Example

```yaml
- name: Deploy Maven artifacts
  uses: ./.github/actions/core-cicd/deployment/deploy-maven-s3
  with:
    access-key-id: ${{ secrets.MAVEN_BUNNY_RW_USERNAME }}
    secret-access-key: ${{ secrets.MAVEN_BUNNY_RW_PASSWORD }}
    github-token: ${{ secrets.GITHUB_TOKEN }}
    artifact-run-id: ${{ inputs.artifact-run-id }}
```

Public URL for the above with the defaults:

```
https://dotcms-repo.b-cdn.net/libs-release/com/dotcms/<artifactId>/<version>/<artifactId>-<version>.jar
```

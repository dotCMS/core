# Temporary S3 credential acceptance

This fixture creates a fresh MinIO instance on loopback port 19004 with no host
data mounts. Its temporary test account can access only `binary-test-*` buckets.
It does not modify the demo or the existing storage test server. Stop the project
after testing to remove all account, policy, and object state.

From the repository root:

```sh
fixture=docker/docker-compose-examples/s3-asset-storage-poc/sts-acceptance/compose.yaml
docker compose -f "$fixture" up -d minio
docker compose -f "$fixture" run --rm provision
./mvnw install -pl :dotcms-core -Dmaven.build.cache.enabled=false \
  -DskipTests=false -Ddocker.skip \
  -Dtest=AssetStorageFeatureTest,BinaryS3StorageTest,S3StorageConfigurationTest \
  -Ds3.test.endpoint=http://127.0.0.1:19004 \
  -Ds3.test.sts.accessKey=binary-sts-test \
  -Ds3.test.sts.secretKey=binary-sts-local-test-password
docker compose -f "$fixture" down --volumes
```

Check each exit status before continuing; run cleanup even if a test fails.
The STS test requests bucket-scoped session credentials through the real AWS SDK
and MinIO, uploads a binary, explicitly refreshes its credentials, evicts the local
copy, retrieves the original bytes, and deletes the object. It is skipped unless
`s3.test.sts.accessKey` is supplied. This verifies S3-compatible STS issuance and
refresh, not deployment-specific AWS IAM trust policies, EC2/ECS credentials,
OIDC token exchange, or expiry-driven refresh timing.

The default-chain test supplies disposable Java system-property credentials.
Existing AWS environment credentials take precedence. When running in an environment
that already defines them, set both `AWS_ACCESS_KEY_ID` and `AWS_ACCESS_KEY` to
`binary-storage-test`, both `AWS_SECRET_KEY` and `AWS_SECRET_ACCESS_KEY` to
`binary-storage-test`, and clear `AWS_SESSION_TOKEN` for this command only. In this
SDK, an empty `AWS_SECRET_KEY` masks `AWS_SECRET_ACCESS_KEY`.

MinIO documents the compatible STS request and session-policy behavior in its
[AssumeRole documentation](https://github.com/minio/minio/blob/master/docs/sts/assume-role.md).

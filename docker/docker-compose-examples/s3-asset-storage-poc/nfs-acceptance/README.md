# NFS provider acceptance

This disposable fixture runs the current worktree's compiled Java classes against
a real kernel NFS 4.1 server and two separate client mounts. It requires Docker
with kernel NFS support, the local `dotcms/s3-assets-poc:acceptance-20260915` image,
and a completed core test build including `BinaryS3StorageTest`. The runtime
classpath comes from that test's report; Maven dependencies are mounted read-only.

From the repository root:

```sh
python3 docker/docker-compose-examples/s3-asset-storage-poc/nfs-acceptance/prepare-runtime.py
fixture=docker/docker-compose-examples/s3-asset-storage-poc/nfs-acceptance/compose.yaml
docker compose -f "$fixture" build nfs
docker compose -f "$fixture" up -d nfs
docker compose -f "$fixture" up -d client-a client-b
docker compose -f "$fixture" exec -T client-a mkdir -p /mnt/nfs/runner /mnt/nfs/junit
docker compose -f "$fixture" exec -T client-a /java/bin/javac @/workspace/dotCMS/target/s3-nfs/javac.args /workspace/docker/docker-compose-examples/s3-asset-storage-poc/nfs-acceptance/NfsAcceptance.java
docker compose -f "$fixture" exec -T -w /mnt/nfs client-a /java/bin/java @/workspace/dotCMS/target/s3-nfs/java.args com.dotcms.storage.NfsAcceptance unit
docker compose -f "$fixture" exec -T -w /mnt/nfs client-a /java/bin/java @/workspace/dotCMS/target/s3-nfs/java.args com.dotcms.storage.NfsAcceptance seed
docker compose -f "$fixture" exec -T -w /mnt/nfs client-b /java/bin/java @/workspace/dotCMS/target/s3-nfs/java.args com.dotcms.storage.NfsAcceptance replace
docker compose -f "$fixture" exec -T -w /mnt/nfs client-a /java/bin/java @/workspace/dotCMS/target/s3-nfs/java.args com.dotcms.storage.NfsAcceptance verify
docker compose -f "$fixture" exec -T -w /mnt/nfs client-b /java/bin/java @/workspace/dotCMS/target/s3-nfs/java.args com.dotcms.storage.NfsAcceptance delete
docker compose -f "$fixture" exec -T -w /mnt/nfs client-a /java/bin/java @/workspace/dotCMS/target/s3-nfs/java.args com.dotcms.storage.NfsAcceptance deleted
docker compose -f "$fixture" down --volumes
```

Check each command's exit status before continuing. Clients retry the initial mount
for up to 25 seconds. Always run the last cleanup command after a failure; it removes
only this fixture's `binary-nfs-it` project and disposable data volume.

The unit phase requires exactly 30 successful tests and no failures. The five
provider phases verify cross-process visibility, replacement, copy, deletion,
neighbor preservation, and refusal to evict with the S3 flag disabled. This is not
a full CMS cluster or an NFS outage/failover test. The fixture publishes no host ports.

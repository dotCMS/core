# Reproduction: #35592 — App configs silently wiped (dotSecretsStore.p12)

Two dotCMS nodes share a single assets volume and a single DB, so they read/write the **same**
`/data/shared/assets/server/secrets/dotSecretsStore.p12`. With no file locking and a
truncate-then-stream write (`DOT_CONTENT_VERSION_HARD_LINK=false`, simulating NFS/EFS), a save on
one node can be observed as a torn/empty PKCS12 by the other node, which then "self-heals" by
backing up + deleting + regenerating an **empty** store — silently wiping every configured App
secret. See `SecretsStoreConcurrentWriteRaceTest` for the license-free unit-level repro; this is
the higher-fidelity cluster confirmation.

## Prerequisites

- Docker + Docker Compose.
- A **2+ seat enterprise license** (Apps/secrets is an enterprise feature). Once the stack is up,
  drop `license.zip` into the shared volume so both nodes see it:
  ```bash
  docker compose cp /path/to/license.zip dotcms-node-1:/data/shared/assets/license.zip
  ```
- Optional: to test the **current source** instead of the released image, build the local image and
  export it before `up`:
  ```bash
  just build         # produces dotcms/dotcms-test:1.0.0-SNAPSHOT
  export DOTCMS_IMAGE=dotcms/dotcms-test:1.0.0-SNAPSHOT
  ```

## Run

```bash
cd docker/docker-compose-examples/issue-35592-secrets-store-race
docker compose up -d
# node-1 UI: https://localhost:8443/dotAdmin  (admin / admin)
# node-2 UI: https://localhost:8444/dotAdmin
```

## Fastest confirmation (no license, no UI) — the boot race

Just bringing both nodes up on the shared volume with the torn-write path on is enough to
corrupt the store. Both nodes run `createStoreIfNeeded()` concurrently at startup and collide on
the truncating write, leaving a torn store:

```bash
docker compose up -d
# wait for both nodes to boot, then:
docker compose exec dotcms-node-1 sh -c \
  'ls -la /data/shared/assets/server/secrets/ && \
   keytool -list -keystore /data/shared/assets/server/secrets/dotSecretsStore.p12 \
           -storetype pkcs12 -storepass any'
```

Observed: a **103-byte** `dotSecretsStore.p12` (the exact pathological size from the field report)
that fails to load:

```
keytool error: java.io.IOException: Integrity check failed:
  java.security.UnrecoverableKeyException: Failed PKCS12 integrity checking
```

— i.e. the exact stack-trace signature from #35592, with zero manual intervention.

## Reproduce (full Apps flow — needs license)

1. On **node-1**, configure any App with secrets (e.g. dotCDN, or a custom App) at
   `/dotAdmin/#/apps`. Confirm the secrets are present on **node-2** too.
2. Drive concurrent save+read load across both nodes so their writes to the shared p12 overlap.
   Either:
   - repeatedly save an App secret on node-1 while repeatedly opening the Apps portlet /
     reading secrets on node-2, or
   - script it against both nodes' Apps endpoints in a tight loop.
3. Watch for the symptom: on one node the Apps list goes empty / secret fields clear, while the
   other still shows them ("taking turns"). Inspect the store on the shared volume:
   ```bash
   docker compose exec dotcms-node-1 ls -la /data/shared/assets/server/secrets/
   ```
   You should see the store shrink to a fresh empty p12 (~1.7 KB) and a timestamped
   `yyyyMMddHHmmss-dotSecretsStore.p12` backup appear. The only log line of interest:
   ```bash
   docker compose logs dotcms-node-2 | grep -i "integrity\|recover secrets\|PKCS12"
   ```

## cluster_salt read-vs-write check (issue AC)

Confirm both nodes derive the same keystore password from the shared `cluster_salt`:
```bash
docker compose exec db psql -U dotcmsdbuser -d dotcms -c "select cluster_id, cluster_salt from dot_cluster;"
```
Both nodes point at this one DB, so the salt (and thus the derived password) should match. A
divergence here would indicate a per-node `DOT_SECRETS_KEYSTORE_PASSWORD_KEY` override or a stale
in-JVM salt cache after a rotation — a separate, secondary trigger from the write race.

## Teardown

```bash
docker compose down -v      # -v also removes the shared volume (and the wiped store)
```

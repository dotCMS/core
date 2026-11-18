# Contract: the CLI's bundled compose file

**File**: `core-web/libs/sdk/create-app/assets/docker-compose.yml`
**Consumer**: `@dotcms/create-app` only — it is shipped **inside the npm package**, not downloaded.

> **The shared `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml` is NOT
> changed by this work.** It keeps serving README readers and already-installed CLIs (≤1.2.5)
> exactly as before. See [cli-design-decisions.md](../cli-design-decisions.md) D4.

Because nothing else reads this file, it can be strict without imposing on anyone. That is the
whole reason for owning it.

---

## C1 — Startup ordering

`dotcms` MUST NOT start until `db` and `opensearch` report **healthy**.

```yaml
depends_on:
  db:
    condition: service_healthy
  opensearch:
    condition: service_healthy
```

*Deviates from the sibling examples, which use `service_started` for opensearch. Safe here in a way
it was not on the shared file: an unattended CLI wants the strongest ordering available, and a
future OpenSearch image bump that invalidated the probe would affect only this CLI's own stack —
not README readers or other examples.*

## C2 — Health probes

| Service | Probe | Source |
|---|---|---|
| `db` | `pg_isready -U dotcmsdbuser -d dotcms -h localhost -p 5432` | already present; now actually consumed |
| `opensearch` | `curl -sk https://localhost:9200 -u admin:admin \| grep -q cluster_name` | adapted from `single-node-os-migration` — `-k` for the self-signed cert, `-u` because `DOT_ES_AUTH_BASIC_PASSWORD: 'admin'` |
| `dotcms` | `curl -f http://127.0.0.1:8090/dotmgt/livez` | matches `lgtm-observability` / `single-node-metrics-monitoring` |

`dotcms` MUST use **`start_period: 180s`** — roughly 4x the measured ~46s boot. (Precedents: lgtm
120s, metrics-monitoring 20s; this is above both.)

Erring high is deliberate and costs nothing: `start_period` is the window in which failing probes do
not count toward `retries`, and **the first successful probe ends it immediately**, so a stack that
boots in 46s leaves the window at 46s whatever the ceiling. Erring low does cost. Once the window
elapses, probes start counting, `retries` is exhausted, the container is marked `unhealthy`, and
`docker compose up --wait` **aborts** — abandoning an instance that would have been healthy moments
later. `restart: unless-stopped` cannot rescue it, because Compose restart policies react to
container *exit*, not health status.

> An earlier draft of this contract recorded the risk of a short window as "`--wait` blocks until
> timeout". That is backwards; the corrected direction is what justifies 180s over 120s.

The OpenSearch probe is **verified on this stack**: it succeeds at ~15s, and `curl` is present in
the `opensearchproject/opensearch:1` image.

`curl` is present in the image (`Dockerfile:47`), so `CMD`-form probes are valid.

## C3 — Restart policy

`db`, `opensearch` and `dotcms` MUST all declare `restart: unless-stopped`.

**Semantics that matter**: this reacts to container **exit**, not to `unhealthy` (research R4). It
is what stops the reported failure — dotCMS dying and staying dead — and it cannot cause a
health-driven restart loop.

## C4 — Published ports

| Port | Binding | Contract |
|---|---|---|
| `8082`, `8443` | `0.0.0.0` | unchanged — the application |
| `9200`, `9600` | `0.0.0.0` | unchanged |
| `8090` | **`127.0.0.1:8090:8090`** | management port; loopback ONLY |

**8090 MUST NOT be published on `0.0.0.0`.** `InfrastructureManagementFilter` authorizes by arrival
port with no credential check and no IP allowlist (research R3), so a wildcard binding exposes
`/dotmgt/health` and `/dotmgt/metrics` to the local network. The container's own healthcheck uses
`127.0.0.1` internally and is unaffected.

## C5 — `--starter` compatibility (**do not break**)

The file MUST retain a line matching:

```
/^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*).+$/m
```

`updateDockerComposeStarterUrl` (`src/index.ts:487`) rewrites the file **on disk after it is
written** with this regex when `--starter` is passed, and **throws if there is no match**.
Converting that key to a YAML block scalar, an anchor, or `- CUSTOM_STARTER_URL=…` list form breaks
`--starter`.

Less severe than before — the blast radius is now this CLI version rather than every installed one —
but still a silent break. **Two** guards cover it, and neither subsumes the other:

- `core-web/libs/sdk/create-app/scripts/verify-cold-start.sh --static` (T008) greps the **file** for
  the line shape installed CLIs depend on. No Docker; safe to run on every PR.
- A Jest spec runs `updateDockerComposeStarterUrl()` itself against the real bundled asset and
  asserts the **function's** output (AC-012).

## C6 — Documentation

`core-web/libs/sdk/create-app/README.md` MUST state that the stack publishes 8090 on loopback, what
it serves, and that the compose file is bundled rather than downloaded — including the
`DOTCMS_COMPOSE_URL` escape hatch for pointing at a remote file instead (D4a).

## C7 — Delivery

The file is read through the `ComposeSource` interface (D4a) and **written** to the project
directory, never downloaded to it. `resolveComposeSource()` returns the bundled asset unless
`DOTCMS_COMPOSE_URL` is set. The asset MUST be listed in both `package.json` `files` and
`project.json`'s esbuild `assets`, or it will silently not ship.

## C8 — Image tag

The file pins **no** dotCMS version: `dotcms/dotcms:latest` stays for now (D8). The drift risk the
issue flagged — `latest` paired with a hardcoded `starter-20260630` — is therefore **still open**,
and ADR-0019 alignment (SDK version = dotCMS release version) is deferred, not resolved.

---

## Verification

`core-web/libs/sdk/create-app/scripts/verify-cold-start.sh` — cold start with `--wait`,
`docker kill` recovery, loopback-only exposure, and the `CUSTOM_STARTER_URL` guard for C5. Run
`--static` for the config-only assertions (no Docker required).

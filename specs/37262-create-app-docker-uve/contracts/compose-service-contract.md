# Contract: `single-node-demo-site/docker-compose.yml`

**Consumers**: every installed `@dotcms/create-app` (fetched from `main` at runtime via
`src/git/index.ts:68`), and humans following `single-node-demo-site/README.md`.

This file has no version negotiation — a change reaches all consumers at once. These are the
guarantees it must keep.

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

*Deviates from sibling examples, which use `service_started` for opensearch — justified in
research R1: this stack is driven by an unattended CLI.*

## C2 — Health probes

| Service | Probe | Source |
|---|---|---|
| `db` | `pg_isready -U dotcmsdbuser -d dotcms -h localhost -p 5432` | already present; now actually consumed |
| `opensearch` | `curl -sk https://localhost:9200 -u admin:admin \| grep -q cluster_name` | adapted from `single-node-os-migration` — `-k` for the self-signed cert, `-u` because `DOT_ES_AUTH_BASIC_PASSWORD: 'admin'` |
| `dotcms` | `curl -f http://127.0.0.1:8090/dotmgt/livez` | matches `lgtm-observability` / `single-node-metrics-monitoring` |

`dotcms` MUST use `start_period: 180s` — a cold first boot performs a full demo-starter import.
(Above both precedents: lgtm 120s, metrics-monitoring 20s. See research R4 open item 2.)

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

## C5 — Backward compatibility with installed CLIs (**do not break**)

The file MUST retain a line matching:

```
/^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*).+$/m
```

`updateDockerComposeStarterUrl` (`src/index.ts:487`) rewrites the file with this regex when
`--starter` is passed and **throws if there is no match**. Converting that key to a YAML block
scalar, an anchor, or `- CUSTOM_STARTER_URL=…` list form would break `--starter` for every already
-installed CLI, with no release able to fix them.

A CLI at ≤1.2.5 running `docker compose up -d` (no `--wait`) against this file MUST still work — it
gains correct startup and a restart policy, and ignores 8090 entirely.

## C6 — Documentation

`single-node-demo-site/README.md` MUST state that 8090 is published on loopback and what it serves.
An undocumented new port mapping in a file users read as a template is a surprise.

---

## Verification

See [quickstart.md](../quickstart.md) — cold start with `--wait`, `docker kill` recovery, and a
`--starter` regression run to prove C5.

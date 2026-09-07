# Phase 1 Data Model: create-app local Docker start failure + permanent UVE 403

**Feature**: `37262-create-app-docker-uve` · **Plan**: [plan.md](./plan.md)

This fix introduces no persisted data — no DB table, no OpenSearch mapping, no serialized state.
The "entities" that matter are three in-memory / on-disk shapes whose **lifecycle** is the actual
bug: state that exists, is valid, and gets thrown away.

---

## 1. `RunState` — the CLI's accumulated, recoverable state

The central entity. Today it exists only as loose locals in `main()`; the defect is that it has no
identity, so nothing guarantees it survives an early exit.

| Field | Type | Available from | Notes |
|---|---|---|---|
| `host` | `string` | before any network call | `http://localhost:8082` (`DOTCMS_HOST`) |
| `finalDirectory` | `string` | after prompts | target scaffold directory |
| `selectedFramework` | `SupportedFrontEndFrameworks` | after prompts | drives port + env var names |
| `composePath` | `string \| null` | after compose download | needed for `docker compose down` recovery |
| `token` | `string \| null` | after `getAuthToken` | **currently discarded on UVE failure** |
| `siteId` | `string \| null` | after `getDefaultSite` | **currently discarded on UVE failure** |
| `uveConfigured` | `boolean` | after UVE call | new — false must not be fatal |
| `scaffolded` | `boolean` | after clone + install | new |

### State transitions

```
prompts ──▶ ports checked ──▶ compose downloaded ──▶ containers up ──▶ readyz green
                                                                          │
                                                              ┌───────────┴───────────┐
                                                              ▼                       ▼
                                                       token issued            (fail: no token)
                                                              │
                                                       site resolved
                                                              │
                                              ┌───────────────┴───────────────┐
                                              ▼                               ▼
                                    UVE GET 200 → POST ok            UVE unavailable
                                    uveConfigured = true             uveConfigured = false
                                              │                               │
                                              └───────────────┬───────────────┘
                                                              ▼
                                                    scaffold (clone + install)
                                                              ▼
                                              write .env  +  emit RunState
```

**Invariant (this is the fix):** once `token` and `siteId` are non-null, **every** terminal path —
success, handled failure, or unexpected throw — emits them and writes `.env`. The UVE branch merges
back into the main line instead of terminating it.

**Validation rules**
- `token`/`siteId` are never logged before they are non-null (avoids printing `null` as a value).
- `.env` is written only when absent (research R7); when present, the block is printed instead.
- `composePath` must be non-null before any recovery instruction that says `docker compose down`.

---

## 2. `ComposeTopology` — the service dependency + health graph

Not a runtime object; the contract **the CLI's own bundled compose file** encodes
(`core-web/libs/sdk/create-app/assets/docker-compose.yml`). The shared `single-node-demo-site`
example is not changed by this work — see cli-design-decisions.md D4. The bug is a missing edge and
two missing health states.

| Service | Healthcheck | Restart | `dotcms` depends on it via |
|---|---|---|---|
| `db` | exists today (`pg_isready`), **unused** | `unless-stopped` ✓ | `condition: service_healthy` ← **new** |
| `opensearch` | **none** → add (`curl -sk … \| grep -q cluster_name`, from `single-node-os-migration`) | **none** → `unless-stopped` | `condition: service_healthy` ← **new** |
| `dotcms` | **none** → add (`curl -f http://127.0.0.1:8090/dotmgt/livez`, `start_period: 180s`) | **none** → `unless-stopped` | — |

**Health states** (Docker semantics, per research R4):

```
starting ──(within start_period, failures ignored)──▶ healthy
    │                                                    │
    └──(retries exhausted after start_period)──▶ unhealthy
```

- `service_healthy` gates dependents on `healthy`.
- `docker compose up --wait` blocks until all services are `healthy` or the wait times out.
- **`restart:` does not react to `unhealthy`** — only to container *exit*. An unhealthy container
  is not restarted by Compose.

**Port bindings**

| Published | Binding | Rationale |
|---|---|---|
| `8082`, `8443` | `0.0.0.0` (unchanged) | the app itself; users browse to it |
| `9200`, `9600` | `0.0.0.0` (unchanged) | existing behavior, out of scope |
| `8090` | **`127.0.0.1` only** ← new | management port is unauthenticated (research R3) |

**Compatibility constraint**: the file must retain a line matching
`/^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*).+$/m`, or `--starter` throws. This forbids converting
that key to a block scalar or `- KEY=value` list form. (Blast radius is now this CLI version rather
than every installed one, since the file is bundled — but it is still a silent break.)

---

## 3. `UVEAppConfig` — the payload and its readiness precondition

Unchanged in shape; what changes is when it may be written.

| Field | Type | Source |
|---|---|---|
| `siteId` | `string` | `RunState.siteId` — path segment on `/api/v1/apps/dotema-config-v2/{siteId}` |
| `configuration.hidden` | `boolean` | constant `false` |
| `configuration.value` | `string` | `getUVEConfigValue(http://localhost:${getPortByFramework(framework)})` |

**Precondition (new).** A `POST` is permitted only after a **single** `GET` of the same resource
returns 200. The `GET` is a probe, not a poll. The `POST` retries on `5xx` only.

**A 403 is terminal, not transient.** Measured: after an interrupted starter import the endpoint
returned 403 on 193 consecutive attempts over ~7 minutes. Retrying or polling cannot succeed,
because the site's permission rows were never written. On a clean boot the same call returns 200 at
~46s — earlier than `/dotmgt/readyz` — so there is no window to wait out either.

**Failure semantics (new).** `Err` from this call sets `uveConfigured = false` and is non-fatal.
The message depends on *why*:

| Status | Meaning | What the CLI says |
|---|---|---|
| `403` | permissions missing from an interrupted first boot — unrecoverable | Recreate the instance: `docker compose down -v && docker compose up -d --wait`. **Do not** offer manual UVE steps; they fail identically. |
| `5xx` | genuinely transient | Retry; on exhaustion, warn and continue |
| other | unexpected | Warn with the [headless UVE guide](https://dev.dotcms.com/docs/author/pages-and-visual-editing/universal-visual-editor/uve-headless-config), `host`, `siteId`, and app key `dotema-config-v2` |

In every case scaffolding continues and the run exits 0.

---

## Relationships

```
ComposeTopology ──guarantees──▶ dotCMS reachable & data-plane settled
                                          │
                                          ▼
                                     RunState.token, .siteId
                                          │
                          ┌───────────────┴───────────────┐
                          ▼                               ▼
                   UVEAppConfig (optional)          .env + printed state (mandatory)
```

The arrow that does not exist today is the mandatory one: `RunState` reaching output regardless of
what happens to `UVEAppConfig`.

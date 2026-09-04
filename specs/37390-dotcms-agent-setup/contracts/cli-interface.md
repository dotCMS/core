# Contract: `dotcms` CLI interface

The tool's public surface. Anything here is observable by a user or a script and cannot change without a breaking-change note. Per-editor file formats are in [target-configs.md](./target-configs.md).

---

## Command

```
npx dotcms agent setup [options]
```

`agent` is a sub-command group with exactly one member in this release. The group exists as the seam for `create-app` and the dotCLI port to fold into later (FR-002). `status` and `remove` are deliberately not shipped.

## Options

| Option | Argument | Default | Notes |
|---|---|---|---|
| `--url` | URL | `DOTCMS_URL`, else prompt (`http://localhost:8082`) | **Required input.** Normalized (trailing slashes stripped) and validated. |
| `--user` | string | prompt | Half of the username/password auth mode. |
| `--password` | string | `DOTCMS_PASSWORD`, else masked prompt | ⚠︎ Visible in the process list and shell history — the help text must say so and point at the env var. |
| `--authToken` | string | `DOTCMS_AUTH_TOKEN`, else prompt | The other auth mode. **Mutually exclusive** with `--user`/`--password`. |
| `--agent` | target id, repeatable | every detected target | Unknown id ⇒ usage error listing valid ids. |
| `-g, --global` | flag | off (folder scope) | Selects the user-account scope. |
| `--skip-mcp` | flag | off | Skip writing configuration. Implies `--skip-verify`. |
| `--skip-skills` | flag | off | Skip skills installation. |
| `--skip-verify` | flag | off | Skip the connection check. For offline/air-gapped use only (FR-024b). |
| `-y, --yes` | flag | off | **Confirmations only.** Never suppresses a prompt for a missing required input, never changes which inputs are required (FR-003l). On the `.gitignore` offer it takes the *safe* answer — exclude (FR-023). |
| `--force` | flag | off | Replace an existing `dotcms` entry without asking. Cannot disable token verification (FR-008c). |

**Required inputs:** `--url` plus exactly one auth mode. Supply both and the run completes with no prompts, whether or not a terminal is attached (FR-003i).

## Environment variables

| Variable | Read for | Precedence |
|---|---|---|
| `DOTCMS_URL` | instance address | after `--url`, before prompt |
| `DOTCMS_PASSWORD` | password | after `--password`, before prompt |
| `DOTCMS_AUTH_TOKEN` | auth token | after `--authToken`, before prompt |
| `CODEX_HOME` | Codex config location | honoured when set |

All environment reads are confined to `src/shared/env.ts`. Env vars are the **documented preferred** way to supply a secret, because unlike options they do not appear in the process list (FR-003f).

## Exit codes

| Code | Meaning |
|---|---|
| `0` | Every selected target configured, and — unless skipped — the server confirmed to respond. |
| `1` | At least one target failed, **or** the connection check failed. Configurations already written are kept (FR-020d, FR-024d). |
| `2` | Usage error — conflicting auth modes, unknown target id, missing required input with no terminal to prompt on. Nothing minted, nothing written. |

Skills-installation failure alone never changes the exit code (FR-026).

## Ordering guarantee

Load-bearing, and directly tested:

```
1. resolve URL          → 2. reachability check    → 3. resolve auth mode
4. mint token (if needed)  → 5. VERIFY TOKEN
   ── nothing has touched the filesystem up to this point (FR-008a) ──
6. write configs (per target, continue on failure)
7. install skills (non-fatal)
8. confirm the server responds
9. summary
```

A failure at step 5 leaves **no file, no directory, and no skills install** (FR-008a). Steps 6 and 8 fail forward: what succeeded stays, and the exit code reports the shortfall.

## Output contract

Because there is no verbose, debug, or log-file mode (FR-032a), stdout **is** the entire diagnostic surface. Consequently:

- Every error names the file, address, or target involved **and** what to do about it. A message that only reports that something failed is a defect by definition (FR-032a).
- An unhandled internal error is never surfaced (FR-032).
- The summary lists per target: target, scope, file, result, and reason on failure (FR-020b).
- The summary states the connection-check result explicitly, and must not report the run as ready when it did not succeed (FR-024e).
- A target whose skills location is unconfirmed is reported as such, never as installed (FR-027).
- Where file permissions could not be applied (Windows — research R5), the summary says so rather than implying protection.
- Tokens appear only as first 6 + last 4. Passwords never appear at all (FR-022, FR-022a).

## Endpoints consumed

| Purpose | Request | Contract |
|---|---|---|
| Reachability | `GET {url}/api/v1/appconfiguration` | Used instead of `/probes/alive`, whose IP ACLs fail from outside the container (see `create-app/src/constants/index.ts` and issue #34509). |
| CMS compatibility | same response | Compare CMS version to the build-injected CLI version; **warn fail-open**, never block (FR-005a, ADR-0019). |
| Mint token | `POST {url}/api/v1/authentication/api-token` — `{ user, password, expirationDays: '365', label: … }` → `entity.token` | `expirationDays` is a **string**. Shape follows `create-app`'s `DotCMSApi.getAuthToken`. 3 attempts on rejection. |
| Verify token | `GET {url}/api/v1/users/current` with `Authorization: Bearer <token>` | Applies to every token source, minted or supplied (FR-008). |

## Delegated sub-process

```
npx -y skills add dotCMS/agent-toolkit -a <id> [-a <id> …] [-g] -y
```

One invocation for all selected targets. Failure is non-fatal: the summary reports it and prints this exact command for the developer to run (FR-026). No secret is passed — the toolkit repository is public.

# Data Model: Action Version Manifest

The only durable data structure this change introduces is the **manifest** that
`.github/scripts/check-action-versions.sh` enforces. It is the single source of truth for "which
major is acceptable", and it is what makes the drift guard auditable instead of a code-review
convention.

Verified against the GitHub API on **2026-08-24**, re-resolved **2026-08-27**.

## Entity: `ActionFloor`

One record per action the guard governs.

| Field | Type | Required | Description |
|---|---|---|---|
| `action` | string | yes | Fully-qualified action reference without version, e.g. `actions/checkout`. Sub-path actions are distinct records (`actions/cache`, `actions/cache/restore`, `actions/cache/save`). |
| `min_major` | integer | yes | Lowest acceptable major. A `@vN` pin with `N < min_major` is a violation. Newer majors pass — the guard is a **floor**, not an equality check, so upstream releases don't create false failures. |
| `pinned_version` | string | yes | Exact version used when a site is SHA-pinned, e.g. `v7.0.1`. Also the value written into the `# vX.Y.Z` trailing comment. |
| `pinned_sha` | string (40 hex) | yes | Commit SHA for `pinned_version`. Only SHA-pinned sites use it. |
| `required_inputs` | list of string | no | Inputs that must be present **explicitly** at every call site of this action, regardless of upstream default. Currently only `download-artifact` → `["digest-mismatch"]`. |

### Validation rules

1. **Floor** — every `uses: <action>@vN` in `.github/**/*.{yml,yaml}` satisfies `N >= min_major`.
   Non-numeric tags (`@v2-beta`, `@master`) are violations by definition.
2. **SHA/comment agreement** — every `uses: <action>@<40-hex>` must have `<40-hex> == pinned_sha`
   **and** a trailing `# <pinned_version>` comment. Both halves matter: a correct SHA with a stale
   comment is worse than no comment, and there is no other tooling in the repo that catches it.
   Two such mismatches exist on `main` today (`issue_autodoc.yml:100,103` labels a checkout SHA
   `# v4.2.2` while `dotbot-review.yml:45` / `dotbot-act.yml:40` label the **same** SHA `# v4.2.0`).
3. **Explicit required inputs** — for each `required_inputs` entry, the input must appear in the
   step's `with:` block. This exists so `digest-mismatch` can never silently change again under us.

### State transitions

None. The manifest is declarative config, read at each guard invocation. Raising a `min_major`
(or rotating a `pinned_sha`) is an ordinary edit whose effect is that the guard starts failing until
the sweep catches up — which is exactly the Red→Green cycle this change is built around.

## Instance data (the manifest as of this change)

### In-scope: the 8 actions named by #36850

| `action` | `min_major` | `pinned_version` | `pinned_sha` | refs |
|---|---|---|---|---|
| `actions/checkout` | 7 | `v7.0.1` | `3d3c42e5aac5ba805825da76410c181273ba90b1` | 62 |
| `actions/download-artifact` | 8 | `v8.0.1` | `3e5f45b2cfb9172054b4087a40e8e0b5a5461e7c` | 14 |
| `actions/cache/restore` | 6 | `v6.1.0` | `55cc8345863c7cc4c66a329aec7e433d2d1c52a9` | 13 |
| `actions/upload-artifact` | 7 | `v7.0.1` | `043fb46d1a93c77aae656e7c1c64a875d1fc6a0a` | 11 |
| `actions/cache/save` | 6 | `v6.1.0` | `55cc8345863c7cc4c66a329aec7e433d2d1c52a9` | 8 |
| `actions/setup-node` | 7 | `v7.0.0` | `820762786026740c76f36085b0efc47a31fe5020` | 8 |
| `actions/cache` | 6 | `v6.1.0` | `55cc8345863c7cc4c66a329aec7e433d2d1c52a9` | 6 |
| `pnpm/action-setup` | 6 | `v6.0.10` | `0977fd99725f1db4007ccb2928dbb4e90d06cc86` | 2 |

`required_inputs` for `actions/download-artifact`: `["digest-mismatch"]`.

The three `actions/cache*` records share one `pinned_sha` because `restore` and `save` are
sub-paths of the same repository — the guard must still treat them as distinct records, since their
input contracts differ.

### Also in scope: the 4 third-party actions on the PR pipeline

Included because the runner emits its deprecation annotation **per job**, so leaving these on
`node20` would keep AC-001 failing on `initialize`, `label-pr` and `test`.

| `action` | `min_major` | `pinned_version` | `pinned_sha` | refs |
|---|---|---|---|---|
| `actions/github-script` | 8 | `v8` | `ed597411d8f924073f98dfc5c65a23a2325f34cd` | 14 |
| `docker/login-action` | 4 | `v4.6.0` | `dbcb813823bdd20940b903addbd779551569679f` | 12 |
| `dawidd6/action-download-artifact` | 24 | `v24` | `d63b86af1b34672e53c440b1b83979861906bad7` | 3 |
| `dorny/paths-filter` | 4 | `v4.0.3` | `ceb8a2b8f2d89434be7ff52d3de7ec3738c5cc9d` | 1 |

**Totals** (measured 2026-08-27 by the guard itself, `uses:` lines in `.yml`/`.yaml` only):

| Scope | References | Files |
|---|---|---|
| The 8 actions named by #36850 | 124 | 51 |
| The 4 third-party PR-path actions | 30 | +6 |
| **Whole manifest** | **154** | **57** |

The guard reports **168 violations**, not 154, because `actions/download-artifact` trips two
assertions at every one of its 14 sites: A1 (v4 below floor 8) *and* A3 (no explicit
`digest-mismatch`). 154 + 14 = 168.

The 8 documentation references across 7 `README.md` files are governed separately (the contract's
`--include-docs` mode) and are not in these totals. All counts are descriptive and **no acceptance
criterion depends on them** — per AC-007 the criterion is non-zero → zero, precisely because they
drift as `.github/` changes on `main`.

`actions/github-script` is deliberately floored at **8, not 9**: v8 is a pure node24 bump, while v9
moves Octokit v5 → v7 under 14 inline scripts. See research R4 — including a correction to the
widely-repeated claim that v9 breaks `require()`.

## Out of manifest (deferred to [#37194](https://github.com/dotCMS/core/issues/37194))

Deliberately **not** governed by this manifest yet, so the guard stays green after this change:
the 4 first-party actions on `runs.using: 'node16'` (they need `dist/` rebuilds, not pin edits), and
the cold-path third-party actions (`docker/setup-buildx-action`, `docker/build-push-action`,
`actions/setup-python` — still node16, `aws-actions/configure-aws-credentials` — still node12, etc.).
Adding them to the manifest is the natural first commit of that follow-up: the guard turns red, then
the sweep turns it green.

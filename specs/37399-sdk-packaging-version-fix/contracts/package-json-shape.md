# Contract: `package.json` shape after this fix

The "interface" this fix changes is the shape of published `package.json` manifests and example
pins — what every `npm install` reads. This is the durable contract other devs/tools should be
able to check the built result against.

## SDK libraries — `core-web/libs/sdk/*/package.json` (source, pre-publish)

| Package | `dependencies` (`@dotcms/*`) | `peerDependencies` (`@dotcms/*`) | `devDependencies` (`@dotcms/*`) |
|---|---|---|---|
| `analytics` | — (moved out) | `@dotcms/uve: "0.0.0"` | `@dotcms/types: "latest"` (unchanged) |
| `angular` | — (moved out) | `@dotcms/client: "0.0.0"`, `@dotcms/uve: "0.0.0"` | `@dotcms/types: "latest"` (unchanged) |
| `client` | — | — | `@dotcms/types: "latest"` (unchanged) |
| `experiments` | — | `@dotcms/client: "0.0.0"`, `@dotcms/react: "0.0.0"`, `@dotcms/uve: "0.0.0"`, `@dotcms/types: "0.0.0"` | — |
| `react` | — (moved out) | `@dotcms/client: "0.0.0"`, `@dotcms/uve: "0.0.0"` | `@dotcms/types: "latest"` (unchanged) |
| `uve` | — | — | `@dotcms/types: "latest"` (unchanged) |
| `vue` | — (moved out) | `@dotcms/client: "0.0.0"`, `@dotcms/uve: "0.0.0"` | `@dotcms/types: "latest"` (unchanged) |

Non-`@dotcms/*` dependencies (`@tinymce/*`, `react`/`react-dom`, `@angular/*`, `rxjs`, etc.) are
untouched by this fix.

`core-web/libs/sdk/` holds **eleven** directories, not the seven listed above. The other four —
`ai`, `cli`, `create-app`, `types` — declare **no `@dotcms/*` entry in any dependency field**, so
this table has nothing to say about them and the fix had nothing to change in them. They are not
exempt from the contract: the CI validator and the release pipeline's rewrite loop both walk all
eleven, and the moment one of them takes a sibling dependency the rows above apply to it too.

One of those four matters for the release pipeline in particular: **`cli` publishes as the
unscoped `dotcms`**, not `@dotcms/cli`. Any code that resolves a sibling by name must read the
sibling's own `.name` field rather than assuming an `@dotcms/` prefix — see the comments in
`deploy-javascript-sdk/action.yml`.

## SDK libraries — published tarball (post-publish, at release version `X`)

Every `@dotcms/*` entry in `dependencies`, `peerDependencies`, **and `devDependencies`** (all
three, once the rewrite loop is extended per research.md §3) is rewritten to the exact normalized
release version `X` (e.g. `26.8.3-1`, never `26.08.03-01` or `latest`). The package's own
`version` field is also `X`. A strict (non-loose) semver parse of every one of these strings must
succeed.

## Example apps — `examples/*/package.json`

| Location | `@dotcms/*` pin | Notes |
|---|---|---|
| `examples/nextjs` on `main` | `"latest"` | changed from `"next"` |
| `examples/vuejs` on `main` | `"latest"` | changed from `"next"` |
| `examples/angular` on `main` | `"latest"` | unchanged (already correct) |
| `examples/astro` on `main` | `"latest"` | unchanged (already correct) |
| `examples/nextjs` on `release-25.07.10_lts_v12` | `"1.2.0"` | changed from `"latest"`; verified via static GraphQL schema analysis (PR #37475) |
| `examples/nextjs` on `release-25.07.10_lts_v16` | `"1.2.0"` | changed from `"latest"`; verified via static GraphQL schema analysis (PR #37476) |

### How the pin is resolved at branch-cut time

The decision lives in **`.github/scripts/resolve-sdk-pin`** — a dependency-free, unit-tested
CommonJS module — not inline in the workflow. `cicd_comp_release-prepare-phase.yml` invokes it
with a bare `node` (no install, no build, nothing in the release path that a registry hiccup
can break) and acts on the JSON it prints.

It is a module rather than shell because as inline shell it could only ever be exercised by
cutting a real release, and three separate defects shipped or nearly shipped that way: an
existence check that aborted every normal release, an npm-`latest` lookup that handed a
back-dated LTS line an SDK newer than its own server, and an LTS-shaped string that is not
valid semver. Each is now a named test in `resolve.test.js` under `describe('regressions')`.

It decides the pin from **what the branch is being cut from**, never by parsing the release
version string:

| Situation | Signal | Pin |
|---|---|---|
| Normal release off `main` | examples float on `"latest"`, `is_lts=false` | the version being released — this release publishes its own SDK in date lockstep (ADR-0019) |
| **New** LTS line | examples float on `"latest"`, `is_lts=true` | the newest `@dotcms/client` published **on or before** the branch's code date |
| LTS **patch** off an LTS lineage | examples already carry an exact version | untouched — the line stays frozen where it was |

The LTS row exists because **`cicd_release-sdk.yml` skips the SDK publish for LTS releases**
(`IS_LTS != 'true'`, an open policy question per its own comment). There is therefore no SDK
published at an LTS release's own version, and `26.09.15_lts_v1` is not valid semver in any
case.

The pin is resolved **by date**, never from npm's `latest`. `latest` is only correct for a
line cut from `main` at that moment; a line cut from an older commit would get an SDK months
ahead of its own server — the Freshdesk #38677 drift, reintroduced by us. The resolution
therefore takes the newest `@dotcms/client` published **on or before** the branch's code
date, so it can only ever be at or behind the server, never ahead.

"On or before" rather than "nearest": for a line dated `26.08.09` the nearest published
version is `26.8.10-1`, one day *after* — ahead of the server. The cutoff is the earlier of
the branch commit's own date and the date encoded in the LTS version; they coincide when a
line is named after the release it is cut from, and taking the earlier means a mis-named line
errs toward an older SDK.

Candidates are restricted to the date-lockstep stable form (`26.8.7-1`), which excludes the
`-next.<run>` prereleases, the `0.0.1-alpha`/`beta` line, and the pre-lockstep `1.x` scheme.
A line whose date predates lockstep publishing resolves to nothing and **fails the release**
with a message pointing at the manual-pin precedent (PRs #37475/#37476) — it never guesses.

Worked example: an LTS dated `26.08.08` resolves to `26.8.7-1` (published 2026-08-07), not
`26.9.9-1`.

Deriving the pin from the release **name** (`26.09.15_lts_v1` → `26.9.15-1`) was considered
and rejected: 13 dates in 2026 carry 2–4 releases (`26.08.19` has four), so assuming counter
`-01` silently pins a real but **wrong** published build — one that installs cleanly and is
wrong in production. `git describe` cannot disambiguate it either, because release tags point
at each release branch's own commit and are never ancestors of `main`.

Two guards run before anything is written, and both fail the release rather than ship a bad
pin:

1. **Shape** — the value must be an exact version. Catches an LTS-shaped string, a range or a
   dist-tag, all of which the CI guardrail below would reject on the branch anyway.
2. **Existence** — every `@dotcms/*` package the examples reference is confirmed published at
   that version on the npm registry. Siblings publish in lockstep, but an individual publish
   can fail, and pinning to a version a package never got is the same broken install.
   This check applies **only to a pin that should already exist** — i.e. the LTS path. A
   normal release pins the version this very pipeline is about to publish: `Create GitHub
   Release` is what triggers `cicd_release-sdk.yml`, so at pin time that version is
   legitimately absent from npm and checking it would fail every release.

Separately, a patch on an existing LTS line must be dispatched with an explicit
`release_commit`; without one it would be cut from `main` and ship main's code under an LTS
version. `set-version` refuses when a tag for the line already exists.

## CI guardrail (the enforcement of this contract)

A new check in `cicd_1-pr.yml` fails the build if any file above is edited to violate this table
— specifically: a floating (`latest`/`next`/`*`) `peerDependencies` entry in any SDK lib; a
reintroduced `@dotcms/client`/`@dotcms/uve` in `dependencies` of `react`/`angular`/`vue`/`analytics`;
or an example pin that is not an exact version on any branch other than `main`/`master`
(`latest`, `next`, `*` and ranges such as `^1.2.0`/`~1.2.0`/`1.2.x` all fail there — a range
drifts forward past the server the release branch was cut for exactly as `latest` does).

`main` and `master` are both treated as trunk because `cicd_1-pr.yml` accepts PRs targeting
either. The same check also runs on `cicd_5-lts.yml` (`push:` to `release-*`), where the branch
context is the pushed ref rather than a PR base.

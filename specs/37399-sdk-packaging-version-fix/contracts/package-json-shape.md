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

## CI guardrail (the enforcement of this contract)

A new check in `cicd_1-pr.yml` fails the build if any file above is edited to violate this table
— specifically: a floating (`latest`/`next`/`*`) `peerDependencies` entry in any SDK lib; a
reintroduced `@dotcms/client`/`@dotcms/uve` in `dependencies` of `react`/`angular`/`vue`/`analytics`;
or a floating example pin anywhere except `"latest"` on `main`.

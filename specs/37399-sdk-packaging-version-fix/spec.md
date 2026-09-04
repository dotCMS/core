# Issue Resolution Specification: SDK packaging — malformed published version strings and floating/orphaned dist-tags in SDK sources and example apps

**Feature Branch**: `37399-sdk-packaging-version-fix`

**Created**: 2026-09-04

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#36891](https://github.com/dotCMS/core/issues/36891)

**Input**: User description: "SDK packaging malformed version strings and floating dist-tags — see agreed scope below"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

The `@dotcms/*` npm SDK packaging/release mechanism (introduced a few weeks ago) has two related classes of defect that together make the SDK's published packages, and the example apps that scaffold customer projects, unreliable:

1. **Published packages report a version that does not exist.** The version string written into a published package's `package.json` (and into the version ranges other `@dotcms/*` packages depend on) is not valid semver and does not match what the npm registry advertises for that same package.
2. **Floating version specifiers leak into what customers install.** SDK library source files and example-app scaffolds pin `@dotcms/*` dependencies to `"latest"` or `"next"` (an internal dev/QA tag) instead of an exact, compatible version. On two package managers (yarn, pnpm) this silently overrides a customer's own explicit version pin, and on both active LTS branches it makes the officially-documented example scaffold install a version far ahead of the LTS server it's meant to run against.

**Severity / Impact**: High. This has already caused a live, escalated customer support ticket (Freshdesk #38677, against a 25.07.10 LTS server) and three prior tickets of the same class (Freshdesk #36678, #37710, #38038). Affected customers are anyone who: (a) inspects an installed `@dotcms/*` package's reported version (SBOM/license scanners, vulnerability tooling, bundler metadata), (b) installs the SDK with yarn or pnpm alongside an explicit version pin, or (c) scaffolds one of the four documented example apps, especially against an LTS server. The scaffold path is the most severe because it is the officially-documented onboarding path and, per the SDK's own `DotCMSPage` GraphQL fragment being hardcoded, pinning the SDK version is the *only* customer-side mitigation available — and that mitigation is exactly what's broken.

## Reproduction *(mandatory)*

**Environment**: npm registry (`registry.npmjs.org`), `dotCMS/core` `main` branch and the two active LTS branches (`release-25.07.10_lts_v12`, `release-25.07.10_lts_v16`); reproduced with npm 11, yarn 1.22.21, pnpm 11.7.0.

**Steps to Reproduce**:

1. **Malformed version (Defect A)**: `curl -s https://registry.npmjs.org/@dotcms/client/<version> | jq -r .version` returns a well-formed version (e.g. `26.8.3-1`); downloading and extracting that same tarball and reading its bundled `package.json` returns a different, invalid-semver string with leading zeros (e.g. `26.08.03-01`) — and `@dotcms/react`'s own dependency on `@dotcms/client` is pinned to that same malformed string, which is not a version that exists in the registry's `versions` map.
2. **Silent pin override (Defect B1, yarn/pnpm only)**: create a manifest pinning `@dotcms/client`, `@dotcms/react`, `@dotcms/uve`, `@dotcms/types` all at an explicit version (e.g. `1.2.0`); `yarn install` produces a second, nested copy of `@dotcms/client` at a different (floating-resolved) version under `node_modules/@dotcms/react/node_modules/`, and `pnpm install` symlinks `@dotcms/react`'s dependency to the floating-resolved version instead of the pin. `npm install` is unaffected (dedupes correctly).
3. **Orphaned/floating example scaffold (Defect B2/B3)**: `npx create-next-app my-app --example https://github.com/dotCMS/core/tree/main/examples/nextjs`, then `npm ls @dotcms/client` inside the scaffolded app — installs whatever `next` currently resolves to, an internal dev/QA pre-release never meant for customers. Repeating this against `examples/nextjs` on `release-25.07.10_lts_v12` or `_v16` installs `"latest"`, which resolves to the current SDK release (months ahead of that LTS server), and the scaffolded page fails at runtime because its hardcoded GraphQL fragment requests fields (`numberContents`, `styleEditorSchemas`, `lockedBy`, `lockedByName`, layout `metadata`) that don't exist on the older LTS schema.

**Expected Behavior**: An installed `@dotcms/*` package reports the exact version it was published as, matching registry metadata and dependency pins byte-for-byte. A customer's explicit version pin is honored identically on npm, yarn, and pnpm. A scaffolded example app installs a published, supported release compatible with the branch/server it came from — `main`'s examples install a current stable release, and each LTS branch's examples install a version compatible with that LTS server.

**Actual Behavior**: The tarball-internal version and inter-package dependency ranges contain an invalid, non-existent semver string; a customer's explicit pin is silently overridden on yarn and pnpm; scaffolded examples install an internal pre-release tag (`main`) or a version far too new for the server (LTS branches), producing runtime GraphQL schema-mismatch errors with no way to trim the query as a workaround.

**Reproducibility**: Always, for every package currently published under the new release mechanism (confirmed live as of 2026-09-04: `@dotcms/client` `latest` = `26.9.3-1`, `next` = `26.9.3-1-next.2632`) and for every example app on `main` and on both active LTS branches, as verified directly against the registry and against `origin/main`/`origin/release-25.07.10_lts_v12`/`origin/release-25.07.10_lts_v16` in this repo.

## Scope of Investigation *(mandatory)*

- **Affected area**: SDK release/packaging pipeline (npm publish automation) and the example apps used for customer onboarding/scaffolding. Not the SDK's runtime application logic (data fetching, rendering, page building), except for one downstream verification noted below.
- **Suspected surface**: Entirely CI/CD + repo configuration, not `com.dotcms.*`/`com.dotmarketing.*` Java code:
  - `.github/actions/core-cicd/deployment/deploy-javascript-sdk/action.yml` — the composite action that writes the release version into every SDK package's `package.json` and publishes to npm; used by both `cicd_release-sdk.yml` (real releases, `latest` tag) and `cicd_3-trunk.yml` (`next` tag on SDK-touching merges to `main`).
  - `core-web/libs/sdk/react/package.json`, `core-web/libs/sdk/angular/package.json`, `core-web/libs/sdk/experiments/package.json` — hardcode floating `"@dotcms/*": "latest"` specifiers in source.
  - `core-web/bump-sdk-versions.js` — confirmed dead code (no workflow references it) that only ever rewrote `peerDependencies`, never `dependencies`.
  - `examples/nextjs/package.json`, `examples/vuejs/package.json` (pin `"next"`), `examples/angular/package.json`, `examples/astro/package.json` (pin `"latest"`) on `main`.
  - `examples/nextjs/package.json` on `release-25.07.10_lts_v12` and `release-25.07.10_lts_v16` (pins `"latest"`).
  - No existing CI check guards against a floating `@dotcms/*` specifier reappearing in any of the above.
- **Related known decisions**: ADR-0019 (SDK/CMS date-lockstep versioning), referenced directly in `cicd_release-sdk.yml`'s header comments, governs the CalVer (`yy.mm.dd-##`) release-version format that Defect A's malformed strings originate from. The plan phase must consult `dotCMS/platform-adrs` for ADR-0019 and any related ADR before finalizing the fix, since the normalization must preserve the date-lockstep invariant while producing valid semver.

## Root-Cause Hypothesis

**Defect A**: The release pipeline's version string (e.g. `26.08.03-01`, zero-padded per the `yy.mm.dd-##` CalVer format mandated by ADR-0019) is written verbatim into every SDK package's `version` field and into inter-package `@dotcms/*` dependency/`peerDependencies` ranges via `jq`, with no semver normalization step. npm normalizes leading zeros when it records registry *metadata*, but not the tarball-packed `package.json` contents nor the dependency range strings — producing the observed mismatch. Confirmed by direct code inspection of `deploy-javascript-sdk/action.yml`'s "Update package.json versions" step.

**Defect B1**: `deploy-javascript-sdk/action.yml` does correctly rewrite `dependencies`/`peerDependencies` (not just `peerDependencies`) at publish time today — so current `latest`-tag publishes should not literally contain `"latest"`. However, the *source* `package.json` files on `main` still hardcode `"latest"`, which is masked only because the publish pipeline happens to fix it before every current release. This is real residual risk (any publish path that bypasses this specific action step — historical or future — reintroduces literal `"latest"` into a published tarball, which is exactly what shipped in `@dotcms/react`/`@dotcms/angular` `1.0.6` through `1.7.0`, before this rewrite behavior existed in its current form).

**Defect B2/B3**: Example apps' pinned specifiers are simply wrong/stale in source and are never rewritten by any part of the release pipeline — unlike the SDK libraries' own manifests, nothing updates `examples/*/package.json` at publish time on `main`, and nothing has ever back-filled the two LTS branches.

**Downstream, out-of-scope-by-design**: The server↔SDK minimum-version compatibility check (`MinSdkVersion.java`, `SdkVersionWebInterceptor.java`, `core-web/libs/sdk/client/src/lib/utils/sdk-compatibility.ts`) sources its own notion of "the SDK's version" (`SDK_VERSION`) from the same `package.json.version` field Defect A corrupts. Its comparison logic (`compareVersions`/`parseVersionSegments`, splitting on `[.-]` and using JS `Number()` per segment) was independently verified to already treat `"26.08.03-01"` and `"26.8.3-1"` as equal (`Number("08")` is `8` in JS string-to-number conversion, not octal). This mechanism therefore has no bug to fix and is automatically corrected once Defect A's normalization lands — it is named here only so the plan phase does not re-open it, and so a regression test can lock in that this remains true.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Normalize the release-version string to valid semver (strip leading zeros from each numeric segment) at a single point before it is written into any `package.json` field in `deploy-javascript-sdk/action.yml`, and use that one normalized string consistently for: the package's own `version`, every inter-package `@dotcms/*` dependency/`peerDependencies` pin, and the `npm view`/idempotency check used to detect an already-published version.
- Remove the hardcoded `"latest"` specifiers from `core-web/libs/sdk/react/package.json`, `core-web/libs/sdk/angular/package.json`, and `core-web/libs/sdk/experiments/package.json`.
- Retire `core-web/bump-sdk-versions.js` (dead code, superseded by `deploy-javascript-sdk/action.yml`'s rewrite logic).
- Pin `examples/nextjs`, `examples/vuejs`, `examples/angular`, `examples/astro` on `main` to an exact, currently-compatible published SDK version instead of `"next"`/`"latest"`.
- Backport the same class of fix (exact, LTS-compatible pin) to `examples/nextjs` on `release-25.07.10_lts_v12` and `release-25.07.10_lts_v16`.
- Add a CI check that fails the build if any `core-web/libs/sdk/*/package.json` or `examples/*/package.json` declares a floating specifier (`latest`, `next`, `*`) for an `@dotcms/*` dependency. This check only fails the build — it does not auto-fix or auto-commit.
- Add one regression test asserting `compareVersions("26.08.03-01", "26.8.3-1")` (or the equivalent post-fix normalized/unnormalized pair) is treated as equal, to protect the already-correct `sdk-compatibility.ts` behavior against future refactors.

**Explicitly out of scope / non-goals**:

- No code change to `core-web/libs/sdk/client/src/lib/utils/sdk-compatibility.ts`, `fetch-http-client.ts`, or `rollup.config.cjs` — only the one regression test noted above.
- No change to `MinSdkVersion.java` or `SdkVersionWebInterceptor.java` (Java backend) or any other `com.dotcms.*`/`com.dotmarketing.*` code.
- No change to Angular/React UI components.
- No automated, ongoing mechanism to keep example-app pins in sync with future SDK releases (e.g. a pipeline step that auto-commits new pins to `main`/LTS branches on every release). The developer explicitly chose the simpler CI-guardrail-only approach over auto-commit, given the risk of unattended commits to protected/LTS branches. Keeping example pins current going forward is a manual, per-release responsibility that the new CI check merely guards against regressing to a *floating* specifier — it does not guard against a *stale but pinned* specifier.
- No retroactive fix of already-published `1.x` npm packages (npm immutability makes this impossible); support-docs guidance for customers stuck on those versions is tracked separately per the original issue's own note (`overrides`/`resolutions`/`pnpm-workspace.yaml` guidance), not as engineering work here.
- No change to the `next` dist-tag publishing mechanism itself (`cicd_3-trunk.yml`'s `publish-sdk-next` job) — it was already restored by a prior, unrelated PR (#36722) and is confirmed live/current as of this writing; only the example apps' *pinning* of `next` is in scope.

## Regression Risk *(mandatory)*

- **Blast radius**: `deploy-javascript-sdk/action.yml` is the single publish path for **every** future dotCMS release's SDK packages (both `latest` real releases and `next` dev/QA builds off `main`). A defect introduced in the normalization step would affect every subsequent SDK publish, not just this fix's target versions. This is the highest-risk piece of the change and needs a dry-run/manual-dispatch verification (the workflow already supports `dry-run: true`) before trusting it against a real release.
- **Backward compatibility**: Already-published `1.x`/pre-fix versions cannot be changed (npm immutability) — this fix is forward-only. The normalized version string must still satisfy ADR-0019's date-lockstep invariant (one dotCMS release version ↔ one SDK version) so existing tooling that parses the release tag format continues to work; the plan phase must confirm this against ADR-0019 explicitly.
- **Data considerations**: None (no persisted application data; this is packaging/config only).

## Acceptance & Verification *(mandatory)*

- **AC-001**: For a newly published `@dotcms/*` package, the tarball-internal `package.json` `version` field is valid semver and matches the npm registry metadata `version` exactly (no leading zeros, no divergence).
- **AC-002**: Injected `@dotcms/*` inter-package dependency/`peerDependencies` pins use that same exact, normalized version string; a strict (non-loose) semver parse of every published `@dotcms/*` version and internal dependency range succeeds.
- **AC-003**: No `core-web/libs/sdk/*/package.json` declares a floating specifier (`latest`, `next`, `*`) for an `@dotcms/*` entry in `dependencies` or `peerDependencies`.
- **AC-004**: `examples/nextjs`, `examples/vuejs`, `examples/angular`, `examples/astro` on `main` pin exact published versions; scaffolding each, installing, and running `npm ls @dotcms/client` yields exactly one copy at the expected published release.
- **AC-005**: `examples/nextjs` on `release-25.07.10_lts_v12` and `release-25.07.10_lts_v16` pins a version verified compatible with that LTS server's GraphQL schema; scaffolding and rendering a page against a 25.07.10 LTS server produces no `FieldUndefined` errors.
- **AC-006**: Installing a pinned `@dotcms/react` under npm, yarn, and pnpm each yields exactly one `@dotcms/client` at the pinned version (no silent override).
- **AC-007**: A CI run that reintroduces a floating `@dotcms/*` specifier into any `core-web/libs/sdk/*/package.json` or `examples/*/package.json` fails the build.
- **AC-008**: `compareVersions()` in `sdk-compatibility.ts` continues to treat a zero-padded and a normalized form of the same version as equal (regression test, not new behavior).
- **Verification method**: A dry-run (`workflow_dispatch` with `dry-run: true`) of the updated `cicd_release-sdk.yml`/`deploy-javascript-sdk` action against a scratch/test scope to confirm the written `package.json` contents before any real publish is risked; a new Jest unit test for the version-normalization logic and for `compareVersions()` (AC-008); manual scaffold-and-install verification per AC-004/AC-005/AC-006 across npm/yarn/pnpm; a new CI job/script test for AC-007 (e.g. run the guardrail check against a fixture `package.json` that intentionally contains a floating specifier and confirm it fails).

## Assumptions

- The LTS-compatible pin for `release-25.07.10_lts_v12`/`_v16`'s `examples/nextjs` is assumed to be `1.2.0` (the newest release the original issue's author identified as validating against that LTS server's GraphQL schema). **Resolution (developer decision)**: this has not been independently re-verified against a live 25.07.10 LTS server in this investigation, and will not be taken as given — `/speckit-plan`/`/speckit-implement` MUST confirm `1.2.0` (or identify the correct version) by actually scaffolding `examples/nextjs` and rendering a page against a real 25.07.10 LTS server before it is committed as the LTS example's pin. This confirmation step is now folded into AC-005's verification method, not left as an open question blocking this spec.
- The exact version to pin for the four `main` examples is assumed to be "the current stable `latest` release at the time the fix is implemented" (a moving target by design) rather than a version hardcoded in this spec — the plan/implementation should pin whatever is current `latest` at merge time.
- No separate GitHub issue/spec is being opened for the "keep example pins from going stale over time" concern; it is accepted as a manual, per-release process step going forward, guarded only against the floating-specifier regression (see Non-Goals).

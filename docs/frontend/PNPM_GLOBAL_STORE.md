# pnpm's Global Virtual Store (for git worktrees)

**Optional, per-developer.** The repository does not enable this. Nothing in CI uses it except one
nightly detector. If you keep a single checkout, you gain nothing — skip this page.

If you keep several worktrees of `core-web` open at once, each one materialises its own
**~2.1 GB** `node_modules`. With pnpm's global virtual store, each becomes **~2.2 MB** of symlinks
into one shared content-addressable store, and a fresh install drops from ~15 s to ~4.6 s.

Five worktrees: ~10.5 GB → a few megabytes each, plus a store your machine already carries for
every other pnpm project.

---

## Read this before you enable it

### It is machine-wide, not per-project

`pnpm config set --global` writes to your user-level pnpm config. It applies the layout to **every
pnpm project on your machine**, not just this repository. There is no per-project opt-in: from
pnpm 11 on, `.npmrc` is read for registry and auth only, and putting the key in the repository's
`pnpm-workspace.yaml` would turn it on for everyone including CI — which is deliberately not what
we do.

### Do not use it on a shared machine

One writable store shared between mutually untrusted users or agents is a security problem, and
[pnpm's own guidance](https://pnpm.io/git-worktrees) says so directly:

> This setup assumes the worktrees and agents share the same trust boundary. Do not use one
> writable pnpm store for mutually untrusted agents or users.

A single-user developer laptop satisfies that boundary. An ephemeral single-tenant CI runner
satisfies it. A **shared or multi-tenant build box does not — do not enable it there.**

### What is verified, and what is not

Third-party packages that import modules they never declare break under this layout (see
[Why it breaks](#why-it-breaks)). The repository carries corrections for the ones we found, across
three module graphs: the production bundle, the Vitest suite, and the Stencil webcomponents build.

Coverage is **bounded to what a static scan and those three builds can see**. An import reached
only through a computed or dynamic specifier is invisible to both and may still surface. The
nightly detector is what catches those; you may be the one who finds one first.

---

## Enable it

```bash
pnpm config set --global virtualStoreType global

# Then, in each worktree — the existing tree does not convert in place:
cd <worktree>/core-web
rm -rf node_modules
pnpm install --frozen-lockfile

du -sh node_modules   # expect megabytes
```

Check the current state at any time with `pnpm config get virtualStoreType` (`undefined` means you
never opted in) and find the store with `pnpm store path`.

## Disable it

```bash
pnpm config delete --global virtualStoreType
cd <worktree>/core-web && rm -rf node_modules && pnpm install --frozen-lockfile
```

Complete and immediate. The repository default is unchanged, so nothing else needs undoing.

---

## Living with a shared store

**`pnpm store prune` affects every worktree.** The store is machine-wide, so pruning from one
worktree can delete content another worktree's symlinks still point at, breaking a checkout you did
not touch. Recover by reinstalling in the affected worktree.

**Deleting a worktree does not reclaim store space.** Only `pnpm store prune` does, with the caveat
above.

**Branches with different lockfiles coexist fine.** They resolve to different store entries.

---

## Why it breaks

Some third-party packages import modules they never declare in their own manifest. With the default
local store they resolve by accident of directory geometry: Node's module walk-up escapes the
isolated package directory, passes through the hoisted fallback, and reaches `core-web`'s own
`node_modules`, where the module happens to be a direct dependency.

With the global store the package physically lives outside the repository, so walking up leaves the
project and never reaches `core-web`. Nothing rescues the undeclared import.

The fix is to declare what those packages actually import, in `core-web/pnpm-workspace.yaml` under
`packageExtensions` — always as a **`peerDependency`**, never a `dependency`. A dependency would
let pnpm resolve a second copy, and `yjs` throws when two instances load while Angular quietly ends
up with two injector registries. A peer resolves from the consumer, so the package links the single
instance `core-web` already installs.

Each entry is keyed by exact version on purpose: when a package moves past it, the correction stops
applying and the nightly detector reports the failure.

> `preserveSymlinks` is not a fix. It makes resolution fail the opposite way, on transitive
> dependencies. Both directions fail, for opposite reasons — this is recorded in
> [#37573](https://github.com/dotCMS/core/issues/37573); please do not re-test it.

---

## If a build breaks under the global store

1. Confirm it is this and not your change: `pnpm config delete --global virtualStoreType`, reinstall,
   and build again. If it passes, it is a missing correction.
2. Find what is undeclared:
   ```bash
   cd core-web
   pnpm exec node tools/scan-inlined-imports.mjs      # test-layer graph — small, precise
   pnpm exec node tools/scan-undeclared-imports.mjs   # whole tree — noisy cross-check
   ```
3. Add a `packageExtensions` entry following the pattern in `pnpm-workspace.yaml`: exact version
   key, `peerDependencies`, a comment naming the undeclared import and how you found it.
4. `pnpm install`, then verify nothing else moved:
   ```bash
   pnpm exec node tools/audit-lockfile-drift.mjs
   ```
5. Always verify with `--skip-nx-cache`. A cached Nx run replays recorded output and reports success
   without executing anything.

**Always run all three graphs before believing it is fixed** — a green production build says nothing
about the other two:

```bash
pnpm nx build dotcms-ui --configuration=production --skip-nx-cache
pnpm exec node tools/assert-dist-assets.mjs    # asset copies, by file count
pnpm nx run-many -t test --skip-nx-cache
```

---

## Background

- Issue [#37573](https://github.com/dotCMS/core/issues/37573) — investigation and measurements
- `specs/37573-pnpm-global-virtual-store/` — specification and design notes
- `.github/workflows/cicd_scheduled_pnpm-global-store.yml` — the nightly detector
- [pnpm: Git worktrees](https://pnpm.io/git-worktrees)

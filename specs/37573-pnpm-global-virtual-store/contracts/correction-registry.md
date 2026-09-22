# Contract: The correction registry

**Location**: `core-web/pnpm-workspace.yaml`, `packageExtensions` block
**Consumers**: pnpm at install time; the nightly detector; any maintainer doing a dependency bump

`packageExtensions` is the repository's standing answer to a third-party package that imports what it
does not declare. This file fixes the shape so entries stay reviewable and removable.

---

## Entry shape

```yaml
packageExtensions:
    # y-protocols imports `yjs` but declares only `lib0` and has no peers at all.
    # Peer, not dependency: yjs throws if two instances load. discoveredBy: build (#37573)
    y-protocols@1.0.1:
        peerDependencies:
            yjs: '^13'

    # @tinymce/tinymce-angular imports `rxjs`; declares only tinymce + tslib.
    # discoveredBy: build (#37573)
    '@tinymce/tinymce-angular@7.0.0':
        peerDependencies:
            rxjs: '^7'
```

The illustrative entries above are **not** the committed list. The real set comes from the discovery
scan, and the ranges must be taken from what `core-web` actually declares.

---

## Rules

1. **The key carries a version.** `pkg@<exact installed version>`, never a bare package name. The key
   is the correction's expiry date: when the package moves past it the entry stops applying, and the
   nightly (FR-019) reports the build failure that follows. A bare key would keep applying silently
   to versions nobody validated.
2. **`peerDependencies`, not `dependencies`.** Non-negotiable. A `dependency` lets pnpm resolve a
   second instance; `yjs`, `rxjs` and the Angular packages all break as duplicates — loudly for
   `yjs`, quietly for Angular. A peer resolves from the consumer, which is the single instance
   `core-web` installed.
3. **The added range must be satisfied by a direct dependency of `core-web`.** Verify against
   `core-web/package.json` before committing. A peer nothing satisfies changes an unresolved import
   into an unmet-peer warning — still broken, now quieter.
4. **Use `peerDependenciesMeta.<name>.optional: true`** when the import is conditional, so the
   absence of the peer is not an install-time warning for consumers who never hit that code path.
5. **Every entry carries a comment** with the undeclared specifier, why the peer is the right field,
   and how it was found (`scan` or `build`, with the issue number). This is FR-018; the YAML alone
   cannot express it.
6. **One entry per package, one peer line per undeclared import.** Removable independently as
   upstream fixes land.
7. **Removal is the goal.** An entry is debt with a stated expiry, not a permanent fixture. When
   upstream declares the dependency, delete the entry and let the nightly confirm.

---

## Finding the corrections: scan the graph, do not iterate the failures

Learned the expensive way on this feature. There are **two module graphs** and a correction list
derived from one is complete only for that one:

| Graph | Authority | How to enumerate it |
|---|---|---|
| Production bundle | `nx build dotcms-ui --skip-nx-cache` | The build names them, one failure at a time |
| Vitest suite | `nx run-many -t test` | `tools/scan-inlined-imports.mjs` — the packages in the vite configs' `test.server.deps.inline` |

The production build went green with four corrections while the entire test suite failed to load,
because `@analogjs/vitest-angular` and `@openng/spectator` carry the same defect in a graph the
bundler never enters.

Iterating on failed runs costs one full suite run per package found. Scanning the inline list — 28
packages, the set Vitest actually resolves — found all ten defects in a single pass. Prefer the
scan; let the run confirm.

`tools/scan-undeclared-imports.mjs` sweeps the whole installed tree instead and reports ~480
findings against ~10 real ones. It is a cross-check for packages behind lazily loaded routes, not a
work list.

## Guarantees this contract makes

- **To a developer who opts in**: every import a package performs resolves from inside that package's
  own isolated directory. No reliance on walking up into the project.
- **To everyone who does not opt in**: the corrections change resolution *keys* but no resolved
  version outside the corrected packages themselves. That is FR-014 and it is checked, not assumed —
  `packageExtensions` participates in resolution, so the lockfile diff will be large and mostly
  semantically empty, which is exactly the shape that hides a real change.
- **To a future maintainer**: every entry states what it compensates for and what version it was
  written against, so the question "can this go now?" has an answer without re-running the
  investigation.

---

## What this contract does **not** cover

- `overrides` — already used in this file for version pinning. A different concern: the problem here
  is an absent declaration, not a wrong version. Do not fix an undeclared import with an override.
- `patchedDependencies` — rejected in [research R-002](../research.md#r-002--how-to-correct-the-third-party-manifests).
  Patches the package's code when the defect is in its manifest, and breaks on every bump.
- The `virtualStoreType` setting itself, which by FR-006 never appears in this file.

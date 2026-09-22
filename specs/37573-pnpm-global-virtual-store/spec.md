# Feature Specification: Adopt pnpm's global virtual store for git worktrees

**Feature Branch**: `nicobytes/build-adopt-pnpm-virtualstoretype-global-for-git`

**Created**: 2026-09-18

**Status**: Draft

**Type**: New Feature (local-development build-tooling adoption, blocked by third-party manifest defects)

**Issue**: [#37573](https://github.com/dotCMS/core/issues/37573)

**Input**: User description: "https://github.com/dotCMS/core/issues/37573 — adopt pnpm `virtualStoreType: global` so git worktrees share one content-addressable store instead of each materialising a ~2.1 GB `node_modules`"

---

## Context

pnpm's git-worktrees guidance recommends a **global virtual store**: each checkout's dependency
directory holds symlinks into one shared content-addressable store on the machine rather than
materialising its own copy of every package.

Measured in `core-web` on the pnpm 12 branch ([#37563](https://github.com/dotCMS/core/pull/37563)),
same commit and same lockfile, only the setting changed:

| | global virtual store | without |
|---|---|---|
| `core-web/node_modules` | **2.2 MB** | **2.1 GB** |
| warm frozen-lockfile install | 4.3 s | 15.1 s |
| lockfile | unchanged | unchanged |
| admin UI production build | **fails** | green, 38 s |

Several developers keep 5–10 worktrees of this repository open at once, so the prize is roughly
2.1 GB **per checkout**. It is a local-development win only: a continuous-integration job is a
clean machine with a single checkout and gains nothing from sharing.

The setting was deliberately left out of the pnpm 12 migration because it breaks the build, and
the breakage cannot be fixed from this repository's own build configuration. This specification
turns that recorded investigation into a scoped piece of work.

### Why it breaks today

Several third-party packages **import modules they do not declare** in their own manifests:

- `y-protocols@1.0.1` imports `yjs`; its manifest declares only `lib0` and has no peer
  dependencies at all.
- `@tinymce/tinymce-angular@7.0.0` imports `rxjs`; its manifest declares `tinymce` and `tslib`
  and lists the Angular packages as peers.
- `@materia-ui/ngx-monaco-editor@6.0.0` imports `@angular/forms` without a reachable declaration.
- At least one further package imports `chart.js/auto`.

With the **local** virtual store these resolve by accident of directory geometry: the module
resolver walks up out of the isolated package directory, through the hoisted fallback directory,
and finally reaches the project's own top-level dependency directory, where the undeclared module
happens to exist as a direct dependency of `core-web`. That last step is the rescue.

With the **global** virtual store the package physically lives outside the repository, so walking
up leaves the project entirely and never reaches `core-web`. Nothing rescues the undeclared
import. This is inherent to placing the virtual store outside the project directory — it is not a
setting the team has failed to find.

### Dead end already explored — do not re-test

Telling the bundler to preserve symlinks makes the build fail the opposite way: resolution then
walks up the *symlinked* path into `core-web`'s top-level dependency directory, where **transitive**
dependencies correctly do not exist — that is strict isolation working as designed. So the default
breaks undeclared imports and preserving symlinks breaks transitive ones. Both directions fail, for
opposite reasons.

### What an actual fix requires

Correcting the offending packages' declared dependencies from this repository — via manifest
extensions, patches, or overrides — so that every import a package performs is satisfied from
inside its own isolated directory, with no reliance on walking up into the project. At least four
packages are implicated and the only way to find the rest is to iterate on failed builds.

---

## Clarifications

### Session 2026-09-18

- **Q: Is the global virtual store committed as the repository-wide default, or left as a
  per-developer opt-in?** → **A: Per-developer opt-in.** Only the third-party manifest corrections
  are committed; the setting itself lives in each developer's own machine-level package-manager
  configuration, exactly as the upstream git-worktrees guidance describes it. The benefit is local
  and per-machine — a developer with one worktree gains nothing — so a repository-wide default would
  impose the risk on everyone, including every pipeline job, to deliver a saving only some people
  can collect. It also keeps the blast radius of a wrong correction small: with the local store
  still the default everywhere, a mistake degrades to the behaviour that works today.
  **Affects**: FR-006, FR-002, US3, SC-002, Out of Scope.

- **Q: What protects the adoption from a future dependency bump that reintroduces an undeclared
  import?** → **A: A periodic (nightly) pipeline job that builds under the global virtual store.**
  Because the store is opt-in, the configuration is by construction the one no pull request
  exercises, so without a dedicated job the first adopter to upgrade discovers the breakage by hand.
  A per-pull-request check was rejected: it charges every change on every branch for a benefit that
  is purely local, and the recovery window a nightly leaves open — hours, on a developer-environment
  concern — costs far less than that. The trade is accepted explicitly: whoever broke it has already
  merged, so the finding needs triage rather than a block.
  **Affects**: FR-019, US4.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reclaim disk and install time across worktrees (Priority: P1)

A frontend developer keeps several worktrees of this repository open at once. Today each one
materialises its own ~2.1 GB dependency tree, so five worktrees cost over 10 GB of disk that is
almost entirely duplicated content. With the global virtual store, the developer installs
dependencies in a fresh worktree and gets a dependency directory measured in megabytes, backed by
one shared store, and a noticeably faster warm install — and the admin UI still builds.

**Why this priority**: This is the entire point of the issue. Disk reclamation and install speed
are the value; everything else in this specification exists to make them safe. Without a green
production build the setting cannot be adopted at all, so this story is the one that unblocks the
rest.

**Independent Test**: In a worktree configured with the global virtual store, run a frozen-lockfile
install followed by a cache-skipping production build of the admin UI. Measure the dependency
directory size and the install duration, and confirm the build reports no resolution errors.
Delivers the disk and time savings on its own.

**Acceptance Scenarios**:

1. **Given** a worktree with no installed dependencies and the global virtual store in effect,
   **When** the developer runs a frozen-lockfile install, **Then** the install completes without
   error, the lockfile is unchanged, and `core-web`'s dependency directory occupies megabytes
   rather than gigabytes.
2. **Given** dependencies installed against the global virtual store, **When** the developer runs a
   cache-skipping production build of the admin UI, **Then** the build succeeds with no
   "could not resolve" errors for any module.
3. **Given** a successful production build, **When** the shipped third-party asset directories are
   inspected, **Then** each contains at least its baseline file count — the rich-text editor bundle
   and the code-editor bundle are both populated, not empty.
4. **Given** two worktrees of this repository on the same machine at the same commit, **When** both
   have dependencies installed, **Then** the second install reuses the shared store and the combined
   disk cost is a small fraction of two full dependency trees.

---

### User Story 2 - Everyday development keeps working (Priority: P2)

The same developer runs the unit test suites and the dev server from a worktree using the global
virtual store, and everything behaves exactly as it did before: tests pass at the same rate, the dev
server serves every file the application requests, and no tool refuses a path because it now
resolves outside the workspace.

**Why this priority**: A green production build is necessary but not sufficient. Testing and the dev
server are where a developer spends the day, and both resolve modules differently from the production
bundler — the dev server in particular guards file-system access by real path, which the shared store
changes. Adoption that breaks the daily loop is worse than no adoption.

**Independent Test**: With the global virtual store in effect, run the full workspace test target and
start the admin UI dev server; exercise the application in a browser and confirm no forbidden-path
errors appear in the network log or the server output.

**Acceptance Scenarios**:

1. **Given** the global virtual store in effect, **When** the developer runs the full workspace test
   target, **Then** every project that passed before the change still passes — including the
   component library and the content-editing application.
2. **Given** the global virtual store in effect, **When** the developer starts the admin UI dev
   server and loads the application, **Then** every file request is served and none is rejected as a
   path outside the permitted roots.
3. **Given** the global virtual store in effect, **When** the project graph is computed across the
   whole workspace, **Then** graph inference completes without crashing — including the software
   development kit projects whose native resolver is already known to be fragile.

---

### User Story 3 - Everyone who did not opt in is unaffected (Priority: P2)

A developer who never enables the global virtual store, and every pipeline job, keep working exactly
as before. They still get the local virtual store — but they do inherit the committed corrections to
third-party manifests, and possibly a moved lockfile along with them. Nothing about their install,
build, test run or cache behaviour changes.

**Why this priority**: The opt-in decision means the setting itself carries no risk for non-adopters
— but the corrections do, because those are committed for everybody. That is the real exposure of
this change and it must be verified before merge: a correction that alters resolution for the local
store too would break every developer and every job on every branch, in exchange for a benefit none
of them asked for.

**Independent Test**: On a branch carrying the corrections but **without** the global virtual store
enabled, run a frozen-lockfile install and the frontend pipeline. Compare job outcomes,
dependency-cache hit or miss, and wall-clock duration against a recent trunk run, and diff the
installed tree against the trunk's.

**Acceptance Scenarios**:

1. **Given** a branch carrying the corrections and the default local virtual store, **When** the
   frontend pipeline runs, **Then** every job that passes on the trunk also passes.
2. **Given** that same branch, **When** a frozen-lockfile install runs, **Then** it succeeds against
   the committed lockfile, and any package whose resolved version changed is one of the corrected
   packages and no other.
3. **Given** a pipeline run on that branch, **When** the dependency cache step is inspected, **Then**
   it reports a hit on the same key basis as before.
4. **Given** a pipeline run on that branch, **When** total wall-clock time is compared with a recent
   trunk run, **Then** it has not materially regressed.

---

### User Story 4 - The change survives the next dependency bump (Priority: P3)

Months later, a routine dependency upgrade lands. The developers who adopted the global virtual
store are not the ones who discover — by a broken build on their own machine — that the upgrade
reintroduced an undeclared import or moved a package past the version whose manifest was corrected.

**Why this priority**: The corrections made here describe specific third-party packages at specific
versions. They are silently invalidated by an upgrade, and the failure surfaces only under the
global virtual store — which, being opt-in, is by construction the configuration no pull request
exercises. A nightly build closes that gap. Valuable, but the adoption is useful before this exists
— so it is the lowest priority of the four.

**Independent Test**: Simulate a bump of one corrected package to a version the corrections do not
cover, and confirm the nightly job goes red on it rather than absorbing it silently.

**Acceptance Scenarios**:

1. **Given** the nightly job exists, **When** a change reintroduces an undeclared import or moves a
   corrected package past its corrected version, **Then** the next nightly run fails and names the
   unresolved module.
2. **Given** the nightly job is red, **When** a developer reads its output, **Then** they can tell
   which package and which import caused it without reproducing the global virtual store locally
   first.
3. **Given** a developer with an existing worktree installed the old way, **When** they follow the
   documented migration steps, **Then** they end up on the shared store with no leftover state and
   without hand-debugging.
4. **Given** a pull request that would reintroduce the defect, **When** it is merged, **Then** it is
   **not** blocked — the nightly is a detector, not a gate, and the resulting triage cost is
   accepted.

---

### Edge Cases

- **Stale local dependency tree**: a worktree installed before the switch already holds a 2.1 GB
  local tree. Does switching migrate it, or must it be removed first? Leaving both states half
  present is the most likely source of confusing failures.
- **Store pruning across worktrees**: the shared store is machine-wide. A prune triggered from one
  worktree can remove content that another worktree's symlinks still point at, breaking a checkout
  nobody touched.
- **Divergent branches on one machine**: two worktrees on branches with different lockfiles, or one
  on a branch from before the pnpm 12 migration, share the same store directory. Store-format and
  content differences must not corrupt either checkout.
- **Cached build results masking failure**: a cached build replays recorded output and reports
  success without executing anything. Every verification in this specification must therefore skip
  the build cache, or it proves nothing.
- **Asset copies whose source became a symlink**: the shipped rich-text-editor and code-editor
  bundles are recursive copies out of directories that are now symlinks pointing outside the
  workspace root. A copy that silently yields zero files still exits successfully, so the build
  would go green while shipping an unusable editor.
- **Path-guarded tooling**: the dev server resolves real paths before applying its permitted-roots
  guard, so files that now live outside the repository can be rejected even though they resolve.
- **Platform differences**: developers on a platform with restricted symlink support, or a
  case-insensitive file system, may see resolution behave differently from the machine where the
  change was verified.
- **A newly added dependency with the same defect**: any future package that imports what it does
  not declare will work for everyone on the local store and fail only for adopters.

---

## Requirements *(mandatory)*

### Functional Requirements

**Adoption**

- **FR-001**: `core-web` MUST be installable with pnpm's global virtual store in effect, producing a
  dependency tree that is symlinks into one shared machine-wide content-addressable store rather
  than a materialised per-checkout copy.
- **FR-002**: Turning the global virtual store on or off MUST NOT change `pnpm-lock.yaml` — it is a
  layout choice, not a resolution one, and this was measured. The **corrections** of FR-003 are a
  different matter: they alter what packages declare and may therefore move the lockfile. If they do,
  the regenerated lockfile MUST be committed with them and a frozen-lockfile install MUST succeed
  from it **with the local virtual store as well**, since that is what every developer and every
  pipeline job will keep using.
- **FR-003**: The repository MUST carry corrected dependency declarations for every third-party
  package that imports a module it does not declare, such that each import resolves from inside the
  package's own isolated directory without relying on walking up into the project.
- **FR-004**: Those corrections MUST be expressed in repository-owned configuration and MUST NOT
  require an unreleased upstream change or a vendored copy of a third-party package.
- **FR-005**: The set of corrected packages MUST be discovered across **every module graph this
  workspace builds**, not just the production bundle. A cache-skipped production build only exercises
  imports it happens to reach, so passing it proves nothing about the others. The graphs, and the
  enumeration required for each:
  - **Production bundle** — the build names them, one failure at a time (FR-007).
  - **Vitest suite** — enumerated by scanning the packages named in the vite configs'
    `test.server.deps.inline`, the set Vitest actually resolves.
  - **Stencil / webcomponents** — enumerated from its build's `UNRESOLVED_IMPORT` warnings, which do
    **not** fail the build and must therefore be read deliberately.

  The scan reports are **required verification artifacts**, attached to the pull request, not
  transient console output. The four packages named in the issue are a starting point, not the list.
- **FR-005a**: Coverage is explicitly **bounded** to those three graphs. An import reached only by a
  computed or dynamic specifier is invisible to a static scan and may still surface later; FR-019's
  detector is what catches it. This bound MUST be stated in the documentation of FR-017 rather than
  left as an implied guarantee of completeness.
- **FR-006**: The global virtual store MUST remain a **per-developer opt-in**. The repository MUST
  NOT set it as the default for `core-web`: only the corrections of FR-003 are committed, and the
  setting itself is enabled by each developer in their own machine-level package-manager
  configuration. The repository default therefore stays the local virtual store, for developers and
  for the pipeline alike.

**Verification — all of it with the build cache skipped**

- **FR-007**: A production build of the admin UI MUST complete with zero module-resolution errors.
- **FR-008**: The shipped rich-text-editor and code-editor asset directories MUST be asserted
  non-empty **by file count**, not by the build's exit status. Baselines from a known-good build are
  217 files / 10 MB and 1068 files / 84 MB respectively.
- **FR-009**: The full workspace test target MUST pass, with explicit confirmation for the component
  library and the content-editing application.
- **FR-010**: The admin UI dev server MUST serve the application with no request rejected as a path
  outside the permitted roots.
- **FR-011**: Project-graph inference MUST complete across the whole workspace without a native
  crash, verified in continuous integration and not only on a developer machine — the software
  development kit's Vue project is already documented as fragile here.
- **FR-012**: Build output produced under the global virtual store MUST be equivalent to output
  produced without it: same set of emitted files, with no missing bundle or asset.

**Continuous integration**

- **FR-013**: Every pipeline job that passes on the trunk MUST still pass with the corrections
  committed and the default local virtual store in effect.
- **FR-014**: A frozen-lockfile install against the committed lockfile MUST succeed under the local
  virtual store, and the lockfile diff against the trunk MUST be reviewed on **three** axes, not one:
  1. **Resolved versions** — no package outside the correction set may change version.
  2. **Dependency edges and peer-resolution snapshots** — a package extension adds edges and new
     peer-suffixed `snapshots:` keys *without* changing any version, so a version-only check would
     pass while the graph shifted underneath it. Every changed snapshot key MUST trace to a
     corrected package or to a dependent of one.
  3. **New declared dependencies** — the only edges added may be the peers FR-003 introduces.

  A check that inspects versions alone is insufficient and MUST NOT be treated as satisfying this
  requirement.
- **FR-015**: The pipeline's dependency cache MUST still hit on the same key basis, and pipeline
  wall-clock time MUST NOT materially regress.
- **FR-016**: In the nightly job of FR-019, the cached package store MUST cover the global virtual
  store's contents. The pipeline caches the package store and never caches the installed dependency
  directory, so a global store landing outside the cached path would cost that job a full download
  every night.

**Living with it**

- **FR-017**: Developer documentation MUST state how a developer opts in to the global virtual store,
  how to migrate a worktree that was installed the old way, and what a machine-wide shared store means
  for pruning and for cleaning up a worktree. It MUST additionally carry, as an explicit warning:
  - **The trust boundary.** One writable store shared between mutually untrusted users or agents is
    unsafe, and the upstream guidance says so. A single-user developer machine and an ephemeral
    single-tenant pipeline runner both satisfy the boundary; a **shared or multi-tenant build box does
    not**, and the setting MUST NOT be applied there.
  - **The machine-wide reach.** Opting in applies the layout to every pnpm project on that machine,
    not only this repository.
  - **The coverage bound** from FR-005a.
- **FR-018**: The reason each third-party correction exists MUST be recorded alongside it, naming the
  package, the undeclared import, and the version the correction was written against — so a future
  maintainer can tell whether an upgrade makes it removable.
- **FR-019**: A **periodic (nightly) pipeline job** MUST install and build `core-web` with the global
  virtual store explicitly enabled — the only place in the pipeline where that configuration is
  exercised, because FR-006 keeps it off by default. With the build cache skipped it MUST cover
  **every check the user stories promise**, because a guard narrower than the promise silently
  degrades into a false assurance:
  - the FR-007 production build and the FR-008 asset file-count assertions;
  - the FR-009 workspace test run — the Vitest graph broke under the global store while the
    production build was green, so a build-only detector would have reported success;
  - the FR-011 project-graph inference;
  - the FR-005 scans, so a newly added dependency with an undeclared import is reported before
    anyone hits it.

  The FR-010 dev-server check is **explicitly excluded** and remains manual: it needs a browser, and
  a resolution regression fails the build first. That exclusion is the one narrowing of the guarantee,
  and it MUST be stated in the documentation rather than implied.

  The job MUST report a failure loudly enough to be triaged rather than accumulate unnoticed, and
  MUST NOT run on pull requests or block a merge.
- **FR-020**: If exhaustive discovery shows the build cannot be made green from repository-owned
  configuration alone, the work MUST close by recording the newly found blockers in the issue rather
  than shipping a partially working adoption.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A freshly installed `core-web` dependency directory occupies under 50 MB, against
  ~2.1 GB today — at least a 97% reduction per checkout.
- **SC-002**: A developer who opts in with five worktrees reclaims at least 8 GB **of total machine
  footprint**, measured as the sum of all five `core-web/node_modules` directories **plus** the shared
  store, before versus after migration. The shared store counts against the saving: five local trees
  at ~2.1 GB each is ~10.5 GB, while five symlink trees plus one store is a few megabytes each plus
  a store the machine already carried for other pnpm projects. Comparing only the `node_modules`
  directories would overstate the win.
- **SC-003**: A warm frozen-lockfile install completes in under 6 seconds, against ~15 seconds today
  — at least 60% faster.
- **SC-004**: A cache-skipping production build of the admin UI completes successfully, reporting
  zero unresolved modules, where today it reports at least four.
- **SC-005**: The two shipped third-party editor asset directories each contain at least their
  known-good file count (217 and 1068 files).
- **SC-006**: The full workspace test run finishes with no failure that does not also occur without
  the change.
- **SC-007**: A developer can load every screen of the admin UI from the dev server with zero
  forbidden-path errors.
- **SC-008**: For everyone who does not opt in — every non-adopting developer and every pipeline job
  — nothing measurable changes: same job outcomes, same dependency-cache hit rate, and pipeline
  wall-clock time within 5% of a recent trunk run.
- **SC-009**: A developer migrating an existing worktree completes the switch in under 5 minutes by
  following the documentation, with no step they have to work out themselves.
- **SC-010**: A reintroduced undeclared import is reported by the nightly job within 24 hours of the
  change that caused it, and its output names the package and the unresolved module.

---

## Assumptions

- The pnpm 12 migration ([#37553](https://github.com/dotCMS/core/issues/37553) /
  [#37563](https://github.com/dotCMS/core/pull/37563)) has landed on the trunk; `core-web` pins
  pnpm 12.4.2 and configuration lives in the workspace configuration file rather than in `.npmrc`,
  which is now read for registry and authentication only. This work builds on that state.
- Scope is `core-web` only. It holds the 2.1 GB and the failing build; the other package-managed
  projects in the repository are small enough that sharing their store is not worth the risk in this
  change.
- Enabling the global virtual store is a machine-level setting a developer applies once, outside the
  repository. This specification does not require it to be expressible per project, and it assumes a
  developer who opts in accepts that the setting then applies to their other pnpm projects too.
- The benefit is local-development only. Continuous integration is treated purely as something not
  to break — no pipeline speed-up is expected or claimed.
- The measurements quoted from the issue (sizes, timings, asset file counts) were taken on the pnpm
  12 branch at a single commit and are used as the baseline. They will be re-taken on the branch that
  implements this before being asserted.
- Corrections to third-party manifests are version-specific by nature and will need revisiting on
  upgrade. That maintenance cost is accepted as the price of the disk savings.
- Verification happens on the platforms the team develops on. Platform-specific symlink behaviour
  elsewhere is out of scope and will be recorded, not solved, if it appears.
- No application behaviour changes. This is a build and developer-environment change only — no
  runtime code, no API surface, no database or search-index impact, and nothing in the rollback-unsafe
  categories.

---

## Out of Scope

- Making the global virtual store the repository default, for developers or for the pipeline — see
  the Clarifications. The only pipeline job that enables it is the nightly detector of FR-019.
- Blocking a pull request on the global virtual store building green.
- Upgrading, replacing, or removing any of the offending third-party packages to avoid the defect.
- Extending the global virtual store to package-managed projects outside `core-web`.
- Re-testing the symlink-preserving bundler option — the issue records both failure directions and
  why neither is a path forward.

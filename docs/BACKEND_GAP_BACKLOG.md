# Backend Context Gap Backlog (M4 — Team Falcon)

Backend half of next quarter's AI-context gap backlog, drafted as part of the Backend AI-Context
Rock (epic #37124, milestone M4 / #37129). To be merged with Team Scout's testing/tooling half
into one document, reviewed with the team, and confirmed as next quarter's Rock.

Each entry below is a **gap to be filled next quarter** — not something fixed as part of this
Rock. Sizes are rough (S/M/L) estimates of the documentation-writing effort, not the underlying
subsystem's complexity.

## 1. Content Lifecycle — Size: L

No AI-context doc **adequately explains** dotCMS's core content lifecycle: the identifier-vs-inode
relationship, working vs. live versions, and the `checkin`/`publish`/`archive`/`unpublish`/`checkout`
semantics on `ContentletAPI`. (Existing mentions are incidental, not explanatory — see below.)

- `ContentletAPI` (2,672 lines) exposes 10 separate `checkin(...)` overloads alone, plus
  `publish`/`unpublish`/`archive`/`unarchive`/`checkout` families.
- The real implementation, `ESContentletAPIImpl`, is **10,943 lines** — one of the largest,
  most heavily-used, highest-blast-radius classes in the codebase.
- Workflow fires automatically as part of checkin (`fireWorkflowPreCheckin`/`fireWorkflowNoCheckin`),
  making this a genuinely cross-cutting concept, not a narrow one.
- **Locking** (`lock`/`unlock`/`canLock`) — the edit-contention model that prevents two users from
  clobbering each other's changes — is entirely undocumented.
- **Relationships and multi-language** (`relateContent`/`getRelatedContent`/`RelationshipAPI`,
  `getAllLanguages`/`findContentletForLanguage`) — how content relates to other content, and how a
  single identifier spans multiple language versions — is a distinct, substantial concept with no
  documentation.
- Existing docs only touch this tangentially (e.g. `ROLLBACK_UNSAFE_CATEGORIES.md` flags risky
  patterns without explaining the underlying model).

## 2. Caching Architecture — Size: M/L

No AI-context doc **adequately explains** dotCMS's caching architecture: cache region semantics,
the pluggable storage-backend system, cross-node invalidation propagation, or which of the two
cache systems to use for new code. (Existing mentions are incidental code examples, not
architectural explanations — see below.)

- `CacheLocator` (595 lines) registers **59 distinct cache regions**.
- A separate, newer `com.dotcms.cache` package (10 classes, e.g. `DynamicTTLCache`) exists for
  TTL-based caching, with no documented guidance on when to use it over the classic
  region-based `DotCacheAdministrator` pattern.
- **Pluggable `CacheProvider` backends** — 5 real implementations exist (caffine, guava, redis,
  timedcache, h22) behind the `CacheProvider` interface — with no documentation on how the active
  backend is selected or when a new one would be needed.
- **`CacheTransport`** — a separate abstraction specifically for propagating cache invalidation
  across cluster nodes (distinct from the local `flushGroup`/`flushAll`/`flushAlLocalOnly` calls,
  which only affect the current node) — is undocumented, despite being the mechanism that keeps
  caches consistent across a multi-node dotCMS cluster.
- **Tomcat Redis Session Manager** — all dotCMS instances are meant to run with the
  [`tomcat-redis-session-manager`](https://github.com/dotCMS/tomcat-redis-session-manager) plugin
  (`com.dotcms:tomcat-redis-session-manager:2.0`, real dependency in `bom/application/pom.xml`),
  which persists Tomcat sessions in Redis so they survive a server restart. It's wired into
  `context.xml` via a live `${TOMCAT_REDIS_SESSION_CONFIG}` placeholder — the file's own comment
  confirms commenting it out "disable[s] session persistence across Tomcat restarts." Currently
  only a bare one-line mention exists (`DOCKER_BUILD_PROCESS.md`: "Redis Session Manager: Session
  clustering"), with no explanation of what it does, why it's required, or how to configure it —
  despite a real working cluster example already existing
  (`docker/docker-compose-examples/with-redis-session/`).
- Existing mentions (`DATABASE_PATTERNS.md`, `TELEMETRY_IMPLEMENTATION.md`) are narrow,
  incidental code examples, not architectural explanations.

## 3. Workflow Engine — Size: M

No AI-context doc **adequately explains** dotCMS's workflow engine: the scheme → step → action →
actionlet model, the assignable-task/history layer, or how to extend it. (Other docs use the word
"workflow" for entirely unrelated concepts — see below.)

- `WorkflowAPI` (1,261 lines) implements the scheme/step/action model.
- **36 real actionlet classes** exist as a genuine extensibility surface (custom actions
  triggered on workflow transitions) with no documented pattern for writing a new one.
- **`WorkflowTask`/`WorkflowHistory`/`WorkflowComment`** — the assignable-task and audit-trail
  layer (who a piece of content is assigned to, its transition history, comments left along the
  way) — is a distinct, substantial concept from the scheme/step/action model and is entirely
  undocumented.
- **`SystemActionWorkflowActionMapping`** — how a content type maps its default system actions
  (e.g. "what happens on publish") to a specific workflow action — is undocumented.
- Step-level permission gating on transitions is undocumented.
- Existing "workflow" mentions in `ARCHITECTURE_OVERVIEW.md`/`docs/claude/WORKFLOW_PATTERNS.md`
  refer to unrelated concepts (package listings; Claude's own task-approach workflow) — genuinely
  zero overlap with the actual engine.

## 4. Upgrade Tasks — Size: S ⚠️ Likely Critical Rule candidate

No AI-context doc **adequately explains** dotCMS's upgrade-task system (`runonce`/`runalways`,
`Task<number><Description>` naming) — and it has the **same silent-registration failure mode**
already fixed for integration tests in M1's MainSuite Critical Rule. There's also a second,
sibling task system with the identical risk. (Existing mentions reference the task names without
explaining the mechanism — see below.)

- 246 real `runonce` upgrade tasks exist under `com.dotmarketing.startup.runonce`.
- They are **not auto-discovered**: `TaskLocatorUtil.getStartupRunOnceTaskClasses()` maintains an
  explicit, manually-updated list of imports (245 currently registered).
- **A new upgrade task not added to that list compiles fine and silently never runs on
  upgrade** — the exact same class of gap the MainSuite Critical Rule was written to prevent,
  just in a different subsystem, and currently with zero documentation or safeguard.
- **`FixTask`** — a separate, sibling system (18 real tasks under `com.dotmarketing.fixtask.tasks`,
  for data-consistency fixes rather than schema upgrades) is registered through the exact same
  `TaskLocatorUtil` mechanism (`getFixTaskClasses()`/`systemFixTasks`) and carries the identical
  silent-registration risk — currently undocumented alongside `runonce`.
- `ROLLBACK_UNSAFE_CATEGORIES.md` mentions `runonce` tasks extensively as a review-risk signal,
  but never explains the mechanism itself.
- Recommendation: given the direct parallel to an already-established Critical Rule, the real
  production risk of a silently-skipped migration, and that the same risk applies to *two*
  registration lists (not one), this is worth prioritizing highly next quarter — possibly
  promoted straight to a new Critical Rule in `CLAUDE.md` rather than just a standalone doc.

## 5. `docs/integration/` Ownership — Size: S (decision, not a content fix)

`docs/integration/API_CONTRACTS.md` exists, is real and substantial (726 lines of
frontend↔backend REST integration patterns), and is referenced from
`docs/core/SECURITY_PRINCIPLES.md:80` — but it is **not linked from `CLAUDE.md`**, and doesn't
fall cleanly under either team's current split.

- Discovered during M0 (#37125); explicitly out of scope for M1–M3, which only covered
  Team Falcon's backend slice.
- **Both teams can create REST endpoints and consume them from Angular** — this content is
  genuinely cross-cutting, not something either team owns in isolation. The gap isn't the
  content (which already exists and looks solid) — it's that nobody is explicitly responsible
  for keeping it current and linked.
- **Ownership decision (resolved):** Both teams jointly own updating and keeping
  `API_CONTRACTS.md` correct. If a dev needs to update content covering endpoints clearly
  created by the other team, they must raise it and touch base with that team first to confirm
  the change makes sense before merging.
- Still open: (1) the file's actual content has not been audited against real source the way
  M1–M3 audited Falcon's backend docs — it may have the same kinds of staleness/fabrication
  issues found elsewhere; (2) it still needs to be linked into `CLAUDE.md`'s navigation index.

## Next Steps

1. Merge this with Team Scout's testing/tooling half into one backlog document.
2. Review with the team; confirm scope and priority order for next quarter's Rock.
3. `docs/integration/` (item 5): ownership model is now decided (joint, cross-team touch-base
   required) — link it into `CLAUDE.md` and audit its content against real source before or as
   part of whichever Rock picks it up.

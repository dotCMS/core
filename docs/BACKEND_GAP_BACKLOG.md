# AI-Context Gap Backlog (M4 — Teams Falcon and Scout)

Next quarter's AI-context gap backlog, drafted as part of the Backend AI-Context Rock (epic
#37124). Items 1–5 are Team Falcon's backend half (M4 / #37129); items 6–10 are Team Scout's
testing and tooling half (M4 / #37580). To be reviewed with the team and confirmed as next
quarter's Rock.

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

## 6. `.claude/skills/` — Unowned and Unaudited — Size: L

No team owns `.claude/skills/`, and no milestone in this Rock read it — yet at **14,052 lines
across 23 first-party skills** it is the single largest AI-context surface in the repository,
larger than `docs/backend/`, `docs/testing/` and `docs/core/` combined.

- The ownership split (#37124) assigns Team Scout `.claude/commands/` — **7 files, 629 lines**. It
  does not mention `.claude/skills/` at all.
- The commands are thin wrappers over the skills. `.claude/commands/create-issue.md` is **5 lines**
  and says only "use the `dot-issue-manage` skill in CREATE mode"; the skill it points at,
  `.claude/skills/dot-issue-manage/SKILL.md`, is **630 lines**. Auditing `.claude/commands/` in M1
  audited the pointers, not the payload.
- Governance exists and is real — `CATALOG.md` is generated by `.claude/tools/gen-skills-catalog.mjs`,
  `skill-lint.mjs` runs as a CI gate (`.github/workflows/cicd_pr_skill-lint.yml`), and
  `CONTRIBUTING.md` documents the process. But the lint validates **naming, frontmatter and catalog
  freshness**. Nothing checks whether a skill's instructions match real source.
- **A fabrication was found on the first spot-check.**
  `.claude/skills/dot-cicd-diagnose/ISSUE_TEMPLATE.md:90` tells the reader to run
  `./mvnw test -Dtest=ContentTypeAPIImplTest#testCreateContentType`. The class is real, but
  `testCreateContentType` does not exist in it — and the class lives in `dotcms-integration`, so it
  needs `verify ... -Dit.test=`, not `test -Dtest=`. That is the **same `-Dtest=` vs `-Dit.test=`
  error** found in `INTEGRATION_TESTS.md` during M3 (#37128), alive in a surface no milestone
  covered.
- Skills already carry an `Owner` field in frontmatter (`@dotcms/scout`, `@dotcms/falcon`,
  `@dotcms/platform`, `@dotcms/maintenance`, individuals). Per-skill ownership exists; what is
  missing is ownership of *auditing them against source*, and any process that keeps them current.

## 7. 86 Integration Tests That Never Run in CI — Size: M

`scripts/validate-integration-test-registration.sh` (added in #37590) measures it: **632 classes
registered** in a CI suite against **693 concrete `*Test.java`** in `dotcms-integration` — **86 that
exist, compile, and are never executed by any CI job.**

- This is standing backlog, not an active leak. The flow is clean: **0 of the 33** integration tests
  added in the last 30 days went unregistered. The registration failure mode this Rock set out to
  stop is not currently recurring.
- They accumulated over more than two years — `WorkflowProcessorTest` dates to January 2024.
- Nobody knows what they are. Each is one of three things: dead code that should be deleted,
  redundant coverage already provided elsewhere, or **genuinely missing CI coverage on a real
  subsystem**. The third case is a silent quality gap, because the class looks like a passing test
  to anyone reading the source.
- The gap is the triage, not the mechanism. Detection already exists and can run in CI with
  `--strict`; what is missing is a pass over the 86 and a decision on each.

## 8. No Content-Accuracy Check for Any AI-Context Surface — Size: M

Every automated check that exists validates **structure**. Nothing validates whether a claim is
true — which is the failure mode this entire Rock was created to address.

- Surfaces found carrying fabricated content so far: `INTEGRATION_TESTS.md` and
  `REST_API_PATTERNS.md` (#37128), `BACKEND_UNIT_TESTS.md` (#37666), `CLI_OVERVIEW.md` (#37664),
  `CICD_PIPELINE.md` (#37711), `.claude/commands/gh-issue-troubleshoot.md` (#37610), five files
  under `.cursor/rules/` (#37590 and #37629 merged; #37715 open), and
  `.claude/skills/dot-cicd-diagnose` (item 6, still unfixed). **Every one found by reading. None by any tool.**
- The shape is consistent: **real API names arranged around a fictional subject.** A reviewer
  spot-checking `WorkflowAPI` gets a hit and moves on, while `WorkflowManager` in the same snippet
  does not exist. `PushContext` is real but an interface, not the record the doc showed.
  `HealthStateManager` is real but has `getLivenessHealth()`, not `getLivenessResponse()`.
- Two variants are worth designing against, because a naive "does this symbol exist" check would
  miss both. `CICD_PIPELINE.md`'s **prose was accurate** — phase model, zero-trust design, artifact
  strategy all matched — and only the YAML blocks were invented, so a reader skims the text, trusts
  it, and copies the part that is wrong. And `e2e-rules.mdc` (#37715) was not invented from nothing
  but **duplicated from a real doc and then drifted**: it told readers to *always* use `data-testid`
  and *never* CSS selectors, where its source of truth
  (`core-web/apps/dotcms-ui-e2e/AGENTS.md`) puts `getByRole` first and permits CSS inside the Dojo
  iframe, which is where `data-testid` does not exist. Every symbol in it resolved; the guidance was
  still wrong, and the Cursor rule auto-loads while the doc has to be opened deliberately.
- Existing gates do not catch any of this: `skill-lint` checks frontmatter, the reachability check
  proposed in #37709 catches unreachable files, and CI compiles production code — but no doc
  example is ever compiled or resolved.
- **The failure reproduces even under active hunting.** The replacement example written for #37666,
  in a PR whose entire purpose was removing fabricated content, stubbed
  `healthStateManager.getLivenessResponse()` — a method that does not exist. Caught in review, not
  by a tool.
- Worth scoping, not assuming: compiling every fenced Java block is expensive and probably the
  wrong first cut. Resolving each `ClassName`, `#method`, `-Dflag` and Maven profile named in an
  AI-context file against the source tree, and failing on those with no referent, is cheap — and
  would have caught seven of the nine above.

## 9. No Upkeep Process for `.cursor/rules/` and `.claude/commands/` — Size: S

Both directories drifted out of sync with `docs/` because nothing connects them. They are updated by
hand, by whoever remembers.

- `.cursor/rules/java-context.mdc` still said `Core: Java 11 syntax. CLI: Java 21 ok` months after
  M1 (#37126) corrected the same claim in the docs — the rule sat outside Falcon's slice and was
  left behind (fixed in #37590).
- `.cursor/rules/README.md` indexed five of six rules, and documented one rule's glob as what it
  *should* have been rather than what the frontmatter said (fixed in #37629).
- `doc-updates.mdc` — the rule whose entire job is telling a reader where to update documentation —
  had a glob of `**/*.mdc`, so it never loaded when editing a `.md` (fixed in #37629).
- `e2e-rules.mdc` is 223 lines restating `core-web/apps/dotcms-ui-e2e/AGENTS.md` rather than
  pointing at it — the only rule with zero `@docs` pointers — and has drifted into contradicting it
  on locator strategy while describing a `src/config/` directory that does not exist. **#37715 is
  open** and folds it to a ~50-line pointer; until that merges the contradiction is still what
  Cursor loads on every spec edit.
- The fixes are merged; the process gap is not. Nothing makes a change to `docs/testing/` prompt a
  look at `test-context.mdc`, and the CI gate that exists for skills has no equivalent for rules or
  commands. The reachability check in #37709 (approved, not yet merged) covers `docs/`, not these
  two directories.

## 10. The Rock's Own Ownership Split Omitted 10 Files — Size: S (decision, not a content fix)

The split in #37124 covers `docs/backend/` (18 files), `docs/infrastructure/` (1), `docs/testing/` (5)
and nine of the ten then in `docs/core/` — **43 of the 53 `.md` files under `docs/` at the time**.

- Unassigned to either team: `docs/claude/` (4 files), `docs/cli/` (2), `docs/integration/` (1),
  `docs/test-cases/` (1), `docs/README.md`, and one `docs/core/` file, since the split's own
  arithmetic gives 4 + 5 = 9 against 10. (`docs/frontend/`, 10 files, is out of scope for both teams
  by design.)
- Confirmed with Jose (Sep 21): the split was built from the file list available when the epic was
  written, and he has not touched any of the unassigned files.
- `docs/integration/` was already caught this way during M0 and is item 5 above — the **second time
  the same omission surfaced**, which makes it a process finding rather than a one-off.
- Resolved since: `docs/cli/` taken by Scout, audited and linked (#37664, merged — it found the
  Java language level stated as 21 against a pom pinned to 11), and `docs/claude/` and
  `docs/test-cases/` likewise (#37721, merged). #37709 links every remaining orphan and rebuilds
  `docs/README.md` as a complete index of all 54 files; it is approved but **not yet merged**.
  Still needing an owner: the loose `docs/core/` file.
- The durable fix is not a better manual split. It is the complete index plus an automated
  reachability check that fails a PR leaving any doc unreachable — an index over every file makes an
  omission visible, and the check stops one being added silently. Both are in #37709, approved and
  awaiting merge; until it lands, nothing prevents the next omission.

## Next Steps

1. ~~Merge this with Team Scout's testing/tooling half into one backlog document.~~ Done — items
   6–10 added.
2. Review with the team; confirm scope and priority order for next quarter's Rock. Two items are
   candidates for promotion rather than plain backlog entries: **item 4** (upgrade tasks, the same
   silent-registration failure as the MainSuite rule) and **item 8** (no content-accuracy check —
   the gap that let every fabrication this Rock found reach `main`).
3. `docs/integration/` (item 5): ownership model is now decided (joint, cross-team touch-base
   required) — link it into `CLAUDE.md` and audit its content against real source before or as
   part of whichever Rock picks it up.

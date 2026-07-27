---
allowed-tools: Bash(gh pr view:*), Bash(gh pr diff:*), Bash(gh pr list:*), Bash(gh issue list:*)
---

# Autonomous PR Review System

Intelligent, self-validating pull request reviewer that automatically selects the appropriate review lens based on changed file types. Covers **frontend (Angular/TypeScript/SCSS)** and **backend (Java/Maven/REST)** changes.

## Usage

```bash
/review <PR_NUMBER>
/review <PR_URL>
```

## How It Works

This skill performs an **autonomous, multi-stage review** with intelligent PR classification:

1. **Fetch & Analyze**: Gets PR diff and classifies all changed files by domain
2. **Domain Detection**: Determines which domains the PR touches (frontend, backend Java, or both)
3. **Multi-Agent Review**: Launches specialized agents (TypeScript, Angular, Test, Style, Java backend) in parallel for the domains present
4. **Self-Validation**: Verifies all file references, line numbers, and findings before output
5. **Structured Output**: Delivers consistent, actionable review format

## Review Process

### Stage 1-3: File Classification with Dedicated Agent

Launch the **File Classifier** agent (subagent type: `dotcms-file-classifier`) to handle PR data collection, file classification, and review decision:

```
Task(
    subagent_type="dotcms-file-classifier",
    prompt="Classify PR #<NUMBER> files by domain (Angular, TypeScript, tests, styles, java-backend) and determine which domains need review (REVIEW if any frontend or java-backend files; SKIP only if all out-of-scope).",
    description="Classify PR files"
)
```

The `dotcms-file-classifier` agent will:
1. **Fetch** PR metadata and diff (`gh pr view`, `gh pr diff`)
2. **Classify** every changed file into reviewer buckets:
   - Frontend: `angular`, `typescript`, `test` (frontend `.spec.ts`), `styles`
   - Backend: `java-backend` (`.java` incl. backend JUnit/integration tests under `dotcms-integration/`, `pom.xml`, `openapi.yaml`, backend config/resources)
   - `out-of-scope` (docs, CI yaml, docker, unrelated assets)
3. **Determine** which domains are present (frontend and/or backend)
4. **Return** a structured file map with the review decision (REVIEW or SKIP)

**Decision rule**: REVIEW if the PR has **any** reviewable files — i.e. any non-empty frontend bucket **or** the `java-backend` bucket. SKIP only when every changed file is `out-of-scope`.

**If decision is SKIP**: Report to the user that the PR has no reviewable frontend or backend code (only docs/CI/infra) and stop.

**If decision is REVIEW**: Proceed to Stage 4, launching only the agents whose buckets are non-empty.

### Stage 4: Domain-Specific Review with Specialized Agents

**Using the file map from the dotcms-file-classifier agent**, launch **parallel specialized agents** only for buckets that have files:

1. **TypeScript Type Reviewer** (subagent type: `dotcms-typescript-reviewer`)
   - Receives the `typescript-reviewer` file list from the file map
   - Focus: Type safety, generics, null handling, type quality
   - Confidence threshold: ≥ 75
   - **Skip if**: No files in the typescript bucket

2. **Angular Pattern Reviewer** (subagent type: `dotcms-angular-reviewer`)
   - Receives the `angular-reviewer` file list from the file map
   - Focus: Modern syntax, component architecture, lifecycle, subscriptions
   - Confidence threshold: ≥ 75
   - **Skip if**: No files in the angular bucket

3. **Test Quality Reviewer** (subagent type: `dotcms-test-reviewer`)
   - Receives the `test-reviewer` file list from the file map
   - Focus: Spectator patterns, coverage, test quality
   - Confidence threshold: ≥ 75
   - **Skip if**: No files in the test bucket

4. **SCSS/HTML Style Reviewer** (subagent type: `dotcms-scss-html-style-reviewer`)
   - Receives the `styles` file list from the file map (`.scss`, `.css`, `.html` files)
   - Focus: BEM compliance, CSS custom properties, unused classes, SCSS standards, Angular encapsulation, PrimeNG theming
   - Confidence threshold: ≥ 75
   - **Skip if**: No `.scss`, `.css`, or `.html` files in the styles bucket

5. **Java Backend Reviewer** (subagent type: `dotcms-java-backend-reviewer`)
   - Receives the `java-backend` file list from the file map (`.java`, `pom.xml`, `openapi.yaml`, backend config)
   - Focus: Config/Logger usage, Maven/BOM version placement, REST `@Schema` correctness, endpoint documentation (`@Operation`/`operationId`/`@Parameter` descriptions for OpenAPI & AI-agent consumption), OpenAPI drift, immutables/generics/exceptions, `APILocator`/CDI patterns, batch permission filtering, no in-place mutation of cache-returned `Contentlet`s (copy via `new Contentlet(original.getMap())` before modifying), Page API REST/GraphQL response parity (changes to `PageView` must mirror in the GraphQL page providers), `SecurityLogger` audit trail (who/what) on sensitive-resource (Apps/secrets/permissions/tokens) changes, prefer CDI over manual instantiation, `@WrapInTransaction`/`@CloseDBIfOpened` DB-lifecycle correctness (write vs read semantics, exception-driven rollback), **commit listeners** for work that must only run if the preceding persistence succeeded (defer index/cache/event/external side effects via `HibernateUtil.addCommitListener`, and perform post-commit-only reads inside the listener body), **JavaBean getters on records that reach VTL** (Velocity introspection cannot resolve a record's `foo()` accessor — it renders as literal text, silently), `respectFrontendRoles` semantics (internal/system code should pass `false`), minimal distributed-FS disk I/O (avoid repeated `File.exists()`, prefer cached `FileMetadataAPI`), `Config.getProperty` not called in loops (cache via `Lazy`/constructor), integration tests registered in a JUnit `@SuiteClasses` suite, input/query/file-name security (bound SQL params, `FileUtil.sanitizeFileName`/`isValidFilePath`, path-traversal guards, OWASP output encoding), security (secrets, input validation, sensitive logging), and ES→OS migration invariants when indexing code changes
   - Confidence threshold: ≥ 75
   - **Skip if**: No files in the java-backend bucket

#### Agent availability — check before dispatching

Only the agent definitions committed under `.claude/agents/` can be dispatched; a `Task(subagent_type=...)` naming a type that does not exist fails. As of this revision the repo ships **`dotcms-java-backend-reviewer`** only. `dotcms-file-classifier`, `dotcms-typescript-reviewer`, `dotcms-angular-reviewer`, `dotcms-test-reviewer` and `dotcms-scss-html-style-reviewer` are **not yet in the repo**.

Therefore:
- **Backend Java** → dispatch `dotcms-java-backend-reviewer` as described below.
- **File classification (Stage 1-3)** → until `dotcms-file-classifier` lands, perform the classification **inline** (`gh pr view --json files`, `gh pr diff`) using the same buckets and the same REVIEW/SKIP decision rule.
- **Frontend buckets** → until those agents land, review the non-empty frontend buckets **inline** against `core-web/CLAUDE.md` and the `docs/frontend/` standards, and say so in the output rather than claiming a specialized agent ran.

Do not invent a subagent type: verify it exists in `.claude/agents/` before dispatching, and fall back to inline review for the rest.

**Launch agents in parallel** using the Task tool (only for non-empty buckets **whose agent exists**):
```
Task(subagent_type="dotcms-typescript-reviewer", prompt="Review TypeScript type safety for PR #<NUMBER>. Files: <file-list from dotcms-file-classifier>", description="TypeScript review")
Task(subagent_type="dotcms-angular-reviewer", prompt="Review Angular patterns for PR #<NUMBER>. Files: <file-list from dotcms-file-classifier>", description="Angular review")
Task(subagent_type="dotcms-test-reviewer", prompt="Review test quality for PR #<NUMBER>. Files: <file-list from dotcms-file-classifier>", description="Test review")
Task(subagent_type="dotcms-scss-html-style-reviewer", prompt="Review SCSS/HTML styling standards for PR #<NUMBER>. Files: <styles file-list from dotcms-file-classifier>", description="Style review")
Task(subagent_type="dotcms-java-backend-reviewer", prompt="Review backend Java standards for PR #<NUMBER>. Files: <java-backend file-list from dotcms-file-classifier>", description="Java backend review")
```

**For Config/Docs/CI/Docker changes** in the `out-of-scope` bucket: these are not reviewed by the specialized agents. If a PR is entirely out-of-scope it SKIPs (see Stage 1-3).

### Stage 5: Consolidate Agent Results

**When multiple specialized agents were invoked:**

1. **Collect** all agent outputs
2. **Merge** findings by severity:
   - Critical Issues 🔴 (95-100): Must fix before merge
   - Important Issues 🟡 (85-94): Should address
   - Quality Issues 🔵 (75-84): Nice to have
3. **Remove duplicates**: If multiple agents flag the same issue, keep the highest confidence score
4. **Organize** by domain section (TypeScript Types, Angular Patterns, Tests, Styling, Backend Java)
5. **Calculate** overall statistics and recommendation

### Stage 6: Self-Validation Checklist

**Before outputting the review, verify:**

1. **File Existence**: Every file mentioned in findings exists in the PR diff
2. **Line Number Accuracy**: All line references are within the actual changed line ranges
3. **Domain Matching**: Review lens matches the actual file types changed
4. **Agent Scope**: Each agent only reported issues in their domain
5. **Completeness**: All significant changes are addressed (no major files skipped)
6. **Consistency**: Recommendations don't contradict each other (across agents)
7. **Evidence**: Every finding cites specific files and line numbers
8. **No duplicates**: Same issue not reported by multiple agents

**If validation fails**, re-analyze before presenting to the user.

### Stage 7: Structured Output

```markdown
# PR Review: #<NUMBER> - <TITLE>

## Summary
[2-3 sentence overview of what changed and overall quality assessment]

**Files Changed**: <count> reviewable files (<frontend count> frontend, <backend count> backend Java)
**Review Decision**: REVIEW
**Domains**: <Frontend | Backend | Frontend + Backend>
**Risk Level**: <Low|Medium|High>

## Risk Assessment

**Security**: <None|Low|Medium|High> - [explanation if not None]
**Breaking Changes**: <None|Potential|Confirmed> - [explanation if not None]
**Performance Impact**: <None|Low|Medium|High> - [explanation if not None]
**Test Coverage**: <Good|Partial|Missing> - [explanation]

---

## Frontend Findings
[Only if frontend files changed - consolidate from specialized agents]

### TypeScript Type Safety
[From dotcms-typescript-reviewer agent]

#### Critical Issues 🔴 (95-100)
[Type safety violations, raw generics, unsafe casts]

#### Important Issues 🟡 (85-94)
[Missing type guards, weak types, null safety]

#### Quality Issues 🔵 (75-84)
[Type improvements, better generics]

### Angular Patterns
[From dotcms-angular-reviewer agent]

#### Critical Issues 🔴 (95-100)
[Legacy syntax, missing standalone, memory leaks]

#### Important Issues 🟡 (85-94)
[OnPush, subscriptions, component structure]

#### Quality Issues 🔵 (75-84)
[Pattern improvements, optimizations]

### Test Quality
[From dotcms-test-reviewer agent]

#### Critical Issues 🔴 (95-100)
[Wrong Spectator usage, missing detectChanges]

#### Important Issues 🟡 (85-94)
[Coverage gaps, poor mocking, async issues]

#### Quality Issues 🔵 (75-84)
[Test organization, clarity]

### Styling Standards
[From dotcms-scss-html-style-reviewer agent — only if .scss/.css/.html files changed]

#### Critical Issues 🔴 (95-100)
[BEM violations, hardcoded colors/spacing, ::ng-deep misuse]

#### Important Issues 🟡 (85-94)
[Unused classes, missing CSS variables, nesting depth exceeded]

#### Quality Issues 🔵 (75-84)
[Selector improvements, mixin usage, PrimeNG theming patterns]

---

## Backend Findings
[Only if backend Java/POM/OpenAPI files changed - from dotcms-java-backend-reviewer agent]

### Backend Java

#### Critical Issues 🔴 (95-100)
[In-place mutation of a cache-returned Contentlet (corrupts shared cache — copy via new Contentlet(original.getMap()) first), System.out/getProperty/getenv, dependency version outside bom/application/pom.xml, @Schema type mismatch, OpenAPI not regenerated, hardcoded secrets, SQL/Lucene built by concatenating user input (use DotConnect.addParam bound params), unsanitized uploaded file name / path traversal (FileUtil.sanitizeFileName / isValidFilePath), unvalidated input, sensitive data logged]

#### Important Issues 🟡 (85-94)
[New/changed endpoint missing @Operation summary+description / operationId / @Parameter descriptions (undocumented = unusable by AI agents), new endpoint shipped without a Karate .feature or Postman collection test, REST Page API response changed without mirroring the GraphQL page response (or vice versa), Direct instantiation instead of APILocator, per-item permission loop instead of filterCollection, new Oracle/MySQL/MSSQL engine branch or dialect SQL (PostgreSQL is the only supported DB — write Postgres-only), wrong DB annotation (writes under @CloseDBIfOpened / no @WrapInTransaction), swallowed exception on a @WrapInTransaction write path, both DB annotations stacked, manual connection lifecycle inside an annotated method, single @WrapInTransaction wrapping a large batch loop (should use manual chunked commits), repeated/defensive File.exists() or disk stats on hot paths instead of the cached FileMetadataAPI (distributed FS round-trips block threads), Config.getProperty called inside a loop (hoist/cache via Lazy or constructor), new integration test not registered in a @SuiteClasses suite (silently never runs), respectFrontendRoles=true in internal/system code (leaks access via CMS Anonymous role), swallowed/raw exceptions, unparameterized DotConnect, missing null guards]

#### Quality Issues 🔵 (75-84)
[Advisory (non-blocking) signal that the PR couples to a third-party library — recommend wrapping it behind a dotCMS abstraction/DTO (anti-corruption layer), Raw generics, missing @Override/@Nullable, mutable get/set POJO for a new data carrier (prefer a record, or Immutables @Value.Immutable), brace-less if/for/while scopes (always use braces), prefer CDI injection over manual instantiation for new beans, progressive-enhancement opportunities on changed lines]

---

## Approval Recommendation

**✅ Approve** | **⚠️ Approve with Comments** | **❌ Request Changes**

[Clear rationale based on findings above]

**Statistics**:
- Total Critical Issues: <count>
- Total Important Issues: <count>
- Total Quality Issues: <count>

**Next Steps**:
- [Actionable items if changes needed]
- [Or confirmation message if approved]
```

## Error Handling

If PR fetch fails:
- Verify PR number is valid: `gh pr list --limit 100`
- Check if PR is from a fork (may need different permissions)
- Suggest: "Unable to fetch PR #<number>. Does it exist in this repo?"

If no files changed:
- This shouldn't happen, but if it does: "PR #<number> appears to have no changed files. This may be a merge commit or empty PR."

If unable to classify domain:
- Default to **Multi-Domain Review** and analyze all files
- Flag unusual file types for user attention

## Examples

**Example 1: Frontend-Only PR**
```
Files changed: 3 TypeScript components, 2 SCSS files, 1 spec file
Decision: REVIEW (frontend buckets non-empty)
Output: Focuses on Angular patterns, component structure, testing, styling
```

**Example 2: Backend-Only PR**
```
Files changed: 8 Java files, 1 pom.xml, 1 openapi.yaml
Decision: REVIEW (java-backend bucket non-empty)
Output: Java backend review — Config/Logger, BOM versions, @Schema/OpenAPI, exceptions, security
```

**Example 3: Mixed PR (Full-stack)**
```
Files changed: 8 Java files, 3 TypeScript files, 1 docker-compose.yml
Decision: REVIEW (both frontend and java-backend buckets non-empty; docker-compose is out-of-scope)
Output: Frontend Findings (TypeScript) + Backend Findings (Java); docker-compose not reviewed
```

**Example 4: Docs/CI-Only PR (Skipped)**
```
Files changed: 2 markdown docs, 1 GitHub Actions workflow
Decision: SKIP (all files out-of-scope)
Output: "PR has no reviewable frontend or backend code. Skipping review."
```


Use this as your **single entry point** for all PR reviews.

## Tips for Best Results

- Run after PR is updated: `/review <NUMBER>` again to see if issues were addressed
- For large PRs (50+ files), Claude may need to focus on specific areas - you can guide with: "Focus the review on security concerns" or "Check test coverage especially"
- If the PR is draft or WIP, mention it so review adjusts expectations
- For urgent reviews, add: "This is blocking deployment, prioritize critical issues only"

## Skill Metadata

- **Author**: Generated from usage insights analysis
- **Last Updated**: 2026-07-20
- **Replaces**: dotcms-code-reviewer-frontend
- **Scope**: Frontend (Angular/TypeScript/SCSS/tests) + Backend Java (Maven/REST/OpenAPI)
- **Dependencies**: `gh` CLI, access to repository

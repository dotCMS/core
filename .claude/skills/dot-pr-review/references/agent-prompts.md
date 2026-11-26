# Prompt Templates

Read at Stage 4, after `agents.md` built the bundles and slices. Local mode uses the same templates with the wording changes in `local-mode.md`.

Every template below is incomplete on its own: the **Inputs block** below goes first, and the **Crash-Safe Findings Sidecar + Turn Budget** block from `sidecar-and-budget.md` is appended verbatim to each one, with that agent's `<SIDECAR>`, `<MAX_TURNS>` and `<STOP_AT>` substituted. An agent launched without it has no crash protection and no stop rule.

`<slice>` is the agent's own `<RUN_DIR>/slices/<agent>-<i>.patch`. A file list in a template is pasted as paths, one per line; a diff or a standards doc is never pasted, only its path.

## The Inputs block (prepend to every prompt)

```
Read each of these files once, at the start, and never again:
- your diff slice: <slice>
- existing feedback on this PR (do not re-report it; add only what is new): <RUN_DIR>/bundles/existing-feedback.md
- the files named under <standards> or <acceptance-criteria> below, when there are any
Do not run `gh pr diff` and do not fetch this PR again: the change is in these files.

The PR head is checked out at <HEAD_DIR>. Read full files from <HEAD_DIR>/<path> and run git as `git -C <HEAD_DIR>`, but report repo-relative paths (`<path>:<line>`), never `<HEAD_DIR>/...`. Cite line numbers as they appear in the FILE, never a diff or hunk offset.
```

## Templates

**dotcms-pr-code-reviewer** (one per instance):

```
Review frontend code for PR #<N> '<TITLE>'.

Your assigned files:
<file-list>

PR diff for your files: <slice>

<standards>
Bundle: <RUN_DIR>/bundles/frontend-standards.md, the full text of ANGULAR_STANDARDS.md, TYPESCRIPT_STANDARDS.md and STYLING_STANDARDS.md. Read it once, first.
</standards>

Review Angular patterns, TypeScript type safety, and SCSS/HTML styling against the <standards> bundle. Do NOT read the docs under docs/ — the bundle holds them in full. Read <HEAD_DIR>/core-web/** for full file context when the diff alone is insufficient.

In addition to the <standards> bundle, apply the **Form Structure — Global Classes** checklist built into your agent definition (`.form` on the form tag, `.field` grouping per control, clean labels with no typography classes, `dotFieldRequired` on labels of required controls, `.p-field-hint` / `.p-field-error`). Those rules are NOT in STYLING_STANDARDS.md — the absence of a rule there does not mean it is optional. If an assigned `.html` file contains a `<form>`, also Read the paired component `.ts` to resolve which controls declare `Validators.required`.
```

**dotcms-pr-test-reviewer** (one per instance):

```
Review test quality for PR #<N> '<TITLE>'.

Test files:
<test-file-list>

PR diff for test files: <slice>

<testing-rules>
Bundle: <RUN_DIR>/bundles/testing-rules.md, the full text of TESTING_REVIEW_RULES.md. Read it once, first.
</testing-rules>

Review Spectator patterns, coverage, and test quality against the <testing-rules> bundle. Do NOT read docs/frontend/TESTING_REVIEW_RULES.md — the bundle holds it in full.
```

**dotcms-pr-historical-reviewer**:

```
Review historical context for PR #<N> '<TITLE>'.

Files changed:
<full-file-list>

PR diff (for identifying changed lines): <slice>

Run git blame and git log on changed lines (`git -C <HEAD_DIR>`), search for past PRs that touched these files, and fetch review comments from the 2-3 most relevant past PRs. Flag regressions, recurrences of known issues, and churn hotspots.
```

**dotcms-pr-silent-failure-hunter**:

```
Review error handling for PR #<N> '<TITLE>'.

TypeScript files with error-handling changes:
<ts-file-list>

PR diff for those files: <slice>

Flag empty catches, swallowed observables, unjustified fallbacks, and error handlers without user feedback or logging.
```

**dotcms-pr-comment-analyzer**:

```
Review code comments for PR #<N> '<TITLE>'.

Files with comment changes:
<file-list-with-comment-changes>

PR diff for those files: <slice>

Check added or modified comments for factual accuracy, comment rot, stale TODOs, and JSDoc correctness.
```

**dotcms-pr-logic-reviewer**:

```
Review behavioral correctness for PR #<N> '<TITLE>'.

Logic-bearing files to verify (read the FULL files, not just the diff):
<logic-file-list>   # stores, components, services, utilities/helpers, mappers, guards, resolvers, interceptors, pipes

PR diff (entry points only — shows what changed): <slice>

<acceptance-criteria>
<RUN_DIR>/bundles/acceptance-criteria.md, or the literal token NO_LINKED_ISSUE when there is no linked issue
</acceptance-criteria>

Trace each unit's logic end-to-end and verify it against the acceptance criteria above.
- Reactive units (stores/components): build the write graph (action → effect → patchState → state) and the read graph (state → computed/selector → template) and trace each AC through them.
- Functional units (services, utilities, mappers, guards, resolvers, interceptors, pipes): trace input → transformation steps → output, and enumerate edge cases (null/empty/boundary/unexpected shape) checking each is handled.
Cite file:line for every hop. Report only findings you can ground in a traced path. If ACCEPTANCE_CRITERIA is NO_LINKED_ISSUE, skip AC-traceability and report pure logic defects only.
```

**dotcms-pr-java-code-reviewer** (one per instance):

```
Review backend Java for PR #<N> '<TITLE>'.

Your assigned files:
<java-file-subset>   # plus any JSPs: review them for display regressions

PR diff for your files: <slice>

<standards>
Bundle: <RUN_DIR>/bundles/java-code-standards.md, the full text of JAVA_STANDARDS.md, CONFIGURATION_PATTERNS.md, VIRTUAL_THREADS.md and PROGRESSIVE_ENHANCEMENT.md. Read it once, first.
</standards>

Review general Java correctness and adherence to the <standards> bundle. Do NOT read those docs again. Read <HEAD_DIR>/dotCMS/** for full file context when the diff alone is insufficient. When the diff touches a topic in your agent definition's "Read on demand" table, Read that doc before reporting on it.

Do NOT report findings owned by the specialist reviewers running in parallel: REST endpoint contracts, SQL/transactions/caching, authentication/authorization/injection, or test quality. Note them and move on.
```

**dotcms-pr-java-api-reviewer**:

```
Review REST endpoint design for PR #<N> '<TITLE>'.

Resource classes, Form/View objects, and exception mappers:
<api-file-subset>

PR diff for those files: <slice>

All files changed in the PR (paths only — used to check whether openapi.yaml was regenerated):
<full-changed-file-list>

<standards>
Bundle: <RUN_DIR>/bundles/java-api-standards.md, the full text of REST_API_PATTERNS.md and the rest/README.md and rest/CLAUDE.md guides. Read it once, first.
</standards>

Verify WebResource context init, versioning and package placement, required annotations (@ApplicationScoped, @NoCache, method-level @Produces, @Consumes only with a body), ResponseEntityView wrapping, boundary input validation, Form/View object usage, exception mapping, status-code semantics, OpenAPI/@Schema rules, openapi.yaml sync, and response-shape backward compatibility — all against the <standards> bundle.

Every NEW endpoint must ship a complete OpenAPI definition (@Tag registered in DotRestApplication, meaningful @Operation, @ApiResponses covering every status the method can return, @Parameter/@RequestBody, @Schema matching the real return type) plus a regenerated openapi.yaml. Missing or incomplete documentation on a new endpoint is Important, never Quality.

Read the full resource class, not only the diff: a missing context init or an undocumented exit path is proven by what is absent from the method, which a diff cannot show.
```

**dotcms-pr-java-persistence-reviewer**:

```
Review the data layer for PR #<N> '<TITLE>'.

Query-bearing classes, SQL files, and upgrade tasks:
<persistence-file-subset>

PR diff for those files: <slice>

<standards>
Bundle: <RUN_DIR>/bundles/java-persistence-standards.md, the full text of DATABASE_PATTERNS.md. Read it once, first.
</standards>

Verify parameter binding (every value through addParam — concatenated SQL is Critical without exception), LocalTransaction wrapping of multi-statement writes, pagination on unbounded queries, batch operations, null-safe result mapping, cache/database consistency on write paths, and idempotent upgrade tasks.

Read the full method around every query — a transaction boundary is established outside the line that runs the SQL.
```

**dotcms-pr-java-security-reviewer** (exactly 1 instance):

```
Review backend security for PR #<N> '<TITLE>'.

ALL Java files in this PR (you are not given a subset — authorization bugs live between files):
<full-java-bucket>

Changed .properties files:
<properties-files>

Changed JSPs:
<jsp-files, or "none">

PR diff: <slice>

<standards>
Bundle: <RUN_DIR>/bundles/java-security-standards.md, the full text of SECURITY_BACKEND.md and SECURITY_PRINCIPLES.md. Read it once, first.
</standards>

For every entry point the PR adds or changes, trace caller-controlled input to each sensitive sink and verify: authentication resolves a real user, the permission check matches the operation's level, admin operations use the documented admin gate, no injection sink is reachable, output is encoded for its context, no secret or PII is logged, security config has a safe (closed) default, and error responses disclose nothing internal.

If the PR changes a DWR class (`*Ajax.java`) or a Struts action, apply "Legacy surfaces: DWR and Struts" from your agent definition. On both surfaces the exception message reaches the page. For each changed method, list the exceptions it can throw and the message each one carries, and check that a missing id and a forbidden id get the same answer.

If the PR changes JSPs, check that every value written to the page is encoded for its context (HTML body, attribute, JavaScript, URL), and grep the sibling JSP and JS files for the same values: a fix in one page that misses its sibling is incomplete. `UtilMethods.webifyString` replaces only the double quote; it is not HTML encoding.

Report only findings with a traced source → sink path, naming every hop. A missing permission check has no keyword to grep — verify by reading the method, and treat absence as the finding.
```

**dotcms-pr-java-test-reviewer** (one per instance):

```
Review Java test quality for PR #<N> '<TITLE>'.

Test files:
<java-test-file-subset>

Production classes they cover (for judging adequacy):
<paired-production-files>

PR diff: <slice>

<standards>
Bundle: <RUN_DIR>/bundles/java-test-standards.md, the full text of BACKEND_UNIT_TESTS.md and INTEGRATION_TESTS.md. Read it once, first.
</standards>

If the test files include anything under test-karate/ or dotcms-postman/, or the PR adds a REST endpoint, Read docs/testing/API_TESTING.md and check the API tests against it (new tests go in Karate; Postman is legacy).

FIRST, for every NEW integration test class: Grep dotcms-integration/src/test/java/com/dotcms/MainSuite*.java and Junit5Suite*.java for the class name. A class absent from every suite compiles but never runs in CI — report it as Critical before anything else.

Then judge whether each test is the RIGHT test: with the production change reverted, would it fail? Is it at the right level (SQL, permissions, transactions and REST wiring need an integration test, not mocks)? Flag tautological tests (stubs echoed back, expected values built by the code under test, verify() of a call the test itself made, getter/setter round-trips) and say that deleting one is an acceptable fix. Then: coverage of the branches this PR added, a regression test for any bug fix, and no order dependence, wall-clock, or network flake sources.
```

**dotcms-pr-ac-validator** (only when `RUN_AC_VALIDATOR` is true):

```
Validate acceptance-criteria coverage for PR #<N> '<TITLE>' (linked issue #<ISSUE_NUMBER>).

Changed files (read the FULL files, not just the diff):
<full-file-list>

PR diff (to know what changed): <slice>

<acceptance-criteria>
<RUN_DIR>/bundles/acceptance-criteria.md (never NO_LINKED_ISSUE: this agent runs only with ACs)
</acceptance-criteria>

Verify coverage in BOTH directions and return your standard AC Coverage Report:
- AC → code: for each AC assign COVERED / PARTIAL / MISSING / UNTESTABLE with cited evidence. Before declaring MISSING, Grep/Glob beyond the changed files to confirm the behavior does not already live elsewhere.
- code → AC: classify every meaningful change that maps to no AC as ENABLER / SCOPE_CREEP / HIDDEN_REQUIREMENT / RISK.
Also flag AC regressions and per-AC test presence (⚠ NO_TEST). Do NOT emit style/type/test-quality findings — other agents own that.
```

**dotcms-pr-test-case-designer** (only when `RUN_TEST_DESIGNER` is true):

```
Design the test plan for PR #<N> '<TITLE>' (linked issue #<ISSUE_NUMBER>, type <issueType.name>).

<acceptance-criteria>
<RUN_DIR>/bundles/acceptance-criteria.md
</acceptance-criteria>

Production files (read them in FULL; every decision point is a partition source):
<changed non-test files>

Test files in this PR (may be empty):
<test bucket + java_test bucket, or "none">

PR diff: <slice>

For each AC: find the input variables (from the AC and from the changed code's decision points), define equivalence partitions (valid/invalid) and boundary values, derive the test cases (every valid class covered, every invalid class in its own case, every boundary, pairwise where variables combine), and map each case to an existing test in the PR or the repo: COVERED / PARTIAL / NOT COVERED / UNSPECIFIED. Flag partitions the code distinguishes that no AC mentions. Return your standard Test Design report. A test plan only: no test code, no line-level findings.
```

**dotcms-pr-ci-reviewer** (one per instance, only when `RUN_CI`):

```
Review the CI changes of PR #<N> '<TITLE>'.

Your assigned files (workflows, composite actions, and their scripts, tests and READMEs):
<ci-file-subset>

PR diff for your files: <slice>

All files changed in the PR (paths only):
<full-changed-file-list>

How the changed workflows are exercised (CI_GATE_EVIDENCE):
<one line per changed workflow: its triggers, and the check of this PR that ran it or "first runs on <event>">

<standards>
Bundle: <RUN_DIR>/bundles/ci-standards.md, CICD_PIPELINE.md and .github/workflows/README.md as they were at the base. Read it once, first.
</standards>

Review these files until your agent definition's "Done when" holds.
```

**dotcms-pr-maven-reviewer** (exactly 1 instance, only when `RUN_MAVEN`):

```
Review the Maven build changes of PR #<N> '<TITLE>'.

Changed POMs and Maven files:
<maven-file-list>

PR diff for those files: <slice>

All files changed in the PR (paths only; a workflow may pass -P<profile> to these POMs):
<full-changed-file-list>

Effective POM, base against head, for the changed modules: <RUN_DIR>/bundles/effective-pom.diff
(a single line "NOT COMPUTED: <reason>" means you resolve inheritance from the POM chain yourself)

<standards>
Bundle: <RUN_DIR>/bundles/maven-standards.md, MAVEN_BUILD_SYSTEM.md as it was at the base. Read it once, first.
</standards>

Review these files until your agent definition's "Done when" holds.
```

**dotcms-pr-docs-reviewer** (one per instance, only when `RUN_DOCS`):

```
Review the documentation changes of PR #<N> '<TITLE>'.

Your assigned files (docs, agent-instruction files, READMEs next to changed code):
<docs-file-subset>

PR diff for your files: <slice>

All files changed in the PR (paths only; a doc may describe code this PR changes):
<full-changed-file-list>

Review these files until your agent definition's "Done when" holds.
```

**Vendored imports** (`VENDORED`): add to the ci and maven reviewers' prompts: "This PR imports upstream source. Check that the import builds and is tested the way the rest of core is: the module is wired into the build, dependency versions come from bom/application/pom.xml only, no workflow step is skipped or made non-blocking for the imported code, and no new secret, token or network fetch is introduced. Behavior the upstream already had is PRE-EXISTING UPSTREAM."

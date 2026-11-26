# Prompt Templates

Read at Stage 4, after `agents.md` built the bundles and slices. Local mode uses the same templates with the wording changes in `local-mode.md`.

A prompt is three parts: the **Inputs block** below, the agent's template, and the **sidecar line** from `sidecar-and-budget.md` with that instance's sidecar path. Without the sidecar line the agent writes to its fallback path, outside the run, and Stage 4b reads it as `MISSING`.

The templates carry inputs only. Each agent's rules, method and output live in its own file in `.claude/agents/`, so a rule is changed there, once, and never restated in a prompt.

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

Review these files until your agent definition's rules hold.
```

**dotcms-pr-test-reviewer** (one per instance):

```
Review test quality for PR #<N> '<TITLE>'.

Test files:
<test-file-list>

PR diff for test files: <slice>

<testing-rules>
Bundle: <RUN_DIR>/bundles/testing-rules.md, the full text of TESTING_REVIEW_RULES.md and right-test.md. Read it once, first.
</testing-rules>

Review these files until your agent definition's rules hold.
```

**dotcms-pr-historical-reviewer**:

```
Review historical context for PR #<N> '<TITLE>'.

Files changed:
<full-file-list>

PR diff (for identifying changed lines): <slice>

Review these files until your agent definition's rules hold.
```

**dotcms-pr-silent-failure-hunter**:

```
Review error handling for PR #<N> '<TITLE>'.

TypeScript files with error-handling changes:
<ts-file-list>

PR diff for those files: <slice>

Review these files until your agent definition's rules hold.
```

**dotcms-pr-comment-analyzer**:

```
Review code comments for PR #<N> '<TITLE>'.

Files with comment changes:
<file-list-with-comment-changes>

PR diff for those files: <slice>

Review these files until your agent definition's rules hold.
```

**dotcms-pr-logic-reviewer**:

```
Review behavioral correctness for PR #<N> '<TITLE>'.

Logic-bearing files to verify:
<logic-file-list>   # stores, components, services, utilities/helpers, mappers, guards, resolvers, interceptors, pipes

PR diff (entry points only — shows what changed): <slice>

<acceptance-criteria>
<RUN_DIR>/bundles/acceptance-criteria.md, or the literal token NO_LINKED_ISSUE when there is no linked issue
</acceptance-criteria>

Review these files until your agent definition's rules hold.
```

**dotcms-pr-java-code-reviewer** (one per instance):

```
Review backend Java for PR #<N> '<TITLE>'.

Your assigned files:
<java-file-subset>   # plus any JSPs

PR diff for your files: <slice>

<standards>
Bundle: <RUN_DIR>/bundles/java-code-standards.md, the full text of JAVA_STANDARDS.md, CONFIGURATION_PATTERNS.md, VIRTUAL_THREADS.md and PROGRESSIVE_ENHANCEMENT.md. Read it once, first.
</standards>

Review these files until your agent definition's rules hold.
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

Review these files until your agent definition's rules hold.
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

Review these files until your agent definition's rules hold.
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

Review these files until your agent definition's rules hold.
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
Bundle: <RUN_DIR>/bundles/java-test-standards.md, the full text of BACKEND_UNIT_TESTS.md, INTEGRATION_TESTS.md and right-test.md. Read it once, first.
</standards>

Review these files until your agent definition's rules hold.
```

**dotcms-pr-ac-validator** (only when `RUN_AC_VALIDATOR` is true):

```
Validate acceptance-criteria coverage for PR #<N> '<TITLE>' (linked issue #<ISSUE_NUMBER>).

Changed files:
<full-file-list>

PR diff (to know what changed): <slice>

<acceptance-criteria>
<RUN_DIR>/bundles/acceptance-criteria.md (never NO_LINKED_ISSUE: this agent runs only with ACs)
</acceptance-criteria>

Return your AC Coverage Report.
```

**dotcms-pr-test-case-designer** (only when `RUN_TEST_DESIGNER` is true):

```
Design the test plan for PR #<N> '<TITLE>' (linked issue #<ISSUE_NUMBER>, type <issueType.name>).

<acceptance-criteria>
<RUN_DIR>/bundles/acceptance-criteria.md
</acceptance-criteria>

Production files:
<changed non-test files>

Test files in this PR (may be empty):
<test bucket + java_test bucket, or "none">

PR diff: <slice>

Return your Test Design report.
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

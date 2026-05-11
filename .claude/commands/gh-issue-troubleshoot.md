---
name: gh-issue-troubleshoot
description: Fix a dotCMS GitHub issue end-to-end — fetches the issue, researches the codebase, proposes a concrete code fix with before/after diffs, iterates on developer feedback, then applies the approved fix to a new git branch.
argument-hint: <issue-number|issue-url>
allowed-tools: Bash(gh issue view:*), Bash(gh api:*), Bash(gh auth status:*), Bash(gh repo view:*), Bash(git checkout -b:*), Bash(git checkout:*), Bash(git pull:*), Bash(git show-ref:*), Bash(git diff:*), Bash(git log:*), Bash(git blame:*), Bash(./mvnw *), Read, Edit, Write, Grep, Glob, Agent, WebFetch
---

**Input:** $ARGUMENTS

You are a senior dotCMS engineer fixing a GitHub bug end-to-end. Follow these steps in order. Do not skip steps.

---

## Step 0 — Environment check

Run both commands:
```
gh auth status
gh repo view --json nameWithOwner --jq '.nameWithOwner'
```

Stop with a clear error message if:
- `gh` is not authenticated
- The repo is not `dotCMS/core`

---

## Step 1 — Parse issue number

Extract the issue number from $ARGUMENTS:
- `12345` → use directly
- `#12345` → strip the `#`
- `https://github.com/dotCMS/core/issues/12345` → extract `12345`
- `https://github.com/dotCMS/core/issues/12345#issuecomment-XXXXXXX` → extract `12345` as issue, note the comment ID
- `https://github.com/dotCMS/core/pull/12345` → stop with a clear error: "That looks like a PR URL, not an issue. Provide an issue number or issues/ URL."
- Any other unrecognised format → stop with: "Could not parse an issue number from '<input>'. Provide a number, #N, or a dotCMS/core issues URL."

---

## Step 2 — Fetch the issue

```
gh issue view <N> --repo dotCMS/core --json number,title,body,labels,comments
```

If the command fails (non-zero exit code), stop immediately with a clear error:
- **404 / "Could not resolve to an Issue"** → "Issue #N not found in dotCMS/core. Verify the number or check your GitHub permissions."
- **Any other error** → print the raw error from `gh` and stop.

If a comment ID was present in the input, also fetch it:
```
gh api repos/dotCMS/core/issues/comments/<COMMENT_ID>
```
If this call fails, warn the developer ("Could not fetch comment <COMMENT_ID> — proceeding with issue body only") and continue without it.

Treat that comment as the **primary symptom** — it often contains QA failure details more specific than the issue body.

Read carefully. Identify:
- Backend (Java), Frontend (Angular), or Full-stack?
- What feature/component is affected?
- Any stack traces, error messages, or reproduction steps?

---

## Step 3 — Research the codebase

Use the Agent tool to spawn the `dotcms-code-researcher` sub-agent.

Pass it: issue number, title, full body (and QA comment if present), inferred issue type (Bug), and the repo root path.

Wait for the result. Extract:
- **Entry Point** — file + line number
- **Hypothesis** — what is broken and why
- **Relevant Files** — list of files to read
- **Suggested Fix Approach** — numbered steps
- **Test Command** — Maven or nx command

---

## Step 3b — Consult dotCMS documentation (when needed)

**Only run this step if** the issue or the researcher's output involves a dotCMS feature whose expected behavior, API contract, or configuration is not fully clear from reading the code alone.

Signals that docs are needed:
- The issue references a dotCMS feature by name (Workflows, Experiments, Pages API, Push Publishing, etc.)
- The researcher's hypothesis mentions an API contract that could be misunderstood
- The fix approach involves a dotCMS extension point, event system, or plugin API
- The bug is about incorrect behavior that requires knowing what the *correct* behavior should be

**How to look up docs:**

Start with the table of contents to find the right page:
```
WebFetch: https://dev.dotcms.com/docs/table-of-contents
```

Then fetch the specific page(s) relevant to the issue. URL pattern: `https://dev.dotcms.com/docs/<topic-slug>`

Extract from the docs:
- The expected behavior or API contract being violated
- Any configuration flags relevant to the fix
- Known limitations or gotchas that affect the fix approach

If the docs clarify something that changes the researcher's hypothesis, update your understanding before proceeding.

**If the code is self-evident, skip this step entirely.**

---

## Step 4 — Read files and generate fix proposal

Read the relevant files from Step 3. Trace the code path to the defect location.

Then generate a **concrete fix** — actual code changes, not descriptions.

Present the proposal in this exact format:

---

### Fix Proposal: #<N> — <Issue Title>

**Confidence:** HIGH | MEDIUM | LOW — one sentence justifying the rating.
> - **HIGH**: entry point is certain, root cause is directly visible in the code, fix mirrors an existing pattern
> - **MEDIUM**: hypothesis is well-supported but one or more relevant files could not be fully traced, or the fix touches a non-trivial code path
> - **LOW**: entry point was inferred, key files are generated/cached/dynamic, or the fix is speculative

**Root cause:** One paragraph describing what is broken and why, including the mechanism that causes the reported symptom.

**Changes:**

`path/to/File.java`
```diff
- old line(s)
+ new line(s)
```
*Why:* one sentence explaining this specific change.

`path/to/Other.java` *(if needed)*
```diff
- old line(s)
+ new line(s)
```
*Why:* one sentence.

**Test to verify:**
```
<test command from researcher>
```

---

> **Type `approve` to apply this fix, or describe what to change:**

---

## Step 5 — Feedback loop

After presenting the proposal:

- If the developer types **`approve`** → proceed to Step 6
- If the developer provides any other text → treat it as feedback, revise the proposal accordingly, and re-present it using the same format from Step 4
- Repeat until approved. There is no round limit.

**Do NOT apply any changes to the filesystem until you receive `approve`.**

---

## Step 6 — Apply the fix

### 6a. Pull latest main and create a branch

First, ensure the local `main` branch is up to date:

```
git checkout main
git pull origin main
```

Where `<short-slug>` is 3–5 words from the issue title, lowercased, hyphenated.
Examples:
- "NPE in workflow transitions" → `issue-34901-workflow-npe`
- "Content editor fails to save" → `issue-34902-content-editor-save`
- "REST endpoint returns 500 on missing param" → `issue-34903-rest-missing-param-500`

Check whether the branch already exists before creating it:

```
git show-ref --verify --quiet refs/heads/issue-<N>-<short-slug>
```

- If the branch **does not exist** → `git checkout -b issue-<N>-<short-slug>`
- If the branch **already exists** → `git checkout issue-<N>-<short-slug>` (switch to it and continue applying changes on top)

### 6b. Apply changes

Before applying, verify that every file path in the approved diff is within the repository root (no `..` traversal, no absolute paths outside the repo). Abort and alert the developer if any path resolves outside the repo.

Use Edit (for modifying existing files) or Write (for new files). Apply the approved diff exactly as proposed.

### 6c. Register new integration tests in the test suite

**Only run this step if any new test file was written in `dotcms-integration/src/test/java/`.**

For each new test class created:

1. **Determine the JUnit version** by reading the new file:
   - Imports `org.junit.platform.*` or `org.junit.jupiter.*` → **JUnit 5**
   - Imports `org.junit.Test` or `org.junit.runner.*` → **JUnit 4**

2. **Pick the target suite file:**

   - **JUnit 5** → `dotcms-integration/src/test/java/com/dotcms/Junit5Suite1.java`
     - Add a `@SelectClasses` entry and the corresponding `import` statement.

   - **JUnit 4** → one of `MainSuite1a`, `MainSuite1b`, `MainSuite2a`, `MainSuite2b`, `MainSuite3a`
     (all in `dotcms-integration/src/test/java/com/dotcms/`).
     - Read each suite file to count `@SuiteClasses` entries.
     - Add the new class to the **suite with the fewest entries** to keep load balanced.
     - Add the corresponding `import` statement at the top of that suite file.

3. **Apply the suite edit** using Edit. Add:
   - The `import com.fully.qualified.NewTestClass;` line in alphabetical order with the other imports.
   - The `NewTestClass.class,` entry at the end of the `@SuiteClasses` / `@SelectClasses` list, before the closing `})`.

4. **Confirm** in the output: "Added `NewTestClass` to `<SuiteFileName>.java`."

If the test class package cannot be determined, warn and skip this step rather than guessing.

### 6d. Show the diff

```
git diff
```

Display the full diff output so the developer can review what was applied.

### 6e. Offer to run tests

Ask:
> **Run the test to verify the fix? (`y` / `n`)**
> `<test command from Step 3>`

If `y`, run the command and display its output.


---

## Step 7 — Summary

Print this block:

```
## Fix applied

- **Branch:** issue-<N>-<slug>
- **Files changed:** <list each file>
- **Tests:** passed / skipped / failed

### Suggested commit message

fix(<area>): <short description of what was fixed> (#<N>)

<One sentence: root cause and what the fix does>

Refs: #<N>
```

Where `<area>` follows conventional commits and matches the dotCMS domain:
`workflow`, `content-type`, `rest-api`, `experiments`, `ui`, `permissions`, `cache`, `osgi`, etc.

---

## dotCMS-specific rules to apply when generating the fix

When writing the fix, check for and apply these patterns:

- Use `UtilMethods.isSet(value)` for null/empty checks on strings and objects — never `value != null` alone
- Use `Logger.error(this, message, e)` — never `System.out.println` or swallowed exceptions
- Wrap multi-step DB operations in `HibernateUtil.startTransaction()` / `HibernateUtil.commitTransaction()`
- Use `Config.getStringProperty("KEY", "default")` — never `System.getProperty()`
- REST endpoints must call `webResource.init(...)` before any business logic
- Invalidate caches after write operations
- Use `@Override` on all overriding methods
- Add generics to raw types: `List<String>` not `List`
- Angular: use `@if` not `*ngIf`, `input()` signals not `@Input()` for new code

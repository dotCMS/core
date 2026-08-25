# Comment format, statuses, and lifecycle

The exact shape of the GitHub comment. Reproduce it verbatim — automation finds and chains plans by
the marker, and humans navigate by the fixed headings.

---

## The comment

````markdown
<!-- dotcms-post-merge-test-plan
pr: 12345
issues: 31904,31905
merge-sha: abcdef1
generator-version: v1
-->

## 🧪 Post-Merge Test Plan

**Source PR:** #12345
**Related issues:** #31904, #31905

### Ownership and status

| Responsibility | Developer | Status |
|---|---|---|
| Plan Reviewer — developer who implemented the fix | @fix-developer | Pending Review |
| Test Executor — independent developer | TBD | Not Started |

> The Plan Reviewer must validate scope, correctness, missing cases, and possible hallucinations
> before execution begins.

> This is a living test plan. The Plan Reviewer and Test Executor may add relevant test cases.
> Every added case must follow the existing format and receive a final result.

### Test summary

| Test Case Number | Scenario | Result | Notes |
|---|---|---|---|
| TC-001 | Verify the original issues no longer reproduce | Not Run Yet | |
| TC-002 | Verify adjacent behavior remains unchanged | Not Run Yet | |
| TC-003 | Verify the change does not introduce a permission regression | Not Run Yet | |

<details>
<summary>View full test plan</summary>

[Summary → Test Cases]

</details>
````

### Marker rules

Line 1 of the comment, always. Fields, in order:

| Field | Value |
|---|---|
| `pr` | Source pull request number |
| `issues` | Comma-separated, ascending, no spaces. Never empty. |
| `merge-sha` | The merge commit SHA — short form is fine, but be consistent |
| `generator-version` | `v1`. Bump only on a breaking format change. |

`pr` + `merge-sha` together are the **idempotency key**: the same plan is posted to several issues,
so deduplicate previous plans on that pair, and never post twice for the same pair.

### The summary table

Exactly four columns: `Test Case Number`, `Scenario`, `Result`, `Notes`.

`Notes` is the executor's column and is **always emitted empty**. It is where a `Failed`, `Blocked`
or `Not Applicable` result gets its required explanation — a sentence, a link to the issue filed for
a failure, or the reason a case turned out not to apply. Keep it to one line; anything longer
belongs in a follow-up comment that names the case ID.

> **Note on `Scenario`.** In this table it holds the case's **short name** (a readable sentence,
> as in the example above). In the full plan, `Scenario` is the classification enum
> (`Happy Path` / `Negative` / `Edge` / `Boundary`). Same word, two jobs — the table is for
> scanning, the field is for classification.

Every case appears in both the summary table and the full plan, including cases a developer adds
later.

---

## Status vocabularies

Use these exact strings. Nothing else is valid.

### Test-case Result

| Value | Meaning |
|---|---|
| `Not Run Yet` | Not executed. **The only value a generated plan may contain.** |
| `Passed` | Executed, behaved as expected |
| `Failed` | Executed, did not behave as expected |
| `Blocked` | Could not be executed (environment down, prerequisite missing). Not a failure. |
| `Not Applicable` | Turned out not to apply to this change |

> **Where results live.** The `Result` and `Notes` columns of the summary table, and nowhere else.
> Case blocks carry no result line, so the table and the plan can never disagree.

`Failed`, `Blocked`, and `Not Applicable` each require a short explanation in that row's **`Notes`**
cell. A `Failed` case should also link the issue filed for it. If the explanation needs more than a
line, put it in a follow-up comment naming the case ID and point at it from `Notes`.

### Plan Review Status

`Pending Review` → `Approved`

Only the Plan Reviewer sets `Approved`, and never merely because generation succeeded.

### Execution Status

`Not Started` → `In Progress` → `Completed` | `Completed With Failures` | `Blocked`

> **Where these live.** Both statuses appear **only** in the `Status` column of the ownership
> table — the reviewer's row carries the Plan Review Status, the executor's row carries the
> Execution Status. There is no separate Completion block; it duplicated the same two values.

---

## Lifecycle

```
Generated
   ↓
Pending Review
   ↓
Apply Changes (if necessary)
   ↓
Approved
   ↓
Execution In Progress
   ↓
Completed / Completed With Failures / Blocked
```

Approval does not lock the plan. A case added after approval takes the next sequential `TC-###`,
goes into both the summary and the full plan, starts `Not Run Yet`, and must be executed before
completion.

---

## What a generated plan always looks like

Before posting, confirm all of the following. If any fails, fix it and re-check — in `automated`,
never post a plan that fails a check, and never post nothing instead.

- The marker is on line 1 and every field is populated.
- `issues` is non-empty, and every listed issue is named in the `Issues:` field of at least one case.
- The summary table row count equals the number of cases in the full plan.
- Case IDs run `TC-001`, `TC-002`, … with no gaps or repeats.
- Each case sits in its own fenced block, with a blank line between consecutive blocks.
- Every `Result` cell in the summary table reads `Not Run Yet`, and every `Notes` cell is empty.
- No case block contains a `Type of Test` or `Result` line — every case here is manual, and the
  summary table is the single source of truth for results.
- The ownership table reads `Pending Review` for the reviewer and `Not Started` for the executor,
  with `Test Executor` as `TBD`.
- The plan body contains **only** `## Summary` and `## Test Cases` — no Assumptions, no Coverage by
  Issue, no Out of Scope, and no Completion block.
- The Summary says the cases have not been executed.

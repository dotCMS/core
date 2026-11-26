# Stages 5 and 6: consolidate and validate

Read after Stage 4b. Your own work runs both stages as written here; `local-mode.md` names its differences (the head check and anchors for `LOCAL`, and `SELF_TRIAGE`).

## Stage 5: Consolidate Findings

### 0. Head check before consolidating

The PR can move while the agents run. Before merging anything, re-read the head:

```bash
gh pr view <N> --repo dotCMS/core --json headRefOid,changedFiles
```

If `headRefOid` still equals `HEAD_SHA`, go on. If it moved: fetch the new head, move `HEAD_DIR` to it (`git -C <HEAD_DIR> checkout --detach <new sha>`), rebuild `changed-files.txt` and the slices with `diff-slices.sh --git <BASE_SHA> --head <new sha>`, and set `HEAD_SHA` to the new sha. Step 2 below re-baselines the findings against it: a finding whose evidence is gone from the new head (`STALE`) was fixed by the new commits and is dropped. Files the new head added or changed that no reviewer saw go into `COVERAGE_GAPS`, and the user is told the review covers the older head for those files.

### AC coverage stays out of the finding stream

Store the `dotcms-pr-ac-validator` report as `AC_COVERAGE` (coverage table, evidence, out-of-scope list, regression risks, verdict); Stage 7 renders it in its own section, and it skips the dedupe and sort below. Only the **MISSING** and the **contested** ACs cross into Stage 6. Its "Test?" column comes from `TEST_DESIGN` (below), per AC: ✅ when a case of that AC is COVERED, ⚠ when its best case is PARTIAL, ❌ when its cases are all NOT COVERED or UNSPECIFIED, and n/a when the test designer did not run (a Spike) or came back empty.

**Its table is an upper bound**: the ac-validator marks an AC COVERED when a named test and the code exist, without checking that the test can fail. Reconcile it with the other reviewers before building the AC table:
- An AC whose only backing tests the test reviewer or java-test reviewer flagged as tautological is at most PARTIAL.
- An AC on whose path the logic reviewer found a defect is at most PARTIAL.
- An AC the ac-validator calls COVERED while another reviewer's finding says it is not met is **contested**: all contested ACs go to one Sonnet validator (Stage 6, "Contested ACs"), which decides from the code and the tests, not from how many agents agree.

### Test design stays out of it too

Merge the `dotcms-pr-test-case-designer` sidecar rows with its text report (the sidecar wins per `TC-###`) and store the result as `TEST_DESIGN`: partitions, cases, gaps, unspecified-behavior questions, totals. Write it as markdown to `TEST_PLAN_PATH`, so the complete plan (every partition, every TC block) outlives the session; Stage 7 renders only the gaps.

| Linked issue | `TEST_PLAN_PATH` |
|---|---|
| In this repo (`ISSUE_NUMBER`) | `local/agents/<ISSUE_NUMBER>/pr-<N>-test-plan.md` (`mkdir -p` its folder), the issue's work folder |
| In `dotCMS/private-issues` (`PRIVATE_LINKED`) | `local/security/<ISSUE_NUMBER>/pr-<N>-test-plan.md` |
| None resolved | `local/code-review/pr-<N>-test-plan.md` (`mkdir -p local/code-review`) |
| Local mode, any issue | `<OUT_DIR>/local-<BRANCH_SLUG>-<SHORT_SHA>-test-plan.md`, next to the report |

### Line-level findings (every other agent)

1. **Collect** from the **union of the sidecars and the final reports**; the sidecars are the system of record. `merge-sidecars.sh` already wrote `<RUN_DIR>/merged/findings.jsonl` at the end of Stage 4 (exact duplicates and invalid lines dropped, each counted); run it again if Stage 4b added rows:

```bash
bash <SKILL_DIR>/scripts/merge-sidecars.sh --dir <SIDECAR_DIR> --out <RUN_DIR>/merged --roster <RUN_DIR>/roster.txt
```

   A finding in only one of an instance's sidecar and text report is still a finding; when both hold the same `file:line` and claim, the sidecar row wins (the text report may be truncated). A high invalid-line count means an agent ignored the row shape.

2. **Re-derive every anchor before the report.** Agents cite diff offsets as file lines (line 1348 of a 185-line file), so no line number is trusted as written:

```bash
bash <SKILL_DIR>/scripts/verify-anchors.sh --findings <RUN_DIR>/merged/findings.jsonl --base <BASE_SHA> --rev <HEAD_SHA> --root <CORE_REPO> --out <RUN_DIR>/merged/anchored.jsonl
```

   (local mode: `--worktree` instead of `--rev <HEAD_SHA>`). Use each finding's `anchor.line` from here on. `REANCHORED` moves the finding to the line its evidence is on. `PAST_EOF` with no match, `STALE` or `NO_FILE` means the cited code is not there: the finding goes to Stage 6 marked as unanchored, its validator re-checks it from the claim, and it is postable only if that check re-anchors it. `UNVERIFIABLE` (no evidence) stays in the report and is never posted. Then check:
   - Every file mentioned exists in the PR diff (local mode: in the Local Stage 2 file list).
   - `anchor.in_diff` true means the line is inside the changed ranges: **ADDED-BY-THIS-PR**. False means **PRE-EXISTING** (or **PRE-EXISTING UPSTREAM** on an unchanged vendored line). Record the origin on every finding; the report and the posting table show it.
   - Every entry carries the required fields.
3. **Dedupe**: when two agents flag the same `file:line` with similar claims, keep the highest-confidence one and note which agents agreed (`merge-sidecars.sh` prints `OVERLAP` for every line flagged with different claims). **Fold** a finding that exists only because of another (a missing test for a branch another finding says should not exist; a log line inside a catch another finding removes) into that finding, as one more point of it. Each merge saves a Stage 6 validator.
4. **Contradictions** stay as separate findings ("add a null check" next to "this can't be null"): the validator and the user decide.
5. **Flatten** into one list, sorted by severity (Critical → Important → Quality), then by confidence.
6. **Carry `COVERAGE_GAPS`** from Stage 4b untouched: it is reported, not filtered.

Filtering waits for Stage 6. Every finding keeps its `evidence` through Stages 6-8: Stage 7b cannot verify an anchor without it.

## Stage 6: Confidence Validation

Validation is **routed by domain**, and every route keeps scores of 80 or more:

- **`domain == Logic`** → a **Sonnet** validator (*Logic findings*, below): a multi-hop trace needs real reasoning, and Haiku rejects true ones.
- **`domain` is `CI`, `Maven` or `Docs`** → a **Sonnet** validator with its own template (*CI, Maven and Docs findings*, below). These findings rest on a second file the diff does not show (the caller of a removed input, the parent a module overrides, the code a doc describes), and many are no broken data-flow path, so the Logic rubric would score them as no defect.
- **Every other domain** (code, test, styling, silent-failure, comment, historical) → one tool-free Haiku per finding (below).

**At most 20 subagents run at once**; calls past 20 are rejected, not queued. Send the validators in order of value: the Sonnet validators for Logic, CI, Maven and Docs findings and MISSING or contested ACs first, then the Haiku validators for the strongest findings (Critical, then Important, by reviewer confidence), and the rest as slots free up. A finding whose validator times out or never ran scores 50 (below the cut-off), never dropped silently.

### Haiku findings

Launch one **Haiku** agent per finding, in parallel. It decides from its prompt alone and calls no tool: everything it needs is inlined, and re-reading files would cost the speed it is there for. Inline:
- The **finding** (file, line, issue, suggested fix, domain, reviewer confidence).
- The **diff chunk** the finding references, with a few lines of surrounding context.
- The **cited standard snippet**: only the rule the finding cites (3–10 lines), extracted from the standards bundles; omit it when no standard is cited. A `FORM_STRUCTURE` finding has no standards doc (its rules live in `.claude/agents/dotcms-pr-code-reviewer.md`), so quote the rule from the finding.
- A **file excerpt** of ~10 lines, only when the finding references a line outside the diff window.

**Haiku prompt template**:

```
You are validating a PR review finding. Score 0–100 how confident you are the finding is real.

RULES:
- Do NOT use any tools (no Read, Grep, Bash, or others). Every piece of context you need is inlined below.
- Decide from the inlined content only. If something seems missing, that itself is a signal the finding is weak.
- Respond in exactly the two lines specified at the bottom. No preamble, no analysis section.

Rubric:
- 100: Definitely real, directly violates the cited standard, clearly affects correctness or maintainability
- 80:  Highly confident. Real and important. Matches the cited standard AND the diff evidence.
- 50:  Moderately confident. Real but minor or nitpicky.
- 25:  Possibly real but may be a false positive. Not fully verifiable from the inlined evidence.
- 0:   False positive, pre-existing issue, or not actually present in the changed lines.

Finding to validate:
<finding JSON or markdown>

Diff context (changed lines ± a few lines of surrounding code):
<relevant diff chunk>

Cited standard snippet (the specific rule, not the full doc):
<3–10 lines extracted from the relevant *_STANDARDS.md / TESTING_REVIEW_RULES.md — or "not cited" if none>

Relevant file excerpt (only if finding references lines outside the diff window):
<~10 lines — or "not needed" if the diff above is sufficient>

Respond with exactly two lines:
SCORE: <0-100>
REASON: <one line>
```

### Logic findings → Sonnet validation

One **Sonnet** agent per `domain == Logic` finding, in parallel. It may `Read(core-web/**)` to confirm a hop. Inline the **finding** (file, line, traced path, AC reference, broken-behavior description, suggested fix, reviewer confidence), the **referenced AC** text (or "none" for a Pass-2 defect), and the **diff chunks** for the hops in the path.

**Sonnet validator prompt template**:

```
You are validating a LOGIC review finding for a dotCMS frontend PR. The finding claims a traced data-flow path produces behavior that is wrong or contradicts an acceptance criterion.

Score 0–100 how confident you are the finding is REAL:
- 100: The traced path provably yields the wrong behavior; every hop checks out.
- 80:  Highly confident. The break is real and the path is sound.
- 50:  Plausible but the path has a gap you can't confirm.
- 25:  Likely a false positive — the path doesn't actually break, or the "expected" behavior was misread.
- 0:   False positive: the behavior is correct, the AC was misread, or the issue is pre-existing/unchanged.

You MAY Read <HEAD_DIR>/core-web/** (the PR head) to confirm a single hop if the inlined context is insufficient. Verify the path hop-by-hop; do not take the reviewer's word for it.

Acceptance criterion referenced (or "none"):
<AC text>

Finding (with traced path, each hop as file:line):
<finding>

Diff context for the hops:
<relevant diff chunks>

Respond with exactly two lines:
SCORE: <0-100>
REASON: <one line>
```

### CI, Maven and Docs findings → Sonnet validation

One Sonnet agent per finding, with the template below. It may `Read`/`Grep` the whole head, `<HEAD_DIR>/**`. Fill it with:
- the **finding** with its Stage 5 origin (ADDED-BY-THIS-PR or PRE-EXISTING);
- the **rules**: the `## Rules` table of the reviewer that raised it, copied from `.claude/agents/dotcms-pr-<ci|maven|docs>-reviewer.md`;
- the **intent**: the PR title and body from Stage 2 (local mode: the commit messages, `git log --format='%s%n%b' <BASE_SHA>..<HEAD_SHA>`);
- the **run evidence**: for a CI finding, the file's `CI_GATE_EVIDENCE` line; for a Maven finding, the path of `<RUN_DIR>/bundles/effective-pom.diff`; for a Docs finding, "n/a";
- the **diff chunks** the finding cites.

```
You are validating a <CI | Maven | documentation> review finding for dotCMS PR #<N> (local mode: the local branch). Decide whether it is REAL: its premise holds at the head, and this change introduced it or made it reachable.

You MAY Read and Grep <HEAD_DIR>/** (the head). Open the second file the finding rests on (the caller, the parent POM or property, the code the doc names) and check the premise there yourself.

Take the first line that fits, in this order, and give its score:
1. The premise is false at the head: 10.
2. The premise depends on something the head cannot show (repository settings, an external service): 60.
3. The defect sits in lines this change leaves untouched and does not make reachable: 30.
4. The intent, a comment or a README in this change already states this exact behavior as a choice or a known limitation, so the finding tells the author nothing new: 30.
5. The finding is about style, naming, wording or a preference, with no consequence a rule row below names: 50.
6. Otherwise the finding is real: 90.

The change states its intended behavior in the intent, its comments and its READMEs; where those and the code disagree, the disagreement is the defect. Severity, how rarely the triggering event happens (a release dispatch, a merge into another branch), whether the outcome might be welcome and the quality of the suggested fix are the author's to weigh; the line you took sets the score.

Reviewer's rules (the defects this domain covers, one per row):
<## Rules table>

Finding (origin: <ADDED-BY-THIS-PR | PRE-EXISTING>):
<finding>

Intent (PR title and body, or the commit messages):
<intent>

Run evidence (CI: how the changed workflow runs; Maven: the effective-POM diff, base against head, to Read):
<run evidence>

Diff context:
<relevant diff chunks>

Respond with exactly two lines:
SCORE: <the score of the line you took>
REASON: <one line: the premise as checked, and the line you took>
```

### MISSING ACs → Sonnet re-check

A `MISSING` verdict drives the Stage 7 recommendation to **Request Changes**, so each one gets **one Sonnet agent** (in parallel with the finding validators) that tries to disprove it. It may `Read(core-web/**)`, `Grep` and `Glob` the whole repo, not only the changed files.

**Sonnet MISSING-AC re-check prompt template**:

```
An AC-coverage validator claims this acceptance criterion is NOT implemented in PR #<N>. Try to DISPROVE that — search the repo for code that satisfies it.

Acceptance criterion:
<AC text>

Changed files in the PR:
<file-list>

Decide:
- CONFIRMED_MISSING — no code anywhere satisfies this AC (or it lives elsewhere but this PR was required to change it and didn't).
- FALSE_MISSING — the behavior exists (cite file:line); either pre-existing and untouched, or implemented in a file the validator overlooked.

Respond with exactly two lines:
VERDICT: CONFIRMED_MISSING | FALSE_MISSING
REASON: <one line, cite file:line if FALSE_MISSING>
```

Downgrade each `FALSE_MISSING` AC to COVERED ("pre-existing/overlooked") in `AC_COVERAGE`; only `CONFIRMED_MISSING` stays MISSING. PARTIAL, UNTESTABLE and out-of-scope rows are not re-checked: they never block.

### Contested ACs → one Sonnet validator

The ACs Stage 5 marked contested go to **one** Sonnet agent together, in parallel with the other validators. It may `Read`/`Grep` the changed files and their tests.

```
The AC-coverage validator claims these acceptance criteria are COVERED in PR #<N>. Other reviewers disagree. For each one, decide from the code and the tests, not from who agrees with whom.

For each AC: the AC text, the ac-validator's evidence, and the finding that contests it.
<contested ACs>

A test only counts if it would fail with the change reverted: an assertion of empty against empty, or an expected value built by the code under test, is not coverage.

Respond with one line per AC:
AC<n>: COVERED | PARTIAL | MISSING — <one line, cite file:line>
```

Replace each contested row in `AC_COVERAGE` with its verdict. A MISSING verdict here drives the recommendation like a CONFIRMED_MISSING one.

### Own PR on a private issue (`SELF_TRIAGE = true`)

On the author's own security fix nothing is posted, and a real defect that scores 30 still has to be fixed before merge. So:

- **Every finding gets a Sonnet validator that can read the repo**, never the tool-free Haiku: the Logic prompt above for every domain, with "frontend" removed from its first line, `Read`/`Grep` on `dotCMS/**`, `dotcms-integration/**` and `core-web/**`, and the added line "Check each premise of the finding against the code before you score it." A tool-free validator scores premises it cannot check (a past self-review saw a Haiku give 35 to a real finding on an assumption about `FieldBuilder.builder(field)` that the code disproves).
- **Every finding is kept**, with its validator score and one-line reason; Stage 7 adds an "All findings (local triage)" table after the severity sections.
- **The reviewers' facts count, not only their claims.** When a finding says another call already does the work ("`FieldAPIImpl#save` already clears the content type cache"), the line it points at may be redundant even though the finding argued something else: list it for triage.

### Filter

Keep findings whose validator score is 80 or more and drop the rest, except under `SELF_TRIAGE` (above; for your own work `SELF_TRIAGE` follows `PRIVATE_LINKED`). Zero survivors is a valid result: Stage 7 reports "no issues found" in its usual shape.

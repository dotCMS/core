# Stages 5 and 6: consolidate and validate

Read after Stage 4b. Local mode runs both stages as written here; `local-mode.md` names its two differences (the head check and `SELF_TRIAGE`).

## Stage 5: Consolidate Findings

When all agents complete:

### 0. Head check before consolidating

The PR can move while the agents run. Before merging anything, re-read the head:

```bash
gh pr view <N> --repo dotCMS/core --json headRefOid,changedFiles
```

If `headRefOid` still equals `HEAD_SHA`, go on. If it moved: fetch the new head, move `HEAD_DIR` to it (`git -C <HEAD_DIR> checkout --detach <new sha>`), rebuild `changed-files.txt` and the slices with `diff-slices.sh --git <BASE_SHA> --head <new sha>`, and set `HEAD_SHA` to the new sha. Re-baseline the findings against it in step 2 below: a finding whose evidence is gone from the new head (`STALE`) was fixed by the new commits and is dropped. Files the new head added or changed that no reviewer saw go into `COVERAGE_GAPS`, and the user is told the review covers the older head for those files.

### AC coverage is handled separately

The `dotcms-pr-ac-validator` output is **not a finding stream** — do not flatten its rows into the severity-sorted list. Store its report as `AC_COVERAGE` (the coverage table, detailed evidence, out-of-scope list, regression risks, and verdict). It bypasses the normal dedupe/sort below and is rendered in its own Stage 7 section. The pieces that cross over into Stage 6 are the **MISSING ACs**, which get an adversarial re-check, and the contested ACs below (see Stage 6).

**Its table is an upper bound.** The ac-validator marks an AC COVERED when a named test and the code both exist, without checking that the test can fail (an assertion of empty against empty, a fixture that is the only source of the expected value). Before building the AC table, reconcile it with the other reviewers:
- An AC whose only backing tests the test reviewer or java-test reviewer flagged as tautological is at most PARTIAL.
- An AC on whose path the logic reviewer found a defect is at most PARTIAL.
- An AC the ac-validator calls COVERED while another reviewer's finding says it is not met is **contested**: send all contested ACs to one Sonnet validator (Stage 6, "Contested ACs"). Several agents agreeing is not evidence; the code and the tests are.

### Test design is handled separately too

The `dotcms-pr-test-case-designer` output is also **not a finding stream**. Merge its sidecar rows with its text report (sidecar wins per `TC-###`) and store the result as `TEST_DESIGN`: partitions, cases, gaps, unspecified-behavior questions, totals. Then write the full plan to disk so it outlives the session (the run directory is a scratch directory):

Pick `TEST_PLAN_PATH` by the linked issue, so the plan sits with that issue's other files:

| Linked issue | `TEST_PLAN_PATH` |
|---|---|
| In this repo (`ISSUE_NUMBER`) | `local/agents/<ISSUE_NUMBER>/pr-<N>-test-plan.md`, the issue's work folder in the main core checkout. Resolve it first with `bash <skill-dir>/../solve-issue/scripts/agents-dir.sh <ISSUE_NUMBER>` (the skill's base directory is given when it loads); from a worktree it links `local/agents` to the main checkout's. If it exits non-zero, use the last row. |
| In `dotCMS/private-issues` (`PRIVATE_LINKED`) | `local/security/<ISSUE_NUMBER>/pr-<N>-test-plan.md` |
| None resolved | `local/code-review/pr-<N>-test-plan.md` (`mkdir -p local/code-review`) |
| Local mode, any issue | `<OUT_DIR>/local-<BRANCH_SLUG>-<SHORT_SHA>-test-plan.md`, next to the report |

Write `TEST_DESIGN` as markdown to `TEST_PLAN_PATH`. It is the complete plan (every partition and every TC block). Stage 7 renders only the gaps.

The steps below apply to the line-level findings from all the *other* agents:

1. **Collect** raw findings from the **union of the sidecars and the final reports** — the sidecars are the system of record. The merged sidecars are already on disk from Stage 4 (`<RUN_DIR>/merged/findings.jsonl`, written by `merge-sidecars.sh`, which drops exact duplicates and invalid lines and prints the count of each); run it again if Stage 4b added rows:

```bash
bash <SKILL_DIR>/scripts/merge-sidecars.sh --dir <SIDECAR_DIR> --out <RUN_DIR>/merged --roster <RUN_DIR>/roster.txt
```

   For each instance, merge its sidecar rows with its text report: a finding present in only one of the two is still a finding. When the same `file:line` + claim appears in both, **the sidecar row wins** (the text report is a summary and may have been truncated mid-write). Note how many invalid lines the script dropped — a high count means an agent was ignoring the schema.

2. **Validate structurally, and re-derive every anchor before the report.** Agents cite diff offsets as file lines (line 1348 of a 185-line file), so no line number is trusted as written:

```bash
bash <SKILL_DIR>/scripts/verify-anchors.sh --findings <RUN_DIR>/merged/findings.jsonl --base <BASE_SHA> --rev <HEAD_SHA> --root <CORE_REPO> --out <RUN_DIR>/merged/anchored.jsonl
```

   (local mode: `--worktree` instead of `--rev <HEAD_SHA>`). Use each finding's `anchor.line` from here on. `REANCHORED` moves the finding to the line its evidence is on. `PAST_EOF` with no match, `STALE` or `NO_FILE` means the cited code is not there: the finding goes to Stage 6 marked as unanchored, so its validator re-checks it from the claim, and it cannot be posted unless that check re-anchors it. `UNVERIFIABLE` (no evidence) stays in the report but is never posted.
   - Every file mentioned exists in the PR diff (local mode: in the Local Stage 2 file list)
   - `anchor.in_diff` true means the line is inside the changed ranges: the finding is **ADDED-BY-THIS-PR**. False means **PRE-EXISTING** (or **PRE-EXISTING UPSTREAM** on an unchanged vendored line). Record it on every finding; the report and the posting table show it
   - No malformed entries missing required fields
3. **Dedupe**: if two agents flagged the same `file:line` with similar issue titles, keep the highest-confidence one and note which agents agreed (`merge-sidecars.sh` prints `OVERLAP` for every line flagged with different claims). Also **fold** a finding that exists only because of another (a missing test for a branch another finding says should not exist; a log line inside a catch another finding removes) into that finding, as one more point of it. Every duplicate merged here is one validator fewer in Stage 6
4. **Resolve contradictions**: if one agent says "add null check" and another says "this can't be null", keep both as separate findings — let the Haiku validator and user decide
5. **Flatten** into a single list, sorted by severity (Critical → Important → Quality) then by confidence within each tier
6. **Carry `COVERAGE_GAPS` forward** from Stage 4b untouched — it is reported, not filtered

Do NOT filter yet. Stage 6 does the filtering.

Every finding keeps its `evidence` string all the way through Stages 6-8. Stage 7b cannot verify an anchor without it, and a finding that loses its evidence becomes unpostable.

## Stage 6: Confidence Validation

Validation is **routed by domain**:

- **`domain == Logic`** → the **Sonnet** validator (see *Logic findings → Sonnet validation* before the Filter). A multi-hop data-flow trace needs real reasoning to confirm; Haiku would produce false rejections.
- **`domain` is `CI`, `Maven` or `Docs`** (findings of the ci, maven and docs reviewers) → a **Sonnet** validator with its own template, *CI, Maven and Docs findings* below. These findings rest on a second file the diff does not show: the caller of a removed input, the parent a module overrides, the code a doc describes. A tool-free Haiku scores that premise without seeing it. Many of them are not a broken data-flow path (a test no workflow runs, a comment claiming an invariant the code lacks), so the Logic rubric scores them as no defect.
- **Every other domain** (code, test, styling, silent-failure, comment and historical findings) → the Haiku flow below (fast, tool-free, ≥80 cutoff).

All paths share the same `≥ 80` cutoff and feed the same Stage 7 report.

**At most 20 subagents run at once**; calls past 20 are rejected, not queued. Send the validators in order of value: the Sonnet validators for Logic, CI, Maven and Docs findings and MISSING or contested ACs first, then the Haiku validators for the strongest findings (Critical, then Important, by reviewer confidence). Send at most 20, and send the rest as slots free up. A finding whose validator times out or never ran scores 50 (below the cut-off), never dropped silently.

For every finding outside the Sonnet domains (Logic, CI, Maven, Docs), launch a **Haiku agent in parallel** (one Haiku per finding). Each Haiku must be able to decide purely from the prompt — **it must NOT call any tools** (no `Read`, no `Grep`, no `Bash`). Tool use here defeats the speed and cost benefit of Haiku and re-reads files the reviewer already analyzed.

Each Haiku receives, inlined in its prompt:
- The **finding** (file, line, issue, suggested fix, domain, reviewer confidence)
- The **relevant diff chunk** — the ± lines the finding references, with a few lines of surrounding context
- The **cited standard snippet** — only the rule the finding cites (3–10 lines), NOT the full standards doc. The orchestrator extracts this from the preloaded `ANGULAR_STANDARDS` / `TYPESCRIPT_STANDARDS` / `STYLING_STANDARDS` / `TESTING_REVIEW_RULES` content. If the finding does not cite a standard, omit this section. Findings citing `FORM_STRUCTURE` have no standards doc to extract from — the rule text lives in the code-reviewer agent definition, so quote the cited rule from the finding itself.
- The **file excerpt** — if the finding references a line outside the diff window (e.g., a helper two methods away), include ~10 lines of the file around that line. Otherwise skip.

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

For every finding with `domain == Logic`, launch a **Sonnet** agent in parallel (one per finding) instead of a Haiku. It MAY use `Read(core-web/**)` to confirm a hop, because verifying a multi-hop trace can require seeing a line the finding references. Each Sonnet validator receives:
- The **finding** (file, line, traced path, AC reference, broken-behavior description, suggested fix, reviewer confidence)
- The **referenced AC** text (or "none" for a Pass-2 defect)
- The **relevant diff chunk(s)** for the hops in the path

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

One Sonnet agent per finding, with the template below. It MAY `Read`/`Grep` the whole head, `<HEAD_DIR>/**`. Fill it with:
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

A `MISSING` verdict from the ac-validator is high-stakes: it drives the Stage 7 recommendation to **Request Changes**. Before trusting it, launch **one Sonnet agent per MISSING AC** (in parallel with the finding validators). Each is prompted to *disprove* the MISSING claim — it MAY `Read(core-web/**)`/`Grep`/`Glob` to find the behavior anywhere in the repo, not just the changed files.

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

Downgrade any `FALSE_MISSING` AC to COVERED (note "pre-existing/overlooked") in `AC_COVERAGE`. Keep only `CONFIRMED_MISSING` as MISSING. PARTIAL / UNTESTABLE / out-of-scope rows are NOT re-checked — they never block.

### Contested ACs → one Sonnet validator

The ACs Stage 5 marked contested (the ac-validator says COVERED; a test, java-test or logic reviewer finding says otherwise) go to **one** Sonnet agent together, in parallel with the other validators. It MAY `Read`/`Grep` the changed files and their tests.

```
The AC-coverage validator claims these acceptance criteria are COVERED in PR #<N>. Other reviewers disagree. For each one, decide from the code and the tests, not from who agrees with whom.

For each AC: the AC text, the ac-validator's evidence, and the finding that contests it.
<contested ACs>

A test only counts if it would fail with the change reverted: an assertion of empty against empty, or an expected value built by the code under test, is not coverage.

Respond with one line per AC:
AC<n>: COVERED | PARTIAL | MISSING — <one line, cite file:line>
```

Replace each contested row in `AC_COVERAGE` with its verdict. A MISSING verdict here drives the Stage 7 recommendation like a CONFIRMED_MISSING one.

### Own PR on a private issue (`SELF_TRIAGE = true`)

The `≥ 80` cutoff decides what is worth posting on someone else's PR. On the author's own security fix nothing is posted, and a real defect that scores 30 still has to be fixed before merge. So the validation and the filter change:

- **Validate every finding with a Sonnet agent that can read the repo**, not with the tool-free Haiku. Use the Logic validator prompt above for every domain, with "frontend" removed from its first line. Allow `Read`/`Grep` on `dotCMS/**`, `dotcms-integration/**` and `core-web/**`, and add: "Check each premise of the finding against the code before you score it." A tool-free validator scores premises it cannot check. On a past self-review a Haiku gave 35 to a real finding because it assumed `FieldBuilder.builder(field)` loses the concrete field type, which it does not.
- **Drop nothing.** Keep every finding with its validator score and its one-line reason. Stage 7 adds an "All findings (local triage)" table after the severity sections.
- **Read the reviewers' facts, not only their claims.** When a finding says another call already does the work (for example "`FieldAPIImpl#save` already clears the content type cache"), the line it points at may be redundant even if the finding argued something else. List it for triage.

### Filter

Keep findings where the validator score (Haiku, or Sonnet for the Logic, CI, Maven and Docs domains) is `≥ 80`. Drop the rest silently, except when `SELF_TRIAGE` is true (see above; in local mode `SELF_TRIAGE` follows `PRIVATE_LINKED`, see `local-mode.md`).

If 0 findings survive, proceed to Stage 7 with an empty findings list. Do NOT abort — Stage 7 still reports "no issues found" in the expected format.


# Crash-Safe Findings Sidecar + Turn Budget (append VERBATIM to every reviewer prompt)

Read at Stage 4, before writing any prompt. Stage 4b and Stage 5 rely on what it sets up.

Two failure modes have cost whole reviewers in past runs, and both are fixed here.

**Failure mode 1 — the agent hits `maxTurns` mid-thought and returns nothing.** Everything it found is lost, and Stage 5 consolidates a hole where that reviewer's findings should be.

**Failure mode 2 — the agent spends its entire budget investigating** and never writes a report at all.

The fix is a **sidecar file written incrementally**: a finding that is on disk survives a turn-limit kill, a crash, or a truncated final message. The final text report is a convenience, not the system of record — **the sidecar is the system of record.**

## Sidecar directory (orchestrator, once, before launching any agent)

The sidecars live in `<RUN_DIR>/sidecars/`, inside the fresh run directory SKILL.md creates in the session scratchpad for every run (`<SIDECAR_DIR>` below). A new, empty directory per run is what keeps a leftover `.jsonl` from a previous review out of this one; never reuse a run directory, and never wipe one with `rm -rf` (hooks block `rm -rf` on absolute paths, and the run stops there). Two reviews on one machine no longer share a directory, so they can run at the same time.

## The block to append to every prompt

Append this verbatim to **every** prompt template in the next section, substituting the agent's own `<SIDECAR>`, `<MAX_TURNS>` and `<STOP_AT>` from the table below.

```
## Findings sidecar — write as you go (MANDATORY)

Append every finding to `<SIDECAR>` **the moment you confirm it** — never batch them up to write at the end. One JSON object per line, no wrapping array:

{"file":"<repo-relative path>","line":<int>,"severity":"Critical|Important|Quality","claim":"<one sentence: what is wrong>","evidence":"<VERBATIM source line(s) at that line — copied exactly, not paraphrased>","fix":"<one sentence: the concrete change>","confidence":<0-100>,"agent":"<your agent name>"}

Append with a single shell redirect per finding, e.g.:
  echo '{"file":"...","line":42,...}' >> <SIDECAR>

Use `echo` and nothing else to write it: no python, no node, no heredoc script. A hook can reject an interpreter call, and an agent whose write was rejected tends to stop writing altogether.

Rules:
- **`evidence` must be the literal text at `file:line` in the file you read**, copied character-for-character. Stages 5 and 7b search the reviewed revision for this exact string to verify your anchor; a paraphrase makes the finding unpostable and it will be dropped.
- `line` is the line number as it appears in the FILE (count from the file's first line, or run `grep -n` on it) — never a diff offset or a hunk position. A line past the end of the file, such as line 1348 of a 185-line file, is a diff offset and the finding will be re-checked from its evidence.
- Write it the moment you are sure. If you are killed on the next turn, that finding still counts. Findings you confirm in the same turn may share one append, but **never carry an unwritten finding past the file you are currently reviewing**.
- This sidecar is the ONLY file you may write. Do not Write/Edit any other file, do not post GitHub comments, do not create reports on disk. The orchestrator consolidates and posts; you only find and record.

## Turn budget — hard contract

You have <MAX_TURNS> turns. **At turn <STOP_AT> (~70%), stop investigating.** Whatever is in your sidecar by then is your review — do not open one more file, do not chase one more hypothesis.

Spend the remaining turns on a **text-only** final message that mirrors the sidecar:
- Plain text in the message itself. No files, no diff re-prints.
- One line per finding: `file:line` · severity · confidence · claim · fix.
- **Ordered by confidence, highest first**, so anything truncated is the weakest.
- Nothing found by <STOP_AT>? Say `NO FINDINGS` plus one line on what you covered. That is a valid result.
- Reached <STOP_AT> mid-investigation? Add a final line: `UNREVIEWED: <files or areas you never reached>`.

If your final message and your sidecar disagree, the sidecar wins.
```

| Agent | `<SIDECAR>` | `maxTurns` | Stop at (~70%) |
|---|---|---|---|
| `dotcms-pr-code-reviewer` | `<SIDECAR_DIR>/code-reviewer-<i>.jsonl` | 28 | 20 |
| `dotcms-pr-test-reviewer` | `<SIDECAR_DIR>/test-reviewer-<i>.jsonl` | 24 | 17 |
| `dotcms-pr-historical-reviewer` | `<SIDECAR_DIR>/historical-reviewer.jsonl` | 22 | 15 |
| `dotcms-pr-silent-failure-hunter` | `<SIDECAR_DIR>/silent-failure-hunter.jsonl` | 16 | 11 |
| `dotcms-pr-comment-analyzer` | `<SIDECAR_DIR>/comment-analyzer.jsonl` | 14 | 10 |
| `dotcms-pr-logic-reviewer` | `<SIDECAR_DIR>/logic-reviewer-<i>.jsonl` | 28 | 20 |
| `dotcms-pr-java-code-reviewer` | `<SIDECAR_DIR>/java-code-reviewer-<i>.jsonl` | 32 | 22 |
| `dotcms-pr-java-api-reviewer` | `<SIDECAR_DIR>/java-api-reviewer-<i>.jsonl` | 24 | 17 |
| `dotcms-pr-java-persistence-reviewer` | `<SIDECAR_DIR>/java-persistence-reviewer-<i>.jsonl` | 24 | 17 |
| `dotcms-pr-java-security-reviewer` | `<SIDECAR_DIR>/java-security-reviewer.jsonl` | 28 | 20 |
| `dotcms-pr-java-test-reviewer` | `<SIDECAR_DIR>/java-test-reviewer-<i>.jsonl` | 28 | 20 |
| `dotcms-pr-ac-validator` | `<SIDECAR_DIR>/ac-validator.jsonl` | 48 | 34 |
| `dotcms-pr-test-case-designer` | `<SIDECAR_DIR>/test-case-designer.jsonl` | 40 | 28 |
| `dotcms-pr-ci-reviewer` | `<SIDECAR_DIR>/ci-reviewer-<i>.jsonl` | 36 | 25 |
| `dotcms-pr-maven-reviewer` | `<SIDECAR_DIR>/maven-reviewer.jsonl` | 24 | 17 |
| `dotcms-pr-docs-reviewer` | `<SIDECAR_DIR>/docs-reviewer-<i>.jsonl` | 24 | 17 |

Every budget above was raised when the sidecar became mandatory: appending findings costs turns that used to go to investigation, so holding the old numbers would have bought crash-safety by shrinking the review.

`<i>` is the 1-based instance index for multi-instance agents — two instances must never share a sidecar. These `maxTurns` values must match each agent's frontmatter; change one, change both.

Each reviewer agent file also carries the sidecar schema and the write-as-you-go rules ("Findings sidecar and budget"), so an agent launched directly, outside this skill, (e.g. the fallback in the `dot-security-issue` skill's `references/fix.md`), still writes a sidecar. The block above wins where the two differ; keep them in sync.

The **ac-validator is the exception to the schema**: its sidecar rows carry its AC-coverage shape (`{"ac":"AC3","status":"MISSING","evidence":"...","note":"..."}`), not the finding shape, because it does not emit line-level findings. The **test-case-designer** is the second exception: one row per designed case (`{"tc":"TC-004","ac":"AC1","partitions":["EP1.3"],"scenario":"Negative","risk":"Critical","level":"Integration","coverage":"NOT COVERED","covered_by":"","title":"...","expected":"..."}`).

## After the batch returns

Check the sidecars before anything else (Stage 4b reads the result):

```bash
bash <SKILL_DIR>/scripts/merge-sidecars.sh --dir <SIDECAR_DIR> --out <RUN_DIR>/merged --roster <RUN_DIR>/roster.txt
```

It prints the rows and invalid lines per sidecar, `MISSING` for a launched agent with no sidecar and `EMPTY` for one with no valid row. An `EMPTY` or `MISSING` sidecar from an agent whose final message lists findings means that message is the only copy of them: Stage 4b turns it into rows before anything is merged.

# Stage 4b: Recovery Sweep (automatic — never ask the user to nudge an agent)

Fan-out is not complete when the agents return; it is complete when every launched agent has **either** a usable final report **or** a sidecar you have drained. An agent that hit `maxTurns`, crashed, or returned an empty/garbled message is a silent hole in the review — and the user must never be the one who notices it. Most agents that end with no report spent their turns exploring; the bundles and slices of Stage 4 are the prevention, this sweep is the cure.

Run this sweep automatically after the Stage 4 batch returns:

**1. Build the expected roster.** `<RUN_DIR>/roster.txt` lists every agent instance you launched with its sidecar file. This is the ground truth for what "complete" means.

**2. Classify each instance:**

| Condition | Verdict |
|---|---|
| Final text report present, with findings or an explicit `NO FINDINGS` | `OK` |
| No final report, but sidecar has ≥1 line | `PARTIAL` |
| No final report and empty/absent sidecar | `LOST` |
| Final report present but contains `UNREVIEWED:` | `OK_PARTIAL_COVERAGE` |

The sidecar counts come from the `merge-sidecars.sh` run at the end of Stage 4 (`sidecar-and-budget.md`, "After the batch returns"): `SIDECAR <name> rows=<n>`, `EMPTY <name>`, `MISSING <name>`.

**3. Resume every `PARTIAL` and `LOST` instance — automatically, via `SendMessage` to that agent.** Do **not** re-launch a fresh agent (it would redo the investigation from zero and burn the budget again), and do **not** stop to ask the user to nudge it. Send exactly this, with its scope and sidecar filled in:

```
You hit your turn limit before emitting your report. Stop exploring now: no further tool calls. Do not read another file, run another command, or verify anything further.

Write your findings report NOW, from what you already know, in your very next message — text only, nothing else. Your scope was: <its assigned files>.
- One line per finding: file:line · severity · confidence · claim · fix. The line is the line number in the FILE, not a diff offset.
- Ordered by confidence, highest first.
- If you found nothing, reply `NO FINDINGS` and one line on what you covered.
- End with `UNREVIEWED: <areas you never reached>`.

Whatever is already in your sidecar (<SIDECAR>) counts; this message confirms what you reached. Do not write to the sidecar again.
```

**4. One resume attempt per instance.** If the resume also returns nothing, fall back to the sidecar: the rows on disk ARE that agent's findings. Mark the instance `SIDECAR_ONLY` and carry its rows into Stage 5 with `UNREVIEWED: <its whole assigned file list>` — the work it did counts, the coverage it claimed does not.

**5. A report with no sidecar rows is the only copy.** When an agent's final message (or its resume answer) lists findings but its sidecar is `EMPTY` or `MISSING`, turn each listed finding into a row before Stage 5: write them with the Write tool to `<SIDECAR_DIR>/<agent>.from-report.jsonl`, in the sidecar schema, with `"source":"report"`, and fill `evidence` from the file at the cited line (`sed -n '<line>p' <HEAD_DIR>/<path>`) so the anchor check can run. Run `merge-sidecars.sh` again afterwards.

**A test-case designer still empty after its resume** leaves Stage 7 printing "Test design unavailable", and its gate is not applied.

**6. Record coverage gaps.** Collect every `UNREVIEWED:` area plus every `LOST` instance's assigned files into `COVERAGE_GAPS`. Stage 7 must print this; a review that silently under-covers is worse than one that admits it.

Never present a review as complete while any instance is `LOST` with an empty sidecar and a failed resume. Say plainly which files went unreviewed.

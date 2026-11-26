# Stage 4b: Recovery Sweep (automatic)

Fan-out is complete when every launched instance has a usable final report or a sidecar you have drained. An agent that hit `maxTurns`, crashed or returned an empty or garbled message is a hole in the review, and you find it here, before the user does. Run this sweep as soon as the Stage 4 batch returns:

**1. The roster** (`<RUN_DIR>/roster.txt`) lists every launched instance and its sidecar: it defines "complete".

**2. Classify each instance**, with the sidecar counts of the `merge-sidecars.sh` run that ended Stage 4 (`SIDECAR <name> rows=<n>`, `EMPTY <name>`, `MISSING <name>`):

| Condition | Verdict |
|---|---|
| Final text report present, with findings or an explicit `NO FINDINGS` | `OK` |
| No final report, but sidecar has ≥1 line | `PARTIAL` |
| No final report and empty/absent sidecar | `LOST` |
| Final report present but contains `UNREVIEWED:` | `OK_PARTIAL_COVERAGE` |

**3. Resume every `PARTIAL` and `LOST` instance yourself, with `SendMessage` to that agent.** A fresh agent would redo the investigation from zero, and the user is never asked to nudge one. Send exactly this, with its scope and sidecar filled in:

```
You hit your turn limit before emitting your report. Stop exploring now: no further tool calls. Do not read another file, run another command, or verify anything further.

Write your findings report NOW, from what you already know, in your very next message — text only, nothing else. Your scope was: <its assigned files>.
- One line per finding: file:line · severity · confidence · claim · fix. The line is the line number in the FILE, not a diff offset.
- Ordered by confidence, highest first.
- If you found nothing, reply `NO FINDINGS` and one line on what you covered.
- End with `UNREVIEWED: <areas you never reached>`.

Whatever is already in your sidecar (<SIDECAR>) counts; this message confirms what you reached. Do not write to the sidecar again.
```

**4. One resume per instance.** If the resume also returns nothing, the rows on disk are that agent's findings: mark it `SIDECAR_ONLY` and carry its rows into Stage 5 with `UNREVIEWED: <its whole assigned file list>`. The work it did counts; the coverage it claimed does not.

**5. A report whose sidecar is `EMPTY` or `MISSING` is the only copy of its findings.** Turn each finding it lists into a row before Stage 5: write them with the Write tool to `<SIDECAR_DIR>/<agent>.from-report.jsonl`, in the finding row shape (`merge-sidecars.sh --help`), with `"source":"report"`, and fill `evidence` from the file at the cited line (`sed -n '<line>p' <HEAD_DIR>/<path>`) so the anchor check can run. Run `merge-sidecars.sh` again afterwards.

**A test-case designer still empty after its resume** leaves Stage 7 printing "Test design unavailable", and its gate is not applied.

**6. Record coverage gaps.** Collect every `UNREVIEWED:` area plus every `LOST` instance's assigned files into `COVERAGE_GAPS`; Stage 7 prints them.

The review is complete only when no instance is `LOST` with an empty sidecar and a failed resume; otherwise the report names the files that went unreviewed.

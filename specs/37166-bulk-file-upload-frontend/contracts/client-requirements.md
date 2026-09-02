# What the client needs from the batch contract

**Status: INPUT, not agreement.** This states the consumer's side of the boundary so the two halves of #37166 have something concrete to converge on. It is not a settled contract and must not be implemented against until it is.

**The agreed contract belongs in `specs/37166-bulk-file-upload/contracts/`**, owned by the half that defines the outcome shape (backend FR-018), reviewed by this half as the consumer. It goes there and not in either `plan.md` because in this repo `plan.md`, `research.md`, `tasks.md`, `quickstart.md` and `checklists/` are gitignored, while `contracts/` is committed. An agreement written in a plan does not survive the branch.

## Open items that must be answered before frontend Phase 3 starts

| # | Question | Raised by | Why the client cares |
|---|---|---|---|
| 1 | `failedCount` or `failCount`? | backend FR-018 | The client reads it. One spelling, both halves. |
| 2 | What is the generic item key? | backend FR-018 | An uploading file has neither identifier nor inode. The client keys per-file results by it to name failures (FR-023). |
| 3 | What makes a retry safe: same handle returned, or duplicates refused as collisions? | backend C-002a / FR-040 / FR-040a | Changes what a successful retry *looks like*. Under the collision branch a retry that worked reports 50 of 50 failed with "already exists", and the client must report that as the success it is rather than a total failure. |
| 4 | Submission field names and endpoint | this plan | Cannot be guessed. |
| 5 | The shape of the handle | this plan | Determines how the client correlates the pushed completion signal to its own run. |
| 6 | How the client declares the batch total size | frontend FR-038 | The fast refusal exists only where the caller declares a total, and the caller is the browser. Without it, the only enforcement left refuses the batch *after* the author has uploaded it. |

## What the client requires, behaviourally

Restating the consumer's side of the frontend spec's §Contract Consumed, at the level the contract document must make concrete.

1. **Submit** several files with one target and one upload type, in a single call carrying the content, answered immediately with a handle. The client never stages content itself and never handles a staging identifier.
2. **Distinguishable submission refusals**, separable by the client: no files; bad upload type; missing target; too many files; batch over the total size ceiling; permission denied. The client shows different copy for each, so they cannot arrive as one undifferentiated error.
3. **Retry after an uncertain submission is safe** — whichever mechanism provides it, plus enough information to tell a successful retry from a genuine failure (item 3 above).
4. **Progress, if available.** Optional by design: the client renders indeterminate when it is absent and must never receive a fabricated position. See plan §Degradation.
5. **A terminal state and outcome**: counts, plus per-item results carrying a reason code for each failure.
6. **A pushed completion signal** carrying counts *and* per-item results, correlatable to the run the client submitted, reaching the browser without polling — plus the durable record behind it.

## Constraints the contract must respect

- **The reason set is closed and shared.** Six reasons (see `../data-model.md`). Each needs client copy; a reason with no copy is a hole the author sees. Adding one later is a change to both halves.
- **The server's human-readable message is diagnostic and is never displayed.** The client maps the reason code to product copy (frontend FR-030, backend FR-016a).
- **Counts are authoritative.** The client will not substitute the number of files the author chose, and will report an outcome whose counts do not close as an error rather than inventing a number.
- **A terminal state the client does not recognise is reported as an error, never a success.** This is what makes deferring cancellation to #33331 safe, so the contract must not assume the client silently tolerates unknown states.

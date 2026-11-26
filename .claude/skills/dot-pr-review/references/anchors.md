# Stage 7b: Pre-Post Verification (runs before any comment is posted)

For your own work, `local-mode.md` says how this stage runs: `LOCAL` checks the working tree, and `OWN_PR` runs 7b.1 to 7b.4 below and posts nothing.

Everything so far was computed from the Stage 2 snapshot. Since then the PR may have moved, and other reviewers, human or bot, may have said what you are about to say: a stale anchor puts a comment on the wrong line, and a duplicate is worse than silence. This stage runs **every time** before Stage 8, interactive or `auto`, and nothing reaches GitHub without passing it.

## 7b.1 — Refetch the head SHA and every existing comment

```bash
gh api repos/dotCMS/core/pulls/<N> --jq '.head.sha'
```

```bash
gh api --paginate repos/dotCMS/core/pulls/<N>/comments --jq '.[] | {id, path, line, original_line, side, user: .user.login, body}'
```

```bash
gh api --paginate repos/dotCMS/core/pulls/<N>/reviews --jq '.[] | select(.body != "") | {user: .user.login, state, body}'
```

```bash
gh api --paginate repos/dotCMS/core/issues/<N>/comments --jq '.[] | {id, user: .user.login, body}'
```

Store the SHA as `CURRENT_SHA` and the comments as `EXISTING_COMMENTS`, **bot** comments included (sonar, codecov, copilot, dotcms bots): a duplicate of a bot's finding is still noise for the author. The Stage 2 digest is minutes old, and a bot such as `claude[bot]` turns its "working..." comment into a full review after posting it.

## 7b.2 — If the SHA moved, re-slice the diff from local git

```
SHA_CHANGED = (CURRENT_SHA != HEAD_SHA)
```

When `SHA_CHANGED` is true, the Stage 2 diff is stale. Re-slice it **per file, from the local clone**: `gh pr diff` returns one blob in which a file with no trailing newline glues the next file's `diff --git` header onto its last line, and every line number after it is off.

First make sure the commit is local; skip the fetch when it already is (a merged PR's head is usually reachable from `main`, and `fetch origin pull/<N>/head` fails on a clone whose refspec has no PR refs):

```bash
git -C <CORE_REPO> cat-file -e <CURRENT_SHA>^{commit}
```

Only if that errors:

```bash
git -C <CORE_REPO> fetch origin pull/<N>/head
```

Take the base from the **API**, which is stable whether the PR is open or merged:

```bash
gh api repos/dotCMS/core/pulls/<N> --jq '.base.sha'
```

(`git merge-base origin/<BASE_REF> <CURRENT_SHA>` returns the head itself once the PR is merged: the diff comes back empty and every finding is dropped as stale.)

```bash
git -C <CORE_REPO> diff --unified=0 <BASE_SHA> <CURRENT_SHA> -- <path>
```

One invocation **per file path**, so no hunk header can bleed into its neighbour. Rebuild each file's changed-line ranges from these per-file diffs and replace the Stage 2 ranges; `diff-slices.sh --git <BASE_SHA> --head <CURRENT_SHA> --root <CORE_REPO>` does exactly this for a list of paths, and `verify-anchors.sh` (7b.3) takes the ranges the same way.

Then set `HEAD_SHA = CURRENT_SHA`: every link in the report and every `commit_id` in Stage 8 uses the new SHA. Re-verify **every** finding's anchor in 7b.3, not only those in files that changed.

If the local clone is missing or unreadable, or the fetch fails, abort posting, keep the report, and tell the user the anchors could not be re-verified. `gh pr diff` is never the fallback.

## 7b.3 — Verify every anchor by grepping its evidence at the head version

`verify-anchors.sh` checks every surviving finding against the head version and its per-file diff in one call:

```bash
bash .claude/skills/dot-pr-review/scripts/verify-anchors.sh --findings <RUN_DIR>/merged/anchored.jsonl --base <BASE_SHA> --rev <HEAD_SHA> --root <CORE_REPO> --out <RUN_DIR>/merged/postable.jsonl
```

Its `anchor.status` maps onto the table: `OK` is `ANCHOR_OK`, `REANCHORED` is a re-anchor (use `anchor.line`), `AMBIGUOUS`, `STALE` and `UNVERIFIABLE` are the drops below, and `NO_FILE` is stale. By hand, for one finding, the check is:

```bash
git -C <CORE_REPO> show <HEAD_SHA>:<path> | sed -n '<line>p'
```

Compare against the finding's `evidence` (whitespace-normalized, ignore leading indentation):

| Result | Action |
|---|---|
| Evidence matches the line | `ANCHOR_OK` — keep as-is |
| No match at `<line>` | Search the file for it: `git -C <CORE_REPO> show <HEAD_SHA>:<path> \| grep -n -F '<evidence>'` |
| Found at exactly one other line | **Re-anchor** the finding to that line, note the shift |
| Found at several lines | Pick the one inside a changed-line range; if none, `ANCHOR_AMBIGUOUS` → drop |
| Not found anywhere in the file | `ANCHOR_STALE` → **drop the finding** — the code it describes no longer exists |
| Finding carries no `evidence` | `ANCHOR_UNVERIFIABLE` → drop. An unverifiable anchor is how comments land on innocent lines |

A finding that cannot be anchored to code at the head is no longer about this PR, so dropping it is the expected result. Every finding this stage drops keeps its reason (stale, ambiguous, unverifiable, duplicate) for Stage 8's Not posting list. A line past the end of its file (`PAST_EOF`) is a diff offset cited as a file line: it is re-anchored from its evidence or dropped, never posted on the cited number.

## 7b.4 — Drop duplicates of comments that already exist

A surviving finding is a **duplicate** of an entry in `EXISTING_COMMENTS` when all of these hold:

- same `path`, and
- the existing comment's `line` (or `original_line`) is within **±3 lines** of the finding, and
- the existing comment makes substantially the same claim — same defect, same asked-for change. Judge the meaning, not the wording: "this can be null" and "add a null guard here" are the same claim.

Drop every duplicate and record it as `DUPLICATE — already raised by @<user> at <path>:<line>`, so the table shows the point is covered.

**Overlap is not duplication.** When an existing comment raises the same defect but the finding adds something it lacks (a second place it happens, the reason it breaks, the concrete fix), post only that additive part, as a **reply** in that comment's thread (`posting.md`, "Reply to an existing thread"). A PR-level comment has no thread: post the additive part inline, opening with "Adding to @<user>'s point:". Record it as `REPLY — adds to @<user>`.

Also drop a finding whose claim the PR author already answered in a review body from 7b.1 ("known, tracked in #12345").

## 7b.5 — Report the gate

Print one line before Stage 8:

```
Pre-post verification: <kept>/<total> findings postable — <stale> stale anchors dropped, <reanchored> re-anchored, <dupes> duplicates of existing comments, <replies> replies to existing threads, SHA <unchanged|moved 1a2b3c4 → 5d6e7f8>.
```

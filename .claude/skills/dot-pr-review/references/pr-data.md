# Stages 1 and 2: eligibility and PR data

Your own work skips Stage 1. `LOCAL` replaces this Stage 2 with "Local Stage 2" in `local-mode.md`; `OWN_PR` runs it as written.

## Stage 1: Eligibility Check

Launch a **Haiku** agent with this task:

```
Fetch PR #<N> metadata via `gh pr view <N> --repo dotCMS/core --json state,isDraft,author,reviews,title,body,headRefOid`.

Note the field name: in `gh pr view --json reviews` each review carries `commit.oid` (a nested object). It is **not** `commit_id` — that spelling only exists on the REST endpoint `gh api repos/dotCMS/core/pulls/<N>/reviews`. Using the wrong one yields null, which compares unequal to every SHA and makes this check silently pass every time.

Return one of:
- ELIGIBLE
- SKIP — <reason>

Skip the PR if any of these are true:
- state is CLOSED or MERGED
- isDraft is true
- author.login matches bot pattern: dependabot, renovate, github-actions, gradle[bot]
- reviews[] contains a review by the current user (gh api user → login) **on the current head SHA** — compare `reviews[].commit.oid` against `headRefOid`, NOT the review's timestamp
- title starts with "chore(deps)" or "[WIP]"

Keep the response under 3 lines.
```

On `SKIP`, print the reason and stop, unless `FORCE` or `DRY_RUN` is set: then print it as a warning and continue.

The check compares the head SHA, not a time window: "already reviewed" means this exact code, so a PR you reviewed at an older head proceeds, and Stage 7b drops whatever would duplicate your earlier comments.

## Stage 2: Fetch PR Data

```bash
# Metadata
gh pr view <N> --repo dotCMS/core --json title,body,headRefName,baseRefName,additions,deletions,changedFiles,headRefOid

# Base commit of the diff
gh api repos/dotCMS/core/pulls/<N> --jq '.base.sha'

# Full diff, minus noise that no reviewer should ever read; stdout saved for the slicer
gh pr diff <N> --repo dotCMS/core --exclude 'pnpm-lock.yaml' --exclude 'yarn.lock' --exclude 'package-lock.json' --exclude '**/*.snap' --exclude '**/dist/**' > <RUN_DIR>/pr.diff

# File list WITH per-file churn and change type (one call, replaces `gh pr diff --name-only`)
gh pr view <N> --repo dotCMS/core --json files --jq '.files[] | {path, changeType, additions, deletions}'
```

The `--exclude` list holds only lock, snapshot and generated files, never something a reviewer judges.

The `files` output is the file list for every later stage: `changeType` (`ADDED` / `MODIFIED` / `DELETED` / `RENAMED`) and churn per file. Write its paths, one per line, to `<RUN_DIR>/changed-files.txt`, leaving out the files the diff excludes: the slicer checks every slice against it.

Store `headRefOid` as `HEAD_SHA` (the SHA of every link in Stage 7 and every comment in Stage 8), the base as `BASE_SHA`, and `changedFiles` as `CHANGED_FILES`.

If a fetch fails, run `gh pr list --repo dotCMS/core --limit 100` to help locate the PR and report "Unable to fetch PR #<N>." If `HEAD_SHA` cannot be read, use `main` in the links and warn that they go stale if the PR is rebased.

### Large PRs: build the diff from local git

`gh pr diff` fails with HTTP 406 above 20,000 diff lines and writes nothing, and `gh pr view --json files` stops at 100 files. When either happens (`CHANGED_FILES` > 100, or the 406):

- File list: `gh api --paginate repos/dotCMS/core/pulls/<N>/files --jq '.[] | {path: .filename, changeType: .status, additions, deletions}'` (`status` is `added`, `modified`, `removed` or `renamed`; map `removed` to `DELETED` and the others to their upper case).
- Diff: from local git, one `git diff <BASE_SHA> <HEAD_SHA> -- <path>` per path, with the paths read from a file (`diff-slices.sh --git <BASE_SHA> --head <HEAD_SHA>`, see `agents.md`). Paths go through a file, never a command line.
- A huge generated or JSON slice (a Postman collection, a generated client) is not handed to an agent. Precompute what a reviewer would count, base against head (requests, tests, endpoints, with `jq`), and give the agent those numbers instead.

### Materialize the PR head

The reviewers read full files from a checkout, and the one they run in is usually `main`. Check the PR head out once, detached, in the run directory:

```bash
git -C <CORE_REPO> cat-file -e <HEAD_SHA>^{commit}
git -C <CORE_REPO> fetch origin pull/<N>/head
git -C <CORE_REPO> worktree add --detach <RUN_DIR>/pr<N> <HEAD_SHA>
```

(fetch only when `cat-file` errors). Store it as `HEAD_DIR`. If `git worktree add` fails, abort and say why: reviewers reading `main` instead of the PR review the wrong code. Every reviewer reads code from `<HEAD_DIR>/<path>` and reports repo-relative `file:line`; the historical reviewer runs `git -C <HEAD_DIR>` for blame and log. Stage 10 removes it with `git -C <CORE_REPO> worktree remove --force <HEAD_DIR>`, also when the run stops early.

### Existing feedback digest

Right after the fetches above, read what has already been said on the PR:

```bash
gh api --paginate repos/dotCMS/core/pulls/<N>/comments --jq '.[] | {id, path, line, original_line, user: .user.login, body}'
gh api --paginate repos/dotCMS/core/issues/<N>/comments --jq '.[] | {id, user: .user.login, body}'
```

Write `<RUN_DIR>/bundles/existing-feedback.md`: one line per inline comment (`path:line @user: <first 200 characters>`) and one per PR-level comment (`@user: <first 300 characters>`), bots included. Every reviewer gets its path (`agent-prompts.md`). Bots edit their comments after posting, so Stage 7b fetches both lists again.

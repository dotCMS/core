# PR Body — feat/dx-agent-orchestra
# (Copy this as the PR description when ready to push)

---

## Summary

This PR introduces an **AI agent orchestra** for the dotCMS frontend development workflow. It wires together a set of specialized Claude Code subagents, a Claude skill (slash command), and a shell hook to automate the full journey from a GitHub issue to a reviewed, QA-verified draft PR — all from within a Claude Code session.

The system is **frontend-only** (scoped to `core-web/` — Angular, TypeScript, SCSS) and is invoked by the developer with a single command: `/issue-to-pr <issue-number>`.

---

## Files

### `.claude/skills/issue-to-pr/SKILL.md`

**What it is:** A Claude Code *skill* — a slash command the developer invokes as `/issue-to-pr`. It is the orchestration script for the entire pipeline.

**Why it exists:** Without this skill, working a GitHub issue from end-to-end requires many manual steps: reading the issue, researching the codebase, designing pseudocode, writing code, reviewing it, running QA, and creating a PR. This skill coordinates specialized subagents to handle each concern in the right order, keeping the developer focused on decisions rather than plumbing.

**How the pipeline works (8 stages):**

```
Stage 1 — Resolve issue number
  Parse /issue-to-pr argument. Fall back to branch name pattern (issue-NNNNN-*).

Stage 2 — Fetch issue + run parallel agents
  gh issue view → feeds two agents simultaneously:
    • dotcms-issue-validator   → checks issue completeness (SUFFICIENT / NEEDS_INFO)
    • dotcms-product-analyst   → deep product research: pseudocode, AC, edge cases, coder brief

Stage 3 — Interactive product Q&A
  Present product analysis to developer.
  Ask open questions from the analyst. Gather answers.
  Ask for any additional constraints or concerns.
  Confirm final Coder Brief before coding begins.

Stage 4 — Create feature branch
  git checkout -b issue-<number>-<slug>  (if not already on one)

Stage 5 — Implementation
  Present the Coder Brief + file list to developer.
  Implement directly if asked, using the pseudocode as spec.

Stage 6 — Review changed files
  git diff --name-only HEAD → filter core-web/ frontend files
  dotcms-file-classifier classifies files into review buckets.
  Launch parallel reviewers (TypeScript / Angular / Tests / SCSS+HTML).
  Consolidate findings by severity (🔴 Critical → 🟡 Important → 🔵 Quality).
  Stage 6d: Apply fixes → re-run tests → re-run QA → reply + resolve review threads.

Stage 7 — QA
  Derive positive / negative / edge-case scenarios from the product analysis.
  Ask developer for real test data (host, API key, site ID, content types).
  Execute based on what changed:
    • SDK libraries  → build → npm pack → Node.js qa-test.mjs
    • Angular UI     → dev server + browser inspection
    • Services       → Jest tests + manual API calls
    • SCSS only      → visual screenshot comparison
  Report pass/fail table. Block PR creation on any failure.
  After any review fix (Stage 6d): mandatory QA re-run.

Stage 8 — Create draft PR + move issue
  Ask developer to confirm PR creation.
  github-workflow-manager creates the draft PR with:
    Closes #<number>, implementation summary, AC checklist, review findings, QA results.
  If PR is not draft: move the linked issue to "In Review" on the project board.
```

**Review comment protocol (Stage 6d):**
- Reply to each comment before resolving it. Reply can be brief ("Fixed.") or detailed (for non-obvious decisions or declined changes). Always end with `— 🤖 Claude` signature so the reviewer knows who responded and can continue the conversation from a clean spot.
- Resolve the thread after replying.
- Re-run QA after any code fix — a review fix may introduce a regression.

---

### `.claude/agents/dotcms-product-analyst.md`

**What it is:** A Claude Code *custom subagent* definition. Subagents are specialized AI instances launched by the orchestrator via the `Task` tool. This one plays the "Product Analyst" role in Stage 2.

**Why it exists:** Generic AI assistance tends to jump to solutions. The product analyst is constrained to a specific research process: understand the issue → research existing behavior → map acceptance criteria → write pseudocode in real Angular/NgRx patterns → identify edge cases. It only asks questions that are genuinely blocking — not "nice to know" items.

**Configuration:**
```yaml
model: sonnet          # Cost-balanced: good reasoning, lower latency than opus
color: purple          # Visual identification in Claude Code UI
allowed-tools:
  - Grep               # Search codebase for existing patterns
  - Glob               # Find files by path pattern
  - Read               # Read specific files
maxTurns: 20           # Enough for 5-phase research without runaway loops
```

**Research process (5 phases):**
1. **Understand the issue** — extract goal, current behavior, desired behavior, issue type, affected area
2. **Research existing behavior** — find the affected component/store/service (max 3 Grep/Glob, max 4 Read calls — focused, not exhaustive)
3. **Map acceptance criteria** — user-facing "done" statements
4. **Write pseudocode** — Angular/NgRx patterns: signal names, store methods, API endpoints, selectors
5. **Identify edge cases** — blocking risks, boundary conditions, integration concerns

**Output:** Structured block with Product Understanding → AC → Pseudocode → Files to Modify → Edge Cases → Open Questions → Coder Brief. The Coder Brief is written for the implementation agent, not for a human executive — it is tight and specific.

---

### `.claude/hooks/post-commit-review.sh`

**What it is:** A shell script wired as a `PostToolUse` Claude Code hook. It fires after every `Bash` tool call.

**Why it exists:** Developers often commit without remembering to review the changed files. This hook passively watches for successful frontend commits and nudges the developer to run the appropriate reviewers — without requiring them to remember the pipeline.

**How it works:**
```
1. Receives Claude Code hook JSON on stdin (tool_input + tool_response).
2. Extracts the bash command and its output via Python JSON parsing.
3. Guards:
   a. Only acts if the command contains "git commit"
   b. Only acts if the output matches a successful commit pattern: [branch abc1234]
4. Gets frontend files from the last commit:
   git diff --name-only HEAD~1 HEAD
   → filters: core-web/**/*.{ts,html,scss,css}
   → separates: production files vs *.spec.ts
5. If no frontend files: exits silently (no noise for backend-only commits).
6. Builds a recommended reviewer list based on file types:
   .ts files        → dotcms-typescript-reviewer
   .ts/.html files  → dotcms-angular-reviewer
   .spec.ts files   → dotcms-test-reviewer
   .scss/.css/.html → dotcms-scss-html-style-reviewer
7. Outputs a one-line recommendation to Claude's context:
   "[Post-commit] Frontend commit detected (N files). Recommended reviewers: ..."
```

The hook is informational only — it does not automatically trigger reviewers. The developer decides whether to act on the recommendation.

---

### `.claude/settings.json` (modified)

**What changed:** Added the `PostToolUse` hook registration that wires `post-commit-review.sh` to every `Bash` tool call.

```json
"hooks": {
  "PostToolUse": [
    {
      "matcher": "Bash",
      "hooks": [
        {
          "type": "command",
          "command": ".claude/hooks/post-commit-review.sh"
        }
      ]
    }
  ]
}
```

**Why `PostToolUse` on `Bash` (not a `git commit` hook):** Claude Code runs git via the Bash tool. A standard git `post-commit` hook would also fire but Claude Code's hook system gives us the full tool input + output in JSON, which lets us inspect whether the commit actually succeeded without a separate git command.

---

## Agents used in the pipeline

| Agent | Role | When |
|---|---|---|
| `dotcms-issue-validator` | Checks issue completeness | Stage 2 (parallel) |
| `dotcms-product-analyst` | Deep product research + pseudocode | Stage 2 (parallel) |
| `dotcms-file-classifier` | Sorts changed files into review buckets | Stage 6a |
| `dotcms-typescript-reviewer` | TypeScript type safety review | Stage 6b (if .ts files) |
| `dotcms-angular-reviewer` | Angular patterns review | Stage 6b (if .ts/.html files) |
| `dotcms-test-reviewer` | Test quality review | Stage 6b (if .spec.ts files) |
| `dotcms-scss-html-style-reviewer` | Styling standards review | Stage 6b (if .scss/.html files) |
| `github-workflow-manager` | Creates the draft PR | Stage 8 |

`dotcms-issue-validator`, `dotcms-file-classifier`, and all reviewers are existing agents. `dotcms-product-analyst` is new (added in this PR).

---

## Test plan

- [ ] Run `/issue-to-pr <number>` on a real frontend issue
- [ ] Verify Stage 2 agents run in parallel (both start before either finishes)
- [ ] Verify Stage 3 Q&A presents the product analysis and waits for user answers
- [ ] Verify Stage 6 launches only the reviewers matching the changed file types
- [ ] Verify Stage 7 asks for test data and blocks on QA failure
- [ ] Commit a frontend file and verify the post-commit hook fires with the right reviewer recommendation
- [ ] Commit a backend-only file and verify the hook stays silent
- [ ] Verify Stage 8 moves the issue to "In Review" when PR is not draft (requires `project` GitHub token scope)

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)

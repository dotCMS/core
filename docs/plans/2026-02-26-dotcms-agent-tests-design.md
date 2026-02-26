# Design: dotcms-agent-tests Skill

**Date:** 2026-02-26
**Author:** Steve Bolton
**Status:** Approved

## Problem

The `AGENTS.md` / `CLAUDE.md` file in dotCMS/core is the first context an AI agent receives. Research shows large, dense context files hurt correctness by ~2% while increasing inference costs 20-23% (ETH Zurich). The right pattern is minimal root guidance ("landmines only") with progressive disclosure to linked docs.

We need a way to validate that:
1. Agents can successfully complete common developer tasks starting from `AGENTS.md` alone
2. The context they gather is proportionate to the task (no unnecessary file reads)
3. When agents fail, we know exactly what to fix in `AGENTS.md` or where to add a breadcrumb

## Solution

A local skill (`~/.claude/skills/dotcms-agent-tests/`) that orchestrates isolated sub-agent test runs using worktrunk (`wt`) for worktree isolation, then evaluates transcripts with a review agent.

## Architecture

### Skill (local — never committed)

```
~/.claude/skills/dotcms-agent-tests/
  SKILL.md                          # Orchestration runbook
  review-rubric.md                  # Shared evaluation criteria for judge agent
  test-cases/
    BE-001-run-specific-test.md
    BE-002-build-command-selection.md
    BE-003-add-rest-endpoint.md
    FE-001-serve-test-lint.md
```

### Repo changes (PR content)

```
AGENTS.md                           # Updated: landmines prominent, breadcrumbs added
docs/dev/
  QUICK_REF_BACKEND.md              # Created only if tests reveal navigation gap
  QUICK_REF_FRONTEND.md             # Created only if tests reveal navigation gap
```

## Execution Flow

1. Skill invoked as `/dotcms-agent-tests` (all tests) or `/dotcms-agent-tests BE-001` (single test)
2. For each test case:
   - `wt switch --create agent-test-<id>-<timestamp>` creates an isolated worktree
   - A Task sub-agent is launched with the test prompt — fresh context, no history
3. All tests run in parallel (dispatching-parallel-agents pattern)
4. A review agent receives all transcripts + `review-rubric.md` and scores each test
5. Scorecard output: markdown table with PASS/FAIL, key observations, AGENTS.md diff suggestions
6. Failed worktrees kept for manual inspection; passed worktrees removed via `wt remove`

## Test Case Format

Each test case in `test-cases/` follows this structure:

```markdown
# <ID>: <Name>

## Task Prompt
(Verbatim prompt given to the isolated agent)

## Context Allowed
Starting point: AGENTS.md only (no prior conversation)

## Pass Criteria
- [ ] Agent does X
- [ ] Agent does NOT do Y
- [ ] Agent warns about Z

## Fail Indicators
- Specific wrong behaviors that indicate AGENTS.md gap

## AGENTS.md Gap Signal
If failing: what is missing or unclear
Suggested fix: concrete diff to AGENTS.md or new breadcrumb link
```

## Test Cases (Initial Suite)

### BE-001: Run a Specific Integration Test
**Prompt:** "I need to run just the ContentTypeAPIImplTest integration test. What command do I use?"
**Key landmine:** Silent skipping without `-Dcoreit.test.skip=false`
**Current AGENTS.md coverage:** ✅ Present — test validates it's prominent enough

### BE-002: Build Command Selection
**Prompt:** "I've made a small change to a Java class in dotcms-core only. What's the fastest way to build?"
**Key landmine:** Running full `./mvnw clean install` when a targeted command exists
**Current AGENTS.md coverage:** ⚠️ Commands listed but no decision logic

### BE-003: Add a REST Endpoint
**Prompt:** "I need to add a new GET endpoint to an existing JAX-RS resource class. Walk me through the pattern."
**Key landmine:** Missing `@Schema` implementation, wrong `@PathParam` vs `@QueryParam` usage
**Current AGENTS.md coverage:** ❌ No breadcrumb to REST patterns docs

### FE-001: Frontend Serve/Test/Lint
**Prompt:** "How do I start the frontend dev server and run the tests for dotcms-ui?"
**Key landmine:** OOM error on standalone NX builds without NODE_OPTIONS
**Current AGENTS.md coverage:** ⚠️ OOM gotcha present but Nx commands sparse

## AGENTS.md Gap Analysis

| Test | Gap | Remediation |
|------|-----|-------------|
| BE-001 | Silent skip warning may not be prominent enough | Keep in AGENTS.md, ensure bold/prominent |
| BE-002 | No build decision guidance | Add 1-line heuristic: "core-only change → `just build-quicker`" |
| BE-003 | No REST pattern breadcrumb | Add: `docs/backend/REST_API_PATTERNS.md` link to References section |
| FE-001 | Nx commands not listed, only `core-web/CLAUDE.md` referenced | Add 2-3 Nx commands inline; OOM note already present |

## Remediation Philosophy

Aligns with progressive disclosure principle:
- **Landmines** (non-obvious gotchas) → fix in `AGENTS.md` directly
- **Verbose content** (patterns, examples) → move to `docs/` with a breadcrumb link in `AGENTS.md`
- Never grow `AGENTS.md` past ~60 lines

## Success Criteria

- All 4 tests PASS after AGENTS.md updates applied
- Each test agent reads ≤ 3 files to answer the question (progressive, not exhaustive)
- No test agent suggests running the full 60-min test suite
- PR contains only `AGENTS.md` + optional `docs/dev/*.md` — no code changes

## References

- https://sulat.com/p/agents-md-hurting-you — landmine principle, attention loss research
- https://www.aihero.dev/a-complete-guide-to-agents-md — progressive disclosure, minimal root file
- worktrunk skill — `wt switch --create <name> -x claude -- '<task>'` for isolation

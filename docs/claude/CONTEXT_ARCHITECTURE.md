# AI Agent Context Architecture

How to structure instructions so AI coding agents get the right context at the right time — without bloating every conversation.

The principles in this guide are tool-agnostic. The appendix maps them to specific implementations in Claude Code, Cursor, Codex, and Aider.

## The Problem

Every token loaded into an agent's context window has a cost:
- It consumes space that could hold conversation, code, or tool output
- Large instruction files reduce adherence — important rules get lost in noise
- Irrelevant context distracts the agent from the actual task

The goal is to load **only what's needed, when it's needed** — while ensuring critical rules are never missed.

## Command Aliases as an Abstraction Layer

Before discussing where to put instructions, consider whether you need the instructions at all.

A `just build` command that wraps a complex Maven invocation means your instruction file can say `just build` instead of explaining Maven profiles, skip flags, image tagging, and output filtering. The alias absorbs the complexity — the agent doesn't need to know (or be told) what's behind it.

This creates a powerful separation of concerns:

| Without aliases | With aliases |
|---|---|
| `./mvnw install -pl :dotcms-core -DskipTests -Ddocker.version.tag=$(...)` | `just build-quicker` |
| Agent needs detailed Maven flags in context | Agent needs one command name |
| Changing the build process requires updating `.md` files | Changing the build process updates the `justfile` only |
| 5 lines of instruction per command variant | 1 line per command |

### Why this matters

1. **Reduces token cost.** `just dev-run` is 2 tokens. The Maven command it wraps is 30+. Multiply by every command in your instruction files.

2. **Decouples documentation from implementation.** When you change how `dev-run` detects shared services or resolves ports, you update the `justfile` — not your instruction files. The files stay stable because they reference the alias, not the underlying command.

3. **Makes instruction files more durable.** An instruction file that says `just build` has been correct for every iteration of the build system. One that says `./mvnw install -DskipTests -Pdocker-build` breaks every time a profile changes.

4. **Agents can discover details on demand.** If an agent needs to understand what `just dev-run` does, it can read the `justfile` recipe — which is always up to date because it's the actual implementation. This is more reliable than documentation that may have drifted.

5. **Enables progressive disclosure.** The root instruction file says `just dev-run`. A skill explains modes, port resolution, and shared services. The justfile has the implementation. Each layer adds detail only when needed.

### Practical guidelines

- **Wrap complex commands in aliases** (`just`, `make`, shell scripts, `npm run`). If a command has more than 2 flags or requires environment-specific logic, it should be an alias.
- **Name aliases to be self-documenting.** `just dev-run`, `just build-quicker`, `just test-e2e-node` — an agent can guess what these do without instructions.
- **Document the alias, not the underlying command.** Write `just build` — not the Maven command it wraps.
- **Keep alias discovery easy.** `just --list` (or equivalent) lets agents explore available commands without any instruction file. Mention this once in your root instructions.
- **Let the alias handle complexity.** Port resolution, shared service detection, image tagging, staleness checks — all belong in the alias implementation, not in instruction files.

## Core Concepts

Every AI coding tool provides some combination of these five context delivery mechanisms. The names and implementations differ, but the concepts are universal.

### The Five Layers

| Layer | Concept | When loaded | Reliability | Token cost |
|---|---|---|---|---|
| **Root instructions** | Project-wide guidance loaded into every conversation | Conversation start | **Guaranteed** | Every conversation |
| **Path-scoped rules** | Short guidance triggered by file patterns (e.g., all `*.java` files) | When agent touches matching files | **Deterministic** | Only when triggered |
| **Directory-scoped instructions** | Guidance for a subtree of the repo (e.g., `core-web/`) | When agent reads files in that directory | **Deterministic** | Only when triggered |
| **On-demand skills/workflows** | Multi-step procedures or domain knowledge loaded when relevant | Agent decides or user invokes | **Probabilistic** | On-demand |
| **Reference docs** | Detailed documentation too large for rules or skills | Agent follows a pointer (or doesn't) | **Unreliable** to **Guaranteed** (depends on mechanism) | Varies |

### How they compose

```
Root instructions (always loaded, ~80-100 lines)
  ↓ mentions skills exist, lists docs index

Path-scoped rules (loaded when matching files touched)
  ↓ contains short reminders, points to detailed docs

Directory instructions (loaded when agent enters directory)
  ↓ contains conventions and commands for that subtree

Skills (loaded on demand)
  ↓ contains full workflow instructions, may reference supporting files

Reference docs (loaded if pointed to)
  ↓ detailed standards, patterns, examples
```

Each layer adds detail. An agent editing a Java file gets: root instructions (always) + Java rules (triggered by `*.java`) + REST API directory instructions (if in that directory). An agent fixing a typo in a README gets only: root instructions.

## Decision Framework: Where to Put What

### Step 0: Can This Be an Alias?

If the content is "how to run a command with specific flags," wrap it in an alias and document the alias name only. Don't document implementation — document the interface.

### Step 1: Does Every Conversation Need This?

Ask: **"Would removing this cause the agent to make mistakes on any task?"**

If yes → **root instructions**.

Examples that pass this test:
- Project identity and tech stack (1-2 lines)
- Setup commands the agent can't guess (`just setup`, `just build`, `just dev`)
- Universal gotchas that cause silent failures ("tests are silently skipped without `-Dskip=false`")
- Hard constraints ("don't use `EnterWorktree`", "must work on macOS and Linux")
- Core dev loop (build -> run -> restart cycle)

Examples that fail:
- Java coding patterns (only needed when editing Java)
- Frontend conventions (only needed when editing frontend code)
- Docker/dev-run details (only needed when managing the dev environment)
- E2E test patterns (only needed when writing E2E tests)
- Cross-platform pitfall table (only needed when writing shell scripts)

**Target: under 200 lines.** Under 100 is better. For each line, ask: "Would removing this cause the agent to make mistakes?" If not, cut it.

### Step 2: Is This Tied to a File Type or Directory?

If the guidance applies when working with **specific file patterns** (e.g., all `.java` files, all `*.sh` files, the `justfile`) that span multiple directories, use a **path-scoped rule**.

If the guidance applies to **everything in a directory subtree** (e.g., all of `core-web/`), use a **directory-scoped instruction file**.

| Scope | Use | Why |
|---|---|---|
| `**/*.java`, `**/pom.xml` | Path-scoped rule | Files scattered across tree — can't use directory scoping |
| `justfile`, `**/*.sh`, `.config/wt.toml` | Path-scoped rule | Cross-cutting by file type, not directory |
| Everything in `core-web/` | Directory instruction file | Natural subtree — directory scoping matches perfectly |
| `**/*.spec.ts`, `**/*Test.java` | Path-scoped rule | Test files live everywhere |
| `.github/workflows/*.yml` | Path-scoped rule | Infrastructure files in a specific location |

### Step 3: Is This a Workflow or Task, Not a Rule?

If the content describes **how to do something** (a multi-step process, a command sequence, a workflow), use a **skill** (or equivalent on-demand mechanism).

Skills are ideal for:
- Dev lifecycle operations (starting/stopping services, shared infrastructure)
- Multi-step workflows (create issue -> implement -> test -> PR)
- Domain knowledge that's only relevant during specific tasks
- Procedures with side effects that should be user-invoked

Rules are better for:
- Coding patterns and conventions (always use X, never do Y)
- Architecture constraints that apply whenever touching certain files
- Short reminders (under ~50 lines)

### Step 4: Is This Reference Material?

For detailed documentation that's too long for rules or skills, keep it in `docs/` and reference it. The reliability of the reference depends on how you point to it:

| Mechanism | Reliability | Use when |
|---|---|---|
| Inline import (expanded at launch) | **Guaranteed** | Critical docs — but adds to every conversation's token cost |
| Reference from within a skill | **On-demand** | Detailed reference loaded only when the skill activates |
| Reference from a path-scoped rule | **Deterministic** | Reminder triggered by file pattern, pointing to deeper docs |
| Plain text "see docs/X.md" | **Unreliable** | Nice-to-have pointers the agent may or may not follow |

#### Inline imports

An inline import is a directive in your root instruction file that tells the agent's runtime to fetch another file and expand its contents into the instruction context at launch — before the conversation starts. The imported file's content becomes part of the root instructions as if you had copy-pasted it in.

This is different from a plain text reference like "see docs/X.md" where the agent must decide to open the file. With an inline import, the content is **guaranteed** to be in context — the agent doesn't choose; the runtime injects it automatically.

**When to use inline imports:**
- A small file (<50 lines) that every conversation needs but you want to maintain separately — e.g., importing `package.json` so the agent knows available npm scripts, or importing a short coding standards summary
- Personal overrides that shouldn't be checked in — e.g., importing `~/.claude/my-project-prefs.md` from the shared project instruction file

**When NOT to use inline imports:**
- Large reference docs (>100 lines) — they inflate every conversation even when irrelevant. Use path-scoped rules or skills instead.
- Content that's only relevant for specific file types — use path-scoped rules, which load conditionally
- Volatile content that changes frequently — every change affects every conversation's baseline context

**The key trade-off:** inline imports trade context space for reliability. You get guaranteed loading, but you pay the token cost in every conversation. For most content, path-scoped rules or skills are a better fit — they load reliably when needed without taxing conversations that don't need them.

See Appendix A for the tool-specific syntax (`@path` in Claude Code).

## Skill Architecture

Skills (or their equivalent in different tools) support a richer structure than rules. They can contain instructions, scripts, templates, and supporting files.

### When to Use Instructions vs Scripts

**Use instructions (markdown) when:**
- Providing conventions, patterns, or decision guidance
- The agent should adapt the guidance to the specific context
- The output varies based on what the agent discovers

**Use scripts (bundled in the skill directory) when:**
- The task requires deterministic, repeatable execution
- Complex logic that would be error-prone as natural language instructions
- The output is structured data the agent needs to interpret (JSON, HTML, reports)
- External tool orchestration (CLI tools, Docker commands, API calls)

**Use both when:**
- A script generates data, and instructions tell the agent how to interpret and act on it
- Setup requires deterministic steps, but the follow-up requires judgment
- You want to inject dynamic context before the agent sees the prompt

### Supporting Files

Keep the main skill file under 500 lines. Move detailed reference material to supporting files that the agent loads on demand:

```
my-skill/
  SKILL.md              # Overview, navigation, core instructions (required)
  reference.md          # Detailed API docs (loaded when needed)
  examples.md           # Usage examples (loaded when needed)
  templates/
    template.md         # Templates the agent fills in
  scripts/
    validate.sh         # Scripts the agent executes
```

Reference supporting files from the main file so the agent knows what's available and when to load them.

## Anti-Patterns

### Duplicating content across mechanisms

If the same rule appears in root instructions, a rule file, AND a skill, they will drift. Pick one authoritative location and reference it from others.

### Putting everything in root instructions

Loading 200+ lines of Java patterns, frontend conventions, Docker commands, and test rules into every conversation about a typo fix. Most of it is noise.

### Relying on plain text signposts for critical rules

"See docs/X.md for important security patterns" — the agent may never read it. If a rule must be followed, put it in a path-scoped rule or in the root instructions.

### Making skills too large

A 1000-line skill file defeats the purpose of on-demand loading. Keep it under 500 lines; move reference material to supporting files.

### Using inline imports for large reference docs

Inline imports (see [Step 4](#step-4-is-this-reference-material)) expand at launch into every conversation. Importing a 500-line standards doc means every conversation pays the token cost whether it's relevant or not. Use path-scoped rules or skills instead.

### Putting volatile information in instruction files

Build commands, architecture decisions, and conventions belong in instruction files. Sprint status, ticket numbers, and temporary workarounds do not — those belong in session memory or conversation context.

### Documenting raw commands instead of aliases

Writing `./mvnw install -pl :dotcms-core -DskipTests -Ddocker.version.tag=$(just _worktree-slug)` instead of `just build-quicker`. The raw command consumes more tokens, breaks when flags change, and forces you to update documentation alongside implementation.

### Hard-coding values that the alias resolves

Instruction files that say "start on port 8082" or "the management port is 8090" instead of "run `just dev-urls` to get actual ports." Hard-coded values drift; commands that discover live state are always correct.

### Accumulating workarounds instead of fixing root causes

When a gotcha, caveat, or "make sure you also..." instruction appears in your context files, ask whether it's masking a problem that should be fixed in code. Every workaround has a compounding cost:

- It consumes tokens in every conversation (or every triggered rule) that includes it
- It must be duplicated into every context that touches the affected area — root instructions, rules, skills, and sub-directory files all need the same warning
- When the workaround is missed in one context, the agent silently does the wrong thing
- Over time, instruction files accumulate workarounds faster than they shed them, bloating context and reducing adherence to the rules that actually matter

**The test:** If you find yourself adding the same workaround to a second file, stop and ask whether the underlying code, tooling, or build system should change instead. A well-designed alias, a default that matches the common case, or a guard rail in the code itself eliminates the need for the instruction entirely — across all contexts, all tools, all agents, permanently.

Examples:
- "Always pass `-Dskip=false` or tests silently skip" -> should the default be to run tests, with an opt-out flag instead?
- "Restart the frontend dev server if the backend port changes" -> should the proxy auto-detect port changes?
- "Don't forget to run `yarn install` after rebasing" -> should the post-checkout hook handle this?

Not every workaround can be eliminated — some reflect genuine platform constraints or upstream decisions you can't change. But treating workarounds as a signal to investigate, rather than as permanent fixtures of your documentation, keeps context lean and the codebase healthier.

## Summary Checklist

When adding new instructions, ask these questions in order:

0. **Can this be an alias instead of documentation?** -> Wrap it in `just`/`make`/`npm run` and document the alias name only
1. **Does every conversation need this?** -> Root instructions
2. **Is it tied to a file type across the repo?** -> Path-scoped rule
3. **Is it specific to a directory subtree?** -> Directory-scoped instructions
4. **Is it a workflow or multi-step procedure?** -> Skill / on-demand workflow
5. **Is it detailed reference material?** -> `docs/`, referenced from rules or skills
6. **Does it need to work across multiple AI tools?** -> Use cross-agent file formats

---

## Appendix A: Tool-Specific Implementation

The concepts above map to different file names, directories, and frontmatter across tools.

### File Discovery

| Concept | Claude Code | Cursor | Codex | Aider | Copilot |
|---|---|---|---|---|---|
| Root instructions | `CLAUDE.md` | `AGENTS.md` | `AGENTS.md` | `AGENTS.md` | `.github/copilot-instructions.md` |
| Path-scoped rules | `.claude/rules/*.md` | `.cursor/rules/*.mdc` | -- | -- | -- |
| Directory instructions | Sub-dir `CLAUDE.md` | Sub-dir `AGENTS.md` | Sub-dir `AGENTS.md` | -- | -- |
| Skills / workflows | `.claude/skills/*/SKILL.md` | -- | -- | -- | -- |
| Inline imports | `@path` in CLAUDE.md | -- | -- | -- | -- |

**Cross-agent compatibility** at the root level requires either symlinks (`CLAUDE.md -> AGENTS.md`) or maintaining both files. Sub-directory files need separate `CLAUDE.md` and `AGENTS.md` for full cross-agent coverage.

### Tools with Single-File Constraints (Copilot)

GitHub Copilot coding agents read `.github/copilot-instructions.md` — a single flat file with no conditional loading, no path-scoping, no skills, and no directory-scoped overrides. Every token in this file is loaded into every conversation.

This means the decision framework collapses to **Step 1 only**: does every conversation need this? If not, it shouldn't be in the file.

> **Scope note:** The existing `.github/copilot-instructions.md` and `.devcontainer/` configuration predate this architecture and have not yet been updated to align with these guidelines. Bringing them into alignment is future work — tracked separately from this document.

**Guidelines for single-file tools:**

1. **Prioritize ruthlessly.** Without conditional loading, you can't afford domain-specific content (Java patterns, Angular conventions, test patterns). Keep the file focused on universal essentials.
2. **Lean heavily on aliases — but provide fallbacks.** `just build-quicker` saves tokens and stays correct, but Copilot coding agents may run in devcontainers or Codespaces where `just` (and `mise`) aren't installed. Include the raw Maven/Nx fallback commands for essential operations (build, test) alongside the aliases. Point to the `justfile` as the source of truth for what each alias does.
3. **Accept the gap.** A single-file tool cannot match the context quality of a layered tool. Don't try to compensate by making the file enormous — a 200-line file with high signal-to-noise ratio outperforms an 800-line file where critical rules get lost.
4. **Point to discoverable resources.** `just --list` (when available), `docs/README.md`, and directory-level `AGENTS.md` files can be read on demand by the agent. Mention they exist; don't duplicate their content.
5. **Don't duplicate content from layered files.** The Copilot file will drift from `.claude/rules/`, `.cursor/rules/`, and `AGENTS.md`. Keep it as a lean complement, not a copy.
6. **Account for the execution environment.** Copilot coding agents run in GitHub-hosted containers, not developer machines. The devcontainer configuration (`.devcontainer/`) determines what tools are available. If the devcontainer doesn't install `just` or `mise`, the agent can't use alias commands — document the raw commands as fallbacks for essential operations.

**Key observations about the current state** (for future alignment work):
- `.github/copilot-instructions.md` is ~885 lines — well beyond the recommended ceiling for a single-file tool. It duplicates Java patterns, Angular conventions, security rules, and Maven commands that exist in `.claude/rules/`, `.cursor/rules/`, and domain-specific `AGENTS.md` files.
- `.devcontainer/devcontainer.json` does not install `mise` or `just`, and pins Node 20 instead of the project's required Node 22 (from `.nvmrc`). Copilot coding agents running in this container cannot use `just` aliases.
- The Copilot file documents raw Maven commands (e.g., `./mvnw install -pl :dotcms-core --am -DskipTests`) rather than referencing `just` aliases, which is the opposite of the alias-first approach recommended here — but is currently necessary given the devcontainer gap.

### Rule Frontmatter

Claude Code and Cursor both support path-scoped rules but use different frontmatter formats.

**Claude Code** (`.claude/rules/*.md`):
```yaml
---
paths:
  - "**/*.java"
  - "**/pom.xml"
---
```
- `paths:` — YAML list of glob patterns
- No `paths:` field — rule loads unconditionally

**Cursor** (`.cursor/rules/*.mdc`):
```yaml
---
description: "Java backend patterns"
globs: "**/*.java, **/pom.xml"
alwaysApply: false
---
```
- `globs:` — comma-separated string of glob patterns
- `alwaysApply: true` — loads unconditionally
- `description:` — used for agent-decided loading when no globs match

### Maintaining Rules for Both Tools

The `.claude/rules/` and `.cursor/rules/` directories are completely independent — each tool reads only its own.

**Options for keeping them in sync:**

1. **Separate files, shared content** — simplest. Copy content between the two; update both when rules change. Practical for <10 rule files that change infrequently.

2. **Dual frontmatter + symlinks** — a single file specifies both `paths:` and `globs:`. Each tool reads the field it understands and ignores the other. Symlinks add maintenance friction and the `.md` vs `.mdc` extension difference may cause issues.

3. **Script-generated** — a `just sync-rules` recipe converts between formats. Overkill unless you have many rules.

### Skill Invocation Control (Claude Code)

| Configuration | User can invoke | Agent can invoke | When description loads |
|---|---|---|---|
| Default | Yes (`/name`) | Yes (when relevant) | Always in context |
| `disable-model-invocation: true` | Yes | No | Not in context |
| `user-invocable: false` | No | Yes | Always in context |

Use `disable-model-invocation: true` for workflows with side effects (deploy, send messages). Use `user-invocable: false` for background knowledge the agent should apply automatically.

---

## Appendix B: dotCMS Repository Layout

How these concepts are applied in this monorepo.

### Structure

```
/                               # Root
  AGENTS.md                     # Root instructions (Cursor, Codex, Aider)
  CLAUDE.md -> AGENTS.md        # Symlink for Claude Code
  .github/
    copilot-instructions.md     # Copilot coding agent (single-file, no layering)
  .claude/
    rules/                      # Claude Code path-scoped rules (8 files)
    skills/                     # Claude Code skills
      dotcms-dev-services/      #   Dev lifecycle (start, stop, shared services)
      dotcms-worktree/          #   Worktree management
      ...
  .cursor/
    rules/                      # Cursor path-scoped rules (6 files)
  core-web/
    AGENTS.md                   # Frontend conventions (all agents)
  dotCMS/src/.../rest/
    AGENTS.md                   # REST API patterns (all agents)
  docs/                         # Detailed reference (loaded on demand)
```

### What goes where

| Content | Location | Loaded |
|---|---|---|
| Project identity, tech stack | Root AGENTS.md | Always |
| Setup commands | Root AGENTS.md | Always |
| Core dev loop | Root AGENTS.md | Always |
| Critical gotchas | Root AGENTS.md | Always |
| "Don't use EnterWorktree" | Root AGENTS.md | Always |
| Test skip flags warning | Root AGENTS.md | Always |
| Cross-platform shell rules | Path-scoped rule (`justfile`, `*.sh`) | When editing scripts |
| Java patterns (Config, Logger) | Path-scoped rule (`**/*.java`) | When editing Java |
| Maven BOM hierarchy | Path-scoped rule (`**/pom.xml`) | When editing POMs |
| CI/CD workflow patterns | Path-scoped rule (`.github/workflows/*.yml`) | When editing workflows |
| Frontend Angular conventions | `core-web/AGENTS.md` | When reading core-web/ files |
| Test patterns (Spectator, Jest) | Path-scoped rule (`**/*.spec.ts`) | When editing tests |
| Dev lifecycle (dev-run, shared services) | Skill (`dotcms-dev-services`) | On demand |
| Worktree management | Skill (`dotcms-worktree`) | On demand |
| Detailed Java standards | `docs/backend/JAVA_STANDARDS.md` | Referenced from rules |
| Detailed Angular standards | `docs/frontend/ANGULAR_STANDARDS.md` | Referenced from rules |

### How references chain

```
Root AGENTS.md
  -> mentions skills exist (agent discovers them from descriptions)
  -> lists docs/ index for humans

Path-scoped rule (triggered by *.java)
  -> contains short reminders
  -> references docs/backend/JAVA_STANDARDS.md for full patterns

core-web/AGENTS.md (triggered by core-web/ files)
  -> contains conventions and commands
  -> references docs/frontend/ files for detailed standards

Skill (invoked on demand)
  -> contains full workflow instructions
  -> may reference supporting files for edge cases
```

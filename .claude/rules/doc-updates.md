---
paths:
  - "**/*.mdc"
  - "CLAUDE.md"
  - "AGENTS.md"
  - "docs/**/*"
---

# Documentation Updates

## Single source of truth
- **AGENTS.md** -- navigation and quick reference only.
- **`/docs/`** -- full patterns by domain (backend, frontend, testing, etc.).
- **`.claude/rules/`** -- short reminders scoped by file path; reference `/docs/`, don't duplicate.
- **`.claude/skills/`** -- workflows and procedures; reference supporting files for detail.

## Where to update
- **Java/Config/REST/DB** -> `docs/backend/*.md`
- **Angular/tests/styles** -> `docs/frontend/*.md`
- **Build/CI/Docker** -> `docs/core/`, `docs/backend/MAVEN_BUILD_SYSTEM.md`, `docs/infrastructure/`
- **Testing** -> `docs/frontend/TESTING_FRONTEND.md`, `docs/testing/*.md`

## Context architecture
- See `docs/claude/CONTEXT_ARCHITECTURE.md` for decision framework on where to place new instructions.

## Quality
- Use examples. Cross-reference instead of duplicating. Document aliases, not raw commands.

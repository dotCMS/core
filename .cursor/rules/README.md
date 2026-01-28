# Cursor Project Rules

Project rules live in `.cursor/rules/`. Cursor applies them according to the rule type.

## Rule types (frontmatter)

| Type in Cursor | Frontmatter | When it applies |
|----------------|-------------|-----------------|
| **Always Apply** | `alwaysApply: true` | On every chat |
| **Apply to Specific Files** | `globs: ["..."]` + `alwaysApply: false` | When the open file/context matches the pattern |
| **Apply Intelligently** | `description: "..."` + `alwaysApply: false` | When the Agent deems the description relevant |
| **Apply Manually** | No alwaysApply/globs | Only if you mention the rule with @ (e.g. `@doc-updates`) |

## Current rules

- **dotcms-guide.mdc** – Always Apply. Navigation and critical reminders.
- **frontend-context.mdc** – Globs: `core-web/**/*.{ts,tsx,html,scss,css}`. Nx monorepo, Angular, SDK, docs/frontend index.
- **java-context.mdc** – Globs: `**/*.java`, `**/pom.xml`, `dotCMS/src/**/*`. Config, Logger, Maven.
- **test-context.mdc** – Globs: `**/*.spec.ts`, `**/*Test.java`, etc. Spectator, data-testid.
- **doc-updates.mdc** – Globs: `**/*.md`, `docs/**/*`. Where to update docs, DRY.

## Best practices (Cursor docs)

- Keep rules **short** (< 500 lines; ideally ~50 for reminders).
- **One concern** per rule; split if they grow.
- **Descriptions** that are clear and keyword-rich for Apply Intelligently.
- Use **`.mdc`** (not `.md`) so Cursor interprets `description`, `globs`, `alwaysApply` correctly.
- Put long details in **`/docs/`** and reference with `@docs/...` instead of copying into the rule.

## Reference

- [Cursor Rules (rule.md)](../rule.md) – Official documentation.
- **CLAUDE.md** – Main guide for the repo; rules point to `/docs/` for detail.

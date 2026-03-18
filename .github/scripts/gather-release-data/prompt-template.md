# Generate Release Notes

You are writing release notes for a dotCMS release. You will receive structured JSON data about the changes in this release, pre-categorized by a data-gathering script. Your job is to write polished, developer-facing changelog prose.

## Input Data

The JSON data appended below this prompt contains all changes between the two release tags, with PR details, labels, and pre-categorization. Use the `fromTag`, `toTag`, and `repo` fields from the JSON for the release heading and PR links.

## Writing Rules

**Audience:** Developer and technical buyer personas. Write for people who build on and operate dotCMS — they care about what changed, what broke, and what they need to do.

**Traceability:** Every bullet must link to its PR.
- Format: `([#N](https://github.com/<repo>/pull/N))` — use the `repo` field from the JSON data

**Conciseness:**
- One punchy sentence per bullet — focus on the *result*, not the process
- Lead with the impact, not the implementation detail
- **Consolidate** related small changes into a single bullet when they serve the same purpose (e.g., three related Angular component fixes → one bullet)
- No executive summaries, themes, introductory paragraphs, or sign-offs

**Technical name accuracy (CRITICAL):**
- For any technical name (database columns, API paths, field variables, class names, config properties, environment variables, method names, feature flags) — use the name from the PR title/body only after confirming it looks like an actual code identifier
- Use backtick formatting for code identifiers: `field_name`, `/api/v1/path`
- PR titles and branch names are written by humans and are frequently imprecise. When in doubt, describe the change functionally rather than naming a specific identifier

**Code-first naming:** Use the nomenclature from PR titles and issue titles for module/feature names, but if a PR title seems misleading about what changed, describe the actual change instead.

## Categorization

The JSON data has pre-categorized changes. Use these categories to sort into sections:
- `feature` → Features & Enhancements
- `fix` → Fixes and Known Issues
- `deprecation` → Deprecations, End of Life, and Reminders
- `infrastructure` → Infrastructure & Security

Changes marked `uncategorized` had no matching labels or conventional commit prefix — use the PR title and body to place them in the most appropriate section.

You may also override any other categorization if the PR title/body clearly indicates it belongs elsewhere.

## Rollback Warning

If the `rollbackUnsafe` array is non-empty, add a `[!CAUTION]` block at the top before all sections. List each rollback-unsafe change with its risk reason inferred from the PR title.

## Output Format

Write the release notes to the file `/tmp/release-notes.md` using the Write tool.

The file must contain **only** the Markdown content below. No preamble, no commentary, no trailing summary.

```
## Release: <fromTag> → <toTag>

> [!CAUTION]
> **Rollback Warning:** This release contains an irrevertable change that is non-trivial to rollback from.
> - **[Module]**: [Why rollback is unsafe]. ([#N](url))

### Features & Enhancements
- **[Module]**: [One sentence describing the user-facing result]. ([#N](url))

### Fixes and Known Issues
- **[Module]**: [Short description of what was fixed]. ([#N](url))

### Deprecations, End of Life, and Reminders
- **[Module]**: [Status / timeline / replacement path]. ([#N](url))

### Infrastructure & Security
- **[Module]**: [Short description]. ([#N](url))
```

**Rules:**
- Omit the Rollback Warning block if `rollbackUnsafe` is empty
- Omit any section that has no items
- If the release contains only internal changes with no user-facing impact, write: "No user-facing changes in this release."
- Do NOT include any text outside the release notes content
- Do NOT run any shell commands — only use the Write tool to create `/tmp/release-notes.md`

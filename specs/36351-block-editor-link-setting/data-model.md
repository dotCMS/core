# Phase 1 — Data Model

**Feature**: Block Editor 2.0 — hyperlinks stop depending on Allowed Blocks
**Date**: 2026-09-14

## No persisted model changes

This feature introduces, alters and removes **nothing** in any persisted model:

- No database schema change, no new table or column, no upgrade task.
- No Elasticsearch / OpenSearch mapping change.
- No REST contract, no `@Schema`, no `openapi.yaml` regeneration.
- No change to the stored content JSON produced or accepted by the editor.

Nothing here falls into the
[Rollback-Unsafe Change Categories](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md).

The only entity involved is read-only from this feature's point of view, and is documented below
because the defect is a mismatch *about* it.

---

## Entity: Allowed Blocks setting

**What it is**: the per-field list of editor capabilities an administrator permits on a Block
Editor field.

| Property | Value |
|----------|-------|
| Stored as | A field variable on the content-type field |
| Key | `allowedBlocks` |
| Value | Capability identifiers joined with `,` — e.g. `bulletList,blockquote,image` |
| Absent / empty | Means **unrestricted**: every capability is permitted |
| Written by | The Block Editor field's Settings tab (content-type editor) |
| Read by | Both the current and the legacy Block Editor |

**Changed by this feature**: nothing. Not the key, not the format, not the set of values it may
contain, not the empty-means-everything rule. One consumer simply stops consulting it.

**Validation rules** (unchanged, restated because the edge cases depend on them):

- An identifier the product no longer offers is tolerated and ignored.
- A list naming every available capability behaves identically to an empty list.
- The value is free-form text: it can be written by an API client or by hand, so it may contain
  identifiers the Settings tab would never produce — including `link`. After this change that
  particular case is inert rather than meaningful, which is why the spec lists it as an edge case
  and the plan tests it.

---

## Relationship: capability identifiers

The defect is not in the entity but in the relationship between two sets of identifiers that were
never kept aligned.

- **Producible**: the identifiers the Settings tab can write into the value.
- **Consulted**: the identifiers some part of the editor tests the value for.

A capability that is *consulted but not producible* can never be permitted on a restricted field —
that is the bug class, hit three times now (emoji in #37340, hyperlinks here, and two more still
open). The full membership of both sets, before and after this change, is the contract in
[contracts/allowed-blocks-capability-keys.md](./contracts/allowed-blocks-capability-keys.md).

**State transitions**: none. There is no lifecycle, no status field, and nothing in this feature
moves the entity between states.

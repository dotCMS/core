# Data Model: Inline single-file output mode

**Feature**: [spec.md](spec.md) · **Research**: [research.md](research.md)

No persisted data. The "entities" are the file shapes the skill reads and writes. `SKILL.md` must describe each one exactly as below.

---

## Branch

The complete content of one side of a migration.

| Field | Rule |
|---|---|
| `role` | `migrated` or `legacy` |
| content | Copied exactly. Never re-indented, reformatted or line-ending-normalized (FR-002, FR-009). May lack a trailing newline; may contain CRLF (`tag/tag_storage_field_creation_old.vtl` does). |

## Inline file

```
#if( $structures.isNewEditModeEnabled() )
<migrated branch, from column 0>
#else
<legacy branch, from column 0>
#end
```

| Rule | Source |
|---|---|
| Header exactly `#if( $structures.isNewEditModeEnabled() )` | FR-001 |
| `#else` and `#end` each on their own line, at block depth 0 | research R-005 |
| No `#parse` added by the skill (a branch may carry its own) | FR-001 |
| Branches not indented | FR-002, research R-002 |

## Router (three-file mode only)

```
#if( $structures.isNewEditModeEnabled() )
	#parse('/static/<dir>/<name>_new.vtl')
#else
	#parse('/static/<dir>/<name>_old.vtl')
#end
```

Tab-indented, full server paths, same directory as the original. This is today's shape. When collapsing, any indentation, CRLF and either quote style are accepted: `htmlpage_assets/url-title.vtl` uses four spaces and `tag/tag_storage_field_creation.vtl` uses CRLF.

## Three-file set

`<name>.vtl` (router), `<name>_new.vtl` (migrated branch) and `<name>_old.vtl` (legacy branch), all in one directory.

## Pre-inline findings

| Finding | Severity | What the skill says |
|---|---|---|
| Same `#macro` name in both branches | blocking | the macro name; the first definition in the file (the migrated one) wins in both modes, so the legacy editor runs the new macro; offers three files |
| Legacy branch would not parse (unclosed block, stray `#end`, `#else` outside `#if`) | blocking | the line; inline spreads the error to the new edit mode; offers three files |
| Migrated branch would not parse (FR-010c) | blocking | when the skill migrated it: a migration bug — fix it before emitting anything; when it came from the user (conversion): report it like the legacy case and write nothing |
| Same variable `#set` in both branches | warning | the variable names; only one branch runs; three files is the alternative |
| none | — | one-line note that both branches are parsed together and the checks found nothing blocking |

## Transitions

```
legacy VTL ──migrate (default)──► Inline file ──split (+ server path)──► Three-file set
legacy VTL ──migrate ("three files")────────────────────────────────────► Three-file set
Three-file set ──collapse──► Inline file
any → Inline file: a blocking finding stops it; emitted only if the user then insists
```

Invariant on every arrow except "migrate": both branches' content is unchanged (SC-002).

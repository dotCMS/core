# Handoff to the coordinator session

## New issue to create: the UVE experiments panel

`uve-experiments-panel-issue.md` is the issue body, ready to post. It is not created from this
worktree: GitHub issue edits for this epic happen only in the coordinator session.

**Title**

    Experiments Portlet — the page's experiments as a UVE panel, behind FEATURE_FLAG_EXPERIMENTS_PORTLET

**Metadata** (mirrors #37005)

| Field | Value |
| --- | --- |
| Labels | `Team : Falcon`, `dotCMS : Experiments`, `Type : Task` |
| Issue type | Task |
| Parent | #36763 (Experiments: A/B Testing v2) |
| Project | dotCMS - Product Planning |

**Command**

    gh issue create --repo dotCMS/core \
      --title "Experiments Portlet — the page's experiments as a UVE panel, behind FEATURE_FLAG_EXPERIMENTS_PORTLET" \
      --label "Team : Falcon" --label "dotCMS : Experiments" --label "Type : Task" \
      --body-file specs/37005-experiments-uve-integration/handoff/uve-experiments-panel-issue.md

Type and parent are not settable through `gh issue create`; set them after, as the other epic
issues were.

**Scope decision already taken** (by the developer, this session): the panel carries the **list**
(phase 1) and **Configure** (phase 2). Results stays full-screen. The body records why, and
records that phase 2's width question is deliberately left open.

**Relation to the siblings**

- Depends on #37005 (this branch): the flag, the page-filtered destination and the round-trip.
- Overlaps #37007 (server-side list contract) at one point: the panel should call the existing
  `getAll(pageId)` instead of `getAllUnfiltered()` + the bulk page lookup. Same swap point.
- Makes #37008 (migration) smaller: the routed `experiments` child of the UVE shell and the legacy
  branch of the nav item both go away with the panel, and no full-page eject survives.

## Also worth passing on from this session

- `GET /api/v1/menu` on this instance returns 43 items with no `experiments` entry. The portlet is
  opt-in by design, so FR-021c's "clearable, revealing the full site-wide list" has no in-app
  destination on a stock build. The panel resolves this by being page-scoped by construction, with
  an explicit link out.
- Adding `FEATURE_FLAG_EXPERIMENTS_PORTLET` to `UVE_FEATURE_FLAGS` would **invert its default**:
  `withFlags` maps `FEATURE_FLAG_NOT_FOUND` to `true`. Recorded as item 4 of the issue.

# 03 · Container VTL — rendering the slot

The container itself (folder scaffold, `container.vtl`, `preloop`/`postloop`, one-vs-many
guidance) is in [core/06](../core/06-containers.md) — create it there first. This file
covers only what VTL adds on top.

## `<Var>.vtl` — one file per content type

- **Filename = the content type's `Var`, case-exact** — read it back from dotCMS, never assume
  it matches the type's name ([core/02](../core/02-content-types.md)). `Book.vtl` renders a
  `Book`; `book.vtl` renders nothing, silently.
- Each file renders **one** contentlet, read via `$dotContentMap`. The container loops;
  your file is the body of that loop.
- Put per-type markup here, never in `preloop`/`postloop` — those wrap the whole slot.

```velocity
<section class="card">
  <h2>$esc.html($dotContentMap.title)</h2>
  $dotContentMap.body
</section>
```

VTL syntax and viewtools: [velocity.md](velocity.md). Widgets render through
`widgetCode` instead of a `<Var>.vtl` ([01-choose-mechanism.md](01-choose-mechanism.md)).

## A missing `<Var>.vtl` is a silent blank

If a contentlet is placed in the slot and no matching `<Var>.vtl` exists, the slot
renders empty with HTTP 200 — no error anywhere. That is the first thing to check when
a page comes back blank ([05-verify-and-debug.md](05-verify-and-debug.md)).

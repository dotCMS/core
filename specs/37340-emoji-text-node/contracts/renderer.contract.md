# Contract: Story Block renderer behavior

**Binding on all five implementations.** VTL (via `StoryBlockRenderHelper`), React, Vue, Angular
standard, Angular semantic. Divergence means the same stored content renders differently per
framework — the failure mode the shared fixture exists to catch.

## C1 — Resolving an `emoji` node

Precedence, in order. **Never emit empty output.**

| # | Source | Condition |
|---|---|---|
| 1 | generated map | `attrs.name` is a known shortcode |
| 2 | `attrs.text` | the node carries one |
| 3 | `:<name>:` literal | otherwise — **HTML-escaped** |

On reaching step 3, emit exactly one warning per distinct unresolved name per render scope:

```text
[dotCMS Block Editor]: Emoji <name> is not supported
```

- JS SDKs → `console.warn`; render scope = one render call.
- Java → `Logger.warn`; render scope = one `StoryBlockRenderHelper` instance (one Story Block field).

> **Security.** `attrs.name` reaches renderers from the Contentlet REST API with no schema
> validation. Step 3 MUST escape it (`$esc.html` in VTL; framework escaping in the SDKs). An
> unescaped `:name:` is an XSS vector.

## C2 — Inline placement

An `emoji` node renders as **inline** content. No block-level element (`<div>`, `<p>`) may be
emitted for it, in UVE or on the live site.

## C3 — Link runs

Collapse a link run (see [data-model.md §2](../data-model.md)) into exactly **one** `<a>`.

| Input | Required output |
|---|---|
| adjacent `text(link)` + `text(link)`, identical attrs | one `<a>` |
| `text(link)` + `emoji(no marks)` + `text(link)`, identical attrs | one `<a>`, emoji character inline |
| `text(link)` + `emoji(link)` + `text(link)` | one `<a>` |
| link attrs differ in any of `href`/`target`/`rel`/`title`/`aria-label` | two `<a>` |
| `text(link)` + `hardBreak` (or any non-`emoji` atom) + `text(link)` | two `<a>` — run breaks |
| nothing to coalesce | output identical to today |

Other marks (`bold`, `italic`, …) still nest correctly **inside** the single `<a>`.

Absorption is deliberately narrow: **only an `emoji` node with zero marks.** Widening it would
silently pull unrelated nodes inside links.

## C4 — Equivalence

These two inputs MUST produce byte-identical rendered output:

```json
[{ "type": "text", "marks": [L], "text": "dotCMS © 2026" }]

[{ "type": "text", "marks": [L], "text": "dotCMS " },
 { "type": "emoji", "attrs": { "name": "copyright" } },
 { "type": "text", "marks": [L], "text": " 2026" }]
```

This is what makes mixed representation safe, and what the shared fixture asserts across all five.

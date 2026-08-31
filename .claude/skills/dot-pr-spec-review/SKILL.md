---
name: dot-pr-spec-review
description: Summarize a spec PR (or any spec document) in simple words — proposed fix, scope, implications, and test coverage — so a reviewer can decide whether to approve without reading the full spec. Use when the user shares a PR URL/number or a document containing a spec and asks to review, summarize, or understand it ("summarize this spec", "review this spec PR", "what does this spec propose", "explain PR 1"). Typical input is a dotCMS Spec-Kit "PR 1 of 2" carrying a spec.md, but any spec PR or document works. Works in English and Spanish.
owner: "@dotcms/scout"
status: experimental
related: [speckit-analyze, speckit-clarify]
---

# Spec Review Summary

## Purpose

Produce a concise, plain-language review summary of a specification so the reader can
decide quickly whether to approve it, without reading the full spec. The typical input is
a dotCMS Spec-Kit "PR 1 of 2" carrying a `spec.md`, but any spec PR or document works.

This skill is for the **reviewer** of a spec someone else wrote. It is complementary to the
`speckit-*` skills, which operate inside the spec *authoring* workflow.

## Inputs

The user may share any combination of:
- A GitHub PR URL or number (e.g. `https://github.com/dotCMS/core/pull/37190` or `37190`)
- Related GitHub issue links
- Local file paths or pasted documents

## Workflow

1. **Fetch the PR** (if a PR was given):
   - `gh pr view <number> --repo <owner/repo> --json title,body,state,author,files,additions,deletions,baseRefName,headRefName`
   - `gh pr diff <number> --repo <owner/repo>` to read the actual spec content
   - Infer the repo from the URL; default to `dotCMS/core` when only a number is given.
2. **Follow the trail**: if the PR body or spec references a driving issue, parent epic, or
   investigation issue and the summary would be materially better for it, fetch those with
   `gh issue view` — but only what's needed; don't crawl everything.
3. **Read any extra docs** the user shared (local files with Read, URLs with WebFetch).
4. **Classify the PR first**: is it spec-only (docs, no code) or does it carry
   implementation? State this up front — it changes what "reviewing" means.

## Output format

Lead with a **TL;DR** paragraph: what the PR is (spec-only vs. code), what it adds, and the
one-sentence essence of the proposal.

Then these sections, in prose (short paragraphs and bullets — no walls of headers for a
simple spec):

- **The problem, in simple words** — the underlying issue as a story a non-expert teammate
  can follow. Include the key numbers (measurements, counts, thresholds) because they carry
  the argument.
- **The proposed fix** — what will actually change and why this approach was chosen over
  alternatives the spec considered and rejected. Note anything explicitly optional/descopable.
- **Scope** — explicitly split **in scope** vs. **out of scope**, including hypotheses ruled
  out and related findings deliberately not pursued.
- **Implications** — blast radius (shared components touched beyond the surfacing feature),
  behavior-change guarantees, accepted residual risks/limitations, backward compatibility,
  and any trade-off the spec is quiet about (memory, performance, migration).
- **Test coverage** — what tests exist in THIS PR (for spec-only PRs: typically none, by
  design — say so and cite the TDD/process rule if applicable), and what the spec's success
  criteria / acceptance scenarios commit future tests to prove. Flag success criteria that
  are qualitative ("low count", "a handful") and will need pinning down.

Close with anything a reviewer should push back on: ambiguities, unstated assumptions,
missing sections, or scope creep. If there is nothing, don't invent it.

## Visuals

The goal is clarity: a cold reader should grasp the whole picture from the visuals alone.
Skip visuals entirely when the spec is trivial or purely textual (a naming/policy change)
and a picture would add nothing; don't force one.

**Mermaid diagram (always, when a visual helps).** One mermaid diagram of the core
mechanism, ~5–12 nodes, real names (config keys, classes, endpoints) and real numbers,
quoted labels where they contain parentheses/colons. Include it in the chat summary as a
```mermaid fence — it renders if pasted into GitHub (PR comments render mermaid natively).

**Designed SVG visuals (only in HTML deliverables — see below).** When producing an HTML
deliverable, build:

- **A mechanism diagram** as hand-authored inline SVG. Prefer a **before/after layout**:
  the same pipeline drawn twice ("Today" / "Proposed"), unchanged parts explicitly marked
  "unchanged", the one thing that changes tagged (e.g. a "the only change" pill), and the
  costly path drawn as a visually heavy arrow that becomes thin/dashed in the after panel.
  Show real names and real numbers on the marks. Draw capacities/quantities as proportional
  shapes when possible (e.g. a gauge that visibly overflows), not just as text. Wrap in
  `<figure>` with a `<figcaption>` stating the claim and give the `<svg>` `role="img"` +
  `aria-label`. Inline SVG needs no JavaScript: it renders offline in any browser.
- **A small chart and/or stat tiles whenever the spec's argument rests on measurements.**
  A number pair like "~688 → 1–2 queries" is a pair of stat tiles; a capacity-vs-population
  claim is a bar chart with a dashed threshold line. Direct-label everything; single hue +
  neutral; text in ink tokens, never in series color.
- If diagramming or data-viz helper skills are available in your environment (e.g.
  `artifact-diagramming`, `dataviz`), load them before drawing; if not, follow the
  guidance above directly.
- Also embed the mermaid source in the HTML inside a collapsed
  `<details><summary>Mermaid source — paste into GitHub</summary>` block, so the
  copy-pasteable source travels with the document without duplicating the SVG's story.

## Deliverables

Adapt to what your environment supports — the chat summary is the one non-negotiable
deliverable.

1. **Chat summary** (always) — the sections above, with the ```mermaid fence inline.
2. **Standalone HTML file** (when the spec is substantial enough that visuals help) —
   a self-contained `.html` page with the same sections and the SVG visuals as primary
   figures (each in its own `overflow-x: auto` panel). Requirements:
   - Full document skeleton (`<!DOCTYPE html>`, `<html>`, `<head>` with
     `<meta charset="utf-8">`, `<body>`).
   - For the collapsed mermaid-source block, load mermaid from CDN
     (`https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs`,
     `startOnLoad: true`, theme matched to `prefers-color-scheme`). Tell the user the
     mermaid rendering needs internet; the SVG figures do not.
   - Clickable links to the spec PR, its driving issue, and any parent epic/investigation
     issues referenced (a footer "References" line works well).
   - Name the file after the PR (e.g. `spec-review-pr-37190.html`) and write it somewhere
     outside the repo working tree (a temp/scratch directory), then tell the user the path —
     or deliver it through your client's file-sending mechanism if it has one. Never leave
     it in the repo where it could be committed accidentally.
3. **Hosted/shareable page** (optional, only if your client supports publishing artifacts) —
   publish the same page and give the user the link. Reuse the same artifact URL when
   re-reviewing the same PR, so one spec keeps one page.

**Verifying rendering** (when browser tooling is available): render the HTML file and
screenshot each `<figure>`, eyeballing for label collisions/overflow. If your browser
tooling blocks `file:` URLs, serve the directory with `python3 -m http.server` in the
background, navigate, verify, then kill the server and remove any tool-generated folders
(e.g. `.playwright-mcp`) from the repo. Known quirk: that server sends no charset header,
so em-dashes/× may appear garbled in test screenshots — that's the test harness, not the
page; the standalone file carries its own `<meta charset>`. If no browser tooling is
available, skip verification and say so.

## Style rules

- Simple words. Explain jargon inline the first time it appears (e.g. "N+1 pattern — one
  query per row instead of one query total").
- Keep the key measurements and identifiers exact (query counts, config keys, class names).
- Link every PR/issue mentioned as a markdown link.
- Match the user's language (English or Spanish).
- Selective, not compressed: drop detail that doesn't change the approve/request-changes
  decision, but write what remains in full sentences.

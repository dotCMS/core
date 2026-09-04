# PLAN — <Site name>

> The site creation plan for a dotCMS build: purpose, structure, data model, and every fact. Visual identity lives in **`DESIGN.md`** (don't duplicate it here).
>
> **Where tags go.** Sections marked **— FACT** carry a tag on **every field**, because a
> wrong or invented value there ships as if it were real:
> `[confirmed]` real value the user gave · `[PLACEHOLDER — needs human]` real value exists but not yet available — build renders a visible TODO, never an invented value · `[n/a — intentionally omitted]` fact genuinely doesn't exist — build **removes** the field/section.
>
> Sections marked **— STRUCTURE** (and the voice line) are AI-proposed, so they carry one
> `[ai-draft — approve]` at the **section heading** rather than a tag per row — tagging
> every sitemap row individually adds noise without adding a decision. Once the user
> approves a section, change its heading tag to `[confirmed]`.
>
> A field inside a STRUCTURE section that is itself a fact — a real URL, a real price —
> still gets its own FACT tag.

## ⚠ Before launch — confirm these
<!-- Auto-list every [PLACEHOLDER] and [ai-draft] here so gaps are visible at a glance. -->
- [ ] …

---

## 1. Purpose & audience  — STRUCTURE  `[ai-draft — approve]`
- **What it is:** …
- **Audience:** …
- **Home page's single job:** …
- **Voice (≈3 words):** … <!-- also drives DESIGN.md prose -->

## 2. Hostname / domain  — FACT
- **Hostname:** …  `[confirmed | PLACEHOLDER]`
- **Local/throwaway host for now?** …

## 3. Transaction intent  — STRUCTURE (skip if purely informational)  `[ai-draft — approve]`
- **Sell / book / apply?** … — for a shop: `transact-in-dotcms` (heavy, usually out of v1) / `link-out per item` (adds a buy-URL fact field + external CTA) / `catalog-only` (no buy path).
- **v1 decision:** … `[confirmed]`

## 3b. Delivery mode  — STRUCTURE  `[confirmed]` (asked upfront, never inferred)
- **How is the HTML produced?** `VTL-rendered` (dotCMS renders it; theme + container VTL) / `headless` (dotCMS serves the page, a front-end app renders it via the dotCMS SDK).
- **Decision:** … `[confirmed]`
- **If headless, framework:** … `[confirmed]` — only Next.js has a build branch in this skill.
<!-- Drives which build branch runs: core/ + vtl/, or core/ + nextjs/. -->

## 4. Sitemap  — STRUCTURE  `[ai-draft — approve]`
| Page | URL | Detail via urlmap? | Notes |
|------|-----|--------------------|-------|
| Home | `/` | no | … |
| …    | …   | …   | … |

## 5. Sections per page  — STRUCTURE  `[ai-draft — approve]`
- **Home:** section 1 · section 2 · …
- **<page>:** …
<!-- Order matters; this is the placement order for the build. -->

## 6. Brand assets  — FACT (visual system → DESIGN.md)
- **Logo:** path / asset id · `[confirmed | n/a]`
- **Other images provided:** … `[confirmed]`
- **Color source:** sampled-from-logo / user-provided / ask — see DESIGN.md for the tokens.
<!-- Palette, type, spacing, components all live in DESIGN.md, not here. -->

## 7. Data model  — STRUCTURE  `[ai-draft — approve]`
- **Type name / variable:** … (build checks against the reserved list and prefixes if it collides)
- **Detail URL field:** … (the slug the urlmap uses)
- **Fields:**
  | Field | Type | Required | Facts or prose? | Notes |
  |-------|------|----------|-----------------|-------|
  | title | Text | yes | fact | |
  | buyUrl | Text | no | fact | only if §3 = link-out |
  | …     | …    | …    | …               | |

## 8. Content  — MIXED (tagged per field, see below)
### Data items
<!-- One block per item. Facts tagged confirmed/PLACEHOLDER/n-a; prose may be ai-draft. -->
- **<Item 1>**
  - factual fields (price, date, specs, ids, buy URL): value `[confirmed | PLACEHOLDER | n/a]`
  - description / tagline: `[ai-draft — approve]` …

### Editable sections (hero, newsletter, contact, titles…)
- **Hero:** headline / subcopy / CTA `[ai-draft — approve]`
- **Contact block (all FACT):** address · phone · email · hours · map — each `[confirmed | PLACEHOLDER | n/a]`  ← n/a removes that line (e.g. online-only has no address)
- **Newsletter:** heading / blurb / fine print `[ai-draft — approve]` (real submission? see §10)
- …

## 9. Featured / curation  — FACT if it reflects a real choice
- **Featured items:** … `[confirmed]`  — or "AI picks **N=<count>**, human approves" `[ai-draft — approve]` (never an undefined count)

## 10. Out of scope / deferred complexity  — DECISIONS (no tags; each line is an explicit no)
<!-- Explicit "no" so the build doesn't add these speculatively. -->
- Commerce / checkout: … (from §3)
- i18n / multi-language: no
- Real form submission (newsletter/contact): no — markup only
- Auth / gated content: no
- Search: no
- …

---

## Handoff → Build

Build from this **PLAN.md** (structure + tagged facts) and **`DESIGN.md`** (visual system). The
build sequence and every reference for it live in the `dotcms-best-practices` skill — that
skill's own index routes each step, per §3b's delivery mode. **`[PLACEHOLDER]` renders as a
visible TODO marker, never an invented value; `[n/a]` fields and sections are removed entirely,
not flagged.** `[ai-draft]` copy needs human approval before launch. Verification is a loop, not
a checkbox: fix and re-verify until every page type passes.

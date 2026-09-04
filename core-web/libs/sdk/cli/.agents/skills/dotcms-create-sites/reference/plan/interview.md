# Interview reference — questions & tagging

## Contents
- Tagging: FACT vs VOICE/STRUCTURE, and the three fact states
- Interview method
- The coverage spine (11 items)
- What NOT to ask

## Tagging every answer

- **FACT** — real-world truth only the human knows: hostname, legal/brand name, address, phone, email, hours, prices, real dates, social links, genuine "featured" picks, legal copy. Never invent. A fact takes one of three states:
  - `[confirmed]` — human gave the real value.
  - `[PLACEHOLDER — needs human]` — real value exists but not available yet → build renders a visible TODO.
  - `[n/a — intentionally omitted]` — fact genuinely doesn't exist (e.g. online-only shop has no address) → build removes the field/section, does not flag it.
  Ask which of the latter two applies; never assume a missing fact is a placeholder.
- **VOICE / STRUCTURE** — sitemap, section order, tone, headings, descriptions, taglines, hero copy, CTA labels. AI may **draft**; human **approves**. Mark `[ai-draft — approve]` once at the **section heading**, not on every row — the approval is per section. Swap it to `[confirmed]` when the user signs off.

## Interview method

Ask only what's needed to prevent fabrication.

- **Infer first, then confirm in one batch.** Draft a *complete* plan from the assets and brief —
  sitemap, sections, data model, voice, colors, featured picks — and present it as one "here's what
  I'll build; correct anything." The spine below is the coverage checklist for that draft, **not** a
  script to ask one-by-one.
- **Ask upfront only what can't be inferred** — usually just **delivery mode** (spine 4), the
  transaction model if selling is involved, and the real-vs-placeholder/n-a status of facts. Batch
  related items into one question with multiple fields.
- **Read before asking.** A brand doc, logo or existing content may answer several items.
  **PLAN.md and DESIGN.md are outputs, not inputs** — only if one already exists for this project,
  read it, confirm, and cover the gaps.
- **Colors are never invented.** Given a logo or image assets, sample the palette (prefer the logo)
  and propose it. Only ask when there's nothing to sample. Draft tokens either way, mark for
  approval, record the source.
- **Adaptive depth.** Go deeper only where the human invited the complexity.
- **Don't validate — probe.** Where you do ask, surface the trade-off they missed, then recommend.
- **Facts get a firm gate:** "real value, or placeholder / n-a?" A marked gap is success; a
  plausible fabrication is the failure this skill exists to prevent. Never skip this to save a
  question.

## The coverage spine

What your drafted plan must **cover** — not a list to ask one-by-one. **→PLAN** / **→DESIGN** marks
which artifact each item populates.

1. **Purpose & audience** — what it is, who for, the home page's single job. (VOICE) Confirm in one sentence. **→PLAN** (purpose/audience); voice also seeds **→DESIGN** prose.
2. **Hostname / domain** — (FACT) real hostname, or placeholder; note if it's a throwaway local host for now. **→PLAN**
3. **Transaction intent** — (STRUCTURE, ask before the data model) if the purpose involves selling/booking/applying, pin *how*: **transact-in-dotcms** (real checkout — heavy, usually out of v1), **link-out per item** (adds a buy-URL fact field + external CTA), or **catalog-only** (no buy path). Recommend link-out or catalog for v1. Skip for purely informational sites. **→PLAN**
4. **Delivery mode** — (STRUCTURE, ask upfront) **VTL-rendered** or **headless**. Cannot be
   inferred and changes half the build. If headless, pin **which framework** — only Next.js has a
   build branch; say so rather than improvising for Angular, Vue or Astro. Default to VTL unless
   the user has a front-end app or names one. **→PLAN**
5. **Sitemap** — (STRUCTURE) pages + URLs; which use a urlmap detail. Recommend from purpose. **→PLAN**
6. **Sections per page** — (STRUCTURE) what each page contains, in order. Recommend; confirm. **→PLAN**
7. **Brand & visual identity** — logo / image assets? colors (sample from logo, else ask — never invent)? fonts (pair on a contrast axis)? voice in 3 words? What's fixed vs. open. **→DESIGN** (tokens + prose per the DESIGN.md spec); logo/asset paths also **→PLAN** §6.
8. **Data model** — (STRUCTURE) the core repeating thing + its fields; fold in any field the transaction answer requires. Confirm which are required and which drives the detail URL. **→PLAN**
9. **Content** — per data item & editable section: **facts** (prices, dates, specs, contact) → collect / placeholder / n-a; **prose** (descriptions, taglines, hero/newsletter) → offer to draft, mark `[ai-draft — approve]`. **→PLAN**
10. **Featured / curation** — (FACT if a real merchandising choice) which items, or "AI picks **N=<count>**, human approves" — never an undefined count. **→PLAN**
11. **Deferred-complexity check** — one pass: i18n? form submission? auth? search? (Commerce is
    step 3.) For each yes, ask the 1–2 follow-ups that unblock the build; for each no, record it as
    out of scope. **→PLAN**

## What NOT to ask

The build owns these; asking is noise. Check the `dotcms-best-practices` skill's references and
the codebase first — never re-ask what they answer:

- Content-type vs. widget vs. SimpleWidget; container scaffold; per-type VTL.
- urlmap patterns, detail-page wiring, template/theme mechanics.
- API payload shapes, field `dataType`, reserved-name workarounds, cache-busting.

# DESIGN.md template — visual identity (DESIGN.md spec)

Follows the DESIGN.md format (github.com/google-labs-code/design.md): YAML front matter of machine-readable tokens, then Markdown prose that explains *why* those values exist and how to apply them. The interview **produces** this file; the build's design step reads it.

**Color source rule.** Colors are not invented. If the user provided a **logo or image assets**, sample the palette from them (prefer the logo). Otherwise **ask the user** for the colors. Either way, draft the tokens and mark them for approval; note the source in the Overview prose. Prefer **OKLCH** values (dotCMS ground rules use OKLCH tokens + fluid scale).

Prose sections use `##` headings in this canonical order (omit any, but keep the order): Overview · Colors · Typography · Layout · Elevation & Depth · Shapes · Components · Do's and Don'ts.

Write the file to the project as `DESIGN.md`. Skeleton:

```markdown
---
version: alpha
name: <Site name>
description: <one line — the visual register>
colors:
  primary: "oklch(...)"      # ink / dominant
  accent: "oklch(...)"       # the one committed accent (CTAs, links, rules)
  surface: "oklch(...)"      # reading surface
  on-primary: "oklch(...)"   # text on primary
  on-accent: "oklch(...)"    # text on accent
  muted: "oklch(...)"        # secondary text — verify ≥4.5:1 on surface
typography:
  h1:   { fontFamily: <display>, fontSize: 3rem,   fontWeight: 600, lineHeight: 1.05, letterSpacing: -0.02em }
  body: { fontFamily: <body>,    fontSize: 1rem,   fontWeight: 400, lineHeight: 1.6 }
  label:{ fontFamily: <body>,    fontSize: 0.75rem,fontWeight: 600, letterSpacing: 0.1em }
rounded:
  sm: 4px
  md: 8px
spacing:
  sm: 8px
  md: 16px
  lg: 32px
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.on-accent}"
    rounded: "{rounded.sm}"
    padding: 12px
  button-primary-hover:
    backgroundColor: "{colors.primary}"
---

## Overview
<Register + design philosophy in 2–3 sentences. State the color source
(sampled from logo / user-provided / asked). Name the one place boldness is spent.>

## Colors
<Palette description + usage. Which color carries hero/footer, which is the sole accent,
what the surface is (e.g. true off-white, NOT cream). Note contrast checks.>

## Typography
<Font pairing on a contrast axis (serif + grotesque sans, etc.) and the scale rationale.>

## Layout
<Spacing principles, container width, the grid approach (dotCMS: 12-col mapped to CSS Grid).>

## Elevation & Depth
<Shadow/layering intent, or "flat — no elevation" if that's the choice.>

## Shapes
<Border-radius language and where it applies.>

## Components
<Component-specific rules the tokens above encode: buttons, cards, nav, inputs.>

## Do's and Don'ts
- Do: <the committed moves>
- Don't: <the anti-patterns for this brand — e.g. no cream surfaces, one accent only>
```

Every token is `[ai-draft — approve]` until the user confirms. Colors from a logo/asset are confirmable facts; colors the user dictates are `[confirmed]`.

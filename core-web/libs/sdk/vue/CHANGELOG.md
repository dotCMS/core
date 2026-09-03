# Changelog

All notable changes to the DotCMS Vue SDK will be documented in this file.

## Unreleased

### Added

#### Emoji node rendering in the Block Editor renderer

- **Added**: `emoji` nodes in Story Block content now render their character.
  - Stored `emoji` nodes carry only a shortcode (`{"type":"emoji","attrs":{"name":"copyright"}}`),
    never the literal character, so they previously fell through to the unknown-block component
    and rendered nothing.
  - The shortcode is resolved through a map generated from the editor's own emoji list, so it
    cannot describe a character the Block Editor could not have produced.
  - An unresolvable name renders as `:name:` rather than disappearing, and warns once per
    distinct name.

### Fixed

#### One `<a>` per link, not one per text node

- **Fixed**: adjacent text nodes sharing an identical `link` mark now render as a **single**
  `<a>` element.
  - Previously each text node emitted its own anchor, so one logical link produced duplicate
    keyboard tab stops and duplicate entries in screen-reader link lists — WCAG 2.2 Level A
    failures under 1.3.1, 2.4.4 and 4.1.2.
  - A run also absorbs an intervening `emoji` node that carries no marks, which is how the
    Block Editor used to split a link when an emoji was typed inside it.
  - Links whose marks differ in `href`, `target`, `rel`, `title` or `aria-label` stay separate,
    and other marks such as `bold` still nest correctly inside the single anchor.

> **⚠️ Rendered HTML changes for existing content.** Content you already have will now render one
> anchor where it previously rendered two. Snapshot tests will show a diff, and CSS relying on
> adjacent-anchor selectors may need review. This is deliberate: the previous output was an
> accessibility defect.

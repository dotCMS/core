**dotCMS 26.07.10-01** ships a focused set of changes: a new delivery path for release notes, an editor refinement, and a storage fix — then back about its day.

### Features {#Features-26.07.10-01}
- Release changelogs now publish to the public site automatically when a GA version ships, so the changelog page reflects a release within minutes instead of days. [[#36605](https://github.com/dotCMS/core/issues/36605)]

### Enhancements & Adjustments {#Enhancements-26.07.10-01}
- The Block Editor toolbar groups its formatting controls more predictably, reducing mis-clicks on narrow viewports. [[#36512](https://github.com/dotCMS/core/issues/36512)]

### Fixes {#Fixes-26.07.10-01}
- A `$` typed into a Block Editor field was stored as the HTML entity `&#36;` and shown literally when the content was reopened; dotCMS now decodes it on read, self-healing content already saved with the entity and requiring no migration. [[#35782](https://github.com/dotCMS/core/issues/35782)]

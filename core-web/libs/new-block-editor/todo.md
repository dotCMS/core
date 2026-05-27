# Table Accessibility (#35720) — Follow-ups

- [ ] **Update the VTL file** to render the new table attributes in the table node — `caption`, `aria-label`, `aria-labelledby` (on `<table>`), and `scope` (on `<th>`) — so server-rendered (non-headless) pages match the editor output and meet WCAG 1.3.1 / 4.1.2.
- [x] **Add a merge-cells button** to the table handles to support merge / split actions. Done in Phase 6 — new selection handle (`drag_indicator`) appears at the right edge of a multi-cell CellSelection and opens a popover with Merge cells / Split cell, both bound to `editor.can().mergeCells()` / `splitCell()`. Delete-table remains out of scope (no UI affordance — keyboard shortcut still works).

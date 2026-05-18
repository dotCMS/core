# Table Accessibility (#35720) — Follow-ups

- [ ] **Update the VTL file** to render the new table attributes in the table node — `caption`, `aria-label`, `aria-labelledby` (on `<table>`), and `scope` (on `<th>`) — so server-rendered (non-headless) pages match the editor output and meet WCAG 1.3.1 / 4.1.2.
- [ ] **Add a merge-cells button** to the table handles to support merge / split actions. Merge / split / delete-table were dropped from the Phase 3 a11y popover; they still need a home for parity with the legacy table-editing surface.

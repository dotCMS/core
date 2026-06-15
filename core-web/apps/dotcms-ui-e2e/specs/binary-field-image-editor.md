# Spec: Binary Field Image Editor E2E

## Objective

Verify that after importing an image into a **binary field**, the **Edit** button is visible and opens the legacy Dojo Image Editor in:

1. **New edit-content editor** (`CONTENT_EDITOR2_ENABLED: true`) — PrimeNG dialog + `legacy-image-editor-iframe`
2. **Legacy content editor** (`CONTENT_EDITOR2_ENABLED: false`) — `#dotImageDialog` + `#imageToolIframe` inside `detailFrame`

Issue: [#36056](https://github.com/dotCMS/core/issues/36056)

**Scope:** open-only — confirm the editor shell appears; no crop/save round-trip.

## Tech Stack

- Playwright (`@playwright/test`)
- Nx project: `dotcms-ui-e2e`
- POM helpers under `src/tests/edit-content/fields/binary-field/helpers/`

## Commands

```bash
cd core-web && yarn nx e2e dotcms-ui-e2e --grep "binary field image editor"
HEADLESS=true yarn nx e2e dotcms-ui-e2e --grep "binary field image editor"
```

Requires dotCMS running (`yarn e2e:dev` from `dotcms-ui-e2e/` or `:4200` proxy / `:8080` direct).

## Project Structure

```
core-web/apps/dotcms-ui-e2e/
  specs/binary-field-image-editor.md          # this file
  src/pages/legacyEditContentForm.page.ts     # legacy navigation
  src/tests/edit-content/fields/binary-field/
    binary-field-image-editor.spec.ts         # E2E tests
    helpers/binary-field.ts                   # new editor locators + editor assertions
    helpers/legacy-binary-field.ts            # legacy frame locators
```

## Code Style

- Locators: `getByTestId` / `getByRole` on main page; CSS (`#dotImageDialog`, `#imageToolIframe`) only inside Dojo/legacy iframe per AGENTS.md
- Helper classes scope to field root (`field-{variable}` or `#binary-field-{variable}`)
- Tests: `test('action description @critical')` — action only, no "should"

## Testing Strategy

| Concern | Level |
|---------|-------|
| Edit button visible after PNG import | E2E Playwright |
| New editor opens dialog + standalone iframe | E2E Playwright |
| Legacy editor opens Dojo image dialog | E2E Playwright |
| Launcher service / dialog component | Unit (existing Jest specs) |

Image setup: `importFromUrl(E2E_IMPORT_URL)` — stable PNG URL, already used in `binary-field.spec.ts`.

## Boundaries

- **Always:** create content type via API in `beforeEach`, delete in `afterEach`; serial describe mode
- **Ask first:** changing production `data-testid` attributes; extending scope to file/image fields
- **Never:** full image edit save round-trip in E2E (out of scope); CSS selectors on Angular shell when `data-testid` exists

## Success Criteria

- [ ] New editor: after import, `edit-button` visible; click opens dialog with `legacy-image-editor-iframe` and nested `#dotImageDialog` or `#imageToolIframe`
- [ ] Legacy editor: after import inside `detailFrame`, `edit-button` visible; click opens `#dotImageDialog` and `#imageToolIframe`
- [ ] Both tests tagged `@critical` and pass in CI

## Open Questions

None — open-only verification agreed.

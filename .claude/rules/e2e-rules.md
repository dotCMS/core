---
paths:
  - "core-web/apps/dotcms-ui-e2e/**/*.spec.ts"
  - "e2e/**/*.spec.ts"
---

# E2E Test Conventions (Playwright)

## Page Object Model (POM)
- **One class per page** -- each page has its own Page Object
- **Encapsulate interactions** -- all `page.fill()`, `page.click()` go in Page Objects, not tests
- **Tests use Page Objects only** -- never interact with DOM directly in test files

## Selectors: ALWAYS use data-testid

```typescript
// CORRECT
await this.page.getByTestId("userNameInput").fill(username);
await this.page.getByTestId("submitButton").click();

// WRONG -- never use CSS selectors
await this.page.locator('input[id="userId"]').fill(username);
```

## File naming
- Page Objects: `[PageName].page.ts`
- Components: `[ComponentName].component.ts`
- Tests: `[FeatureName].spec.ts`
- Test data: `[FeatureName]Data.ts`

## Commands
```bash
just test-e2e-node                              # full suite
just test-e2e-node-specific test=login.spec.ts  # single spec
just test-e2e-node-debug-ui test=login.spec.ts  # Playwright UI mode
```

See `e2e/dotcms-e2e-node/README.md` for full documentation.

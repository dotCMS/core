# E2E Testing Guide — dotCMS Playwright

## Project Structure

```
apps/dotcms-ui-e2e/
├── src/
│   ├── fixtures/              # Playwright fixtures (auth, test data, API helpers)
│   ├── pages/                 # Shared page objects (login, content form)
│   ├── utils/                 # Shared utilities (iframe, portlets, credentials)
│   ├── requests/              # API request helpers (contentlets, content types)
│   ├── tests/
│   │   └── edit-content/
│   │       └── fields/
│   │           └── {field-type}/
│   │               ├── helpers/       # Local helpers scoped to this field
│   │               └── *.spec.ts      # Test specs
│   └── components/            # Shared component helpers (sidebar, breadcrumb)
├── playwright.config.ts
└── CLAUDE.md                  # This file
```

## Commands

```bash
yarn nx e2e dotcms-ui-e2e --ui                          # Interactive UI mode
yarn nx e2e dotcms-ui-e2e --grep "test name"             # Run specific test
HEADLESS=true yarn nx e2e dotcms-ui-e2e --grep "pattern"  # Headless mode
npx playwright codegen http://localhost:4200/dotAdmin     # Record selectors
```

## Naming Conventions

### Files

| Type | Pattern | Example |
|---|---|---|
| Test specs | `{feature}-{journey}.spec.ts` | `relationship-field-select.spec.ts` |
| Local helpers | `{component-name}.ts` | `relationship-field.ts` |
| Shared utils | `{concept}.ts` | `iframe.ts`, `portlets.ts` |
| Fixtures | `{feature}.fixture.ts` | `relationship.fixture.ts` |
| Shared pages | `{page-name}.page.ts` | `login.page.ts`, `newEditContentForm.page.ts` |

**Rules:**
- All files use **kebab-case**
- No `POM`, `component`, or `helper` suffixes — names describe what they represent
- Shared pages in `pages/` use `.page.ts` suffix to distinguish from helpers
- Local helpers use plain `.ts` — no suffix needed since the `helpers/` folder provides context

### Test names

```typescript
test('select and apply item @critical', ...)
test('apply button disabled with no selection @smoke', ...)
test('toggle show selected items', ...)
```

**Format:** `{action in lowercase} {@tag}`

- Short, describes the user action — no priority prefixes (P1/P2/P3)
- Tags: `@critical` for core flows, `@smoke` for important secondary flows, none for edge cases
- No "should" or "it should" — just the action

### Test describe blocks

```typescript
test.describe('Single Selection (1:1 / M:1)', () => {
    test.describe('ONE_TO_ONE', () => { ... });
});
```

- Short and descriptive — no "Journey N" prefixes
- The folder structure already provides feature context
- Nesting describes only when there are meaningful subgroups (e.g., cardinality variants)

### Class names

```typescript
export class RelationshipField { ... }          // In helpers/ — locator wrapper
export class SelectExistingContentDialog { ... } // In helpers/ — locator wrapper
export class NewEditContentFormPage { ... }      // In pages/ — shared page object
export class LoginPage { ... }                   // In pages/ — shared page object
```

- No "Component" suffix — these are Playwright locator helpers, not Angular components
- No abbreviations — `RelationshipField` not `RelField`
- Pages in `pages/` use the full name (e.g., `NewEditContentFormPage`)
- Helpers in `helpers/` use the UI element name (e.g., `RelationshipField`, `SelectExistingContentDialog`)

## Locator Strategy (Priority Order)

Always prefer locators in this order:

### 1. `getByRole` — best for interactive elements

```typescript
page.getByRole('button', { name: 'Save' })
page.getByRole('menuitem', { name: 'Existing Content' })
page.getByRole('radio')
page.getByRole('row', { name: 'Blog Post Title' })
```

### 2. `getByTestId` — best for specific Angular elements

```typescript
page.getByTestId('title')                     // Title input field
page.getByTestId('field-mainAuthor')           // Field wrapper by variable
page.getByTestId('relationship-field-table')   // Relationship table
page.getByTestId('apply-button')               // Apply in selection dialog
```

### 3. `getByLabel` — best for menu items with aria-label

```typescript
page.getByLabel('Existing Content').locator('a')   // Menu item link
```

### 4. CSS locators — last resort, for Dojo/legacy elements

```typescript
frame.locator('.dijitDropDownButton [role="button"]')
frame.locator('.dijitMenuItemLabel', { hasText: 'Add New Content' })
```

### What NOT to do

```typescript
// Never use fragile Dojo IDs — they change between page loads
frame.locator('#dijit_form_DropDownButton_0')  // BAD

// Never nest .locator('button') on getByRole('button')
this.addButton.locator('button')  // BAD — getByRole already finds the <button>

// Never use CSS for Angular elements when testid exists
page.locator('.p-button-label')  // BAD
page.getByTestId('apply-button') // GOOD

// Never use isVisible() for waiting — it has no timeout
await el.isVisible({ timeout: 3000 })           // BAD — timeout is ignored
await el.waitFor({ state: 'visible', timeout })  // GOOD
```

### Codegen is the source of truth

When unsure about a selector, **always** run codegen first:

```bash
npx playwright codegen http://localhost:4200/dotAdmin
```

Record the user flow manually. The generated selectors are the correct ones.

## Architecture: Angular Shell + Dojo Iframe

dotCMS admin has **two layers**:

1. **Angular shell** — sidebar, toolbar, new edit content forms, dialogs
2. **Legacy Dojo portlet** — content listing, rendered inside `<iframe id="detailFrame" name="detailFrame">`

### Accessing the Dojo iframe

```typescript
import { getLegacyFrame } from '@utils/iframe';

const frame = getLegacyFrame(page);
frame.locator('.dijitDropDownButton [role="button"]').click();
```

**Key rule**: Dojo elements live inside the iframe. Angular elements live in the main page. Never mix them.

### Dojo dropdown menus

Dojo menus can flicker during animation. Use `force: true` on click:

```typescript
const menuItem = frame.locator('.dijitMenuItemLabel', { hasText: 'Add New Content' });
await menuItem.waitFor({ state: 'visible', timeout: 10000 });
await menuItem.click({ force: true });
```

## Navigation Patterns

### Portlet constants

```typescript
import { Portlet } from '@utils/portlets';

// Portlet.Content      → '/dotAdmin/#/c/content'
// Portlet.ContentTypes  → '/dotAdmin/#/content-types-angular'
```

Add new portlets here as needed. Always use `Portlet.X` instead of hardcoded URLs.

### Creating new content (user flow)

Always navigate through the content listing — never use direct URL to `/content/new/`:

```typescript
const formPage = new NewEditContentFormPage(page);
await formPage.goToNew(contentTypeVariable);
// Internally: goToContentList(ct) → clickNewContentFromList() (Dojo iframe)
```

### Editing existing content

Navigate to Content portlet first to initialize the app, then to the content URL:

```typescript
await formPage.goToContent(contentletInode);
// Internally: goto(Portlet.Content) → goto(/content/{id})
```

## Login

Selectors (verified via codegen):

```typescript
page.getByTestId('userNameInput')   // Email field
page.getByTestId('password')        // Password field
page.getByTestId('submitButton')    // Sign In button
```

**Critical**: Must `click()` → `fill()` → `press('Tab')` on the email field before filling password. Without `Tab`, the password can end up in the email field due to Angular form rendering timing.

## Helpers vs Pages vs Utils

### When to put code where

| Question | Location |
|---|---|
| Used across multiple test features? | `pages/` or `utils/` |
| Wraps locators for a specific component? | `tests/{feature}/helpers/` |
| Generic utility (iframe, portlets, credentials)? | `utils/` |
| API request helpers? | `requests/` |
| Test setup with custom fixtures? | `fixtures/` |

### Local helpers pattern

Helpers scoped to a test suite live in `tests/{feature}/helpers/`:

```
tests/relationship-field/
├── helpers/
│   ├── relationship-field.ts              # Locator wrapper for the field
│   └── select-existing-content-dialog.ts  # Locator wrapper for the dialog
├── relationship-field-select.spec.ts
└── relationship-field-edit.spec.ts
```

A helper wraps locators and actions for a UI component:

```typescript
export class RelationshipFieldComponent {
    readonly table: Locator;
    readonly addButton: Locator;

    constructor(private page: Page, fieldVariable?: string) {
        // Scope by field variable when multiple instances exist
        if (fieldVariable) {
            this.root = page.getByTestId(`field-${fieldVariable}`);
            this.table = this.root.getByTestId('relationship-field-table');
        } else {
            this.table = page.getByTestId('relationship-field-table').first();
            this.root = this.table;
        }
        this.addButton = this.root.getByRole('button', { name: '' }).first();
    }

    async clickRelateExisting() { ... }
    async expectRowCount(count: number) { ... }
}
```

### Shared utils pattern

Utils in `src/utils/` are generic and reusable across all tests:

```typescript
// utils/iframe.ts — access legacy Dojo portlets
export function getLegacyFrame(page: Page): FrameLocator { ... }

// utils/portlets.ts — centralized URL constants
export const Portlet = { Content: '...', ContentTypes: '...' } as const;
```

## Fixtures & API Setup

### Fixture structure

```typescript
export const test = base.extend<{
    adminPage: Page;          // Logged-in browser page
    testSuffix: string;       // Unique suffix per test run (timestamp)
    apiHelpers: {             // API operations for setup/teardown
        createContentType: ...;
        deleteContentType: ...;
        createContentlet: ...;
        authorPayload: (suffix: string) => object;
        blogPayload: (...) => object;
    };
}>({ ... });
```

### SystemWorkflow

Content types must include the SystemWorkflow to enable the Save button:

```typescript
const SYSTEM_WORKFLOW_ID = 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2';

// In payload:
{ workflow: [SYSTEM_WORKFLOW_ID] }
```

### Relationship field velocityVar

The `velocityVar` is **just the related content type variable** — NOT `ParentType.fieldName`:

```typescript
// Correct:
relationships: { velocityVar: 'E2EAuthorXYZ', cardinality: 2 }

// Wrong:
relationships: { velocityVar: 'E2EAuthorXYZ.mainAuthor', cardinality: 2 }
```

### Cardinality constants

Values as defined in `RELATIONSHIP_OPTIONS` (source: `dot-edit-content-relationship-field.constants.ts`):

```typescript
const CARDINALITY = {
    ONE_TO_MANY: 0,   // Multiple selection (checkboxes)
    MANY_TO_MANY: 1,  // Multiple selection (checkboxes)
    ONE_TO_ONE: 2,    // Single selection (radio buttons)
    MANY_TO_ONE: 3    // Single selection (radio buttons)
};
```

**Warning**: These are NOT intuitive. `0` is ONE_TO_MANY, not ONE_TO_ONE.

### Test isolation

- Each test creates its own content types and seed data via API in `beforeEach`
- Cleanup via `deleteContentType` in `afterEach` (cascades contentlets)
- Use `testSuffix` (timestamp) to ensure unique names across parallel runs
- Tests must be independent — no shared state between tests

## Content Form Selectors (Angular)

| Element | Selector |
|---|---|
| Title field | `page.getByTestId('title')` |
| Field wrapper | `page.getByTestId('field-{variable}')` |
| Relationship table | `page.getByTestId('relationship-field-table')` |
| Save button | `page.getByRole('button', { name: 'Save' })` |
| Publish button | `page.getByRole('button', { name: 'Publish' })` |

## Relationship Field Selectors

### Menu items (PrimeNG)

The "+" button opens a PrimeNG menu with `aria-label` on each `<li role="menuitem">`:

```typescript
page.getByRole('menuitem', { name: 'Existing Content' })  // Relate existing
page.getByRole('menuitem', { name: 'New Content' })        // Create new inline
```

**Note**: Labels come from `dotMessageService` translations. Use codegen to verify actual labels — they may differ from what the UI spec says.

### Checking disabled state

In single-cardinality mode with an item already selected, menu items have `aria-disabled="true"`:

```typescript
const option = page.getByRole('menuitem', { name: 'Existing Content' });
await expect(option).toBeDisabled();  // Works with aria-disabled
```

The "+" button itself stays **enabled** — only the menu items become disabled.

### Selection dialog

```typescript
page.getByTestId('apply-button')       // Apply selection
page.getByTestId('cancel-button')      // Cancel
page.getByTestId('search-button')      // Search
page.getByTestId('show-selected-switch') // Toggle selected items
```

Radio buttons (single mode): `row.locator('p-tableradiobutton')`
Checkboxes (multiple mode): `row.locator('p-tablecheckbox')`
Header checkbox (select all): `table.locator('[data-testid="header-checkbox"]')`

## waitForResponse Pattern

Always set up `waitForResponse` **BEFORE** triggering the action:

```typescript
// Correct:
const responsePromise = page.waitForResponse(r => r.url().includes('/api/v1/workflow/actions/'));
await saveButton.click();
await responsePromise;

// Wrong — race condition:
await saveButton.click();
await page.waitForResponse(...);  // May miss the response
```

## Common Gotchas

1. **Login race condition** — Always `click()` + `fill()` + `Tab` on email before filling password
2. **Dojo iframe** — Content listing is Dojo inside `#detailFrame`. Use `getLegacyFrame(page)`
3. **Dojo menu flicker** — Use `force: true` on menu item clicks
4. **Cardinality mapping** — `0` = ONE_TO_MANY, `2` = ONE_TO_ONE (counterintuitive)
5. **velocityVar format** — Just the content type variable, no dot notation
6. **SystemWorkflow required** — Without it, the Save button doesn't appear
7. **Menu labels** — "New Content" not "Create New", "Existing Content" not "Relate Existing"
8. **Inline create dialog** — Stays open after save. Must close via X button manually
9. **PrimeNG button nesting** — `getByRole('button')` finds the `<button>` directly. Don't chain `.locator('button')` on top
10. **`isVisible()` has no timeout** — Use `waitFor({ state: 'visible', timeout })` or `expect().toBeVisible({ timeout })` instead
11. **Direct URL navigation** — Don't go directly to `/content/new/{type}`. Use `goToNew()` which goes through the content listing (Dojo) first
12. **Dojo widget IDs are fragile** — Never use `#dijit_form_DropDownButton_0`. Use role or class selectors instead
13. **Multiple instances** — When a page has multiple relationship fields, pass `fieldVariable` to scope the helper to a specific field via `data-testid="field-{variable}"`
14. **HTML page navigation** — HTML pages don't have `data-testid="title"`, so `goToContent()` (which waits for that field) hangs. Navigate directly with `page.goto('/dotAdmin/#/content/{inode}')` and wait for `dot-edit-content-sidebar` instead.
15. **`hostFolder` for HTML pages** — The API rejects the string `'default'`. Resolve the default site via `GET /api/v1/site` and use `defaultSite.identifier` as `hostFolder`.
16. **New editor for custom content types** — Navigating to `/dotAdmin/#/content/{inode}` shows the legacy editor unless the content type has `metadata: { CONTENT_EDITOR2_ENABLED: true }`. HTML pages have this enabled by default; custom types created in tests must set it explicitly.

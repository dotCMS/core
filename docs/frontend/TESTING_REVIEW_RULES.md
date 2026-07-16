# Test Review Rules

Condensed rules for reviewing test files. For full patterns and tutorials, see [TESTING_FRONTEND.md](./TESTING_FRONTEND.md).

## Required Framework

- **Spectator** (`@ngneat/spectator/jest`) is required for all tests
- **Jest** (or Vitest) as test runner
- **`@dotcms/utils-testing`** createFake functions for domain mocks

## Critical Violations 🔴 (Must Fix)

### Spectator API Misuse
- **Direct input assignment**: `spectator.component.prop = value` → use `spectator.setInput('prop', value)`
- **Missing detectChanges**: After `setInput`, `click`, or state changes, must call `spectator.detectChanges()` before assertions on DOM
- **Wrong factory**: Using `createComponentFactory` for services, or `createServiceFactory` for components
- **Missing mockProvider**: Dependencies not mocked → use `mockProvider(Service, { method: jest.fn() })`

### Broken Test Patterns
- **No assertions**: Test body has no `expect()` calls
- **Test interdependence**: Tests share mutable state or depend on execution order
- **Async not handled**: Observable/Promise results asserted without `fakeAsync/tick`, `async/await`, or `whenStable()`

## Important Violations 🟡 (Should Fix)

### Selection Patterns
- **CSS class selectors**: `spectator.query('.my-class')` → use `spectator.query(byTestId('my-element'))`
- **Generic tag selectors**: `spectator.query('button')` → use `byTestId`
- **Missing data-testid naming convention**: Format should be `[what-it-is]-[what-it-does]`

### Coverage Gaps
- **Only happy path**: No error/failure test cases for API calls or user actions
- **Missing output tests**: Component has `output()` but no test verifies emission
- **Missing form validation tests**: Form has validators but tests don't verify error messages
- **No edge cases**: Empty arrays, null values, boundary conditions not tested

### Mock Quality
- **Manual domain mocks**: Creating `{ inode: '123', ... }` instead of `createFakeContentlet({ inode: '123' })`
- **Incomplete mocks**: `{ provide: Service, useValue: {} }` → use `mockProvider(Service, { ... })`
- **Duplicated mock data**: Same mock object copied across tests → extract to factory function

### Test Structure
- **Vague test names**: `it('works')`, `it('test 1')` → describe expected behavior
- **Mixed AAA pattern**: Arrange/Act/Assert phases interleaved → separate clearly
- **Flat describe blocks**: No grouping by feature → use nested `describe` blocks

## Quality Violations 🔵 (Nice to Have)

- **Implementation detail testing**: Testing private methods, internal state → test user-visible behavior
- **Object notation for class testing**: `toHaveClass({ class: true })` → use `toHaveClass('class1', 'class2')` (separate strings)
- **Missing user-centric approach**: Calling `component.method()` directly → trigger via `spectator.click(byTestId(...))`
- **Hardcoded wait times**: `setTimeout(done, 1000)` → use `fakeAsync/tick` or `whenStable()`

## What NOT to Flag

- **Pre-existing test code** — only flag issues in changed lines
- **Production code** — component logic, types, Angular patterns (code-reviewer handles this)
- **E2E tests** — Playwright tests have different patterns
- **Legacy test patterns** in files that aren't being modified

## Available Spectator API Reference

| API | Purpose |
|-----|---------|
| `createComponentFactory` | Component tests |
| `createServiceFactory` | Service tests |
| `createDirectiveFactory` | Directive tests |
| `createPipeFactory` | Pipe tests |
| `createHostFactory` | Custom host wrapper |
| `createRoutingFactory` | Routing tests |
| `createHttpFactory` | HTTP mock tests |
| `byTestId(id)` | Select by `data-testid` |
| `mockProvider(Service, stubs)` | Mock a service |
| `spectator.setInput(name, value)` | Set component input |
| `spectator.detectChanges()` | Trigger change detection |
| `spectator.click(selector)` | Simulate click |
| `spectator.typeInElement(value, selector)` | Type text |

## Available createFake Functions (`@dotcms/utils-testing`)

| Category | Functions |
|----------|-----------|
| Content | `createFakeContentlet`, `createFakeContentType` |
| Locale/Site | `createFakeLanguage`, `createFakeSite`, `createFakeFolder` |
| Fields | `createFakeTextField`, `createFakeDateTimeField`, `createFakeSelectField`, `createFakeRelationshipField`, `createFakeCustomField`, and 20+ more |
| Events | `createFakeEvent`, `createFakeMouseEvent`, `createFakeKeyboardEvent` |

See [TESTING_FRONTEND.md](./TESTING_FRONTEND.md) for full examples and tutorials.

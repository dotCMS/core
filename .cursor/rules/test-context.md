---
description: Testing-specific context for all test files
globs: ["**/*.spec.ts", "**/*.test.ts", "**/*.spec.js", "**/test/**/*"]
alwaysApply: false
---

# Testing Context

## Backend Testing (Java)
```java
// Integration test pattern
@Test
public void testEndpoint() throws Exception {
    // Use DotConnect for DB setup
    // Use APILocator for services
    // Use TestUtil for common patterns
}
```

## Frontend Testing (TypeScript)
```typescript
// Spectator component test
const createComponent = createComponentFactory({
  component: MyComponent,
  imports: [CommonModule, DotTestingModule],
  providers: [mockProvider(MyService)]
});

// REQUIRED: Use data-testid selectors
const button = spectator.query(byTestId('submit-button'));
expect(button).toBeVisible();

// REQUIRED: Use setInput for component properties
spectator.setInput('inputProperty', 'testValue');
```

## Test Structure
- **Backend**: `*Test.java` in same package as source
- **Frontend**: `*.spec.ts` adjacent to source file
- **Integration**: Separate test modules with real dependencies

Complete testing guide: @docs/testing/
---
description: Testing context - loads only for test files
globs: ["**/*.spec.ts", "**/*.test.ts", "**/*.spec.js", "**/test/**/*", "**/*Test.java"]
alwaysApply: false
---

# Testing Context

## Frontend Testing (Spectator - CRITICAL Patterns)

### Setup Template  
```typescript
import { createComponentFactory, Spectator, byTestId, mockProvider } from '@ngneat/spectator/jest';

describe('MyComponent', () => {
  let spectator: Spectator<MyComponent>;
  
  const createComponent = createComponentFactory({
    component: MyComponent,
    imports: [CommonModule, DotTestingModule],
    providers: [mockProvider(MyService)]
  });
  
  beforeEach(() => {
    spectator = createComponent();
  });
});
```

### Critical Testing Rules
```typescript
// Element selection (ALWAYS use data-testid)
const button = spectator.query(byTestId('submit-button'));
// NEVER: spectator.query('.submit-button')

// Component inputs (CRITICAL - NEVER set directly)  
spectator.setInput('inputProperty', 'value');
// NEVER: spectator.component.inputProperty = 'value';

// CSS verification (separate strings)
expect(icon).toHaveClass('pi', 'pi-update');
// NEVER: expect(icon).toHaveClass({ pi: true });

// User interactions (test what user sees)
spectator.click(byTestId('refresh-button'));
expect(spectator.query(byTestId('success-message'))).toBeVisible();
```

### Data-TestId Naming Convention
```html
<!-- Components -->
<div data-testid="user-profile">
  <!-- Form controls -->  
  <input data-testid="user-profile-name-input" />
  <button data-testid="user-profile-save-button">Save</button>
  
  <!-- States -->
  <div data-testid="loading-spinner">Loading...</div>
  <div data-testid="error-message">Error occurred</div>
  <div data-testid="success-message">Success!</div>
</div>
```

### User-Centric Testing (REQUIRED)
```typescript
// ✅ Test user interactions and outcomes
it('should show success when user saves valid data', () => {
  spectator.typeInElement('John', byTestId('name-input'));
  spectator.click(byTestId('save-button'));
  
  expect(spectator.query(byTestId('success-message'))).toBeVisible();
});

// ❌ Don't test implementation details  
it('should NOT test like this', () => {
  spectator.component.saveData(); // Testing internals
  expect(mockService.save).toHaveBeenCalled(); // Implementation detail
});
```

## Backend Testing (Java)

### Integration Test Template
```java
@Test
public void testEndpointIntegration() throws Exception {
    // Setup - use DotConnect for database
    DotConnect dotConnect = new DotConnect()
        .setSQL("INSERT INTO test_table VALUES (?)")
        .addParam("test_value");
        
    // Execute - use APILocator for services
    UserAPI userAPI = APILocator.getUserAPI();
    User result = userAPI.loadUserById("test-id");
    
    // Verify
    assertNotNull(result);
    assertEquals("expected", result.getName());
}
```

### Test Structure
```
Backend Tests:
├── *Test.java (unit tests in same package)
├── *IT.java (integration tests)  
└── test/resources/ (test data)

Frontend Tests:
├── *.spec.ts (adjacent to source)
├── __mocks__/ (mock data)
└── __tests__/ (complex scenarios)
```

## Quick Test Commands
```bash
# Backend tests
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false

# Frontend tests  
cd core-web && nx run dotcms-ui:test

# Specific test file
cd core-web && nx run dotcms-ui:test --testNamePattern="MyComponent"
```

## On-Demand Documentation
**Load only when needed:**

- **Complete testing guide**: `@docs/frontend/TESTING_FRONTEND.md`
- **Backend testing**: `@docs/testing/BACKEND_UNIT_TESTS.md`
- **Integration testing**: `@docs/testing/INTEGRATION_TESTS.md`
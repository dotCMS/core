# Frontend Testing Patterns

This document follows [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md): use the `$` prefix for signals in component examples, `setInput()` for inputs, `byTestId()` for selection, and Spectator as the single testing harness.

## Tech Stack for Testing
- **Testing Framework**: Jest or Vitest
- **Testing Library**: **Spectator (required)** — use `@ngneat/spectator` with `@ngneat/spectator/jest` (Jest) or `@ngneat/spectator` (Vitest)
- **Coverage Tool**: Jest Coverage / Vitest coverage
- **Mocking**: Jest/Vitest + `mockProvider` from Spectator
- **E2E**: Playwright (when needed)

## Spectator API (Required)

Always use Spectator with Jest or Vitest via the `@ngneat/spectator` package. Use these APIs consistently:

| API | Use for |
|-----|--------|
| **`createComponentFactory`** | Component tests — creates a factory that returns a `Spectator<C>` and the component under test |
| **`createDirectiveFactory`** | Directive tests — creates a host and the directive instance |
| **`createPipeFactory`** | Pipe tests — creates a host and the pipe instance |
| **`createServiceFactory`** | Service tests — creates a Spectator service wrapper |
| **`createHostFactory`** | Custom host component for testing directives/components in a wrapper |
| **`createRoutingFactory`** | Component tests with routing (Router, ActivatedRoute) |
| **`createHttpFactory`** | Service/component tests that need `HttpClient` with built-in mocking |
| **`Spectator`** | Type the spectator instance: `let spectator: Spectator<MyComponent>` |
| **`byTestId(testId)`** | Select elements by `data-testid` — **always prefer over CSS selectors** |
| **`mockProvider(Service, stubs?)`** | Mock a service in `providers` with optional method stubs |
| **`spectator.detectChanges()`** | Trigger change detection after state/input changes |
| **`spectator.setInput(name, value)`** | Set a component input (use this; never set inputs directly on the component) |
| **`spectator.click(selector)`** | Simulate a click on an element (e.g. `spectator.click(byTestId('submit-button'))`) |

### Other Spectator factories
- **`createDirectiveFactory`** — For directives: creates a host component and the directive instance.
- **`createPipeFactory`** — For pipes: creates a host and the pipe instance for testing transform logic.
- **`createHostFactory`** — Custom host component (e.g. for testing a directive or component inside a wrapper).
- **`createRoutingFactory`** — Component tests with routing: provides `Router`, `ActivatedRoute`, and routing helpers.
- **`createHttpFactory`** — Service or component tests that need `HttpClient`: provides a mocked HTTP backend.

Use the factory that matches what you are testing (component, directive, pipe, service, routing, HTTP).

## File Structure and Organization
- Test files must be named with `.spec.ts` suffix
- Place test files adjacent to the file being tested
- Follow pattern: `[name].spec.ts` for all testable files
- Create mock files with `.mock.ts` suffix in a `__mocks__` directory
- Group related test files in a `__tests__` directory for complex components

```
component-name/
├── component-name.component.ts
├── component-name.component.html
├── component-name.component.spec.ts
├── __mocks__/
│   ├── component-name.mock.ts
│   └── service-name.mock.ts
└── __tests__/  # For complex test scenarios
    └── complex-scenario.spec.ts
```

## Spectator Testing Framework (Required)

### Component Testing Setup
Use **`createComponentFactory`** to create the factory, the **`Spectator`** class to type the instance, **`mockProvider`** for mocks, **`byTestId`** for selection, **`setInput`** for inputs, **`detectChanges`** after changes, and **`click`** for user actions.

```typescript
import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';
import { mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

describe('DotMyComponent', () => {
  let spectator: Spectator<DotMyComponent>;
  let mockService: SpyObject<MyService>;

  const createComponent = createComponentFactory({
    component: DotMyComponent,
    imports: [CommonModule, DotTestingModule],
    providers: [
      mockProvider(MyService, {
        getItems: jest.fn().mockReturnValue(of(mockItems)),
        saveItem: jest.fn().mockReturnValue(of(mockItem)),
        deleteItem: jest.fn().mockReturnValue(of(undefined))
      })
    ]
  });

  beforeEach(() => {
    spectator = createComponent();
    mockService = spectator.inject(MyService) as SpyObject<MyService>;
  });

  it('should show items when input is set', () => {
    spectator.setInput('config', mockConfig);
    spectator.detectChanges();
    expect(spectator.query(byTestId('items-list'))).toBeVisible();
  });
});
```

For **Vitest**, use `@ngneat/spectator` (or the Vitest-specific entry if your version provides one) and replace `jest.fn()` with `vi.fn()`.

### Required Testing Patterns

#### Input Signal Testing
Use **`setInput`** to set component inputs and **`detectChanges`** after changes. Component signals follow ANGULAR_STANDARDS (`$` prefix), so the input may be declared as `$config` in the component; use the **binding name** in `setInput` (typically without the `$`).

```typescript
it('should update display when input changes', () => {
  const initialConfig = { apiEndpoint: '/api/v1/test', pageSize: 10 };
  const updatedConfig = { apiEndpoint: '/api/v1/updated', pageSize: 20 };

  spectator.setInput('config', initialConfig);
  spectator.detectChanges();

  expect(spectator.component.$config()).toEqual(initialConfig);

  spectator.setInput('config', updatedConfig);
  spectator.detectChanges();

  expect(spectator.component.$config()).toEqual(updatedConfig);
});

// ❌ NEVER set inputs directly on the component
it('should NOT do this', () => {
  // WRONG — does not trigger change detection; use setInput instead
  spectator.component.$config = signal(newConfig);
});
```

#### Output Signal Testing
Use **`click`** with **`byTestId`** to trigger the action; assert on the output emit (component uses `$` prefix for outputs per ANGULAR_STANDARDS).

```typescript
it('should emit output when item is selected', () => {
  const item = mockItems[0];
  const emitSpy = jest.spyOn(spectator.component.$itemSelected, 'emit');

  spectator.click(byTestId(`item-${item.id}`));

  expect(emitSpy).toHaveBeenCalledWith(item);
});
```

#### User Interaction Testing
Use **`click`**, **`typeInElement`**, and **`byTestId`** to simulate user actions; call **`detectChanges`** when needed after async or state updates.

```typescript
it('should handle user interactions', () => {
  spectator.click(byTestId('submit-button'));
  spectator.detectChanges();
  expect(spectator.query(byTestId('success-message'))).toBeVisible();

  spectator.typeInElement('test value', byTestId('name-input'));
  expect(spectator.query(byTestId('name-input'))).toHaveValue('test value');

  spectator.selectOption(byTestId('status-dropdown'), 'active');
  expect(spectator.query(byTestId('status-dropdown'))).toHaveValue('active');
});
```

## Element Selection Patterns

### Data-TestId Naming Convention (Required)

**ALWAYS use data-testid for element selection following this format:**
```
data-testid="[what-it-is]-[what-it-does]"
```

#### HTML Template Examples
```html
<!-- Components -->
<div data-testid="user-profile">
  <!-- Form Controls -->
  <input data-testid="user-profile-name-input" />
  <button data-testid="user-profile-save-button">Save</button>
  
  <!-- Lists -->
  <ul data-testid="user-profile-items-list">
    <li data-testid="user-profile-item-1">Item 1</li>
  </ul>
</div>

<!-- Forms -->
<form data-testid="login-form">
  <input data-testid="username-input" type="text">
  <button data-testid="submit-button">Login</button>
</form>

<!-- States -->
<div data-testid="error-message">Error message</div>
<div data-testid="success-message">Success message</div>
<div data-testid="loading-spinner">Loading...</div>
<div data-testid="empty-state">No items found</div>
```

#### Common Element Patterns
- **Buttons**: `data-testid="submit-button"`, `data-testid="edit-button"`
- **Forms**: `data-testid="login-form"`, `data-testid="search-form"`
- **Inputs**: `data-testid="username-input"`, `data-testid="search-input"`
- **Lists**: `data-testid="users-list"`, `data-testid="items-list"`
- **List items**: `data-testid="user-item"`, `data-testid="list-item"`
- **Messages**: `data-testid="error-message"`, `data-testid="success-message"`
- **Loading**: `data-testid="loading-spinner"`, `data-testid="loading-indicator"`
- **Empty states**: `data-testid="empty-state"`, `data-testid="no-data-message"`
- **Dialogs**: `data-testid="confirmation-dialog"`, `data-testid="delete-dialog"`

### Data-TestId Usage (Required)
Use the **`byTestId(testId)`** function to select elements by `data-testid`. Do not rely on CSS classes or tag names for behavior tests.

```typescript
// ✅ ALWAYS use byTestId for element selection
const button = spectator.query(byTestId('submit-button'));
const input = spectator.query(byTestId('name-input'));
const listItem = spectator.query(byTestId('item-123'));

// ✅ Use descriptive test IDs
const refreshButton = spectator.query(byTestId('refresh-button'));
const deleteButton = spectator.query(byTestId('delete-button'));
const confirmDialog = spectator.query(byTestId('confirm-dialog'));

// ❌ Don't use generic selectors
const button = spectator.query('button');       // Too generic
const input = spectator.query('.my-input');     // CSS class coupling
```

### Component Template with Test IDs
Templates should expose `data-testid` on interactive and assertable elements so tests can use **`byTestId`**.

```typescript
@Component({
  selector: 'dot-form',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dot-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFormComponent {
  readonly formGroup = this.fb.group({ name: [''], status: ['active'] });
  readonly $showSuccess = signal(false);
  // ...
}
```

Template (`.html`): use `[data-testid]` on inputs, buttons, and messages.

## State Testing Patterns

### Signal State Testing
Component state signals use the `$` prefix per ANGULAR_STANDARDS. Use **`click`** to trigger actions and **`detectChanges`** to flush updates.

```typescript
it('should manage loading state correctly', () => {
  expect(spectator.component.$loading()).toBe(false);

  spectator.click(byTestId('load-button'));
  spectator.detectChanges();

  expect(spectator.component.$loading()).toBe(true);
  expect(spectator.query(byTestId('loading-indicator'))).toBeVisible();

  spectator.detectChanges(); // after async completes

  expect(spectator.component.$loading()).toBe(false);
  expect(spectator.query(byTestId('loading-indicator'))).not.toBeVisible();
});

it('should handle error states', () => {
  mockService.getItems.mockReturnValue(throwError(() => new Error('API Error')));

  spectator.click(byTestId('load-button'));
  spectator.detectChanges();

  expect(spectator.component.$error()).toBe('Failed to load items');
  expect(spectator.query(byTestId('error-message'))).toBeVisible();
});
```

### Computed Signal Testing
Use **`setInput`** for inputs that drive computed state; for internal state signals (e.g. when testing store-less components), set the signal only when not exposed as an input. Prefer testing computed results via user-visible output (e.g. list length, visible rows).

```typescript
it('should compute filtered items correctly', () => {
  const items: MyItem[] = [
    { id: '1', name: 'Apple', status: 'active' },
    { id: '2', name: 'Banana', status: 'inactive' },
    { id: '3', name: 'Cherry', status: 'active' }
  ];

  spectator.setInput('items', items);
  spectator.setInput('statusFilter', 'active');
  spectator.detectChanges();

  const filteredItems = spectator.component.$filteredItems();
  expect(filteredItems).toHaveLength(2);
  expect(filteredItems.map((i: MyItem) => i.name)).toEqual(['Apple', 'Cherry']);
});
```

## Service Testing Patterns

### Using `createServiceFactory` (Preferred)
Use **`createServiceFactory`** from Spectator for service tests so you get a typed **`SpectatorService<MyService>`** and consistent setup with **`mockProvider`**.

```typescript
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';
import { MyService } from './my.service';
import { HttpClient } from '@angular/common/http';

describe('MyService', () => {
  let spectator: SpectatorService<MyService>;
  let httpMock: SpyObject<HttpClient>;

  const createService = createServiceFactory({
    service: MyService,
    mocks: [HttpClient]
  });

  beforeEach(() => {
    spectator = createService();
    httpMock = spectator.inject(HttpClient) as SpyObject<HttpClient>;
  });

  it('should get items from API', () => {
    const mockResponse = [mockItem1, mockItem2];
    httpMock.get.mockReturnValue(of(mockResponse));

    let result: MyItem[] = [];
    spectator.service.getItems().subscribe((items) => (result = items));

    expect(httpMock.get).toHaveBeenCalledWith('/api/v1/items');
    expect(result).toEqual(mockResponse);
  });

  it('should handle API errors', () => {
    const errorResponse = new Error('API Error');
    httpMock.get.mockReturnValue(throwError(() => errorResponse));

    let error: unknown;
    spectator.service.getItems().subscribe({
      next: () => {},
      error: (err: unknown) => (error = err)
    });

    expect(error).toBe(errorResponse);
    expect(spectator.service.$error()).toBe('Failed to load items');
  });
});
```

### Alternative: TestBed + mockProvider
When not using `createServiceFactory`, still use **`mockProvider`** for dependencies:

```typescript
beforeEach(() => {
  TestBed.configureTestingModule({
    providers: [MyService, mockProvider(HttpClient, { get: jest.fn().mockReturnValue(of([])) })]
  });
  service = TestBed.inject(MyService);
});
```

## CSS Class Testing

### Class Assertion Pattern
Use **`detectChanges`** after changing state so the template updates. Use **separate string arguments** for `toHaveClass` (per ANGULAR_STANDARDS). Component signals use `$` prefix.

```typescript
it('should apply correct CSS classes', () => {
  const icon = spectator.query(byTestId('status-icon'));
  expect(icon).toHaveClass('pi', 'pi-check');

  spectator.component.$isActive.set(true);
  spectator.detectChanges();

  const item = spectator.query(byTestId('item-card'));
  expect(item).toHaveClass('item-card', 'item-card--active');

  spectator.component.$isActive.set(false);
  spectator.detectChanges();

  expect(item).toHaveClass('item-card');
  expect(item).not.toHaveClass('item-card--active');
});

// ❌ Don't use object notation for class testing
it('should NOT test classes like this', () => {
  const element = spectator.query(byTestId('element'));
  expect(element).toHaveClass({ pi: true, 'pi-check': true }); // WRONG
});
```

## Form Testing Patterns

### Form Validation Testing
```typescript
it('should validate form inputs', () => {
    // Test required field validation
    spectator.typeInElement('', byTestId('name-input'));
    spectator.blur(byTestId('name-input'));
    
    expect(spectator.query(byTestId('name-error'))).toBeVisible();
    expect(spectator.query(byTestId('name-error'))).toHaveText('Name is required');
    
    // Test valid input
    spectator.typeInElement('Valid Name', byTestId('name-input'));
    spectator.blur(byTestId('name-input'));
    
    expect(spectator.query(byTestId('name-error'))).not.toBeVisible();
    expect(spectator.query(byTestId('submit-button'))).not.toBeDisabled();
});

it('should handle form submission', () => {
    // Arrange
    const formData = { name: 'Test Item', description: 'Test Description' };
    
    // Act
    spectator.typeInElement(formData.name, byTestId('name-input'));
    spectator.typeInElement(formData.description, byTestId('description-input'));
    spectator.click(byTestId('submit-button'));
    
    // Assert
    expect(mockService.saveItem).toHaveBeenCalledWith(
        expect.objectContaining(formData)
    );
});
```

## Async Testing Patterns

### Observable Testing
Use **`click`** to trigger async work, **`detectChanges`** after awaiting, and component signals with `$` prefix.

```typescript
it('should handle async operations', async () => {
  const loadingPromise = new Promise<void>((resolve) => setTimeout(resolve, 100));
  mockService.getItems.mockReturnValue(
    from(loadingPromise).pipe(map(() => mockItems))
  );

  spectator.click(byTestId('load-button'));

  expect(spectator.component.$loading()).toBe(true);
  expect(spectator.query(byTestId('loading-indicator'))).toBeVisible();

  await loadingPromise;
  spectator.detectChanges();

  expect(spectator.component.$loading()).toBe(false);
  expect(spectator.query(byTestId('loading-indicator'))).not.toBeVisible();
  expect(spectator.component.$items()).toEqual(mockItems);
});
```

## Integration Testing Patterns

### Component Integration Testing
Use **`createComponentFactory`**, **`Spectator`**, **`mockProvider`**, **`byTestId`**, **`click`**, and **`detectChanges`** for integration tests.

```typescript
describe('DotFeatureContainer Integration', () => {
  let spectator: Spectator<DotFeatureContainerComponent>;

  const createComponent = createComponentFactory({
    component: DotFeatureContainerComponent,
    imports: [
      CommonModule,
      DotTestingModule,
      DotFeatureListComponent,
      DotFeatureDetailsComponent
    ],
    providers: [
      mockProvider(FeatureService, {
        getItems: jest.fn().mockReturnValue(of(mockItems))
      })
    ]
  });

  beforeEach(() => {
    spectator = createComponent();
  });

  it('should handle full user workflow', () => {
    expect(spectator.queryAll(byTestId('item-card'))).toHaveLength(mockItems.length);

    spectator.click(byTestId('item-1'));
    spectator.detectChanges();
    expect(spectator.query(byTestId('item-details'))).toBeVisible();

    spectator.click(byTestId('edit-button'));
    spectator.typeInElement('Updated Name', byTestId('name-input'));
    spectator.click(byTestId('save-button'));
    spectator.detectChanges();

    expect(spectator.query(byTestId('item-1'))).toContainText('Updated Name');
  });
});
```

## Test Data Management

### Mock Data Setup
```typescript
// test-data.ts
export const mockItems: MyItem[] = [
    {
        id: '1',
        name: 'Test Item 1',
        description: 'Test Description 1',
        status: 'active',
        createdDate: new Date('2023-01-01')
    },
    {
        id: '2',
        name: 'Test Item 2',
        description: 'Test Description 2',
        status: 'inactive',
        createdDate: new Date('2023-01-02')
    }
];

export const mockConfig: MyFeatureConfig = {
    apiEndpoint: '/api/v1/test',
    pageSize: 10,
    enableSelection: true,
    sortBy: 'name'
};
```

## User-Centric Testing Principles (Required)

### 1. Test from User Perspective
Focus on testing what the user sees and experiences, not internal implementation:

```typescript
// ❌ Don't test implementation details
it('should call loadData method', () => {
    spectator.component.loadData();
    expect(service.getData).toHaveBeenCalled();
});

// ✅ Test user interactions and outcomes
it('should load data when user clicks refresh button', () => {
    const spy = jest.spyOn(spectator.inject(DotService), 'getData');
    
    spectator.click(byTestId('refresh-button'));
    
    expect(spy).toHaveBeenCalled();
    expect(spectator.query(byTestId('data-container'))).toContainText('Updated Data');
});
```

### 2. Test Complete User Flows
Test end-to-end user workflows instead of isolated functionality:

```typescript
// ✅ Test complete user workflow
it('should show success message after user submits form', async () => {
    // Arrange: Fill form as a user would
    spectator.typeInElement('John', byTestId('name-input'));
    spectator.typeInElement('john@email.com', byTestId('email-input'));

    // Act: Submit form
    spectator.click(byTestId('submit-button'));

    // Assert: Verify what user sees
    await spectator.fixture.whenStable();
    expect(spectator.query(byTestId('success-message'))).toBeVisible();
});
```

### 3. Test Error States from User Perspective
Verify how users experience error conditions:

```typescript
// ✅ Test error states from user perspective
it('should show error message when server fails', async () => {
    const service = spectator.inject(DotService);
    jest.spyOn(service, 'getData').mockReturnValue(throwError(() => new Error()));

    spectator.click(byTestId('load-data-button'));

    await spectator.fixture.whenStable();
    expect(spectator.query(byTestId('error-message'))).toBeVisible();
});
```

### 4. Dialog/Modal Testing
Test modal interactions from user perspective:

```typescript
describe('ConfirmDialog', () => {
    // ✅ Test modal interactions
    it('should close modal when user clicks cancel', () => {
        spectator.click(byTestId('open-dialog-button'));
        expect(spectator.query(byTestId('confirm-dialog'))).toBeVisible();

        spectator.click(byTestId('dialog-cancel-button'));
        expect(spectator.query(byTestId('confirm-dialog'))).not.toBeVisible();
    });
});
```

### 5. Form Testing Best Practices
Test forms as users would interact with them:

```typescript
describe('UserForm', () => {
    // ✅ Test form validation from user perspective
    it('should show validation errors when user submits empty form', () => {
        spectator.click(byTestId('submit-button'));

        expect(spectator.query(byTestId('name-error'))).toContainText('Name is required');
        expect(spectator.query(byTestId('email-error'))).toContainText('Email is required');
    });

    // ✅ Test successful form submission
    it('should submit form when all fields are valid', () => {
        // Fill form as a user would
        spectator.typeInElement('John Doe', byTestId('name-input'));
        spectator.typeInElement('john@email.com', byTestId('email-input'));
        spectator.click(byTestId('submit-button'));

        expect(spectator.query(byTestId('success-message'))).toBeVisible();
    });
});
```

### Core Principles Summary
1. **User-first mindset**: Test from user's perspective, not developer's
2. **Behavior over implementation**: Test what users see and do
3. **Real interactions**: Use spectator.click(), spectator.typeInElement(), etc.
4. **Visual feedback**: Verify what users see as result of their actions
5. **Error handling**: Test how users experience failures and edge cases
6. **Complete workflows**: Test full user journeys, not isolated functions

## Testing Utilities

### Custom Test Helpers
Use **`detectChanges`** and **`byTestId`** inside helpers. Type the spectator with a generic (avoid `any` per [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md)).

```typescript
// test-helpers.ts
export function waitForAsyncOperation<T>(spectator: Spectator<T>): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(() => {
      spectator.detectChanges();
      resolve();
    }, 0);
  });
}

export function fillForm<T>(spectator: Spectator<T>, formData: Record<string, string>): void {
  Object.entries(formData).forEach(([field, value]) => {
    spectator.typeInElement(value, byTestId(`${field}-input`));
  });
}

export function expectElementToBeVisible<T>(spectator: Spectator<T>, testId: string): void {
  expect(spectator.query(byTestId(testId))).toBeVisible();
}
```

## Common Testing Pitfalls

### What NOT to Do
```typescript
// ❌ Don't set component inputs directly — use setInput()
spectator.component.config = signal(newConfig);

// ❌ Don't use generic selectors — use byTestId
spectator.query('button');
spectator.query('.my-class');

// ❌ Don't test implementation details (e.g. private methods or internal calls)
expect(spectator.component['privateMethod']).toHaveBeenCalled();

// ❌ Don't use object notation for class testing
expect(element).toHaveClass({ class1: true, class2: false });
```

### Best Practices
```typescript
// ✅ Use setInput for component inputs
spectator.setInput('inputProperty', 'value');

// ✅ Use byTestId for element selection
spectator.query(byTestId('submit-button'));

// ✅ Trigger change detection after state/input changes
spectator.detectChanges();

// ✅ Use click for user actions
spectator.click(byTestId('submit-button'));
expect(spectator.query(byTestId('success-message'))).toBeVisible();

// ✅ Use separate string arguments for class testing
expect(element).toHaveClass('class1', 'class2');
```

## Location Information
- **Test files**: Alongside the file under test with `.spec.ts` suffix
- **Test utilities**: `libs/utils/src/lib/testing/`
- **Mock data**: `*.mock.ts` or test setup files
- **Spectator**: `@ngneat/spectator` (Jest: `@ngneat/spectator/jest`). Use **createComponentFactory**, **createDirectiveFactory**, **createPipeFactory**, **createServiceFactory**, **createHostFactory**, **createRoutingFactory**, **createHttpFactory**, **Spectator**, **byTestId**, **mockProvider**, **detectChanges**, **setInput**, **click** as documented above.
- **See also**: [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md), [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) (testing stores), [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md), [docs/frontend/README.md](./README.md)
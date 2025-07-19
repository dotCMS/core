# Frontend Testing Patterns

## Spectator Testing Framework (Required)

### Component Testing Setup
```typescript
import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator';
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
                deleteItem: jest.fn().mockReturnValue(of(void 0))
            })
        ]
    });
    
    beforeEach(() => {
        spectator = createComponent();
        mockService = spectator.inject(MyService) as SpyObject<MyService>;
    });
    
    // Tests here...
});
```

### Required Testing Patterns

#### Input Signal Testing
```typescript
it('should update display when input changes', () => {
    // Arrange
    const initialConfig = { apiEndpoint: '/api/v1/test', pageSize: 10 };
    const updatedConfig = { apiEndpoint: '/api/v1/updated', pageSize: 20 };
    
    // Act - ALWAYS use setInput for component inputs
    spectator.setInput('config', initialConfig);
    spectator.detectChanges();
    
    // Assert initial state
    expect(spectator.component.config()).toEqual(initialConfig);
    
    // Act - Update input
    spectator.setInput('config', updatedConfig);
    spectator.detectChanges();
    
    // Assert updated state
    expect(spectator.component.config()).toEqual(updatedConfig);
});

// ❌ NEVER set inputs directly
it('should NOT do this', () => {
    // This is WRONG - will not trigger change detection
    spectator.component.config = signal(newConfig);
});
```

#### Output Signal Testing
```typescript
it('should emit output when item is selected', () => {
    // Arrange
    const item = mockItems[0];
    const itemSelectedSpy = jest.spyOn(spectator.component.itemSelected, 'emit');
    
    // Act
    spectator.click(byTestId(`item-${item.id}`));
    
    // Assert
    expect(itemSelectedSpy).toHaveBeenCalledWith(item);
});
```

#### User Interaction Testing
```typescript
it('should handle user interactions', () => {
    // ✅ Test user interactions, not implementation details
    spectator.click(byTestId('submit-button'));
    expect(spectator.query(byTestId('success-message'))).toBeVisible();
    
    // ✅ Test form inputs
    spectator.typeInElement('test value', byTestId('name-input'));
    expect(spectator.query(byTestId('name-input'))).toHaveValue('test value');
    
    // ✅ Test dropdown selection
    spectator.selectOption(byTestId('status-dropdown'), 'active');
    expect(spectator.query(byTestId('status-dropdown'))).toHaveValue('active');
});
```

## Element Selection Patterns

### Data-TestId Usage (Required)
```typescript
// ✅ ALWAYS use data-testid for element selection
const button = spectator.query(byTestId('submit-button'));
const input = spectator.query(byTestId('name-input'));
const listItem = spectator.query(byTestId('item-123'));

// ✅ Use descriptive test IDs
const refreshButton = spectator.query(byTestId('refresh-button'));
const deleteButton = spectator.query(byTestId('delete-button'));
const confirmDialog = spectator.query(byTestId('confirm-dialog'));

// ❌ Don't use generic selectors
const button = spectator.query('button'); // Too generic
const input = spectator.query('.my-input'); // CSS class coupling
```

### Component Template with Test IDs
```typescript
@Component({
    template: `
        <form [formGroup]="formGroup" (ngSubmit)="onSubmit()">
            <input 
                formControlName="name"
                [data-testid]="'name-input'"
                placeholder="Enter name"
            />
            
            <select 
                formControlName="status"
                [data-testid]="'status-dropdown'"
            >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
            </select>
            
            <button 
                type="submit"
                [disabled]="!formGroup.valid"
                [data-testid]="'submit-button'"
            >
                Submit
            </button>
            
            @if (showSuccess) {
                <div [data-testid]="'success-message'">
                    Successfully saved!
                </div>
            }
        </form>
    `
})
export class DotFormComponent {
    // Component implementation
}
```

## State Testing Patterns

### Signal State Testing
```typescript
it('should manage loading state correctly', () => {
    // Assert initial state
    expect(spectator.component.loading()).toBe(false);
    
    // Trigger loading
    spectator.click(byTestId('load-button'));
    
    // Assert loading state
    expect(spectator.component.loading()).toBe(true);
    expect(spectator.query(byTestId('loading-indicator'))).toBeVisible();
    
    // Wait for completion
    spectator.detectChanges();
    
    // Assert completed state
    expect(spectator.component.loading()).toBe(false);
    expect(spectator.query(byTestId('loading-indicator'))).not.toBeVisible();
});

it('should handle error states', () => {
    // Arrange - Mock service to return error
    mockService.getItems.mockReturnValue(throwError(() => new Error('API Error')));
    
    // Act
    spectator.click(byTestId('load-button'));
    spectator.detectChanges();
    
    // Assert error state
    expect(spectator.component.error()).toBe('Failed to load items');
    expect(spectator.query(byTestId('error-message'))).toBeVisible();
});
```

### Computed Signal Testing
```typescript
it('should compute filtered items correctly', () => {
    // Arrange
    const items: MyItem[] = [
        { id: '1', name: 'Apple', status: 'active' },
        { id: '2', name: 'Banana', status: 'inactive' },
        { id: '3', name: 'Cherry', status: 'active' }
    ];
    
    spectator.component.items.set(items);
    
    // Act - Set filter
    spectator.component.statusFilter.set('active');
    
    // Assert computed result
    const filteredItems = spectator.component.filteredItems();
    expect(filteredItems).toHaveLength(2);
    expect(filteredItems.map(i => i.name)).toEqual(['Apple', 'Cherry']);
});
```

## Service Testing Patterns

### Service Mock Setup
```typescript
describe('MyService', () => {
    let service: MyService;
    let httpMock: SpyObject<HttpClient>;
    
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                MyService,
                mockProvider(HttpClient)
            ]
        });
        
        service = TestBed.inject(MyService);
        httpMock = TestBed.inject(HttpClient) as SpyObject<HttpClient>;
    });
    
    it('should get items from API', () => {
        // Arrange
        const mockResponse = [mockItem1, mockItem2];
        httpMock.get.mockReturnValue(of(mockResponse));
        
        // Act
        let result: MyItem[] = [];
        service.getItems().subscribe(items => result = items);
        
        // Assert
        expect(httpMock.get).toHaveBeenCalledWith('/api/v1/items');
        expect(result).toEqual(mockResponse);
    });
    
    it('should handle API errors', () => {
        // Arrange
        const errorResponse = new Error('API Error');
        httpMock.get.mockReturnValue(throwError(() => errorResponse));
        
        // Act
        let error: any;
        service.getItems().subscribe({
            next: () => {},
            error: (err) => error = err
        });
        
        // Assert
        expect(error).toBe(errorResponse);
        expect(service.error()).toBe('Failed to load items');
    });
});
```

## CSS Class Testing

### Class Assertion Pattern
```typescript
it('should apply correct CSS classes', () => {
    // ✅ Test multiple classes with separate string arguments
    const icon = spectator.query(byTestId('status-icon'));
    expect(icon).toHaveClass('pi', 'pi-check');
    
    // ✅ Test conditional classes
    spectator.component.isActive.set(true);
    spectator.detectChanges();
    
    const item = spectator.query(byTestId('item-card'));
    expect(item).toHaveClass('item-card', 'item-card--active');
    
    // ✅ Test class removal
    spectator.component.isActive.set(false);
    spectator.detectChanges();
    
    expect(item).toHaveClass('item-card');
    expect(item).not.toHaveClass('item-card--active');
});

// ❌ Don't use object notation for class testing
it('should NOT test classes like this', () => {
    const element = spectator.query(byTestId('element'));
    // This is WRONG
    expect(element).toHaveClass({ 'pi': true, 'pi-check': true });
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
```typescript
it('should handle async operations', async () => {
    // Arrange
    const loadingPromise = new Promise(resolve => setTimeout(resolve, 100));
    mockService.getItems.mockReturnValue(from(loadingPromise).pipe(
        map(() => mockItems)
    ));
    
    // Act
    spectator.click(byTestId('load-button'));
    
    // Assert loading state
    expect(spectator.component.loading()).toBe(true);
    expect(spectator.query(byTestId('loading-indicator'))).toBeVisible();
    
    // Wait for async operation
    await loadingPromise;
    spectator.detectChanges();
    
    // Assert completed state
    expect(spectator.component.loading()).toBe(false);
    expect(spectator.query(byTestId('loading-indicator'))).not.toBeVisible();
    expect(spectator.component.items()).toEqual(mockItems);
});
```

## Integration Testing Patterns

### Component Integration Testing
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
        // Load items
        expect(spectator.queryAll(byTestId('item-card'))).toHaveLength(mockItems.length);
        
        // Select item
        spectator.click(byTestId('item-1'));
        expect(spectator.query(byTestId('item-details'))).toBeVisible();
        
        // Edit item
        spectator.click(byTestId('edit-button'));
        spectator.typeInElement('Updated Name', byTestId('name-input'));
        spectator.click(byTestId('save-button'));
        
        // Verify update
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

## Testing Utilities

### Custom Test Helpers
```typescript
// test-helpers.ts
export function waitForAsyncOperation(spectator: Spectator<any>): Promise<void> {
    return new Promise(resolve => {
        setTimeout(() => {
            spectator.detectChanges();
            resolve();
        }, 0);
    });
}

export function fillForm(spectator: Spectator<any>, formData: Record<string, string>): void {
    Object.entries(formData).forEach(([field, value]) => {
        spectator.typeInElement(value, byTestId(`${field}-input`));
    });
}

export function expectElementToBeVisible(spectator: Spectator<any>, testId: string): void {
    expect(spectator.query(byTestId(testId))).toBeVisible();
}
```

## Common Testing Pitfalls

### What NOT to Do
```typescript
// ❌ Don't set component inputs directly
spectator.component.inputProperty = 'value';

// ❌ Don't use generic selectors
spectator.query('button');
spectator.query('.my-class');

// ❌ Don't test implementation details
expect(spectator.component.privateMethod).toHaveBeenCalled();

// ❌ Don't use object notation for class testing
expect(element).toHaveClass({ 'class1': true, 'class2': false });
```

### Best Practices
```typescript
// ✅ Use setInput for component inputs
spectator.setInput('inputProperty', 'value');

// ✅ Use data-testid for element selection
spectator.query(byTestId('submit-button'));

// ✅ Test behavior, not implementation
spectator.click(byTestId('submit-button'));
expect(spectator.query(byTestId('success-message'))).toBeVisible();

// ✅ Use separate string arguments for class testing
expect(element).toHaveClass('class1', 'class2');
```

## Location Information
- **Test files**: Located alongside component files with `.spec.ts` suffix
- **Test utilities**: Found in `libs/utils/src/lib/testing/`
- **Mock data**: Typically in `*.mock.ts` files or test setup files
- **Spectator**: Imported from `@ngneat/spectator`
- **Jest matchers**: Additional matchers available in `@ngneat/spectator/jest`
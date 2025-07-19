# Frontend Unit Tests

## Overview

Frontend unit tests in dotCMS are located in `core-web/` and use Jest with Angular Testing Utilities and Spectator. These tests focus on component isolation, service testing, and user interface behavior validation.

## Test Structure

### Location
- **Main Path**: `core-web/apps/dotcms-ui/src/app/`
- **Test Files**: `*.spec.ts` files alongside source files
- **Test Runner**: NX with Jest configuration
- **Framework**: Jest + Angular Testing Utilities + Spectator

### Configuration Files
- **Jest Config**: `core-web/jest.config.ts`
- **NX Config**: `core-web/nx.json`
- **Karma Config**: `core-web/karma.conf.js` (legacy/fallback)
- **TypeScript Config**: `core-web/tsconfig.spec.json`

## Testing Patterns

### Component Testing with Spectator
```typescript
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { Component } from '@angular/core';

@Component({
    template: `
        <div data-testid="greeting">Hello {{ name }}</div>
        <button data-testid="change-name" (click)="changeName()">Change Name</button>
    `
})
class TestComponent {
    name = 'World';
    
    changeName() {
        this.name = 'Angular';
    }
}

describe('TestComponent', () => {
    let spectator: Spectator<TestComponent>;
    
    const createComponent = createComponentFactory({
        component: TestComponent,
        imports: [CommonModule],
        providers: []
    });
    
    beforeEach(() => {
        spectator = createComponent();
    });
    
    it('should display greeting', () => {
        expect(spectator.query('[data-testid="greeting"]')).toHaveText('Hello World');
    });
    
    it('should change name when button clicked', () => {
        spectator.click('[data-testid="change-name"]');
        expect(spectator.query('[data-testid="greeting"]')).toHaveText('Hello Angular');
    });
});
```

### Service Testing
```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ContentService } from './content.service';

describe('ContentService', () => {
    let service: ContentService;
    let httpController: HttpTestingController;
    
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [ContentService]
        });
        
        service = TestBed.inject(ContentService);
        httpController = TestBed.inject(HttpTestingController);
    });
    
    afterEach(() => {
        httpController.verify();
    });
    
    it('should get content by id', () => {
        const mockContent = { id: '123', title: 'Test Content' };
        
        service.getContent('123').subscribe(content => {
            expect(content).toEqual(mockContent);
        });
        
        const req = httpController.expectOne('/api/content/123');
        expect(req.request.method).toBe('GET');
        req.flush(mockContent);
    });
});
```

### Mock Service Pattern
```typescript
export class MockContentService {
    getContent = jest.fn().mockReturnValue(of({
        id: '123',
        title: 'Mock Content'
    }));
    
    saveContent = jest.fn().mockReturnValue(of({ success: true }));
}

describe('ContentComponent', () => {
    let spectator: Spectator<ContentComponent>;
    const mockService = new MockContentService();
    
    const createComponent = createComponentFactory({
        component: ContentComponent,
        providers: [
            { provide: ContentService, useValue: mockService }
        ]
    });
    
    it('should load content on init', () => {
        spectator = createComponent();
        expect(mockService.getContent).toHaveBeenCalled();
    });
});
```

## Key Testing Areas

### 1. Component Lifecycle Testing
```typescript
describe('ComponentLifecycle', () => {
    it('should initialize data on ngOnInit', () => {
        spectator = createComponent();
        
        expect(spectator.component.data).toBeDefined();
        expect(spectator.component.loading).toBe(false);
    });
    
    it('should cleanup on ngOnDestroy', () => {
        spectator = createComponent();
        const destroySpy = jest.spyOn(spectator.component, 'ngOnDestroy');
        
        spectator.fixture.destroy();
        
        expect(destroySpy).toHaveBeenCalled();
    });
});
```

### 2. Form Testing
```typescript
import { ReactiveFormsModule } from '@angular/forms';

describe('FormComponent', () => {
    let spectator: Spectator<FormComponent>;
    
    const createComponent = createComponentFactory({
        component: FormComponent,
        imports: [ReactiveFormsModule]
    });
    
    beforeEach(() => {
        spectator = createComponent();
    });
    
    it('should validate required fields', () => {
        const form = spectator.component.form;
        
        expect(form.valid).toBe(false);
        expect(form.get('name')?.errors?.['required']).toBeTruthy();
    });
    
    it('should submit valid form', () => {
        spectator.component.form.patchValue({
            name: 'Test Name',
            email: 'test@example.com'
        });
        
        spectator.click('[data-testid="submit-button"]');
        
        expect(spectator.component.onSubmit).toHaveBeenCalled();
    });
});
```

### 3. Directive Testing
```typescript
import { Component } from '@angular/core';
import { createDirectiveFactory, SpectatorDirective } from '@ngneat/spectator/jest';

@Component({
    template: `<div appHighlight [color]="color">Test Content</div>`
})
class TestDirectiveComponent {
    color = 'yellow';
}

describe('HighlightDirective', () => {
    let spectator: SpectatorDirective<HighlightDirective>;
    
    const createDirective = createDirectiveFactory({
        directive: HighlightDirective,
        host: TestDirectiveComponent
    });
    
    it('should highlight element', () => {
        spectator = createDirective();
        const element = spectator.element;
        
        expect(element.style.backgroundColor).toBe('yellow');
    });
});
```

### 4. Pipe Testing
```typescript
describe('TruncatePipe', () => {
    let pipe: TruncatePipe;
    
    beforeEach(() => {
        pipe = new TruncatePipe();
    });
    
    it('should truncate long text', () => {
        const result = pipe.transform('This is a very long text', 10);
        expect(result).toBe('This is a...');
    });
    
    it('should return original text if shorter than limit', () => {
        const result = pipe.transform('Short', 10);
        expect(result).toBe('Short');
    });
});
```

## Data-TestId Pattern

### Implementation
```typescript
// Component template
<div data-testid="content-list">
    <div 
        *ngFor="let item of items" 
        data-testid="content-item"
        [attr.data-testid]="'content-item-' + item.id"
    >
        {{ item.title }}
    </div>
</div>

// Test
it('should display content items', () => {
    const items = spectator.queryAll('[data-testid="content-item"]');
    expect(items).toHaveLength(3);
});

it('should display specific item', () => {
    const specificItem = spectator.query('[data-testid="content-item-123"]');
    expect(specificItem).toHaveText('Test Title');
});
```

### Helper Functions
```typescript
// test-helpers.ts
export const byTestId = (testId: string) => `[data-testid="${testId}"]`;

// Usage in tests
it('should find element by test id', () => {
    const element = spectator.query(byTestId('submit-button'));
    expect(element).toBeVisible();
});
```

## Running Tests

### Command Line Execution
```bash
# Run all frontend tests
cd core-web && nx test dotcms-ui

# Run tests in watch mode
cd core-web && nx test dotcms-ui --watch

# Run specific test file
cd core-web && nx test dotcms-ui --testNamePattern="ComponentName"

# Run with coverage
cd core-web && nx test dotcms-ui --coverage

# Run tests matching pattern
cd core-web && nx test dotcms-ui --testPathPattern="content"

# Run in CI mode (no watch)
cd core-web && nx test dotcms-ui --watchAll=false
```

### NX Configuration
```json
// nx.json
{
    "projects": {
        "dotcms-ui": {
            "targets": {
                "test": {
                    "executor": "@nx/jest:jest",
                    "options": {
                        "jestConfig": "core-web/jest.config.ts"
                    }
                }
            }
        }
    }
}
```

### Jest Configuration
```typescript
// jest.config.ts
export default {
    preset: 'jest-preset-angular',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    collectCoverageFrom: [
        'src/app/**/*.ts',
        '!src/app/**/*.spec.ts',
        '!src/app/**/*.module.ts'
    ],
    coverageReporters: ['html', 'lcov', 'text-summary'],
    testPathIgnorePatterns: ['/node_modules/', '/dist/'],
    transform: {
        '^.+\\.(ts|js|html)$': 'jest-preset-angular'
    }
};
```

## CI/CD Integration

### GitHub Actions Integration
**Workflow**: Frontend tests run in `cicd_comp_test-phase.yml`

**Change Detection**: Tests triggered by:
```yaml
frontend: &frontend
  - 'core-web/**'
  - 'package.json'
  - 'yarn.lock'
  - '.nvmrc'
```

**Execution**:
```yaml
- name: Install Dependencies
  run: cd core-web && yarn install --frozen-lockfile

- name: Run Frontend Tests
  run: cd core-web && nx test dotcms-ui --watchAll=false --coverage

- name: Upload Coverage
  uses: codecov/codecov-action@v3
  with:
    file: ./core-web/coverage/lcov.info
```

### Test Results
- **Jest Reports**: `core-web/coverage/`
- **JUnit XML**: `core-web/junit.xml`
- **Coverage Reports**: `core-web/coverage/lcov-report/`

## Debugging Test Failures

### Local Debugging

#### 1. Run Tests with Debug Output
```bash
# Run with verbose output
cd core-web && nx test dotcms-ui --verbose

# Run specific test with debug
cd core-web && nx test dotcms-ui --testNamePattern="specific test" --verbose

# Run with debugging enabled
cd core-web && node --inspect-brk node_modules/.bin/jest --runInBand
```

#### 2. IDE Integration
```typescript
// Add debugging in tests
describe('Component', () => {
    it('should work', () => {
        // Set breakpoint here
        debugger;
        
        spectator = createComponent();
        
        // Debug component state
        console.log('Component state:', spectator.component);
    });
});
```

#### 3. Test Isolation
```typescript
// Run only specific test
describe.only('ComponentName', () => {
    it.only('should test specific behavior', () => {
        // Test implementation
    });
});

// Skip problematic tests temporarily
describe.skip('ProblematicComponent', () => {
    it('should skip this test', () => {
        // Skipped test
    });
});
```

### GitHub Actions Debugging

#### 1. Enable Debug Mode
```yaml
- name: Run Frontend Tests
  run: cd core-web && nx test dotcms-ui --watchAll=false --verbose
  env:
    DEBUG: true
    NODE_OPTIONS: --max-old-space-size=4096
```

#### 2. Upload Test Artifacts
```yaml
- name: Upload Test Results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: frontend-test-results
    path: |
      core-web/coverage/
      core-web/junit.xml
      core-web/test-results/
```

#### 3. Common Failure Patterns

**Memory Issues**:
```bash
# Increase Node.js memory
export NODE_OPTIONS="--max-old-space-size=4096"
cd core-web && nx test dotcms-ui
```

**Timeout Issues**:
```typescript
// Increase test timeout
describe('Component', () => {
    it('should handle async operation', async () => {
        // Test implementation
    }, 10000); // 10 second timeout
});
```

**DOM Issues**:
```typescript
// Wait for DOM updates
it('should update DOM', async () => {
    spectator.click('[data-testid="button"]');
    await spectator.fixture.whenStable();
    
    expect(spectator.query('[data-testid="result"]')).toHaveText('Updated');
});
```

## Best Practices

### ✅ Component Testing Standards
- **Use Spectator**: Provides cleaner, more readable tests
- **Data-testid attributes**: Reliable element selection
- **Mock dependencies**: Isolate component under test
- **Test user interactions**: Click, input, form submission
- **Test component lifecycle**: ngOnInit, ngOnDestroy

### ✅ Service Testing Standards
```typescript
describe('DataService', () => {
    let service: DataService;
    let httpController: HttpTestingController;
    
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DataService]
        });
        
        service = TestBed.inject(DataService);
        httpController = TestBed.inject(HttpTestingController);
    });
    
    it('should handle HTTP errors', () => {
        const errorMessage = 'Server error';
        
        service.getData().subscribe({
            next: () => fail('Expected error'),
            error: (error) => expect(error.message).toBe(errorMessage)
        });
        
        const req = httpController.expectOne('/api/data');
        req.flush(errorMessage, { status: 500, statusText: 'Server Error' });
    });
});
```

### ✅ Mock Management
```typescript
// Create reusable mock factories
export const createMockContentService = () => ({
    getContent: jest.fn(),
    saveContent: jest.fn(),
    deleteContent: jest.fn()
});

// Use in tests
describe('ContentComponent', () => {
    let mockService: ReturnType<typeof createMockContentService>;
    
    beforeEach(() => {
        mockService = createMockContentService();
    });
    
    it('should load content', () => {
        mockService.getContent.mockReturnValue(of(mockContent));
        
        spectator = createComponent({
            providers: [{ provide: ContentService, useValue: mockService }]
        });
        
        expect(mockService.getContent).toHaveBeenCalled();
    });
});
```

### ✅ Async Testing
```typescript
describe('AsyncComponent', () => {
    it('should handle async operations', async () => {
        const promise = Promise.resolve('async result');
        mockService.getAsyncData.mockReturnValue(promise);
        
        spectator = createComponent();
        
        await promise;
        spectator.detectChanges();
        
        expect(spectator.query('[data-testid="result"]')).toHaveText('async result');
    });
    
    it('should handle observables', (done) => {
        mockService.getObservableData.mockReturnValue(of('observable result'));
        
        spectator = createComponent();
        
        spectator.component.data$.subscribe(data => {
            expect(data).toBe('observable result');
            done();
        });
    });
});
```

## Common Issues and Solutions

### 1. Angular Testing Module Issues
```typescript
// Problem: Module configuration errors
// Solution: Proper TestBed setup
beforeEach(() => {
    TestBed.configureTestingModule({
        imports: [CommonModule, FormsModule],
        declarations: [ComponentUnderTest],
        providers: [
            { provide: SomeService, useValue: mockService }
        ]
    });
});
```

### 2. Change Detection Issues
```typescript
// Problem: Component not updating
// Solution: Manual change detection
it('should update on change', () => {
    spectator.component.data = newData;
    spectator.detectChanges();
    
    expect(spectator.query('[data-testid="data"]')).toHaveText('new data');
});
```

### 3. Mock Function Issues
```typescript
// Problem: Mock not working as expected
// Solution: Proper Jest mock setup
beforeEach(() => {
    jest.clearAllMocks();
    mockService.getData.mockReturnValue(of(mockData));
});
```

### 4. Memory Leaks in Tests
```typescript
// Problem: Subscriptions not cleaned up
// Solution: Proper cleanup
afterEach(() => {
    spectator.fixture.destroy();
    jest.clearAllMocks();
});
```

## Performance Optimization

### Test Execution Speed
```bash
# Run tests in parallel
cd core-web && nx test dotcms-ui --maxWorkers=4

# Run only changed tests
cd core-web && nx test dotcms-ui --onlyChanged

# Use test sharding for large test suites
cd core-web && nx test dotcms-ui --shard=1/4
```

### Memory Management
```typescript
// jest.config.ts
export default {
    // Limit memory usage
    maxWorkers: '50%',
    
    // Clear cache between tests
    clearMocks: true,
    
    // Optimize for CI
    ci: process.env.CI === 'true'
};
```

## Integration with Development Workflow

### Pre-commit Testing
```bash
# Run tests for changed files
cd core-web && nx affected:test

# Run lint and test together
cd core-web && nx run-many --target=lint,test --all
```

### IDE Integration
- **VS Code**: Jest extension for test runner
- **WebStorm**: Built-in Jest support
- **IntelliJ**: Angular plugin with Jest integration

### Watch Mode Development
```bash
# Run tests in watch mode during development
cd core-web && nx test dotcms-ui --watch

# Watch with coverage
cd core-web && nx test dotcms-ui --watch --coverage
```

## Location Information
- **Test Source**: `core-web/apps/dotcms-ui/src/app/**/*.spec.ts`
- **Test Configuration**: `core-web/jest.config.ts`
- **Test Setup**: `core-web/src/test-setup.ts`
- **Coverage Reports**: `core-web/coverage/`
- **NX Configuration**: `core-web/nx.json`
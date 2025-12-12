# Angular Development Standards

## Tech Stack Configuration
- **Angular**: 20.3.9 standalone components
- **UI**: PrimeNG 17.18.11, PrimeFlex 3.3.1
- **State**: NgRx Signals, Component Store  
- **Build**: Nx 20.5.1
- **Testing**: Jest + Spectator (REQUIRED)

## Modern Template Syntax (Required)
Use Angular's new control flow syntax instead of structural directives:

```typescript
@Component({
  template: `
    <!-- Use @if instead of *ngIf -->
    @if (isLoading()) {
      <dot-spinner />
    } @else {
      <dot-content />
    }

    <!-- Use @for instead of *ngFor -->
    @for (item of items(); track item.id) {
      <div [data-testid]="'item-' + item.id">{{item.name}}</div>
    } @empty {
      <dot-empty-state />
    }

    <!-- Use @switch instead of [ngSwitch] -->
    @switch (status()) {
      @case ('loading') { <dot-loading /> }
      @case ('error') { <dot-error [message]="errorMessage()" /> }
      @default { <dot-content /> }
    }

    <!-- Use @let for reused signal values -->
    @let user = currentUser();
    <h1>{{ user.name }}</h1>
    <p>Email: {{ user.email }}</p>
    @if (user.isAdmin) {
      <dot-admin-panel />
    }

    <!-- Use @defer for lazy loading -->
    @defer (on viewport) {
      <dot-data-grid [data]="gridData()" />
    } @loading {
      <dot-skeleton />
    }
  `
})
```

## File Structure Requirements (Critical)
Every component MUST have three separate files:

```
feature/
├── components/
│   └── feature-list/
│       ├── feature-list.component.ts    # Component logic
│       ├── feature-list.component.html  # Template
│       └── feature-list.component.scss  # Styles
```

❌ Avoid inline templates and styles:
```typescript
@Component({
  selector: "dot-feature",
  template: `<div>Inline template</div>`,
  styles: [`:host { display: block }`]
})
```

✅ Use separate files:
```typescript
@Component({
  selector: "dot-feature", 
  templateUrl: "./feature.component.html",
  styleUrls: ["./feature.component.scss"] // Note: plural styleUrls
})
```

## Component Architecture Requirements

### Component Structure Pattern
```typescript
@Component({
  selector: 'dot-my-component',
  standalone: true,
  imports: [CommonModule, PrimeNGModule],
  templateUrl: './my-component.component.html',
  styleUrls: ['./my-component.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyComponent implements OnInit, OnDestroy {
  // 1. Private fields
  private readonly destroy$ = new Subject<void>();

  // 2. Dependency Injection
  private readonly store = inject(MyStore);
  private readonly service = inject(MyService);

  // 3. Input/Output signals
  name = input<string>();
  config = input<Config>();
  itemSelected = output<Item>();

  // 4. State signals
  protected readonly loading = signal(false);
  protected readonly vm$ = this.store.vm$;

  // 5. Computed signals
  protected readonly state = computed(() => this.store.state());

  // 6. Lifecycle hooks
  ngOnInit(): void {
    this.store.loadData().pipe(takeUntil(this.destroy$)).subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // 7. Public methods
  onAction(item: Item): void {
    this.itemSelected.emit(item);
  }
}
```

### Import Order Convention
```typescript
// 1. Angular Core
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

// 2. RxJS
import { Subject, takeUntil } from 'rxjs';

// 3. Third-party Libraries
import { ButtonModule } from 'primeng/button';

// 4. Application Core (shared/common)
import { ComponentStatus } from '@shared/models';
import { DotHttpErrorManagerService } from '@core/services';

// 5. Feature Specific
import { MyStore } from './store/my.store';
import { MyService } from './services/my.service';
import type { MyConfig } from './models/my.model';
```

## Standalone Component Pattern (Required)
```typescript
@Component({
    selector: 'dot-my-component',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
        @if (condition()) {
            <div>{{data()}}</div>
        }
        @for (item of items(); track item.id) {
            <div [data-testid]="'item-' + item.id">{{item.name}}</div>
        }
    `
})
export class MyComponent {
    // Input signals
    data = input<string>();
    condition = input<boolean>();
    items = input<Item[]>();
    
    // Output signals
    change = output<string>();
    
    // Computed signals
    isValid = computed(() => this.condition() && this.data());
    
    // State signals
    loading = signal(false);
}
```

## Testing Pattern (Required)
```typescript
const createComponent = createComponentFactory({
    component: MyComponent,
    imports: [CommonModule, DotTestingModule],
    providers: [mockProvider(RequiredService)]
});

// ✅ ALWAYS use data-testid for element selection
const button = spectator.query(byTestId('submit-button'));

// ✅ ALWAYS use setInput for component inputs
spectator.setInput('inputProperty', 'value');

// ✅ CSS class verification - separate string arguments
expect(icon).toHaveClass('pi', 'pi-update');

// ✅ Test user interactions, not implementation details
spectator.click(byTestId('refresh-button'));
expect(spectator.query(byTestId('success-message'))).toBeVisible();
```

## Build Commands
```bash
# Development server
nx run dotcms-ui:serve          # → http://localhost:4200/dotAdmin

# Testing
nx run dotcms-ui:test          # Run tests

# Build
nx build dotcms-ui             # Production build

# Dependencies
yarn install                   # NOT npm install
```

## Critical Requirements
> **Security**: All frontend code must follow [Security Principles](../core/SECURITY_PRINCIPLES.md)
> **Progressive Enhancement**: When editing existing code, see [Progressive Enhancement](../core/PROGRESSIVE_ENHANCEMENT.md)
- **data-testid**: Required for all testable elements
- **setInput()**: Never set component inputs directly
- **Spectator**: Required testing framework
- **Signals**: Required for new component state
- **Standalone**: All new components must be standalone
# Angular Development Standards

This document is the single source of truth for Angular development in the dotCMS frontend (Angular v20+, signals, standalone components, modern control flow). It merges project standards with the [Angular style guide](https://angular.dev/style-guide) and essentials.

## Tech Stack Configuration
- **Angular**: 20.3.9 standalone components
- **UI**: PrimeNG 17.18.11, PrimeFlex 3.3.1
- **State**: NgRx Signals, Component Store  
- **Build**: Nx 20.5.1
- **Testing**: Jest + Spectator (REQUIRED)

## Angular Best Practices
- Always use standalone components over `NgModules`
- Do NOT set `standalone: true` inside the `@Component`, `@Directive` and `@Pipe` decorators (it is implied by default)
- Use signals for state management
- Implement lazy loading for feature routes
- Always use `OnPush` change detection strategy
- Use `NgOptimizedImage` for all static images (does not work for inline base64 images)
- Do NOT use the `@HostBinding` and `@HostListener` decorators. Put host bindings inside the `host` object of the `@Component` or `@Directive` decorator instead
- For signals, use the `$` prefix to indicate that it is a signal, example: `$mySignal`
- For observables, use the `$` suffix to indicate that it is an observable, example: `myObservable$`

> **Naming in this document**: All code examples below use the `$` prefix for signals (including `input()`, `output()`, `computed()`, `signal()`) and the `$` suffix for observables.

## Component Rules
- Keep components small and focused on a single responsibility
- Use `input()` signal instead of decorators: [Angular Inputs](https://angular.dev/guide/components/inputs)
- Use `output()` function instead of decorators: [Angular Outputs](https://angular.dev/guide/components/outputs)
- Use `computed()` for derived state: [Signals](https://angular.dev/guide/signals)
- Set `changeDetection: ChangeDetectionStrategy.OnPush` in `@Component` decorator
- Prefer inline templates for small components; use separate files for larger ones
- Prefer Reactive forms instead of Template-driven ones
- Do NOT use `ngClass`, use `class` bindings instead: [CSS class and style bindings](https://angular.dev/guide/templates/binding#css-class-and-style-property-bindings)
- Do NOT use `ngStyle`, use `style` bindings instead: [CSS class and style bindings](https://angular.dev/guide/templates/binding#css-class-and-style-property-bindings)
- Do NOT use `@HostBinding` and `@HostListener` decorators. Put host bindings inside the `host` object of the `@Component` or `@Directive` decorator instead

## TypeScript Best Practices
- Use strict type checking
- Prefer type inference when the type is obvious
- Avoid the `any` type; use `unknown` when the type is uncertain

## Accessibility Requirements
- **AXE**: All components must pass AXE accessibility checks
- **WCAG AA**: Follow WCAG AA minimums (focus management, color contrast, ARIA attributes where needed)

## State Management (Signals)
- **Feature-level state**: Use NgRx Signal Store; see [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md). Avoid manual signal soup in components.
- Use signals for local component state; use `computed()` for derived state.
- Keep state transformations pure and predictable.
- Do NOT use `mutate()` on signals; use `update()` or `set()` instead.

## Template Rules
- Keep templates simple; avoid complex logic in the template
- Use native control flow (`@if`, `@for`, `@switch`) instead of `*ngIf`, `*ngFor`, `*ngSwitch`
- Do not assume globals (e.g. `new Date()`) are available in templates
- Do not write arrow functions in templates (not supported)
- Use the `async` pipe to handle observables
- Use built-in pipes and import custom pipes when used in a template: [Pipes](https://angular.dev/guide/templates/pipes)
- For external `templateUrl` / `styleUrls`, use paths relative to the component TS file

## Modern Template Syntax (Required)
Use Angular's new control flow syntax instead of structural directives:

```typescript
@Component({
  template: `
    <!-- Use @if instead of *ngIf (signals: $ prefix) -->
    @if ($isLoading()) {
      <dot-spinner />
    } @else {
      <dot-content />
    }

    <!-- Use @for instead of *ngFor -->
    @for (item of $items(); track item.id) {
      <div [data-testid]="'item-' + item.id">{{ item.name }}</div>
    } @empty {
      <dot-empty-state />
    }

    <!-- Use @switch instead of [ngSwitch] -->
    @switch ($status()) {
      @case ('loading') { <dot-loading /> }
      @case ('error') { <dot-error [message]="$errorMessage()" /> }
      @default { <dot-content /> }
    }

    <!-- Use @let for reused signal values -->
    @let user = $currentUser();
    <h1>{{ user.name }}</h1>
    <p>Email: {{ user.email }}</p>
    @if (user.isAdmin) {
      <dot-admin-panel />
    }

    <!-- Use @defer for lazy loading -->
    @defer (on viewport) {
      <dot-data-grid [data]="$gridData()" />
    } @loading {
      <dot-skeleton />
    }
  `
})
```

## File Structure Requirements (Critical)
When you create or update a component, put logic in the `.ts` file, the template in the `.html` file, and styles in the `.scss` (or `.css`) file.

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
  imports: [CommonModule, PrimeNGModule],
  templateUrl: './my-component.component.html',
  styleUrls: ['./my-component.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyComponent implements OnInit, OnDestroy {
  // 1. Private fields (observables: $ suffix)
  private readonly destroy$ = new Subject<void>();

  // 2. Dependency Injection
  private readonly store = inject(MyStore);
  private readonly service = inject(MyService);

  // 3. Input/Output signals ($ prefix)
  readonly $name = input<string>();
  readonly $config = input<Config>();
  readonly $itemSelected = output<Item>();

  // 4. State signals ($ prefix); observables ($ suffix)
  protected readonly $loading = signal(false);
  protected readonly vm$ = this.store.vm$;

  // 5. Computed signals ($ prefix)
  protected readonly $state = computed(() => this.store.state());

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
    this.$itemSelected.emit(item);
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
  imports: [CommonModule, FormsModule],
  template: `
    @if ($condition()) {
      <div>{{ $data() }}</div>
    }
    @for (item of $items(); track item.id) {
      <div [data-testid]="'item-' + item.id">{{ item.name }}</div>
    }
  `
})
export class MyComponent {
  // Input signals ($ prefix)
  readonly $data = input<string>();
  readonly $condition = input<boolean>();
  readonly $items = input<Item[]>();

  // Output signals ($ prefix)
  readonly $change = output<string>();

  // Computed signals ($ prefix)
  readonly $isValid = computed(() => this.$condition() && this.$data());

  // State signals ($ prefix)
  protected readonly $loading = signal(false);
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

## Services
- Design services around a single responsibility
- Use `providedIn: 'root'` for singleton services
- Use the `inject()` function instead of constructor injection

## Resources
- [Angular style guide](https://angular.dev/style-guide)
- [Components](https://angular.dev/essentials/components)
- [Signals](https://angular.dev/essentials/signals)
- [Templates](https://angular.dev/essentials/templates)
- [Dependency injection](https://angular.dev/essentials/dependency-injection)

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

## See also
- [COMPONENT_ARCHITECTURE.md](./COMPONENT_ARCHITECTURE.md) — Structure, file layout, data flow
- [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) — NgRx Signal Store for feature state
- [STYLING_STANDARDS.md](./STYLING_STANDARDS.md) — PrimeFlex, BEM, SCSS
- [TESTING_FRONTEND.md](./TESTING_FRONTEND.md) — Spectator, byTestId, setInput
- [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md) — Strict types, as const, # private
- [docs/frontend/README.md](./README.md) — Index of all frontend docs
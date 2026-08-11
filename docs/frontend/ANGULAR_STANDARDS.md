# Angular Development Standards

This document is the single source of truth for Angular development in the dotCMS frontend (Angular 22, signals, standalone components, modern control flow). It merges project standards with the [Angular style guide](https://angular.dev/style-guide) and essentials.

## Tech Stack Configuration

> **Versions are MAJOR-only on purpose.** `core-web/package.json` is the source of truth for the exact pinned versions — read it there instead of copying patch numbers into docs.

- **Angular**: 22.x standalone components
- **Node.js**: 22.22.3+ (pinned via `.nvmrc` / `nodejs-parent/pom.xml`)
- **Package manager**: pnpm 10.x (pinned by the `packageManager` field in `core-web/package.json`). `core-web/pnpm-lock.yaml` is the only lockfile — do NOT use `npm install` or any other package manager
- **UI**: PrimeNG 21.x, Tailwind CSS 4.x
- **Icons**: Material Symbols (see [Icons](#icons-material-symbols)); PrimeIcons (`pi pi-*`) is legacy-only
- **State**: NgRx Signals 21.x, Component Store
- **Build**: Nx 23.x
- **TypeScript**: 6.x
- **Testing**: Jest 30.x + Spectator, imported from `@openng/spectator` (REQUIRED)

## Reuse Before Creating (Required)

Before creating a new component, verify a suitable one does not already exist:

1. **dotCMS components first** — `libs/ui` (`@dotcms/ui`) and the existing feature libs
2. **Then PrimeNG**
3. **Only then** create a new component — and justify why the existing options do not fit

This is the norm, not a suggestion. A new component that duplicates an existing one is a defect, not a feature.

## Angular Best Practices
- Always use standalone components over `NgModules`
- Do NOT set `standalone: true` inside the `@Component`, `@Directive` and `@Pipe` decorators (it is implied by default)
- Use signals for state management
- Implement lazy loading for feature routes
- Use `NgOptimizedImage` for all static images (does not work for inline base64 images)
- Do NOT use the `@HostBinding` and `@HostListener` decorators. Put host bindings inside the `host` object of the `@Component` or `@Directive` decorator instead
- For signals, use the `$` prefix to indicate that it is a signal, example: `$mySignal`
- For observables, use the `$` suffix to indicate that it is an observable, example: `myObservable$`

> **Naming in this document**: All code examples below use the `$` prefix for signals (including `input()`, `output()`, `computed()`, `signal()`) and the `$` suffix for observables.

## Change Detection

`OnPush` is the Angular framework default as of **v22** — see [Advanced component configuration](https://angular.dev/guide/components/advanced-configuration#changedetectionstrategy).

- **New components**: do NOT set `changeDetection` in the `@Component` decorator. The default is already `OnPush`.
- **Existing components marked `ChangeDetectionStrategy.Eager`**: leave them alone. `Eager` is the opt-in eager mode, renamed from `Default` in v22. PR #36907 applied it to legacy components during the Angular 22 upgrade — do NOT convert them to `OnPush` when touching the file for unrelated work.

## Component Rules
- Keep components small and focused on a single responsibility
- Every component selector uses the `dot-` prefix, for example `dot-my-component`
- Use `input()` signal instead of decorators: [Angular Inputs](https://angular.dev/guide/components/inputs)
- Use `output()` function instead of decorators: [Angular Outputs](https://angular.dev/guide/components/outputs)
- Use `computed()` for derived state: [Signals](https://angular.dev/guide/signals)
- Always split logic, template and styles into separate files — see [File Structure Requirements](#file-structure-requirements-critical)
- **Signal Forms first** for new forms — see [Forms](#forms-signal-forms-first)
- Do NOT use `ngClass`, use `class` bindings instead: [CSS class and style bindings](https://angular.dev/guide/templates/binding#css-class-and-style-property-bindings)
- Do NOT use `ngStyle`, use `style` bindings instead: [CSS class and style bindings](https://angular.dev/guide/templates/binding#css-class-and-style-property-bindings)
- Do NOT use `@HostBinding` and `@HostListener` decorators. Put host bindings inside the `host` object of the `@Component` or `@Directive` decorator instead

## TypeScript Best Practices
- Use strict type checking
- Prefer type inference when the type is obvious
- Avoid the `any` type; use `unknown` when the type is uncertain

### TypeScript 6 transitional flag
`core-web/tsconfig.base.json` sets `"ignoreDeprecations": "6.0"`. This flag is **transitional**: it unblocks APIs that TypeScript 6 deprecated so the existing codebase keeps compiling during the migration.

- **New code must not rely on the deprecated APIs it unblocks.** Write against the current, non-deprecated TypeScript surface.
- Do NOT remove the flag as part of unrelated work — retiring it is tracked separately.

## Accessibility Requirements
- **AXE**: All components must pass AXE accessibility checks
- **WCAG AA**: Follow WCAG AA minimums (focus management, color contrast, ARIA attributes where needed)

## Icons (Material Symbols)

Use **Material Symbols** for icons in markup you author:

```html
<span class="material-symbols-outlined">drag_indicator</span>
```

The icon name goes in the element's **text content**, not the class. Fonts are self-hosted via `libs/dotcms-scss/shared/_material-symbols-outlined.scss` (and `_material-symbols-rounded.scss`), loaded through `libs/dotcms-scss/angular/styles.scss`.

- Existing **PrimeIcons** (`pi pi-*`) stay as they are — do NOT mass-migrate them.
- Icons rendered internally by PrimeNG components are out of scope — changing those is a theming concern.
- Do NOT use the deprecated `dot-icon` component in `libs/ui`.

## State Management (Signals)
- **Feature-level state**: Use NgRx Signal Store; see [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md). Avoid manual signal soup in components.
- Use signals for local component state; use `computed()` for derived state.
- Keep state transformations pure and predictable.
- Do NOT use `mutate()` on signals; use `update()` or `set()` instead.

## Component State Handling (Required)

Every component that renders data MUST explicitly handle all of its states: **loading**, **empty**, **error**, and **loaded**. Never leave a blank render path.

- Model the states explicitly — a `$loading` signal, an `$error` signal, and the data signal itself (or a single status signal from the store).
- Use `@if` / `@else` for the loading and error branches.
- Use `@empty` on every `@for` block so an empty collection renders an empty state instead of nothing.
- If a state is genuinely impossible, say so in the template with a `@default` / `@else` branch rather than omitting it.

## Error Handling (Required)

Every data path must handle errors. No silent failures, no unguarded `.subscribe()`, no empty `catchError`. Errors must be surfaced to the user.

- Services: use `catchError` and either rethrow a typed error or return a safe fallback — never swallow.
- Components: subscribe with an `error` callback (or handle the error state coming from the store) and write it into an error signal that the template renders.
- Never leave the user on a spinner: any failure must clear the loading state.

## Forms (Signal Forms First)

**New forms use Signal Forms** — `@angular/forms/signals`, available since Angular v21 and shipped in the Angular 22.x this workspace runs. Signal Forms keep form state in signals, so they compose directly with the rest of the signal-based state in a component.

```typescript
import { form, Control } from '@angular/forms/signals';

protected readonly $model = signal({ name: '', email: '' });
protected readonly userForm = form(this.$model);
```

- **New forms**: Signal Forms.
- **Existing Reactive Forms**: leave them as they are. They remain valid and are NOT to be mass-migrated — convert only when you are already rewriting the form for other reasons.
- **Template-driven forms**: not for new work.

Use `@angular/forms/signals/compat` when a Signal Form has to interoperate with an existing Reactive Forms control.

## Template Rules
- Keep templates simple; avoid complex logic in the template
- Use native control flow (`@if`, `@for`, `@switch`) instead of `*ngIf`, `*ngFor`, `*ngSwitch`
- Do not assume globals (e.g. `new Date()`) are available in templates
- Do not write arrow functions in templates (not supported)
- Use the `async` pipe to handle observables
- Use built-in pipes and import custom pipes when used in a template: [Pipes](https://angular.dev/guide/templates/pipes)
- For external `templateUrl` / `styleUrls`, use paths relative to the component TS file

## Modern Template Syntax (Required)
Use Angular's new control flow syntax instead of structural directives. This example also shows the four required states (loading, error, empty, loaded):

```html
<!-- Use @if / @else instead of *ngIf — loading and error are explicit branches -->
@if ($loading()) {
  <dot-spinner data-testid="loading-indicator" />
} @else if ($error()) {
  <dot-error [message]="$error()!" data-testid="error-message" />
} @else {
  <!-- Use @for instead of *ngFor, and always provide @empty -->
  @for (item of $items(); track item.id) {
    <div [data-testid]="'item-' + item.id">{{ item.name }}</div>
  } @empty {
    <dot-empty-state data-testid="empty-state" />
  }
}

<!-- Use @switch instead of [ngSwitch] — @default covers the remaining states -->
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
  styleUrls: ['./my-component.component.scss']
})
export class MyComponent implements OnInit {
  // 1. Dependency Injection
  private readonly destroyRef = inject(DestroyRef);
  private readonly store = inject(MyStore);
  private readonly service = inject(MyService);

  // 2. Input/Output signals ($ prefix)
  readonly $name = input<string>();
  readonly $config = input<Config>();
  readonly $itemSelected = output<Item>();

  // 3. State signals ($ prefix); observables ($ suffix)
  protected readonly $loading = signal(false);
  protected readonly $error = signal<string | null>(null);
  protected readonly vm$ = this.store.vm$;

  // 4. Computed signals ($ prefix)
  protected readonly $state = computed(() => this.store.state());

  // 5. Lifecycle hooks
  ngOnInit(): void {
    this.$loading.set(true);

    this.store
      .loadData()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.$loading.set(false),
        error: (err) => {
          // Required: surface the failure and clear the loading state
          this.$error.set('Failed to load data');
          this.$loading.set(false);
          console.error('Error loading data:', err);
        }
      });
  }

  // 6. Public methods
  onAction(item: Item): void {
    this.$itemSelected.emit(item);
  }
}
```

### Subscription Teardown
Use `inject(DestroyRef)` + `takeUntilDestroyed(this.destroyRef)` from `@angular/core/rxjs-interop` for every subscription that outlives a single emission. It removes the need for `OnDestroy` boilerplate entirely.

> **Legacy pattern**: older code (concentrated in `apps/dotcms-ui`) still uses a `destroy$` subject with `takeUntil(this.destroy$)` and `ngOnDestroy()`. That code is NOT to be mass-migrated — leave it as you find it. All **new** code uses `takeUntilDestroyed()`.

### Import Order Convention
```typescript
// 1. Angular Core
import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';

// 2. RxJS
import { EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';

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
  templateUrl: './my-component.component.html',
  styleUrls: ['./my-component.component.scss']
})
export class MyComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly service = inject(MyService);

  // Input signals ($ prefix)
  readonly $data = input<string>();
  readonly $condition = input<boolean>();

  // Output signals ($ prefix)
  readonly $change = output<string>();

  // State signals ($ prefix) — loading, error and data are all explicit
  protected readonly $loading = signal(false);
  protected readonly $error = signal<string | null>(null);
  protected readonly $items = signal<Item[]>([]);

  // Computed signals ($ prefix)
  readonly $isValid = computed(() => this.$condition() && this.$data());

  loadItems(): void {
    this.$loading.set(true);
    this.$error.set(null);

    this.service
      .getItems()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.$items.set(items);
          this.$loading.set(false);
        },
        error: (err) => {
          this.$error.set('Failed to load items');
          this.$loading.set(false);
          console.error('Error loading items:', err);
        }
      });
  }
}
```

Template (`my-component.component.html`) — use the `$` prefix when reading signals, and cover every state:
```html
@if ($loading()) {
  <dot-spinner data-testid="loading-indicator" />
} @else if ($error()) {
  <div class="dot-my-component__error" data-testid="error-message">
    <span class="material-symbols-outlined">error</span>
    {{ $error() }}
  </div>
} @else {
  @if ($isValid()) {
    <div data-testid="valid-data">{{ $data() }}</div>
  }

  @for (item of $items(); track item.id) {
    <div [data-testid]="'item-' + item.id">{{ item.name }}</div>
  } @empty {
    <dot-empty-state data-testid="empty-state" />
  }
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
expect(icon).toHaveClass('material-symbols-outlined', 'text-2xl');

// ✅ Material Symbols: the icon name is the element's text content, not a class
expect(icon).toHaveText('update');

// ✅ Test user interactions, not implementation details
spectator.click(byTestId('refresh-button'));
expect(spectator.query(byTestId('success-message'))).toBeVisible();

// ✅ Cover every state the component renders
expect(spectator.query(byTestId('loading-indicator'))).toBeVisible();
expect(spectator.query(byTestId('empty-state'))).toBeVisible();
expect(spectator.query(byTestId('error-message'))).toBeVisible();
```

## Services
- Design services around a single responsibility
- Use `providedIn: 'root'` for singleton services
- Use the `inject()` function instead of constructor injection
- Handle errors with `catchError` — rethrow a typed error or return a safe fallback, never swallow it

## Resources
- [Angular style guide](https://angular.dev/style-guide)
- [Components](https://angular.dev/essentials/components)
- [Signals](https://angular.dev/essentials/signals)
- [Templates](https://angular.dev/essentials/templates)
- [Dependency injection](https://angular.dev/essentials/dependency-injection)

## Build Commands
Nx is not installed globally — always run it through pnpm.

```bash
# Development server
pnpm nx run dotcms-ui:serve     # → http://localhost:4200/dotAdmin

# Testing
pnpm nx run dotcms-ui:test      # Run tests

# Build
pnpm nx build dotcms-ui         # Production build

# Dependencies
pnpm install                    # NOT npm install
```

## Critical Requirements
> **Security**: All frontend code must follow [Security Principles](../core/SECURITY_PRINCIPLES.md)
> **Progressive Enhancement**: When editing existing code, see [Progressive Enhancement](../core/PROGRESSIVE_ENHANCEMENT.md)
- **Reuse first**: Check `libs/ui` (`@dotcms/ui`), then PrimeNG, before creating a component
- **All states**: loading, empty, error and loaded must be handled — no blank render paths
- **Error handling**: No silent failures; errors are surfaced to the user
- **Icons**: Material Symbols for new markup; PrimeIcons is legacy-only
- **data-testid**: Required for all testable elements
- **setInput()**: Never set component inputs directly
- **Spectator**: Required testing framework (`@openng/spectator`)
- **Signals**: Required for new component state
- **Standalone**: All new components must be standalone
- **Three files**: `.ts` + `.html` + `.scss` per component

## See also
- [COMPONENT_ARCHITECTURE.md](./COMPONENT_ARCHITECTURE.md) — Structure, file layout, data flow
- [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) — NgRx Signal Store for feature state
- [STYLING_STANDARDS.md](./STYLING_STANDARDS.md) — Tailwind CSS, PrimeNG theme, BEM, SCSS
- [TESTING_FRONTEND.md](./TESTING_FRONTEND.md) — Spectator, byTestId, setInput
- [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md) — Strict types, as const, # private

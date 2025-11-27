---
description: Frontend development instructions
applyTo: "core-web/**/*.{ts,html,scss,css}"
---

# Angular Frontend Context

This project adheres to modern Angular best practices, emphasizing maintainability, performance, accessibility, and scalability.

## TypeScript Best Practices

* **Strict Type Checking:** Always enable and adhere to strict type checking. This helps catch errors early and improves code quality.
* **Prefer Type Inference:** Allow TypeScript to infer types when they are obvious from the context. This reduces verbosity while maintaining type safety.
    * **Bad:**
        ```typescript
        let name: string = 'Angular';
        ```
    * **Good:**
        ```typescript
        let name = 'Angular';
        ```
* **Avoid `any`:** Do not use the `any` type unless absolutely necessary as it bypasses type checking. Prefer `unknown` when a type is uncertain and you need to handle it safely.

## Angular Best Practices

* **Standalone Components:** Always use standalone components, directives, and pipes. Avoid using `NgModules` for new features or refactoring existing ones.
* **Implicit Standalone:** When creating standalone components, you do not need to explicitly set `standalone: true` inside the `@Component`, `@Directive` and `@Pipe` decorators, as it is implied by default.
    * **Bad:**
        ```typescript
        @Component({
          standalone: true,
          // ...
        })
        export class MyComponent {}
        ```
    * **Good:**
        ```typescript
        @Component({
          // `standalone: true` is implied
          // ...
        })
        export class MyComponent {}
        ```
* **Signals for State Management:** Utilize Angular Signals for reactive state management within components and services.
* **Lazy Loading:** Implement lazy loading for feature routes to improve initial load times of your application.
* **NgOptimizedImage:** Use `NgOptimizedImage` for all static images to automatically optimize image loading and performance.
* **Host bindings:** Do NOT use the `@HostBinding` and `@HostListener` decorators. Put host bindings inside the `host` object of the `@Component` or `@Directive` decorator instead.

## Components

* **Single Responsibility:** Keep components small, focused, and responsible for a single piece of functionality.
* **`input()` and `output()` Functions:** Prefer `input()` and `output()` functions over the `@Input()` and `@Output()` decorators for defining component inputs and outputs.
    * **Old Decorator Syntax:**
        ```typescript
        @Input() userId!: string;
        @Output() userSelected = new EventEmitter<string>();
        ```
    * **New Function Syntax:**
        ```typescript
        import { input, output } from '@angular/core';

        // ...
        userId = input<string>('');
        userSelected = output<string>();
        ```
* **`computed()` for Derived State:** Use the `computed()` function from `@angular/core` for derived state based on signals.
* **`ChangeDetectionStrategy.OnPush`:** Always set `changeDetection: ChangeDetectionStrategy.OnPush` in the `@Component` decorator for performance benefits by reducing unnecessary change detection cycles.
* **Inline Templates:** Prefer inline templates (template: `...`) for small components to keep related code together. For larger templates, use external HTML files.
* **Reactive Forms:** Prefer Reactive forms over Template-driven forms for complex forms, validation, and dynamic controls due to their explicit, immutable, and synchronous nature.
* **No `ngClass` / `NgClass`:** Do not use the `ngClass` directive. Instead, use native `class` bindings for conditional styling.
    * **Bad:**
        ```html
        <section [ngClass]="{'active': isActive}"></section>
        ```
    * **Good:**
        ```html
        <section [class.active]="isActive"></section>
        <section [class]="{'active': isActive}"></section>
        <section [class]="myClasses"></section>
        ```
* **No `ngStyle` / `NgStyle`:** Do not use the `ngStyle` directive. Instead, use native `style` bindings for conditional inline styles.
    * **Bad:**
        ```html
        <section [ngStyle]="{'font-size': fontSize + 'px'}"></section>
        ```
    * **Good:**
        ```html
        <section [style.font-size.px]="fontSize"></section>
        <section [style]="myStyles"></section>
        ```
* **File Structure:** Follow the file structure below for components.
    * component-name/
        * component-name.component.ts      # Logic
        * component-name.component.html    # Template  
        * component-name.component.scss    # Styles
        * component-name.component.spec.ts # Tests

## State Management

* **Signals for Local State:** Use signals for managing local component state.
* **`computed()` for Derived State:** Leverage `computed()` for any state that can be derived from other signals.
* **Pure and Predictable Transformations:** Ensure state transformations are pure functions (no side effects) and predictable.
* **Signal value updates:** Do NOT use `mutate` on signals, use `update` or `set` instead.

## Templates

* **Simple Templates:** Keep templates as simple as possible, avoiding complex logic directly in the template. Delegate complex logic to the component's TypeScript code.
* **Native Control Flow:** Use the new built-in control flow syntax (`@if`, `@for`, `@switch`) instead of the older structural directives (`*ngIf`, `*ngFor`, `*ngSwitch`).
    * **Old Syntax:**
        ```html
        <section *ngIf="isVisible">Content</section>
        <section *ngFor="let item of items">{{ item }}</section>
        ```
    * **New Syntax:**
        ```html
        @if (isVisible) {
          <section>Content</section>
        }
        @for (item of items; track item.id) {
          <section>{{ item }}</section>
        }
        ```
* **Async Pipe:** Use the `async` pipe to handle observables in templates. This automatically subscribes and unsubscribes, preventing memory leaks.

## Services

* **Single Responsibility:** Design services around a single, well-defined responsibility.
* **`providedIn: 'root'`:** Use the `providedIn: 'root'` option when declaring injectable services to ensure they are singletons and tree-shakable.
* **`inject()` Function:** Prefer the `inject()` function over constructor injection when injecting dependencies, especially within `provide` functions, `computed` properties, or outside of constructor context.
    * **Old Constructor Injection:**
        ```typescript
        constructor(private myService: MyService) {}
        ```
    * **New `inject()` Function:**
        ```typescript
        import { inject } from '@angular/core';

        export class MyComponent {
          private myService = inject(MyService);
          // ...
        }
        ```

### Testing Patterns (CRITICAL)

Always use Spectator with jest or Vitest for testing using @ngneat/spectator/jest package.

```typescript
import { createComponentFactory, Spectator, byTestId, mockProvider } from '@ngneat/spectator/jest';

// Spectator setup
const createComponent = createComponentFactory({
  component: MyComponent,
  imports: [CommonModule, DotTestingModule],
  providers: [mockProvider(MyService)]
});

// Element selection (ALWAYS use data-testid)
const button = spectator.query(byTestId('submit-button'));

// Component inputs (CRITICAL - NEVER set directly)
spectator.setInput('inputProperty', 'value');
// NEVER: spectator.component.inputProperty = 'value';

// CSS class verification
expect(icon).toHaveClass('pi', 'pi-update');  // Separate strings
// NEVER: expect(icon).toHaveClass({ pi: true });

// User interactions
spectator.click(byTestId('refresh-button'));
spectator.typeInElement('test', byTestId('name-input'));
```

### SCSS Standards (CRITICAL)
```scss
// ALWAYS import variables first
@use "variables" as *;

// Use global variables, NEVER hardcoded values  
.component {
  padding: $spacing-3;           // NOT: 16px
  color: $color-palette-primary; // NOT: #blue
  background: $color-palette-gray-100;
  box-shadow: $shadow-m;
}

// BEM with flat structure (NO nesting)
.feature-list { }
.feature-list__header { }  
.feature-list__item { }
.feature-list__item--active { }
```

## Build Commands
```bash
# Development server
cd core-web && nx run dotcms-ui:serve  # → http://localhost:4200/dotAdmin

# Testing
cd core-web && nx run dotcms-ui:test

# Dependencies
cd core-web && yarn install  # NOT npm install
```

## Tech Stack
- **Angular**: 20.3.9 standalone components
- **UI**: PrimeNG 17.18.11, PrimeFlex 3.3.1
- **State**: NgRx Signals, Component Store  
- **Build**: Nx 20.5.1
- **Testing**: Jest + Spectator (REQUIRED)

## On-Demand Documentation
**Load only when needed to preserve context:**

- **Complete Angular standards**: `@docs/frontend/ANGULAR_STANDARDS.md`
- **Comprehensive testing guide**: `@docs/frontend/TESTING_FRONTEND.md`
- **Component architecture**: `@docs/frontend/COMPONENT_ARCHITECTURE.md`  
- **Styling standards**: `@docs/frontend/STYLING_STANDARDS.md`
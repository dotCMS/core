---
description: Angular frontend development context - loads only for Angular files  
globs: ["core-web/**/*.ts", "core-web/**/*.html", "core-web/**/*.scss"]
alwaysApply: false
---

# Angular Frontend Context

## Immediate Patterns (Copy-Paste Ready)

### Modern Template Syntax (REQUIRED)
```html
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
  @case ('error') { <dot-error /> }
  @default { <dot-content /> }
}
```

### Component Structure (REQUIRED)
```typescript
@Component({
  selector: 'dot-my-component',
  standalone: true,                    // REQUIRED
  imports: [CommonModule],
  templateUrl: './my-component.html',
  styleUrls: ['./my-component.scss'],  // Note: plural
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyComponent {
  // Input/Output signals (REQUIRED)
  data = input<string>();              // NOT @Input()
  config = input<Config>();
  change = output<string>();           // NOT @Output()
  
  // State signals
  loading = signal(false);
  
  // Computed signals  
  isValid = computed(() => this.data() && this.loading());
  
  // Dependency injection
  private service = inject(MyService);
}
```

### Testing Patterns (CRITICAL)
```typescript
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

### File Structure (REQUIRED)
```
component-name/
├── component-name.component.ts      # Logic
├── component-name.component.html    # Template  
├── component-name.component.scss    # Styles
└── component-name.component.spec.ts # Tests
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
- **Angular**: 18.2.3 standalone components
- **UI**: PrimeNG 17.18.11, PrimeFlex 3.3.1
- **State**: NgRx Signals, Component Store  
- **Build**: Nx 19.6.5
- **Testing**: Jest + Spectator (REQUIRED)

## On-Demand Documentation
**Load only when needed to preserve context:**

- **Complete Angular standards**: `@docs/frontend/ANGULAR_STANDARDS.md`
- **Comprehensive testing guide**: `@docs/frontend/TESTING_FRONTEND.md`
- **Component architecture**: `@docs/frontend/COMPONENT_ARCHITECTURE.md`  
- **Styling standards**: `@docs/frontend/STYLING_STANDARDS.md`
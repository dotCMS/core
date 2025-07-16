# Angular Development Standards

## Stack Configuration
- **Angular**: 18.2.3 with standalone components
- **Build**: Nx workspace
- **Testing**: Spectator (required)
- **State**: Signals (required for new components)

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
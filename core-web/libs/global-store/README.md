# Global Store

A lightweight, reactive state management library for Angular applications using NgRx Signals. This library provides a centralized global store for managing application-wide state with a simple and intuitive API.

## Features

- 🚀 **Signal-based**: Built on Angular's signals for optimal performance
- 🎯 **Type-safe**: Full TypeScript support with strict typing
- 🔄 **Reactive**: Automatic reactivity with computed values
- 🏗️ **Modular**: Easy to extend with additional state slices
- 🧪 **Testable**: Designed with testing in mind

## Installation

This library is part of the dotCMS workspace and is automatically available to other projects in the workspace.

## Usage

### Basic Setup

The `GlobalStore` is automatically provided at the root level and can be injected into any component or service:

```typescript
import { Component, inject } from '@angular/core';
import { GlobalStore } from '@dotcms/store';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  template: `
    @if (store.user()) {
      <div>
        <h2>Welcome, {{ store.user()?.name }}!</h2>
        <p>Email: {{ store.user()?.email }}</p>
      </div>
    } @else {
      <p>Please log in</p>
    }

    <p>Login status: {{ store.isLoggedIn() ? 'Logged in' : 'Not logged in' }}</p>
  `
})
export class UserProfileComponent {
  protected readonly store = inject(GlobalStore);
}
```

### State Management

#### Accessing State

```typescript
// Access user data
const user = this.store.user();

// Check login status
const isLoggedIn = this.store.isLoggedIn();
```

#### Updating State

```typescript
// Login a user
this.store.login({
  name: 'John Doe',
  email: 'john@example.com'
});
```

### State Interface

```typescript
export interface GlobalState {
  user: { name: string; email: string } | null;
}
```

### Available Methods

| Method | Description | Parameters |
|--------|-------------|------------|
| `login(user)` | Sets the current user in the store | `user: { name: string; email: string }` |

### Available Computed Values

| Property | Type | Description |
|----------|------|-------------|
| `isLoggedIn` | `Signal<boolean>` | Returns `true` if a user is logged in, `false` otherwise |

## Development

### Running Tests

```bash
# Run unit tests
nx test global-store

# Run tests in watch mode
nx test global-store --watch

# Run tests with coverage
nx test global-store --coverage
```

### Linting

```bash
# Run ESLint
nx lint global-store
```

### Project Structure

```
libs/global-store/
├── src/
│   ├── lib/
│   │   └── store.ts          # Main store implementation
│   ├── index.ts              # Public API exports
│   └── test-setup.ts         # Test configuration
├── jest.config.ts            # Jest configuration
├── project.json              # Nx project configuration
└── README.md                 # This file
```

## Architecture

This library uses NgRx Signals for state management, providing:

- **Signal Store**: A reactive store that automatically tracks dependencies
- **State Management**: Centralized state with immutable updates
- **Computed Values**: Derived state that updates automatically
- **Methods**: Actions that can modify the store state

## Contributing

When contributing to this library:

1. Follow the established patterns for state management
2. Add comprehensive tests for new features
3. Update this README for any API changes
4. Ensure all code follows the project's TypeScript and Angular guidelines

## Related Libraries

- [@ngrx/signals](https://ngrx.io/guide/signals) - NgRx Signals library
- [@angular/core](https://angular.io/api/core) - Angular core library

## License

This library is part of the dotCMS project and follows the same licensing terms.

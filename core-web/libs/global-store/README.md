# Global Store

A lightweight, reactive state management library for Angular applications using NgRx Signals. This library provides a centralized global store for managing application-wide state with a simple and intuitive API.

## Features

- 🚀 **Signal-based**: Built on Angular's signals for optimal performance
- 🎯 **Type-safe**: Full TypeScript support with strict typing
- 🔄 **Reactive**: Automatic reactivity with computed values
- 🏗️ **Modular**: Easy to extend with additional state slices
- 🧪 **Testable**: Designed with testing in mind

## Navigation menu (`withMenu`)

`withMenu` is the **only** in-memory copy of `GET /api/v1/menu`. `DotMenuService` (in `@dotcms/data-access`) is a
stateless client: one request per subscription, no cache. Nothing else keeps the menu.

**Two kinds of state.** The *server snapshot* (the menu item entities and `status`) is replaced as a whole by every
load. The *UI state* (`isNavigationCollapsed`, persisted at `dotcms.menu.status`; `openParentMenuId`; `activeKey`) is
not deleted when the snapshot is replaced.

| Member | Behavior |
|--------|----------|
| `status()` | `'idle' \| 'loading' \| 'ready' \| 'error'` |
| `ensureLoaded()` | `Observable<void>`. `ready`: completes at once, **no request**. `loading`: waits for the request in flight. `idle`/`error`: starts one. Settles on `ready` **or** `error`; never errors. Guards and dialogs call this first, then read the snapshot. |
| `reload()` | Always sends a request and reports `loading` at once; the latest request wins. Called by `DotNavigationService` for `UPDATE_PORTLET_LAYOUTS`, login and login-as. |
| `whenSettled()` | Emits `ready` or `error` when the load that is running ends. |
| `hasPortlet(id, { jspOnly })` | Is the portlet in the menu (any layout)? `jspOnly` ignores Angular portlets. |
| `layoutIdForPortlet(id)` | Full id of the layout that holds the portlet, or `null`. |
| `loadFreshPortletUrl(id)` | The url of a legacy (JSP) portlet from a request made now, because it carries `r=<currentTimeMillis>`. Never touches the snapshot or the highlight; falls back to the snapshot url if the request fails. |
| `menuGroup()` | Groups ordered by their position in the response (the backend sorts by `tabOrder`), items by their position in the group. |
| `activeKey()` / `activeMenuItem()` | The highlighted item. It is a key, not a flag on the entity, so a replace does not erase it; it is kept when the item still exists and cleared when it is gone. After a browser reload it starts `null` and is filled from the URL once the menu loads. |

An item is identified by `` `${portletId}__${layoutId}` `` with the **full** layout id (`getMenuItemKey`). The 4-character
`mId` of the URL is kept as `shortLayoutId` for lookups only.

**One URL resolver.** `resolveMenuItem` decides which item a URL belongs to: (1) the portlet id inside the layout the
`mId` names (on a short-id tie, the open group, then the first in order); (2) with no query at all, the label of the first
breadcrumb that has a url, then the portlet id; (3) exact `menuLink` equality, never a prefix. The highlight
(`withMenuSelection`, the single listener of `NavigationEnd`), the breadcrumbs and `getPageTitleByUrl` all call it.

**Runtime routes.** Items that declare `initParams['angular-module']` register an Angular route. The app binds
`DOT_MENU_ROUTES_REGISTRAR`; the feature calls it with every item after each successful load, **before** it reports
`ready`, so a guard that waits on `ensureLoaded()` never decides before the routes exist.

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

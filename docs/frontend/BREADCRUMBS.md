# Breadcrumbs

Breadcrumb navigation in the dotCMS admin UI is managed by the **GlobalStore** breadcrumb feature. It is the single source of truth for the crumb trail component and integrates with the router and session storage. Follow [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) for store usage and [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) for component patterns.

## Location

- **Feature**: `libs/global-store/src/lib/features/breadcrumb/`  
- **Store**: `GlobalStore` (injected in app shell) uses the `withBreadcrumbs(menuItems)` signal store feature.  
- **UI**: Breadcrumbs are rendered by the crumb trail component that reads `globalStore.breadcrumbs()`.

## Accessing the store

Inject `GlobalStore` where you need to update or read breadcrumbs (e.g. resolvers, portlet components).

```typescript
import { inject } from '@angular/core';
import { GlobalStore } from '@dotcms/store';

export class MyComponent {
  readonly #globalStore = inject(GlobalStore);

  ngOnInit(): void {
    this.#globalStore.addNewBreadcrumb({
      id: 'my-tab',
      label: 'My Tab',
      url: '/dotAdmin/#/my-feature/tab',
      target: '_self'
    });
  }
}
```

## Public API

### Methods

| Method | Purpose |
|--------|--------|
| `setBreadcrumbs(items)` | Replace the trail (after "Home"). **Home is always prepended automatically.** |
| `appendCrumb(item)` | Add one item at the end. No duplicate checks. |
| `addNewBreadcrumb(item)` | Append or replace last; **avoids duplicates** by `url` or `id`. Prefer this for user-driven navigation. |
| `setLastBreadcrumb(item)` | Replace the last item (e.g. same route, different content). |
| `truncateBreadcrumbs(index)` | Keep breadcrumbs from 0 to `index` (0-based). Used internally when navigating back to an existing URL. |
| `loadBreadcrumbs()` | Restore trail from `sessionStorage` (used on init). |
| `clearBreadcrumbs()` | Clear the trail. |

### Signals (read-only)

| Signal | Description |
|--------|-------------|
| `breadcrumbs()` | Full array of items (PrimeNG `MenuItem[]`). |
| `breadcrumbCount()` | Number of items. |
| `hasBreadcrumbs()` | `true` if the trail is not empty. |
| `lastBreadcrumb()` | Last item or `null`. |
| `selectLastBreadcrumbLabel()` | Label of the last item or `null`. |

## Item shape (PrimeNG MenuItem)

Each breadcrumb is a PrimeNG [MenuItem](https://primeng.org/api/menuitem):

- **`label`** (string): Display text.  
- **`url`** (string, optional): Link for the crumb. Use full admin URL: `/dotAdmin/#/path` or `/dotAdmin/#/path?query=value`.  
- **`id`** (string, optional): Stable id for duplicate detection (e.g. tab or entity id).  
- **`target`** (string, optional): e.g. `'_self'` for same window.  
- **`disabled`** (boolean, optional): Non-clickable (e.g. "Home", parent labels).

## When to use which method

- **Menu / router built the base trail**: The store listens to `NavigationEnd` and builds breadcrumbs from the menu and special route handlers. You usually **do not** call `setBreadcrumbs` yourself for standard menu routes.  
- **Adding a sub-level (e.g. template edit, content edit, app config)**: Use **`addNewBreadcrumb`** so the same URL or same `id` does not duplicate on reload or re-entry.  
- **Tabs or label-only levels (e.g. Analytics: Engagement, Pageview, Conversions)**: Use **`addNewBreadcrumb`** with a stable **`id`** (and optional `url`) so reload does not append the same tab again.  
- **Replacing the last crumb** (e.g. switching content in the same route): Use **`setLastBreadcrumb`** or rely on `addNewBreadcrumb` (it replaces last for content-edit–style URLs when applicable).  
- **Truncating when navigating back**: Handled internally via `_processUrl` when the target URL is already in the trail. You can call **`truncateBreadcrumbs(index)`** only if you need to truncate programmatically (e.g. custom back behavior).

## Recommended patterns

### ✅ Add a sub-route or tab with duplicate prevention (prefer `id` or `url`)

```typescript
// With URL (resolver or component that knows the current URL)
this.#globalStore.addNewBreadcrumb({
  label: this.dotMessageService.get('templates.create.title'),
  target: '_self',
  url: `/dotAdmin/#/templates/create`
});

// With stable id (e.g. tab without changing URL path) — avoids duplicates on reload
this.#globalStore.addNewBreadcrumb({
  id: 'engagement',
  label: this.#messageService.get('analytics.dashboard.tabs.engagement')
});
```

### ✅ Replace last crumb (e.g. same route, different entity)

```typescript
this.#globalStore.addNewBreadcrumb({
  label: page.title,
  url: newURL,
  id: page.identifier
});
// If same content-edit style URL, addNewBreadcrumb replaces the last item automatically.
```

### ✅ Set breadcrumbs explicitly (e.g. custom flow)

```typescript
this.#globalStore.setBreadcrumbs([
  { label: 'Parent', disabled: true },
  { label: 'Current', url: '/dotAdmin/#/my/current', target: '_self' }
]);
```

### ❌ Do not add label-only crumbs without `id` for tabs

```typescript
// BAD: On reload, last crumb has no url/id, so addNewBreadcrumb appends again → duplicates
this.#globalStore.addNewBreadcrumb({
  label: this.#messageService.get('analytics.dashboard.tabs.engagement')
});

// GOOD: Same tab on reload is detected by id and not appended again
this.#globalStore.addNewBreadcrumb({
  id: 'engagement',
  label: this.#messageService.get('analytics.dashboard.tabs.engagement')
});
```

### ❌ Do not build full trail manually when the menu already does it

Let the store derive the base trail from the router and menu. Only call `setBreadcrumbs` for custom flows (e.g. wizards, non-menu portlets).

## Persistence and router

- **Session storage**: The store persists `breadcrumbs` to `sessionStorage` on every change and restores them on init via `loadBreadcrumbs()`.  
- **Router**: On `NavigationEnd`, the store runs `_processUrl(url)` to sync the trail (menu match, special routes, or truncate when navigating back to an existing URL).  
- **Special routes**: Handlers in `breadcrumb.utils.ts` (e.g. templates edit, content filter) can set or append breadcrumbs; see `ROUTE_HANDLERS` and `processSpecialRoute`.

## Summary

- Use **`addNewBreadcrumb`** for sub-levels and tabs; provide **`url`** or **`id`** to avoid duplicates on reload.  
- Use **`setBreadcrumbs`** only when you need to replace the whole trail (e.g. custom flows).  
- Use **`truncateBreadcrumbs(index)`** only when you need programmatic truncation; normal “back to existing URL” truncation is handled by the store.  
- Breadcrumb items are PrimeNG `MenuItem`; use `/dotAdmin/#/...` for admin URLs.  
- The crumb trail UI reads `globalStore.breadcrumbs()`; do not manage a separate local breadcrumb state in components.

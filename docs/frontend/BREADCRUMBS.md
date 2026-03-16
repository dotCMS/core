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
| `addNewBreadcrumb(item)` | Append a new crumb or replace the **last** one when its normalized `url` or `id` matches. Optimized for user-driven navigation (including content-edit URLs). |
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
- **Tabs or label-only levels (e.g. Analytics: Engagement, Pageview, Conversions)**: Use **`addNewBreadcrumb`** with a stable **`id`** prefixed by the feature namespace (e.g. `analytics-engagement`). The store automatically replaces the last tab crumb when both the incoming and last item share the same prefix pattern, so tab switches never accumulate.  
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

### ❌ Do not add label-only crumbs without `id` and `url` for tabs, at least one of them is used to validate and avoid duplication

```typescript
// BAD: On reload, last crumb has no url/id, so addNewBreadcrumb appends again → duplicates
this.#globalStore.addNewBreadcrumb({
  label: this.#messageService.get('analytics.dashboard.tabs.engagement')
});

// GOOD: Same tab on reload is detected by id and not appended again
this.#globalStore.addNewBreadcrumb({
  id: 'analytics-engagement',  // use a stable, feature-namespaced id
  label: this.#messageService.get('analytics.dashboard.tabs.engagement')
});
```

### ❌ Do not build full trail manually when the menu already does it

Let the store derive the base trail from the router and menu. Only call `setBreadcrumbs` for custom flows (e.g. wizards, non-menu portlets).

## How `addNewBreadcrumb` decides to replace vs. append

`addNewBreadcrumb` delegates the replace-vs-append decision to `shouldReplaceLastCrumb` in `breadcrumb.utils.ts`. That function checks a registry called `REPLACE_LAST_CRUMB_RULES` — a `Record<string, { test(item, last): boolean }>` — and returns `true` if any rule matches both the incoming item and the current last crumb.

**Current rules:**

| Key | Condition | Example |
|-----|-----------|----------|
| `contentEdit` | Both `url` (after stripping `#`) match `/\/content[/?].+/` | `/content/abc-123` → `/content/xyz-456` |
| `analyticsTab` | Both `id` match `/^analytics-/` | `analytics-engagement` → `analytics-conversions` |

**To add a new feature with "last-replaces" behavior**, add an entry to `REPLACE_LAST_CRUMB_RULES` in `breadcrumb.utils.ts`:

```typescript
myFeatureTab: {
    test: (item, last) => {
        const regex = /^my-feature-/;
        return regex.test(item.id ?? '') && regex.test(last.id ?? '');
    }
}
```

## Persistence and router

- **Session storage**: The store persists `breadcrumbs` to `sessionStorage` on every change and restores them on init via `loadBreadcrumbs()`.  
- **Router**: On `NavigationEnd`, the store runs `_processUrl(url)` to sync the trail (menu match, special routes, or truncate when navigating back to an existing URL).  
- **Special routes**: Handlers in `breadcrumb.utils.ts` (e.g. templates edit, content filter) can set or append breadcrumbs; see `ROUTE_HANDLERS` and `processSpecialRoute`.  
- **Reload with pending tab crumbs**: If a child component calls `addNewBreadcrumb` with a tab `id` before the menu finishes loading (common on hard reload), the store detects those crumbs (items with `id` but no `url`) and re-appends them after the menu effect resets the base trail. No extra work is needed in components — just always provide a stable `id`.

## Summary

- Use **`addNewBreadcrumb`** for sub-levels and tabs; provide **`url`** or **`id`** to avoid duplicates on reload.  - For tabs, use a **feature-namespaced `id`** (e.g. `analytics-engagement`) so `REPLACE_LAST_CRUMB_RULES` can swap them without accumulating.  
- To register a new tab group with replace behavior, add a rule to `REPLACE_LAST_CRUMB_RULES` in `breadcrumb.utils.ts`.  - Use **`setBreadcrumbs`** only when you need to replace the whole trail (e.g. custom flows).  
- Use **`truncateBreadcrumbs(index)`** only when you need programmatic truncation; normal “back to existing URL” truncation is handled by the store.  
- Breadcrumb items are PrimeNG `MenuItem`; use `/dotAdmin/#/...` for admin URLs.  
- The crumb trail UI reads `globalStore.breadcrumbs()`; do not manage a separate local breadcrumb state in components.

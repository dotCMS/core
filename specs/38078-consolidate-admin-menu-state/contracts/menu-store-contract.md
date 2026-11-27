# Contract: GlobalStore menu feature (internal UI contract)

**Spec**: [../spec.md](../spec.md) · **Data model**: [../data-model.md](../data-model.md)

The "interface" this work exposes is the public surface of `GlobalStore` (menu feature) and the thin HTTP
client. It is consumed by the shell, guards, the iframe host and `libs/edit-content`. Signatures describe
behavior; exact types are fixed in implementation.

## 1. `DotMenuService` (`@dotcms/data-access`, `providedIn: 'root'`)

| Member | Behavior |
|--------|----------|
| `getMenus(): Observable<DotMenu[]>` | One `GET /api/v1/menu` per subscription; returns `entity`; **holds no state**, no `menu$`, no `force`. Errors propagate to the caller. |

Removed: `menu$`, `loadMenu(force)`, `reloadMenu()`, `isPortletInMenu`, `getUrlById`, `getDotMenuId`.

## 2. `GlobalStore` — menu members

### State signals
`status`, `isNavigationCollapsed`, `openParentMenuId`, `activeKey` (see data model).

### Computed signals
`menuGroup`, `activeMenuItem`, `firstMenuItem`, `isGroupActive`, `entityMap`.

### Queries (read the snapshot; no I/O)
| Member | Returns | Used by |
|--------|---------|---------|
| `hasPortlet(id, { jspOnly? })` | `boolean` | `MenuGuardService`, My Account, locales selector |
| `layoutIdForPortlet(id)` | `string \| null` | workflow task dialog (`p_l_id`) |
| `getPageTitleByUrl(url)` | `string` | `DotNavigationService` (title) — uses `resolveMenuItem` |

Queries are only meaningful once `status` is `ready`; callers that can run earlier call `ensureLoaded()` first.

### Commands
| Member | Contract |
|--------|----------|
| `ensureLoaded(): Observable<void>` | `ready` → completes, **no request**. `loading` → waits for the in-flight load. `idle`/`error` → starts a load, waits. Settles on `ready` **or** `error`; never hangs; never errors. |
| `reload(): void` | Always starts a request; sets `status: 'loading'` synchronously; latest request wins. |
| `whenSettled(): Observable<MenuStatus>` | Emits the first `ready`/`error` status after the call and completes. |
| `loadFreshPortletUrl(id): Observable<string>` | Requests the menu at call time; returns the first non-Angular item's `url` (else `''`). **Never patches state.** Falls back to the snapshot `url` if the request fails. |
| `toggleParent(id)`, `closeAllParents()`, `toggleNavigation()`, `collapseNavigation()`, `expandNavigation()` | Unchanged behavior (the collapse effect on the open group is preserved as-is). |
| `activateMenuItem(key)` / `activateMenuItemWithParent(key, parentId)` | Set `activeKey` (and open group); no entity writes. |

Removed: `loadMenu(menus)` (push-in) and the `SetActiveMenuParams` / `setActiveMenu` multi-strategy method — its
job is done by the resolver and the single reaction in §3.

### Success / failure semantics of a load
- **Success**: replace entities; keep `activeKey` if the key still exists, else `null` and re-resolve from the
  current URL; call the routes registrar (§4); then `status = 'ready'`.
- **Failure**: `status = 'error'`; entities that were showing stay; nothing throws.

## 3. Single reaction to navigation

One store-level listener on `NavigationEnd` (plus a re-resolve after each successful replace when `activeKey`
is `null`/stale) calls `resolveMenuItem(...)` and sets `activeKey` and the open group. The breadcrumb feature
and `getPageTitleByUrl` call the same pure function. `DotNavigationService` has **no** highlight logic.

## 4. Routes registrar token

`DOT_MENU_ROUTES_REGISTRAR` — optional `InjectionToken<(items: DotMenuItem[]) => void>` exported by
`@dotcms/store`. The app binds it to `DynamicRouteService.registerRoutesFromMenuItems` in `providers.ts`.
Invoked with all items on every successful load, **before** `status` becomes `ready`. Idempotent by contract.

## 5. `DotNavigationService` (root, single instance)

| Responsibility | Contract |
|----------------|----------|
| Construction | Calls `ensureLoaded()` (no extra request if already loading/ready). No fetching, no highlight. |
| `UPDATE_PORTLET_LAYOUTS` (`portletLayoutUpdated$()`) | Exactly one `reload()` per event. |
| `auth$` with a user or `loginAsUser` | `reload()`, wait for settle, then `goToFirstPortlet()`. |
| `NavigationEnd` | Sets the document title from `getPageTitleByUrl`; **no** HTTP. |
| `goToFirstPortlet()` | Navigates to `firstMenuItem.menuLink` with `mId = shortLayoutId`; reloads the iframe if the route did not change. |

## 6. Backend contract consumed (read-only context, must keep honoring)

`GET /api/v1/menu` — per-user, `@NoCache`, backend user required, groups sorted by `tabOrder`, JSP URLs carry
`r=<currentTimeMillis>`. System event `UPDATE_PORTLET_LAYOUTS` is pushed by `setPortletIdsToLayout`. No change.

## 7. REST used only by the e2e helpers (existing endpoints, no change)

| Call | Use |
|------|-----|
| `GET /api/v1/layouts` | find a layout and its tools |
| `PUT /api/v1/layouts/{layoutId}/portlets` (CMS Administrator) | add/remove a tool → triggers `UPDATE_PORTLET_LAYOUTS` (E6) |

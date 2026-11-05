import { signalStore, signalStoreFeature, withFeature } from '@ngrx/signals';

import { withBreadcrumbs } from './features/breadcrumb/breadcrumb.feature';
import { withMenuSelection } from './features/menu/with-menu-selection.feature';
import { withMenu } from './features/menu/with-menu.feature';
import { withSite } from './features/with-site/with-site.feature';
import { withSystem } from './features/with-system/with-system.feature';
import { withUser } from './features/with-user/with-user.feature';
import { withWebSocket } from './features/with-websocket/with-websocket.feature';

/**
 * GlobalStore: Global application state using NgRx Signals.
 *
 * This store manages essential global state including user authentication,
 * current site information, and system configuration. It composes feature
 * stores (`withSystem`, `withWebSocket`, `withSite`, `withUser`, `withMenu`)
 * rather than holding state directly.
 *
 * Current features:
 * - `withSite` — current site (`siteDetails`, `currentSiteId`, switch/load/sync)
 * - `withWebSocket` — system-events WebSocket lifecycle + status
 * - `withSystem` — system configuration management
 * - `withUser` — current authenticated user
 * - `withMenu` / `withBreadcrumbs` / `withMenuSelection` — navigation menu state, trail and highlight
 *
 * Example usage:
 * ```typescript
 * // Inject the store in a component
 * private readonly globalStore = inject(GlobalStore);
 *
 * // Get current site ID
 * const siteId = this.globalStore.currentSiteId();
 *
 * // Use site ID in your services
 * if (siteId) {
 *   this.someService.doSomething(siteId);
 * }
 *
 * // Access complete site entity
 * const site = this.globalStore.siteDetails();
 * console.log(site?.name, site?.hostname);
 *
 */
export const GlobalStore = signalStore(
    { providedIn: 'root' },
    withSystem(),
    withWebSocket(),
    withSite(),
    withUser(),
    withMenu(),
    withFeature((menu) =>
        signalStoreFeature(
            withBreadcrumbs(menu.menuItemsEntities, menu.openParentMenuId),
            withMenuSelection(menu)
        )
    )
);

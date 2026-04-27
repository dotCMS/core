import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

/**
 * Cross-store signals consumed by the analytics dashboard.
 *
 * The dashboard's `withHooks` bridges `globalStore.currentSiteId()` (a signal
 * outside our store) into `siteChanged` events so the autoload handler can
 * react uniformly to filter changes and global site changes.
 */
export const externalEvents = eventGroup({
    source: 'Analytics External',
    events: {
        /** The active site changed in the global selector. */
        siteChanged: type<{ siteId: string }>()
    }
});

import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed } from '@angular/core';

import { ContentState } from './content.feature';

/**
 * Feature that handles the sidebar's state and persistence.
 *
 * @returns {SignalStoreFeature} The feature object.
 */
export function withSidebar() {
    return signalStoreFeature(
        { state: type<ContentState>() },
        withComputed(({ contentlet }) => ({
            getCurrentContentIdentifier: computed(() => contentlet()?.identifier)
        }))
    );
}

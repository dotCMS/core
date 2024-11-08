import { getState, signalStoreFeature, withHooks } from '@ngrx/signals';

import { effect } from '@angular/core';

/**
 * Feature that adds debugging functionality to the Store
 * @returns
 */
export function withDebug() {
    return signalStoreFeature(
        withHooks({
            onInit(store) {
                effect(() => {
                    // eslint-disable-next-line no-console
                    console.info('🔄 Store state:', getState(store));
                });
            }
        })
    );
}

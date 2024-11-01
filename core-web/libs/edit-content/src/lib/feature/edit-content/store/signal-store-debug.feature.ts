import { effect } from '@angular/core';
import { getState, signalStoreFeature, withHooks } from '@ngrx/signals';

/**
 * Feature that adds debugging functionality to the Store
 * @returns
 */
export function withDebug() {
    return signalStoreFeature(
        withHooks({
            onInit(store) {
                effect(() => {
                    console.info('🔄 Store state:', getState(store));
                });
            }
        })
    );
}

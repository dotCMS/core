import { signalStoreFeature, watchState, withHooks } from '@ngrx/signals';

/**
 * Feature that adds debugging functionality to the Store
 * @returns
 */
export function withDebug() {
    return signalStoreFeature(
        withHooks({
            onInit(store) {
                watchState(store, (state) => {
                    console.warn('🔄 Store state:', state);
                });
            }
        })
    );
}

import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';

import { EditContentState } from '../../edit-content.store';

/**
 * Feature that handles field visibility state, controlled by the BridgeAPI.
 * Uses a Record<string, boolean> of hidden field variables for O(1) lookups
 * while remaining JSON-serializable (DevTools, snapshots, hydration).
 */
export function withFieldVisibility() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withMethods((store) => ({
            /**
             * Sets the visibility of a field by adding or removing it from the hidden map.
             *
             * @param fieldVariable - The variable name of the field to show or hide.
             * @param visible - Whether the field should be visible (`true`) or hidden (`false`).
             */
            setFieldVisibility(fieldVariable: string, visible: boolean): void {
                const hiddenFields = store.hiddenFields();
                const isCurrentlyHidden = !!hiddenFields[fieldVariable];
                const shouldBeHidden = !visible;

                if (isCurrentlyHidden === shouldBeHidden) {
                    return;
                }

                const updated = { ...hiddenFields };
                if (shouldBeHidden) {
                    updated[fieldVariable] = true;
                } else {
                    delete updated[fieldVariable];
                }

                patchState(store, { hiddenFields: updated });
            }
        }))
    );
}

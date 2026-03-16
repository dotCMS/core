import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';

import { EditContentState } from '../../edit-content.store';

/**
 * Feature that handles field visibility state, controlled by the BridgeAPI.
 * Uses a Set<string> of hidden field variables for O(1) lookups in templates.
 */
export function withFieldVisibility() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withMethods((store) => ({
            /**
             * Sets the visibility of a field by adding or removing it from the hidden set.
             *
             * @param fieldVariable - The variable name of the field to show or hide.
             * @param visible - Whether the field should be visible (`true`) or hidden (`false`).
             */
            setFieldVisibility(fieldVariable: string, visible: boolean): void {
                const current = new Set(store.hiddenFields());
                if (visible) {
                    current.delete(fieldVariable);
                } else {
                    current.add(fieldVariable);
                }
                patchState(store, { hiddenFields: current });
            },

            /**
             * Checks whether a field is currently hidden.
             *
             * @param fieldVariable - The variable name of the field to check.
             * @returns `true` if the field is hidden, `false` otherwise.
             */
            isFieldHidden(fieldVariable: string): boolean {
                return store.hiddenFields().has(fieldVariable);
            },

            /**
             * Resets all field visibility, making every field visible again.
             */
            resetFieldVisibility(): void {
                patchState(store, { hiddenFields: new Set<string>() });
            }
        }))
    );
}

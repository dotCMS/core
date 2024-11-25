import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';

export interface FormState {
    formValues: Record<string, string>;
}

const initialState: FormState = {
    formValues: {}
};

/**
 * Feature that handles the form's state.
 *
 * @returns {SignalStoreFeature} The feature object.
 */
export function withForm() {
    return signalStoreFeature(
        withState(initialState),

        withMethods((store) => ({
            /**
             * Handles the form change event and stores the form values.
             *
             * @param {Record<string, string>} formValues
             * @memberof withForm
             */
            onFormChange: (formValues: Record<string, string>) => {
                patchState(store, { formValues });
            }
        }))
    );
}

import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';

import { FormValues } from '../../models/dot-edit-content-form.interface';

export interface FormState {
    formValues: FormValues;
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
             * @param {FormValues} formValues
             * @memberof withForm
             */
            onFormChange: (formValues: FormValues) => {
                patchState(store, { formValues });
            }
        }))
    );
}

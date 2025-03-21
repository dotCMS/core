import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';

import { FormValues } from '../../../models/dot-edit-content-form.interface';
import { EditContentState } from '../../edit-content.store';

export interface FormState {
    formValues: FormValues;
}

/**
 * Feature that handles the form's state.
 *
 * @returns {SignalStoreFeature} The feature object.
 */
export function withForm() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
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

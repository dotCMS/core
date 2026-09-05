import { inject, Provider } from '@angular/core';

import { DotAssetPickerStore } from './dot-asset-picker.store';

import {
    DOT_FIELD_FILTER_HOST,
    DotFieldFilterHost
} from '../../dot-filter-bar/chips/dot-field-filter/field-filter-host.token';

type AssetPickerStore = InstanceType<typeof DotAssetPickerStore>;

/**
 * The AssetPicker's {@link DotFieldFilterHost}.
 *
 * A pass-through onto the store, like its filter facade: the picker holds exactly the shape the
 * shared chips work in, so there is nothing to translate. What it does not have is Content Drive's
 * results table, which is why the raw field list crossing the seam is dropped here — see
 * `setUserSearchableFields`.
 *
 * @param store The picker's store, provided per dialog instance.
 * @return The field-filter host backed by that store.
 */
export function createAssetPickerFieldFilterHost(store: AssetPickerStore): DotFieldFilterHost {
    return {
        $activeFields: store.userSearchableActive,
        $fields: store.userSearchableFields,
        addField: (variable: string): void => store.addUserSearchableField(variable),
        setFields: (fields): void => store.setUserSearchableFields(fields),
        clearFields: (): void => store.clearUserSearchableFilters()
    };
}

/**
 * Provides {@link DOT_FIELD_FILTER_HOST} over the picker's store.
 *
 * Add it to the **component** that provides `DotAssetPickerStore`, never to `root`: each open
 * picker owns its own store, and a root-provided host would reach across dialogs.
 */
export function provideAssetPickerFieldFilterHost(): Provider {
    return {
        provide: DOT_FIELD_FILTER_HOST,
        useFactory: () => createAssetPickerFieldFilterHost(inject(DotAssetPickerStore))
    };
}

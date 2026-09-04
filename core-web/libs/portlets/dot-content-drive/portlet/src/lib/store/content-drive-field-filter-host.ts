import { inject, Provider } from '@angular/core';

import { DOT_FIELD_FILTER_HOST, DotFieldFilterHost } from '@dotcms/ui';

import { DotContentDriveStore } from './dot-content-drive.store';

type ContentDriveStore = InstanceType<typeof DotContentDriveStore>;

/**
 * Content Drive's {@link DotFieldFilterHost}.
 *
 * A pass-through onto the store methods the field filters already used before they were shared, so
 * nothing about the drive's behaviour changes: the same chips, the same clear-on-content-type-change,
 * and the same `showInListFields` feeding the results table's extra columns (FR-021).
 *
 * The `listed` split is applied here rather than in the shared menu on purpose — "Show In List" is
 * a table concern this portlet owns, and the AssetPicker has no such table.
 *
 * @param store The Content Drive store.
 * @return The field-filter host backed by that store.
 */
export function createContentDriveFieldFilterHost(store: ContentDriveStore): DotFieldFilterHost {
    return {
        $activeFields: store.userSearchableActive,
        $fields: store.userSearchableFields,
        addField: (variable: string): void => store.addUserSearchableField(variable),
        setFields: ({ eligible, all }): void => {
            store.setUserSearchableFields(eligible);
            store.setShowInListFields(all.filter((field) => field.listed));
        },
        clearFields: (): void => store.clearUserSearchableFilters()
    };
}

/**
 * Provides {@link DOT_FIELD_FILTER_HOST} over the Content Drive store.
 *
 * Goes on the component that provides `DotContentDriveStore` — the portlet shell — beside
 * `provideContentDriveFilterFacade`, so the toolbar's "More" menu and its chips resolve the same
 * instance.
 */
export function provideContentDriveFieldFilterHost(): Provider {
    return {
        provide: DOT_FIELD_FILTER_HOST,
        useFactory: () => createContentDriveFieldFilterHost(inject(DotContentDriveStore))
    };
}

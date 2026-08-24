import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { DotContentTypeFilterComponent, DotContentTypeFilterSelection } from '@dotcms/ui';

import { MAP_BASE_TYPES_TO_NUMBERS, MAP_NUMBERS_TO_BASE_TYPES } from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/**
 * Store adapter over the shared {@link DotContentTypeFilterComponent}.
 *
 * Its only job is translating between the two representations: the store persists base types as
 * the numeric keys the drive API and the URL use, while the shared filter speaks base-type names.
 */
@Component({
    selector: 'dot-content-drive-content-type-filter',
    template: `
        <dot-content-type-filter
            [selectedBaseTypes]="$selectedBaseTypes()"
            [selectedContentTypes]="$selectedContentTypes()"
            (selectionChange)="onSelectionChange($event)" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotContentTypeFilterComponent]
})
export class DotContentDriveContentTypeFilterComponent {
    readonly #store = inject(DotContentDriveStore);

    protected readonly $selectedBaseTypes = computed(() => {
        const keys = (this.#store.getFilterValue('baseType') as string[]) ?? [];

        return keys
            .map((key) => MAP_NUMBERS_TO_BASE_TYPES[Number(key)])
            .filter((baseType): baseType is DotCMSBaseTypesContentTypes => !!baseType);
    });

    protected readonly $selectedContentTypes = computed(
        () => (this.#store.getFilterValue('contentType') as string[]) ?? []
    );

    protected onSelectionChange({ baseTypes, contentTypes }: DotContentTypeFilterSelection): void {
        if (baseTypes.length) {
            const keys = baseTypes
                .map((name) => MAP_BASE_TYPES_TO_NUMBERS[name as DotCMSBaseTypesContentTypes])
                .filter((key): key is string => !!key);
            this.#store.patchFilters({ baseType: keys });
        } else {
            this.#store.removeFilter('baseType');
        }

        if (contentTypes.length) {
            this.#store.patchFilters({ contentType: contentTypes });
        } else {
            this.#store.removeFilter('contentType');
        }
    }
}

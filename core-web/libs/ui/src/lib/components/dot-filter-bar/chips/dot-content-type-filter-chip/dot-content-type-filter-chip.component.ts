import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import {
    DotContentTypeFilterComponent,
    DotContentTypeFilterSelection
} from '../../../dot-content-type-filter/dot-content-type-filter.component';
import { DOT_FILTER_FACADE, toFilterValues } from '../../filter-facade.token';

/**
 * Connects the presentational {@link DotContentTypeFilterComponent} to whatever surface is
 * rendering it.
 *
 * This is the adapter tier that used to be written once *per consumer* — `Content Drive` had its
 * own, the AssetPicker hand-rolled the binding in its toolbar — which is precisely the tax that let
 * the picker fall behind on every chip Content Drive gained. Written once here, it costs nothing
 * per surface.
 *
 * The presentational filter's own API is untouched: it still takes selections in and reports
 * changes out, and knows nothing about a facade.
 */
@Component({
    selector: 'dot-content-type-filter-chip',
    imports: [DotContentTypeFilterComponent],
    template: `
        <dot-content-type-filter
            [selectedBaseTypes]="$baseTypes()"
            [selectedContentTypes]="$contentTypes()"
            [allowedBaseTypes]="$allowedBaseTypes()"
            (selectionChange)="onSelectionChange($event)" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { 'data-filter-chip': 'contentType' }
})
export class DotContentTypeFilterChipComponent {
    readonly #filters = inject(DOT_FILTER_FACADE);

    /**
     * What the selector may ever OFFER, when the surface's caller restricts it. `null` means no
     * restriction.
     *
     * An input rather than something read through the facade, because it is a caller restriction
     * and not a filter: the editor cannot change it, and it must never appear as a clearable chip.
     */
    readonly $allowedBaseTypes = input<DotCMSBaseTypesContentTypes[] | null>(null, {
        alias: 'allowedBaseTypes'
    });

    protected readonly $baseTypes = computed(() =>
        toFilterValues(this.#filters.getFilterValue('baseType'))
    );

    protected readonly $contentTypes = computed(() =>
        toFilterValues(this.#filters.getFilterValue('contentType'))
    );

    /**
     * Empty selections remove the key rather than writing `[]`.
     *
     * The two are different states downstream: only an absent key leaves the request byte-identical
     * to one that never mentioned the filter.
     */
    protected onSelectionChange({ baseTypes, contentTypes }: DotContentTypeFilterSelection): void {
        if (baseTypes.length) {
            this.#filters.patchFilters({ baseType: baseTypes });
        } else {
            this.#filters.removeFilter('baseType');
        }

        if (contentTypes.length) {
            this.#filters.patchFilters({ contentType: contentTypes });
        } else {
            this.#filters.removeFilter('contentType');
        }
    }
}

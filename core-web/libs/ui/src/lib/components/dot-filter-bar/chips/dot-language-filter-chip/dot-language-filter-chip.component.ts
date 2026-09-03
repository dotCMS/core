import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DotLanguageFilterComponent } from '../../../dot-language-filter/dot-language-filter.component';
import { DOT_FILTER_FACADE } from '../../filter-facade.token';

/**
 * Connects the presentational {@link DotLanguageFilterComponent} to whatever surface is rendering
 * it.
 *
 * Its one piece of translation is the id type: the filter bag holds strings, because on Content
 * Drive it has to survive a URL, while the presentational filter speaks numeric language ids.
 */
@Component({
    selector: 'dot-language-filter-chip',
    imports: [DotLanguageFilterComponent],
    template: `
        <dot-language-filter
            [selectedLanguageIds]="$languageIds()"
            (selectionChange)="onSelectionChange($event)" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { 'data-filter-chip': 'language' }
})
export class DotLanguageFilterChipComponent {
    readonly #filters = inject(DOT_FILTER_FACADE);

    protected readonly $languageIds = computed(() =>
        ((this.#filters.getFilterValue('languageId') as string[]) ?? []).map(Number)
    );

    protected onSelectionChange(languageIds: number[]): void {
        if (languageIds.length) {
            this.#filters.patchFilters({ languageId: languageIds.map(String) });
        } else {
            this.#filters.removeFilter('languageId');
        }
    }
}

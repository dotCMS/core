import { Component, computed, inject, input } from '@angular/core';

import { DotLanguageFilterComponent } from '../../../dot-language-filter/dot-language-filter.component';
import { DOT_FILTER_FACADE, toFilterValues } from '../../filter-facade.token';

/**
 * Connects the presentational {@link DotLanguageFilterComponent} to whatever surface is rendering
 * it.
 *
 * Its one piece of translation is the id type: the filter bag holds strings, because on Content
 * Drive it has to survive a URL, while the presentational filter speaks numeric language ids.
 *
 * It also decides whether the chip offers its "remove" X — see {@link $defaultLanguageId}.
 */
@Component({
    selector: 'dot-language-filter-chip',
    imports: [DotLanguageFilterComponent],
    templateUrl: './dot-language-filter-chip.component.html',
    host: { 'data-filter-chip': 'language' }
})
export class DotLanguageFilterChipComponent {
    readonly #filters = inject(DOT_FILTER_FACADE);

    /**
     * The locale this surface re-seeds when the filter is cleared, or `null` when it seeds none.
     *
     * An input rather than something read through the facade: it is a property of the surface, not
     * a filter value, and the two surfaces genuinely differ. Content Drive always seeds the
     * environment default (`withFilterDefaults`), so clearing that one selection puts it straight
     * back; the AssetPicker only re-seeds on open and on "Clear all", so removing its locale filter
     * really does widen the results — which is why it passes nothing and keeps the X.
     *
     * The *rule* stays here rather than in each toolbar, so a third surface gets it for free.
     */
    readonly $defaultLanguageId = input<number | null>(null, { alias: 'defaultLanguageId' });

    protected readonly $languageIds = computed(() =>
        toFilterValues(this.#filters.getFilterValue('languageId')).map(Number)
    );

    /**
     * Whether to offer the chip's X.
     *
     * Withheld only in the one case where it would do nothing visible: the sole selection is the
     * locale this surface re-seeds. Any other state — nothing selected, a different locale, or the
     * default alongside another — is genuinely clearable.
     *
     * Without this the X shows always, and clicking it calls `removeFilter('languageId')`, which on
     * Content Drive re-seeds the same value and resets paging: a control whose only effect is to
     * send the editor back to page 1.
     */
    protected readonly $removable = computed(() => {
        const seeded = this.$defaultLanguageId();
        const selected = this.$languageIds();

        return !(seeded !== null && selected.length === 1 && selected[0] === seeded);
    });

    protected onSelectionChange(languageIds: number[]): void {
        if (languageIds.length) {
            this.#filters.patchFilters({ languageId: languageIds.map(String) });
        } else {
            this.#filters.removeFilter('languageId');
        }
    }
}

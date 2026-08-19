import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DotLanguageFilterComponent } from '@dotcms/ui';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/**
 * Store adapter over the shared {@link DotLanguageFilterComponent}. The store persists language ids
 * as strings (they travel through the URL); the shared filter works with numbers.
 */
@Component({
    selector: 'dot-content-drive-language-field',
    template: `
        <dot-language-filter
            [selectedLanguageIds]="$selectedLanguageIds()"
            [removable]="$removable()"
            (selectionChange)="onSelectionChange($event)" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotLanguageFilterComponent]
})
export class DotContentDriveLanguageFieldComponent {
    readonly #store = inject(DotContentDriveStore);

    protected readonly $selectedLanguageIds = computed(() =>
        ((this.#store.getFilterValue('languageId') as string[]) ?? []).map(Number)
    );

    /**
     * Whether the chip should offer its "remove" X. Hidden while the selection is exactly the
     * environment default, because removing it re-selects that same default — the X would do nothing
     * visible. Computed here rather than in the shared filter, which has no notion of a default.
     */
    /**
     * Whether the chip should offer its "remove" X. Hidden while the selection is exactly the
     * environment default, because removing it re-selects that same default — the X would do nothing
     * visible. Computed here rather than in the shared filter, which has no notion of a default.
     */
    /**
     * Every configured language, resolved once by the store — which needs the list anyway to find the
     * default one to seed. Passed down so the shared filter does not fetch it a second time.
     */
    protected readonly $removable = computed(() => {
        const selected = this.$selectedLanguageIds();

        return !(selected.length === 1 && selected[0] === this.#store.defaultLanguageId());
    });

    protected onSelectionChange(languageIds: number[]): void {
        if (languageIds.length) {
            this.#store.patchFilters({ languageId: languageIds.map(String) });
        } else {
            this.#store.removeFilter('languageId');
        }
    }
}

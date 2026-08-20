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

    protected onSelectionChange(languageIds: number[]): void {
        if (languageIds.length) {
            this.#store.patchFilters({ languageId: languageIds.map(String) });
        } else {
            this.#store.removeFilter('languageId');
        }
    }
}

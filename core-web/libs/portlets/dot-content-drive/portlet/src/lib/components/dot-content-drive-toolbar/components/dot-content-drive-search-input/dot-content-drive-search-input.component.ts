import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ALL_FOLDER, DotSearchInputComponent } from '@dotcms/ui';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/**
 * Store adapter over the shared {@link DotSearchInputComponent}: binds the `title` filter in and
 * writes the debounced term back. The presentational box (debounce, clear icon) lives in
 * `@dotcms/ui` so AssetPicker can reuse it without the store.
 */
@Component({
    selector: 'dot-content-drive-search-input',
    template: `
        <!-- Placeholder falls back to the shared "search" i18n key. -->
        <dot-search-input [value]="$searchTerm()" (search)="onSearch($event)" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotSearchInputComponent],
    host: { class: 'w-full' }
})
export class DotContentDriveSearchInputComponent {
    readonly #store = inject(DotContentDriveStore);

    protected readonly $searchTerm = computed(
        () => (this.#store.getFilterValue('title') as string) ?? ''
    );

    /**
     * A new search resets the folder scope: results are drive-wide, so leaving the tree pinned to
     * the previously selected folder would contradict what the list shows.
     */
    protected onSearch(term: string): void {
        this.#store.setGlobalSearch(term);
        this.#store.setSelectedNode(ALL_FOLDER);
    }
}

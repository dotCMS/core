import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    viewChild
} from '@angular/core';

import { DotKeyboardShortcutService, DotSearchInputComponent } from '@dotcms/ui';

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
    readonly #shortcuts = inject(DotKeyboardShortcutService);

    private readonly $searchInput = viewChild(DotSearchInputComponent);

    protected readonly $searchTerm = computed(
        () => (this.#store.getFilterValue('title') as string) ?? ''
    );

    /**
     * Claims the search shortcut for as long as this box is on screen.
     *
     * Registered here rather than in the shell because this is the component that holds the search
     * field; the shell would have to reach three levels down to find it. It also means the claim is
     * withdrawn automatically when the toolbar goes away, which is what lets a dialog opening over
     * the portlet take the shortcut and hand it back on close.
     */
    constructor() {
        const unregister = this.#shortcuts.register({
            combination: 'mod+k',
            label: 'content-drive.shortcut.search',
            handler: () => {
                const input = this.$searchInput();

                // Declines rather than swallowing the key if the box is not rendered, so the
                // combination falls through instead of silently doing nothing.
                if (!input) {
                    return false;
                }

                input.focus();

                return true;
            }
        });

        inject(DestroyRef).onDestroy(unregister);
    }

    /**
     * A new search resets the folder scope: results are drive-wide, so leaving the tree pinned to
     * the previously selected folder would contradict what the list shows.
     *
     * `selectRootNode()` rather than pinning a synthetic node: the tree's root is the real site
     * row now (see `createSiteNode`), so there is no "All folders" node left to select.
     */
    protected onSearch(term: string): void {
        this.#store.setGlobalSearch(term);
        this.#store.selectRootNode();
    }
}

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnDestroy,
    viewChild
} from '@angular/core';

import {
    DotKeyboardShortcutService,
    DotKeyboardShortcutUnregister,
    DotSearchInputComponent,
    hasOverlayAbove
} from '@dotcms/ui';

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
export class DotContentDriveSearchInputComponent implements OnDestroy {
    readonly #store = inject(DotContentDriveStore);
    readonly #shortcuts = inject(DotKeyboardShortcutService);

    /** Withdrawals for the claims made in the constructor, released in {@link ngOnDestroy}. */
    #withdrawShortcuts: DotKeyboardShortcutUnregister[] = [];

    // NOTE: `private`, not `#`, despite TYPESCRIPT_STANDARDS.md:87. Angular's compiler rejects a
    // signal query on an ES-private field: "Cannot use 'viewChild' on a class member that is
    // declared as ES private." The standard cannot be followed here.
    private readonly $searchInput = viewChild.required(DotSearchInputComponent);

    protected readonly $searchTerm = computed(
        () => (this.#store.getFilterValue('title') as string) ?? ''
    );

    /**
     * Claims the search shortcuts for as long as this box is on screen.
     *
     * Registered here rather than in the shell because this is the component that holds the search
     * field; the shell would have to reach three levels down to find it. It also means the claim is
     * withdrawn automatically when the toolbar goes away, which is what lets a dialog opening over
     * the portlet take the shortcut and hand it back on close.
     *
     * `/` is the primary. It is the established convention for "focus the search box" specifically,
     * and unlike a modifier combination it stays clear of the one key users actually reach for —
     * the browser's own find-in-page, which must not be taken away from them. `mod+k` remains as an
     * alias for the habit, though it is worth knowing it conventionally opens a command palette, so
     * it is the one to give up if this application ever wants one.
     *
     * Two calls, not one array: labels must be unique within a single `register()` call, and these
     * two are deliberately the same action under two keys.
     */
    constructor() {
        // `viewChild.required`: the search box is rendered unconditionally in this template, so it
        // always resolves by the time a keypress can reach here.
        const focusSearch = () => {
            // Stand down while an overlay is above the portlet, exactly as Escape and the tree
            // toggle do. The shell's own dialogs — Action Center, the folder and content-type
            // selectors, upload — claim neither combination, so without this the shortcut pulls
            // focus to the search box *behind* an open modal. `mod+k` is the worse half: it carries
            // a modifier, so the registry's typing rule never short-circuits it and it fires from
            // anywhere inside the dialog.
            //
            // No container argument: this is a base-layer surface with no entry in the z-index
            // stack, so the question is "is any overlay open at all", which is the right one here.
            // The AssetPicker's toolbar deliberately does *not* do this — it *is* the top overlay,
            // and asking the same question there would make it decline permanently.
            if (hasOverlayAbove()) {
                return false;
            }

            this.$searchInput().focus();

            return true;
        };

        this.#withdrawShortcuts = ['/', 'mod+k'].map((combination) =>
            this.#shortcuts.register({
                combination,
                label: 'content-drive.shortcut.search',
                handler: focusSearch
            })
        );
    }

    /**
     * Hands the combinations back. Without this the claim outlives the toolbar and the surface
     * underneath never gets its shortcut returned.
     */
    ngOnDestroy(): void {
        this.#withdrawShortcuts.forEach((withdraw) => withdraw());
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

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
    DotSearchScopeInputComponent,
    hasOverlayAbove
} from '@dotcms/ui';

import { DEFAULT_SEARCH_SCOPE, SEARCH_SCOPE_FILTER_KEY } from '../../../../shared/constants';
import { DotContentDriveSearchScope } from '../../../../shared/models';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/**
 * Store adapter over the shared {@link DotSearchScopeInputComponent}: binds the `title` filter and the
 * search scope in, and writes the debounced term and the picked scope back. The presentational box
 * and its Title / All Fields dropdown (debounce, clear icon, panel) live in `@dotcms/ui` so the
 * AssetPicker can reuse them without this store.
 */
@Component({
    selector: 'dot-content-drive-search-input',
    templateUrl: './dot-content-drive-search-input.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotSearchScopeInputComponent],
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
    private readonly $searchInput = viewChild.required(DotSearchScopeInputComponent);

    protected readonly $searchTerm = computed(
        () => (this.#store.getFilterValue('title') as string) ?? ''
    );

    /** Absent from the filters means the default — the scope is only stored when it differs. */
    protected readonly $searchScope = computed<DotContentDriveSearchScope>(
        () =>
            (this.#store.getFilterValue(SEARCH_SCOPE_FILTER_KEY) as DotContentDriveSearchScope) ??
            DEFAULT_SEARCH_SCOPE
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
     * All site content is exactly that scope, and selecting it is now the honest way to say so.
     * This used to select the tree's site row, with a comment regretting that there was no "All
     * folders" node left to choose; there is one again, and the site row has since come to mean
     * the root alone, which is narrower than what a search returns.
     */
    protected onSearch(term: string): void {
        this.#store.setGlobalSearch(term);
        this.#store.selectAllSiteContent();
    }

    /**
     * Records the new scope and lets the store re-run the search.
     *
     * Re-selecting the scope that is already active is ignored: the results cannot change, and
     * `patchFilters` would reset the user to page 1 for nothing.
     *
     * The `null` guard is load-bearing, not defensive filler. `p-listbox` is single-select with
     * `metaKeySelection` at its default of `false`, which makes it a TOGGLE: re-clicking the
     * option that is already selected emits `null` instead of the option's value
     * (`onOptionSelectSingle` in `primeng/listbox`). Without the guard that `null` reaches
     * `setSearchScope`, which only special-cases the real default value — `null` is neither that
     * nor the current scope, so it gets written into the filters as `searchScope: null`. That
     * silently flips the active search to All Fields and lights up "Clear all" on a drive with
     * nothing filtered, and the bad value can then never be reselected away because
     * `$searchScope()` already reads back as a real scope.
     */
    protected onScopeChange(scope: DotContentDriveSearchScope | null): void {
        if (scope == null || scope === this.$searchScope()) {
            return;
        }

        this.#store.setSearchScope(scope);
    }
}

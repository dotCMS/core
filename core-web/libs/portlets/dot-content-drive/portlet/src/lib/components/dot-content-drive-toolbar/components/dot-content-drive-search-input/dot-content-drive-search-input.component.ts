import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnDestroy,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { TooltipModule } from 'primeng/tooltip';

import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    CHIP_FILTER_SCROLL_HEIGHT,
    DotKeyboardShortcutService,
    DotKeyboardShortcutUnregister,
    DotMessagePipe,
    DotSearchInputComponent,
    hasOverlayAbove
} from '@dotcms/ui';

import { DEFAULT_SEARCH_SCOPE, SEARCH_SCOPE_FILTER_KEY } from '../../../../shared/constants';
import {
    DOT_CONTENT_DRIVE_SEARCH_SCOPE,
    DotContentDriveSearchScope
} from '../../../../shared/models';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/**
 * Store adapter over the shared {@link DotSearchInputComponent}: binds the `title` filter in and
 * writes the debounced term back. The presentational box (debounce, clear icon) lives in
 * `@dotcms/ui` so AssetPicker can reuse it without the store.
 */
@Component({
    selector: 'dot-content-drive-search-input',
    templateUrl: './dot-content-drive-search-input.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        DotSearchInputComponent,
        ButtonModule,
        InputGroupModule,
        InputGroupAddonModule,
        ListboxModule,
        PopoverModule,
        TooltipModule,
        DotMessagePipe,
        FormsModule
    ],
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
     * The two options, in the order they read best: the narrow one first, because it is the choice
     * the user is here to make. The wide one is called "All Fields" rather than "All Content" — it
     * widens which FIELDS are read, not which content is searched, and Content Drive is separately
     * gaining a browse scope where "All" genuinely means all content.
     */
    protected readonly scopeOptions = [
        {
            label: 'content-drive.search.scope.title',
            value: DOT_CONTENT_DRIVE_SEARCH_SCOPE.TITLE
        },
        {
            label: 'content-drive.search.scope.all-fields',
            value: DOT_CONTENT_DRIVE_SEARCH_SCOPE.ALL_FIELDS
        }
    ];

    /**
     * The addon's own background, repointed to match the input.
     *
     * `p-inputgroup-addon` has no `dt` input (confirmed against its compiled metadata — only
     * `style`/`styleClass` are declared), so a design-token override isn't an option here. `[style]`
     * is the component's own supported way in instead, and it needs no `!important`: an inline style
     * always wins the cascade over an external stylesheet rule, regardless of specificity.
     *
     * `var(--p-inputtext-background)` rather than a literal colour: PrimeNG's `dt()` mechanism
     * compiles every token to a CSS custom property of exactly this shape, so reading the input's
     * own resolved variable keeps the two in step if the active theme ever changes, the same
     * guarantee a `dt` override would have given if one were available.
     *
     * The addon's BORDER needs no such fix — `inputgroup.addon.borderColor` already resolves to the
     * same `{form.field.border.color}` alias `inputtext` uses, confirmed straight from the Lara
     * preset source. Only the background differs (`{surface.50}`, a light grey).
     */
    protected readonly ADDON_STYLE = { background: 'var(--p-inputtext-background)' };

    /**
     * The same popover/listbox pass-through every other Content Drive filter dropdown already uses
     * (`dot-content-drive-workflow-filter`, `dot-status-filter`, …), so this panel matches the rest
     * of the toolbar instead of introducing its own styling.
     */
    protected readonly POPOVER_PT = CHIP_FILTER_POPOVER_PT;
    protected readonly LISTBOX_PT = CHIP_FILTER_LISTBOX_PT;
    protected readonly SCROLL_HEIGHT = CHIP_FILTER_SCROLL_HEIGHT;

    /** Absent from the filters means the default — the scope is only stored when it differs. */
    protected readonly $searchScope = computed<DotContentDriveSearchScope>(
        () =>
            (this.#store.getFilterValue(SEARCH_SCOPE_FILTER_KEY) as DotContentDriveSearchScope) ??
            DEFAULT_SEARCH_SCOPE
    );

    /** The trigger shows the active scope, as the mock does. */
    protected readonly $activeScopeLabel = computed(
        () =>
            this.scopeOptions.find((option) => option.value === this.$searchScope())?.label ??
            'content-drive.search.scope.all-fields'
    );

    /**
     * The trigger's own border, removed via PT rather than a Tailwind `!important` class.
     *
     * `pButtonPT`'s `root.style` is consumed through `[style]`/`[class]` HOST BINDINGS on the
     * directive's own host element (see `Bind`, the directive backing this), which Angular applies
     * the same way any `[style]` binding is — as a real inline style. That wins the cascade over
     * PrimeNG's own injected `.p-button-secondary` rule unconditionally, the same guarantee
     * `!important` gives, without reaching for it.
     */
    protected readonly TRIGGER_PT = {
        root: { style: { border: 'none' } }
    };

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

    /**
     * Records the new scope and lets the store re-run the search.
     *
     * Re-selecting the scope that is already active is ignored: the results cannot change, and
     * `patchFilters` would reset the user to page 1 for nothing.
     */
    protected onScopeChange(scope: DotContentDriveSearchScope): void {
        if (scope === this.$searchScope()) {
            return;
        }

        this.#store.setSearchScope(scope);
    }
}

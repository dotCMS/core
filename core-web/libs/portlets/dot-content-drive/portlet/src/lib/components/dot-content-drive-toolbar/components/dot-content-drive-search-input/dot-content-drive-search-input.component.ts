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

import { DotMessageService } from '@dotcms/data-access';
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
    readonly #messageService = inject(DotMessageService);

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
     *
     * `label` is resolved here, eagerly, rather than left as a raw i18n key for the item template
     * to pipe through `| dm`. `p-listbox` puts its own `[attr.aria-label]` on each option element
     * from `getOptionLabel(option)`, which falls back to the raw `option.label` — it does not go
     * through this component's template, so a piped label left a screen reader announcing the key
     * itself (e.g. "content-drive.search.scope.title") instead of the translated text.
     */
    protected readonly scopeOptions = [
        {
            label: this.#messageService.get('content-drive.search.scope.title'),
            help: 'content-drive.search.scope.title.help',
            value: DOT_CONTENT_DRIVE_SEARCH_SCOPE.TITLE
        },
        {
            label: this.#messageService.get('content-drive.search.scope.all-fields'),
            help: 'content-drive.search.scope.all-fields.help',
            value: DOT_CONTENT_DRIVE_SEARCH_SCOPE.ALL_FIELDS
        }
    ];

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

    /**
     * The trigger shows the active scope, as the mock does. Already-translated text — see the
     * comment on `scopeOptions` for why the label is resolved there rather than piped through
     * `| dm` in the template.
     */
    protected readonly $activeScopeLabel = computed(
        () =>
            this.scopeOptions.find((option) => option.value === this.$searchScope())?.label ??
            this.#messageService.get('content-drive.search.scope.all-fields')
    );

    /**
     * The trigger's own border removed and its background set to the field's white, both via PT
     * rather than Tailwind classes, and its content pinned to the edges so the active scope reads
     * from the same place no matter which scope is active.
     *
     * `pButtonPT`'s `root.style` is consumed through `[style]`/`[class]` HOST BINDINGS on the
     * directive's own host element (see `Bind`, the directive backing this), which Angular applies
     * the same way any `[style]` binding is — as a real inline style. That wins the cascade over
     * PrimeNG's own injected `.p-button-secondary` rule unconditionally, the same guarantee
     * `!important` gives, without reaching for it. A plain `bg-white` class cannot: PrimeNG's
     * styles are appended to the head at runtime, after the Tailwind stylesheet, so at equal
     * specificity the injected rule wins — which is why the border went through PT first and the
     * background follows it.
     *
     * The background value is the same design token the neighboring input paints with —
     * `inputtext.background`, read as the CSS variable PrimeNG emits for every token — so the two
     * halves of the field stay the same white in any theme, including dark mode, and a restyling
     * of the input re-whites the trigger for free. As a side effect the hover recolor
     * `.p-button-secondary` would apply is also beaten by the inline style, which is what a
     * dropdown trigger wants: the surface does not change under the pointer, the way `p-select`
     * behaves.
     *
     * The last entry exists because Lara centers a button's content (`justify-content: center`,
     * hardcoded in the injected stylesheet — no design token exposes it). Centered, the label and
     * the chevron are one group that recenters itself as its width changes, so "Title" and
     * "All Fields" rendered with both at different offsets. `space-between` — the layout PrimeNG's
     * own `p-select` trigger uses — parks the label on the left edge and the chevron on the right
     * edge of the fixed-width button, so switching scopes moves neither.
     */
    protected readonly TRIGGER_PT = {
        root: {
            style: {
                border: 'none',
                background: 'var(--p-inputtext-background)',
                justifyContent: 'space-between'
            }
        }
    };

    /**
     * Squares off the search input's connecting edge — the side that meets the scope addon. This
     * component's own host sits between `p-inputgroup` and the real `<input>`, breaking PrimeNG's
     * structural CSS, so the override rides the design token through `inputDt` instead of
     * competing for specificity (see the template comment). Hoisted like `TRIGGER_PT`: an inline
     * object literal would be recreated on every change detection cycle.
     */
    protected readonly INPUT_DT = {
        border: { radius: '{form.field.border.radius} 0 0 {form.field.border.radius}' }
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
    protected onScopeChange(scope: DotContentDriveSearchScope): void {
        if (scope == null || scope === this.$searchScope()) {
            return;
        }

        this.#store.setSearchScope(scope);
    }
}

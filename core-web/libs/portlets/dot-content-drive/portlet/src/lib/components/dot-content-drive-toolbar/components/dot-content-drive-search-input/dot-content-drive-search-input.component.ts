import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    signal,
    OnDestroy,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { TooltipModule } from 'primeng/tooltip';

import {
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
    template: `
        <!-- Input and scope read as one control: the wrapper owns the border and the rounding, and
             the two children sit flush inside it, divided by a single hairline. Matching the ticket's
             mock, and the panel reuses the popover + listbox the filter chips already use so the
             drive has one dropdown idiom rather than two. -->
        <div
            class="dot-search-scope flex w-full items-stretch overflow-hidden rounded-md border border-slate-300 focus-within:border-slate-400">
            <dot-search-input
                [value]="$searchTerm()"
                [placeholder]="$placeholder()"
                (search)="onSearch($event)"
                class="min-w-0 flex-1" />

            <button
                type="button"
                class="flex shrink-0 items-center gap-2 border-l border-slate-300 px-3 text-sm hover:bg-slate-50"
                [attr.aria-label]="'content-drive.search.scope.label' | dm"
                [attr.aria-expanded]="$panelOpen()"
                [pTooltip]="'content-drive.search.scope.help' | dm"
                aria-haspopup="listbox"
                data-testid="search-scope-trigger"
                tooltipPosition="bottom"
                (click)="panel.toggle($event)">
                {{ $activeScopeLabel() | dm }}
                <i class="pi text-xs" [class.pi-chevron-down]="!$panelOpen()" [class.pi-chevron-up]="$panelOpen()" aria-hidden="true"></i>
            </button>
        </div>

        <p-popover
            #panel
            styleClass="dot-popover-flush"
            (onShow)="$panelOpen.set(true)"
            (onHide)="$panelOpen.set(false)">
            <!-- checkmark is the component's own way of marking the active option, which is what
                 the mock shows. The item template only translates the label. -->
            <p-listbox
                [options]="scopeOptions"
                [ngModel]="$searchScope()"
                [ngModelOptions]="{ standalone: true }"
                [checkmark]="true"
                optionValue="value"
                class="w-48"
                data-testid="search-scope-panel"
                (ngModelChange)="onScopeChange($event); panel.hide()">
                <ng-template let-item pTemplate="item">
                    <span [attr.data-testid]="'search-scope-option-' + item.value">
                        {{ item.label | dm }}
                    </span>
                </ng-template>
            </p-listbox>
        </p-popover>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        DotSearchInputComponent,
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

    /** Absent from the filters means the default — the scope is only stored when it differs. */
    protected readonly $searchScope = computed<DotContentDriveSearchScope>(
        () =>
            (this.#store.getFilterValue(SEARCH_SCOPE_FILTER_KEY) as DotContentDriveSearchScope) ??
            DEFAULT_SEARCH_SCOPE
    );

    /** Whether the panel is open, so the chevron can point the right way. */
    protected readonly $panelOpen = signal(false);

    /** The trigger shows the active scope, as the mock does. */
    protected readonly $activeScopeLabel = computed(
        () =>
            this.scopeOptions.find((option) => option.value === this.$searchScope())?.label ??
            'content-drive.search.scope.all-fields'
    );

    /** The box says what it will do before the user types again. */
    protected readonly $placeholder = computed(() =>
        this.$searchScope() === DOT_CONTENT_DRIVE_SEARCH_SCOPE.TITLE
            ? 'content-drive.search.placeholder.title'
            : 'content-drive.search.placeholder.all-fields'
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

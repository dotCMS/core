import { patchState, signalState } from '@ngrx/signals';
import { EMPTY, of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    computed,
    inject,
    input,
    linkedSignal,
    output,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { ScrollerLazyLoadEvent } from 'primeng/scroller';

import { catchError, debounceTime, map, switchMap, take, takeUntil, tap } from 'rxjs/operators';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentType,
    DotPagination,
    StructureTypeView
} from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { LISTBOX_OPTION_HEIGHT } from '../../theme/theme.config';
import { CHIP_FILTER_LISTBOX_PT, CHIP_FILTER_POPOVER_PT } from '../dot-chip-filter/constants';
import { DotChipFilterComponent } from '../dot-chip-filter/dot-chip-filter.component';
import { DotFilterListItemComponent } from '../dot-filter-list-item/dot-filter-list-item.component';
import { DEFAULT_SEARCH_DEBOUNCE } from '../dot-search-input/constants';

const ALL_CONTENT = '__ALL_CONTENT__';
const ITEMS_PER_PAGE = 10;

/**
 * Row height (px) used by the right column's virtual scroller. Taken from the theme, which fixes
 * every listbox option to the same height — previously an empirically measured 40.6 that silently
 * misaligned the scroller whenever the option padding or font changed.
 */
const LISTBOX_ITEM_HEIGHT = LISTBOX_OPTION_HEIGHT;
/** Approximate column header height (px-4 py-3 with text-xs uppercase). */
const POPOVER_HEADER_HEIGHT = '3rem';
/**
 * Rows the popover grows to before its listboxes start scrolling. Matches the full base-type
 * catalog (8 types + ALL_CONTENT), so an unrestricted host shows the whole left column at once.
 */
const MAX_VISIBLE_ROWS = 9;
/**
 * Floor for the same count, so the popover keeps ONE height for as long as it is open.
 *
 * The right column reloads on every base-type focus change and the catalogs differ wildly — in the
 * AssetPicker, File has a single content type and DotAsset has four. Sizing purely to the focused
 * column made the panel jump on every click, which reads worse than a little unused space. Seven
 * rows covers the restricted hosts without pushing an unrestricted one around (it stays at the max).
 */
const MIN_VISIBLE_ROWS = 7;
/** Row slots the right column spends on chrome rather than options: its search field. */
const RIGHT_COLUMN_CHROME_ROWS = 1;

/** Listbox viewport height for a row count, plus the list container's own vertical padding. */
const rowsToPx = (rows: number) => `${rows * LISTBOX_ITEM_HEIGHT + 14}px`;

interface BaseTypeOption {
    name: string;
    label: string;
}

/** Emitted on every selection change. Empty arrays mean "no type filter". */
export interface DotContentTypeFilterSelection {
    /** Base-type names, e.g. `['CONTENT', 'FILEASSET']`. */
    baseTypes: string[];
    /** Content-type variables. */
    contentTypes: string[];
}

interface State {
    baseTypes: BaseTypeOption[];
    contentTypes: DotCMSContentType[];
    contentTypeFilter: string;
    loading: boolean;
    canLoadMore: boolean;
    currentPage: number;
}

/**
 * Base-type / content-type chip filter shared across Content Drive and AssetPicker.
 *
 * Owns the catalog (fetched through {@link DotContentTypeService}) and all of the two-column
 * popover behavior, but not the selection: the host passes it in through `selectedBaseTypes` /
 * `selectedContentTypes` and receives every change back through `selectionChange`.
 *
 * The selection is expressed in base-type *names* (`CONTENT`, `FILEASSET`) and content-type
 * *variables* — how a host persists them (Content Drive encodes base types as numbers in the URL)
 * is the host's business.
 */
@Component({
    selector: 'dot-content-type-filter',
    imports: [
        FormsModule,
        CheckboxModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-content-type-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // `contents` keeps the host out of the layout, so the chip and its popover sit directly in
    // whatever row the consumer lays out (a toolbar chip row, the AssetPicker header, …).
    host: { class: 'contents' }
})
export class DotContentTypeFilterComponent implements OnInit {
    readonly #destroyRef = inject(DestroyRef);
    readonly #contentTypesService = inject(DotContentTypeService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #fetchSubject = new Subject<{ baseType?: string; filter: string }>();
    /**
     * Fires whenever the focused base type changes, cancelling any in-flight
     * focus or lazy-load fetch so a late response from a previous focus can't
     * overwrite the current state.
     */
    readonly #cancelFetch$ = new Subject<void>();

    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    /**
     * PT applied to the base-type checkbox when it's in the indeterminate
     * (partial) state — paints the box with the checked-state tokens so the
     * pi-minus icon renders white on the primary background. PrimeNG v21 has
     * no built-in indeterminate token / class to target, so we override the
     * inner box + icon directly via passthrough.
     */
    protected readonly partialCheckboxPt = {
        box: {
            style: {
                background: 'var(--p-checkbox-checked-background)',
                borderColor: 'var(--p-checkbox-checked-border-color)'
            }
        },
        icon: {
            style: { color: 'var(--p-checkbox-icon-checked-color)' }
        }
    };
    /**
     * Selected base-type names, owned by the host.
     * @type {string[]}
     * @alias selectedBaseTypes
     */
    readonly $baseTypes = input<string[]>([], { alias: 'selectedBaseTypes' });

    /**
     * Selected content-type variables, owned by the host.
     * @type {string[]}
     * @alias selectedContentTypes
     */
    readonly $contentTypes = input<string[]>([], { alias: 'selectedContentTypes' });

    /**
     * Restricts the catalog to these base types. `null` (default) offers every base type, which is
     * the Content Drive behavior; AssetPicker narrows it to dotAsset / File Asset.
     * @type {DotCMSBaseTypesContentTypes[] | null}
     * @alias allowedBaseTypes
     */
    readonly $allowedBaseTypes = input<DotCMSBaseTypesContentTypes[] | null>(null, {
        alias: 'allowedBaseTypes'
    });

    /**
     * i18n key for the chip title.
     * @type {string}
     * @alias title
     */
    readonly $title = input('content-drive.type-filter.title', { alias: 'title' });

    /** Emits the full selection on every change. */
    readonly selectionChange = output<DotContentTypeFilterSelection>();

    protected readonly ALL_CONTENT = ALL_CONTENT;
    protected readonly ITEMS_PER_PAGE = ITEMS_PER_PAGE;
    protected readonly LISTBOX_ITEM_HEIGHT = LISTBOX_ITEM_HEIGHT;

    readonly $state = signalState<State>({
        baseTypes: [],
        contentTypes: [],
        contentTypeFilter: '',
        loading: true,
        canLoadMore: true,
        currentPage: 1
    });

    /**
     * Cache of every content type ever fetched. Grows monotonically so selected
     * items remain resolvable even when they are no longer in the visible page.
     */
    readonly #contentTypeCache = signal<DotCMSContentType[]>([]);

    /** Base type whose content types are shown in the right column. ALL_CONTENT shows everything. */
    readonly $focusedBaseType = signal<string>(ALL_CONTENT);

    /**
     * Mounted only while the popover is open. Forces the inner listboxes to be
     * recreated on each open so virtual scroll measures the correct dimensions
     * (otherwise it computes 0 visible items while the overlay is hidden).
     */
    readonly $popoverOpen = signal(false);

    /**
     * Working copy of the base-type selection. Re-seeds whenever the host pushes a different set
     * (URL restore, "clear all").
     *
     * Note `allowedBaseTypes` gates what is *offered*, not what is *selected* — a host that seeds a
     * disallowed base type here would see it in the chip. Today they always agree (the AssetPicker
     * feeds both from the same config array), so this is a latent mismatch, not a live bug.
     */
    readonly $selectedBaseTypes = linkedSignal<string[]>(() => this.$baseTypes() ?? []);

    /**
     * Working copy of the content-type selection, resolved from the host's variables through the
     * cache — which holds every content type we have ever loaded, so selections survive focus
     * changes and lazy-load pages that no longer contain them.
     */
    readonly $selectedContentTypes = linkedSignal<DotCMSContentType[]>(() => {
        const variables = this.$contentTypes() ?? [];
        if (!variables.length) return [];
        const cache = this.#contentTypeCache();
        return cache.filter((ct) => variables.includes(ct.variable));
    });

    /** Left column options: ALL_CONTENT prepended to base types. */
    protected readonly $leftOptions = computed<BaseTypeOption[]>(() => [
        {
            name: ALL_CONTENT,
            label: this.#dotMessageService.get('content-drive.type-filter.all-content-types')
        },
        ...this.$state.baseTypes()
    ]);

    /** Banner height matches a single listbox item slot for visual consistency. */
    protected readonly LISTBOX_BANNER_HEIGHT_PX = `${LISTBOX_ITEM_HEIGHT}px`;

    /**
     * How many option rows the popover is sized for.
     *
     * Used to be the constant 9 — the size of Content Drive's base-type catalog — which left any
     * host that restricts the offering with dead space under both columns: the AssetPicker offers
     * only dotAsset + File Asset, so its left column has three rows and six went unused. Sized to
     * whichever column needs more, clamped between {@link MIN_VISIBLE_ROWS} (so focusing a
     * one-type base type does not shrink the panel out from under the cursor) and
     * {@link MAX_VISIBLE_ROWS} (so a growing catalog scrolls instead of running off the screen).
     */
    protected readonly $visibleRows = computed(() => {
        const left = this.$leftOptions().length;
        const right =
            this.$state.contentTypes().length +
            RIGHT_COLUMN_CHROME_ROWS +
            (this.$showAllBanner() ? 1 : 0);

        return Math.min(
            Math.max(left, right, MIN_VISIBLE_ROWS),
            Math.max(MAX_VISIBLE_ROWS, MIN_VISIBLE_ROWS)
        );
    });

    /**
     * Both a floor and a ceiling: the panel holds this height whatever the focused base type brings,
     * so switching focus never resizes it. The inner listboxes scroll instead.
     */
    protected readonly $panelHeight = computed(
        () => `calc(${rowsToPx(this.$visibleRows())} + ${POPOVER_HEADER_HEIGHT})`
    );

    protected readonly $leftScrollHeight = computed(() => rowsToPx(this.$visibleRows()));

    /** Lookup: base type name → human label, used for chip rendering. */
    readonly #baseTypeLabelByName = computed(() => {
        const map = new Map<string, string>();
        for (const bt of this.$state.baseTypes()) map.set(bt.name, bt.label);
        return map;
    });

    /**
     * Chip selections, formatted per ticket rules. Falls back to the raw
     * enum name (e.g. `CONTENT (All)`) if the base-type catalog hasn't loaded
     * yet or its API call failed — better to show the active filter with an
     * unfriendly label than to hide it entirely and leave the user wondering
     * why content is filtered.
     */
    readonly $chipSelections = computed<string[]>(() => {
        const baseTypes = this.$selectedBaseTypes();
        if (!baseTypes.length) return [];

        const labels = this.#baseTypeLabelByName();
        const contentTypes = this.$selectedContentTypes();
        const allSuffix = ` (${this.#dotMessageService.get('content-drive.type-filter.all')})`;

        return baseTypes.flatMap((baseType) => {
            const narrowed = contentTypes.filter((ct) => ct.baseType === baseType);
            if (narrowed.length) return narrowed.map((ct) => ct.name);
            return [`${labels.get(baseType) ?? baseType}${allSuffix}`];
        });
    });

    /**
     * Banner above the right list. Shown when the focused base type is selected
     * with no content types narrowing it — i.e. the filter is "all of this base".
     */
    protected readonly $showAllBanner = computed(() => {
        const focused = this.$focusedBaseType();
        if (focused === ALL_CONTENT) return false;
        if (!this.$selectedBaseTypes().includes(focused)) return false;
        return !this.$selectedContentTypes().some((ct) => ct.baseType === focused);
    });

    /**
     * Right listbox gives up a slot to its search field, and one more to the "all content types"
     * banner while that shows, so the popover height stays put instead of growing — the banner takes
     * over the bottom row's space.
     */
    protected readonly $rightScrollHeight = computed(() =>
        rowsToPx(
            Math.max(
                this.$visibleRows() - RIGHT_COLUMN_CHROME_ROWS - (this.$showAllBanner() ? 1 : 0),
                1
            )
        )
    );

    ngOnInit() {
        this.#loadBaseTypes();
        this.#loadInitialContentTypes();
        this.#setupFilterSubscription();
    }

    protected isBaseTypeSelected(name: string): boolean {
        return this.$selectedBaseTypes().includes(name);
    }

    protected onFocusChange(value: string | null): void {
        const focused = value ?? ALL_CONTENT;
        if (focused === this.$focusedBaseType()) return;
        // Cancel any in-flight focus/lazy fetch from the previous focus so a
        // late response can't overwrite the new state.
        this.#cancelFetch$.next();
        this.$focusedBaseType.set(focused);
        // Eagerly clear the right column so stale items from the previous focus
        // don't linger while the new fetch is in flight.
        patchState(this.$state, {
            contentTypes: [],
            contentTypeFilter: '',
            currentPage: 1,
            canLoadMore: true,
            loading: true
        });
        // Focus changes refetch immediately — no debounce, no race with typing.
        this.#loadContentTypes({
            page: 1,
            filter: '',
            type: focused === ALL_CONTENT ? undefined : focused
        })
            .pipe(takeUntil(this.#cancelFetch$))
            .subscribe(({ contentTypes, pagination }) => {
                patchState(this.$state, {
                    contentTypes,
                    loading: false,
                    canLoadMore: this.#hasMorePages(pagination),
                    currentPage: pagination.currentPage
                });
                this.#cacheContentTypes(contentTypes);
            });
    }

    /**
     * Two-state toggle from the left listbox checkbox:
     * - unchecked → select the base type (no content types added).
     * - any active state (fully checked OR indeterminate/partial) → drop the
     *   base AND its content types. One coherent rule: clicking an active
     *   checkbox clears it.
     *
     * Promoting a partial selection to "all of this base type" is reachable
     * via two clicks (partial → empty → checked). Making the partial click do
     * that promotion fights the standard "indeterminate checkbox click clears
     * the selection" expectation, which was confusing in user testing.
     *
     * The `checked` value emitted by p-checkbox is ignored on purpose; we
     * compute the next state from the current selection.
     */
    protected onBaseTypeToggle(name: string): void {
        const isSelected = this.$selectedBaseTypes().includes(name);

        if (isSelected) {
            this.$selectedBaseTypes.update((list) => list.filter((n) => n !== name));
            this.$selectedContentTypes.update((list) =>
                (list ?? []).filter((ct) => ct.baseType !== name)
            );
            // Unchecking the base type you're viewing resets the right column
            // to "all content types" — the natural no-filter view. Unchecking a
            // base type you're NOT viewing leaves the right column untouched.
            if (this.$focusedBaseType() === name) {
                this.onFocusChange(ALL_CONTENT);
            }
        } else {
            this.$selectedBaseTypes.update((list) => [...list, name]);
            // Checking a base type focuses it, so its content types load on the
            // right — keeps the checkbox click consistent with a title click.
            this.onFocusChange(name);
        }
        this.#emitSelection();
    }

    /**
     * `true` when the base type has narrowing content types selected — drives
     * the indeterminate (`pi-minus`) state on its checkbox.
     */
    protected isBaseTypePartial(name: string): boolean {
        if (!this.$selectedBaseTypes().includes(name)) return false;
        return this.$selectedContentTypes().some((ct) => ct.baseType === name);
    }

    /**
     * Reconciles base-type selection after the user toggled content types.
     * Cascades up (selecting a content type adds its base type) and cascades
     * down (when a base type loses its last selected content type, the base
     * type itself is dropped from the selection).
     */
    protected onContentTypeChange(newValue: DotCMSContentType[] | null): void {
        const previous = this.$selectedContentTypes() ?? [];
        const next = newValue ?? [];

        const previousBasesWithCts = new Set(previous.map((ct) => ct.baseType));
        const nextBasesWithCts = new Set(next.map((ct) => ct.baseType));

        // Bases that lost their last selected content type in this change.
        const droppedBases = [...previousBasesWithCts].filter((bt) => !nextBasesWithCts.has(bt));

        this.$selectedContentTypes.set(next);

        const baseTypes = new Set(this.$selectedBaseTypes());
        for (const bt of nextBasesWithCts) baseTypes.add(bt); // cascade up
        for (const bt of droppedBases) baseTypes.delete(bt); // cascade down
        this.$selectedBaseTypes.set([...baseTypes]);

        this.#emitSelection();
    }

    protected onSearchInput(value: string): void {
        const filter = value ?? '';
        patchState(this.$state, { contentTypeFilter: filter });
        const focused = this.$focusedBaseType();
        this.#fetchSubject.next({
            baseType: focused === ALL_CONTENT ? undefined : focused,
            filter
        });
    }

    protected onPanelHide(): void {
        this.$popoverOpen.set(false);
        // The whole search state goes, not just the term. Clearing `contentTypeFilter` alone leaves
        // `canLoadMore` false whenever the search fitted in a single page, and the lazy load that the
        // `@if ($popoverOpen())` recreate fires on the next open is then dropped by the guard in
        // `onLazyLoad` — so reopening showed the previous narrow result with no way to get past it.
        // `currentPage` goes back to 1 for the same reason: that guard also rejects any page at or
        // below the current one.
        //
        // $focusedBaseType is intentionally NOT reset — the user's last focus
        // persists across popover sessions so reopening lands them where they
        // left off.
        patchState(this.$state, {
            contentTypeFilter: '',
            currentPage: 1,
            canLoadMore: true
        });
    }

    protected onLazyLoad(event: ScrollerLazyLoadEvent): void {
        const last = typeof event.last === 'number' ? event.last : NaN;
        if (!Number.isFinite(last)) return;
        // PrimeNG's virtual scroller emits `last` as the last visible row index;
        // `Math.ceil(last / ITEMS_PER_PAGE) + 1` resolves to the *next* page,
        // which means we prefetch page N+1 as soon as the user reaches any
        // visible item on page N. Intentional: keeps scrolling smooth.
        const page = Math.ceil(last / ITEMS_PER_PAGE) + 1;
        if (!this.$state.canLoadMore() || page <= this.$state.currentPage()) return;

        patchState(this.$state, { currentPage: page });
        const focused = this.$focusedBaseType();
        this.#loadContentTypes({
            page,
            filter: this.$state.contentTypeFilter(),
            type: focused === ALL_CONTENT ? undefined : focused
        })
            // Cancel if the user changes focus mid-flight; the new fetch will
            // own the right list.
            .pipe(takeUntil(this.#cancelFetch$))
            .subscribe(({ contentTypes, pagination }) => {
                if (!contentTypes.length) {
                    patchState(this.$state, { canLoadMore: false, loading: false });
                    return;
                }
                const merged = [...this.$state.contentTypes(), ...contentTypes];
                patchState(this.$state, {
                    contentTypes: merged,
                    canLoadMore: this.#hasMorePages(pagination),
                    loading: false,
                    currentPage:
                        pagination.currentPage > this.$state.currentPage()
                            ? pagination.currentPage
                            : this.$state.currentPage()
                });
                this.#cacheContentTypes(contentTypes);
            });
    }

    protected onClearAll(): void {
        this.$selectedBaseTypes.set([]);
        this.$selectedContentTypes.set([]);
        this.#emitSelection();
    }

    #emitSelection(): void {
        this.selectionChange.emit({
            baseTypes: this.$selectedBaseTypes(),
            contentTypes: (this.$selectedContentTypes() ?? []).map((ct) => ct.variable)
        });
    }

    #loadBaseTypes(): void {
        this.#contentTypesService
            .getAllContentTypes()
            .pipe(
                take(1),
                map((response: StructureTypeView[]) =>
                    response.filter((item) => this.#isBaseTypeOffered(item.name))
                ),
                catchError(() => of([] as StructureTypeView[]))
            )
            .subscribe((response) => {
                patchState(this.$state, {
                    baseTypes: response.map(({ name, label }) => ({ name, label }))
                });
            });
    }

    #loadInitialContentTypes(): void {
        // The cache is empty at this point, so #ensureParam would return
        // `undefined` even when the host arrives with a pre-selected set (e.g. a
        // URL-restored filter). Read the variables straight off the input so the
        // first fetch can ensure those items appear on page 1 (and seed the
        // cache for $selectedContentTypes to resolve them).
        const variables = this.$contentTypes() ?? [];
        const ensure = variables.length ? variables.join(',') : undefined;
        // `loading` is already true from initial state; no pre-fetch tap needed.
        this.#contentTypesService
            .getContentTypesWithPagination({
                ensure,
                type: this.#typeParam(),
                per_page: ITEMS_PER_PAGE
            })
            .pipe(
                catchError(() =>
                    of({
                        contentTypes: [],
                        pagination: { currentPage: 1, totalEntries: 0 } as DotPagination
                    })
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ contentTypes, pagination }) => {
                const filtered = this.#filterContentTypes(contentTypes);
                patchState(this.$state, {
                    contentTypes: filtered,
                    canLoadMore: this.#hasMorePages(pagination),
                    loading: false,
                    currentPage: pagination.currentPage
                });
                // Cache the raw response — `ensure`-restored content types may be
                // system or FORM (filtered out of the visible options) but they
                // still need to resolve in $selectedContentTypes so #emitSelection
                // doesn't drop a restored filter on first user interaction.
                this.#cacheContentTypes(contentTypes);
            });
    }

    #setupFilterSubscription(): void {
        this.#fetchSubject
            .pipe(
                tap(() => patchState(this.$state, { loading: true })),
                debounceTime(DEFAULT_SEARCH_DEBOUNCE),
                switchMap((req) => {
                    // If focus changed during the debounce window, the
                    // focus-change path already kicked off its own fetch and
                    // owns the right list — drop this stale buffered search
                    // so it can't race in and overwrite the new state.
                    const focused = this.$focusedBaseType();
                    const currentType = focused === ALL_CONTENT ? undefined : focused;
                    if (req.baseType !== currentType) {
                        patchState(this.$state, { loading: false });
                        return EMPTY;
                    }
                    return this.#loadContentTypes({
                        page: 1,
                        filter: req.filter,
                        type: req.baseType
                    }).pipe(takeUntil(this.#cancelFetch$));
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ contentTypes, pagination }) => {
                patchState(this.$state, {
                    contentTypes,
                    loading: false,
                    canLoadMore: this.#hasMorePages(pagination),
                    currentPage: pagination.currentPage
                });
                this.#cacheContentTypes(contentTypes);
            });
    }

    #loadContentTypes({ page, filter, type }: { page: number; filter: string; type?: string }) {
        return this.#contentTypesService
            .getContentTypesWithPagination({
                filter,
                type: this.#typeParam(type),
                ensure: this.#ensureParam(),
                page,
                per_page: ITEMS_PER_PAGE
            })
            .pipe(
                take(1),
                takeUntilDestroyed(this.#destroyRef),
                catchError(() =>
                    of({
                        contentTypes: [],
                        pagination: { currentPage: 1, totalEntries: 0 } as DotPagination
                    })
                ),
                map(({ contentTypes, pagination }) => ({
                    contentTypes: this.#filterContentTypes(contentTypes),
                    pagination
                }))
            );
    }

    #filterContentTypes(contentTypes: DotCMSContentType[]): DotCMSContentType[] {
        return contentTypes.filter((ct) => !ct.system && this.#isBaseTypeOffered(ct.baseType));
    }

    /**
     * `type` for the content-type query.
     *
     * With a focused base type it is that one. Without a focus ("All content types") the query would
     * otherwise be unbounded, and since `#filterContentTypes` drops everything outside
     * `allowedBaseTypes` afterwards, a restricted host would page through mostly-discarded results —
     * on a large install the right column renders one or two rows per page and `#hasMorePages`
     * counts the unfiltered total. Narrowing the request server-side fixes both.
     *
     * The service splits this on commas into repeated `type` params, so the whole allowed set can
     * travel in one string. Unrestricted hosts (Content Drive) get `undefined` and are unaffected.
     */
    #typeParam(focusedType?: string): string | undefined {
        return focusedType ?? this.$allowedBaseTypes()?.join(',') ?? undefined;
    }

    /**
     * Gate for both columns: FORM is always excluded (deprecated), and when the host restricts the
     * catalog through `allowedBaseTypes` everything outside that set is excluded too.
     *
     * Still applied client-side even with {@link #typeParam} narrowing the query, because it also
     * strips FORM and system types, which the server does return.
     */
    #isBaseTypeOffered(baseType: string): boolean {
        if (baseType === DotCMSBaseTypesContentTypes.FORM) {
            return false;
        }

        const allowed = this.$allowedBaseTypes();

        return !allowed?.length || allowed.includes(baseType as DotCMSBaseTypesContentTypes);
    }

    /**
     * Source-of-truth for "is there another page to load?". Computes total
     * pages from the server's `totalEntries` (which counts every content type,
     * including the FORM / system items we strip client-side). In the worst
     * case this triggers ONE extra empty fetch — when the final page contains
     * only filtered-out items — which the `if (!contentTypes.length)` guard
     * in `onLazyLoad` catches by setting `canLoadMore: false`. We accept that
     * trade-off rather than tracking a separate "filtered total" client-side.
     */
    #hasMorePages(pagination: DotPagination): boolean {
        const perPage = pagination.perPage || ITEMS_PER_PAGE;
        const totalPages = Math.ceil((pagination.totalEntries ?? 0) / perPage);
        return pagination.currentPage < totalPages;
    }

    #cacheContentTypes(contentTypes: DotCMSContentType[]): void {
        if (!contentTypes.length) return;
        this.#contentTypeCache.update((cache) => {
            const seen = new Set(cache.map((ct) => ct.variable));
            const additions = contentTypes.filter((ct) => !seen.has(ct.variable));
            return additions.length ? [...cache, ...additions] : cache;
        });
    }

    /**
     * Only ensure selected content types that actually belong to the focused
     * base type. Otherwise the server would be told to include items that the
     * current focus would never legitimately return (e.g. a CONTENT-typed
     * selection while focusing FILEASSET).
     */
    #ensureParam(): string | undefined {
        const focused = this.$focusedBaseType();
        const selected = this.$selectedContentTypes() ?? [];
        const relevant =
            focused === ALL_CONTENT ? selected : selected.filter((ct) => ct.baseType === focused);

        return relevant.length ? relevant.map((ct) => ct.variable).join(',') : undefined;
    }
}

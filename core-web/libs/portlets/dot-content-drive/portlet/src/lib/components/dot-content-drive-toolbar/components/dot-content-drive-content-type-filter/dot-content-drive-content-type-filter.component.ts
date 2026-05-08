import { patchState, signalState } from '@ngrx/signals';
import { EMPTY, of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    computed,
    inject,
    linkedSignal,
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
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DEBOUNCE_TIME,
    MAP_BASE_TYPES_TO_NUMBERS,
    MAP_NUMBERS_TO_BASE_TYPES
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

const ALL_CONTENT = '__ALL_CONTENT__';
const ITEMS_PER_PAGE = 10;

/**
 * Row height (px) used by the right column's virtual scroller.
 * Empirically measured against PrimeNG v21 listbox option default styling
 * (`--p-listbox-option-padding: 0 1rem` from CHIP_FILTER_LISTBOX_PT, plus the
 * `dot-filter-list-item` `py-3` host class). If a future PrimeNG / theme
 * upgrade changes the option padding or font, this number needs to be
 * re-measured or the scroller will misalign.
 */
const LISTBOX_ITEM_HEIGHT = 40.6;
/** Left listbox viewport height — fits all 9 base-type rows (incl. ALL_CONTENT). */
const LISTBOX_SCROLL_HEIGHT = `${9 * LISTBOX_ITEM_HEIGHT + 14}px`;
/** Approximate column header height (px-4 py-3 with text-xs uppercase). */
const POPOVER_HEADER_HEIGHT = '3rem';
/**
 * Popover height is derived from the LEFT listbox so the popover always
 * matches the natural height of the base-type list — no leftover space at
 * the bottom and no clipping when the catalog grows.
 */
const POPOVER_MAX_HEIGHT = `calc(${LISTBOX_SCROLL_HEIGHT} + ${POPOVER_HEADER_HEIGHT})`;

interface BaseTypeOption {
    name: string;
    label: string;
}

interface State {
    baseTypes: BaseTypeOption[];
    contentTypes: DotCMSContentType[];
    contentTypeFilter: string;
    loading: boolean;
    canLoadMore: boolean;
    currentPage: number;
}

@Component({
    selector: 'dot-content-drive-content-type-filter',
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
    templateUrl: './dot-content-drive-content-type-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveContentTypeFilterComponent implements OnInit {
    readonly #store = inject(DotContentDriveStore);
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
    protected readonly ALL_CONTENT = ALL_CONTENT;
    protected readonly ITEMS_PER_PAGE = ITEMS_PER_PAGE;
    protected readonly LISTBOX_ITEM_HEIGHT = LISTBOX_ITEM_HEIGHT;
    protected readonly POPOVER_MAX_HEIGHT = POPOVER_MAX_HEIGHT;

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

    /** Selected base types (variable names like 'CONTENT', 'FILEASSET'). */
    readonly $selectedBaseTypes = linkedSignal<string[]>(() => {
        const keys = (this.#store.getFilterValue('baseType') as string[]) ?? [];
        return keys.map((k) => MAP_NUMBERS_TO_BASE_TYPES[Number(k)]).filter(Boolean);
    });

    /**
     * Selected content types. Derived from the store + cache so selections
     * persist across focus changes — the cache holds every content type we
     * have ever loaded.
     */
    readonly $selectedContentTypes = linkedSignal<DotCMSContentType[]>(() => {
        const variables = (this.#store.getFilterValue('contentType') as string[]) ?? [];
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

    protected readonly LISTBOX_SCROLL_HEIGHT = LISTBOX_SCROLL_HEIGHT;
    /** Banner height matches a single listbox item slot for visual consistency. */
    protected readonly LISTBOX_BANNER_HEIGHT_PX = `${LISTBOX_ITEM_HEIGHT}px`;

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
     * Right listbox shrinks by one item slot when the "all content types"
     * banner is visible, so the popover height stays constant — the banner
     * takes over the bottom row's space instead of growing the popover.
     */
    protected readonly $rightScrollHeight = computed(() => {
        const items = this.$showAllBanner() ? 7 : 8;
        return `${items * LISTBOX_ITEM_HEIGHT + 14}px`;
    });

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
        } else {
            this.$selectedBaseTypes.update((list) => [...list, name]);
        }
        this.#syncStore();
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

        this.#syncStore();
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
        patchState(this.$state, { contentTypeFilter: '' });
        // $focusedBaseType is intentionally NOT reset — the user's last focus
        // persists across popover sessions so reopening lands them where they
        // left off. The @if ($popoverOpen()) recreate trick re-triggers the
        // listbox's lazy load on next open, so the data is still fresh.
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
        this.#syncStore();
    }

    #syncStore(): void {
        const baseTypes = this.$selectedBaseTypes();
        const contentTypes = this.$selectedContentTypes() ?? [];

        if (baseTypes.length) {
            const keys = baseTypes
                .map((name) => MAP_BASE_TYPES_TO_NUMBERS[name as DotCMSBaseTypesContentTypes])
                .filter((k): k is string => !!k);
            this.#store.patchFilters({ baseType: keys });
        } else {
            this.#store.removeFilter('baseType');
        }

        if (contentTypes.length) {
            this.#store.patchFilters({
                contentType: contentTypes.map((ct) => ct.variable)
            });
        } else {
            this.#store.removeFilter('contentType');
        }
    }

    #loadBaseTypes(): void {
        this.#contentTypesService
            .getAllContentTypes()
            .pipe(
                take(1),
                map((response: StructureTypeView[]) =>
                    response.filter((item) => item.name !== DotCMSBaseTypesContentTypes.FORM)
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
        // `undefined` even when the store has selected content types from a
        // restored URL. Read the variables straight from the store so the
        // first fetch can ensure those items appear on page 1 (and seed the
        // cache for $selectedContentTypes to resolve them).
        const variables = (this.#store.getFilterValue('contentType') as string[]) ?? [];
        const ensure = variables.length ? variables.join(',') : undefined;
        // `loading` is already true from initial state; no pre-fetch tap needed.
        this.#contentTypesService
            .getContentTypesWithPagination({
                ensure,
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
                // still need to resolve in $selectedContentTypes so #syncStore
                // doesn't drop a URL-restored filter on first user interaction.
                this.#cacheContentTypes(contentTypes);
            });
    }

    #setupFilterSubscription(): void {
        this.#fetchSubject
            .pipe(
                tap(() => patchState(this.$state, { loading: true })),
                debounceTime(DEBOUNCE_TIME),
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
                type,
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
        return contentTypes.filter(
            (ct) => !ct.system && ct.baseType !== DotCMSBaseTypesContentTypes.FORM
        );
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

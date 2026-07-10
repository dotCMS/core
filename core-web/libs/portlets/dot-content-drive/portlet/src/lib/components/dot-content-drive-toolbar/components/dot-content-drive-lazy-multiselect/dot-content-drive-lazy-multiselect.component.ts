import { patchState, signalState } from '@ngrx/signals';
import { EMPTY, Observable, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    input,
    linkedSignal,
    OnInit,
    output
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { ListboxModule } from 'primeng/listbox';
import { ScrollerLazyLoadEvent } from 'primeng/scroller';

import { catchError, debounceTime, take, takeUntil } from 'rxjs/operators';

import {
    CHIP_FILTER_LISTBOX_PT,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DEBOUNCE_TIME, PANEL_SCROLL_HEIGHT } from '../../../../shared/constants';

export interface DotLazyMultiselectOption {
    label: string;
    value: string;
}

/** One page of options plus whether more remain, returned by the loader. */
export interface DotLazyMultiselectPage {
    options: DotLazyMultiselectOption[];
    hasMore: boolean;
}

/** Loads a page (1-based) of options filtered by `filter`. Owned by the consumer (Tag/Category). */
export type DotLazyMultiselectLoader = (params: {
    page: number;
    perPage: number;
    filter: string;
}) => Observable<DotLazyMultiselectPage>;

/**
 * Row height (px) for the virtual scroller — matches the content-type filter's listbox, measured
 * against PrimeNG v21 option styling (`--p-listbox-option-padding: 0 1rem` from
 * CHIP_FILTER_LISTBOX_PT plus the `dot-filter-list-item` `py-3` host class).
 */
const ITEM_HEIGHT = 40.6;
/** Page size requested from the loader. */
const PER_PAGE = 20;

interface State {
    options: DotLazyMultiselectOption[];
    loading: boolean;
    filter: string;
    canLoadMore: boolean;
    currentPage: number;
}

/**
 * Presentational multi-select with server-side search + infinite scroll. It owns only the option
 * list, pagination and search debounce; the caller supplies a `loadPage` and the currently selected
 * values, and receives the chosen options via `(selectionChange)`. Reused by the Tag and Category
 * field filters so neither is capped at a fixed page size.
 *
 * Mirrors the content-type filter's virtual-scroll lazy load. It must be created only once its
 * host overlay is visible (the caller gates it behind the popover's open state) — otherwise the
 * virtual scroller measures a zero-height viewport and renders an empty list.
 */
@Component({
    selector: 'dot-content-drive-lazy-multiselect',
    imports: [
        FormsModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        ListboxModule,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-content-drive-lazy-multiselect.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveLazyMultiselectComponent implements OnInit {
    readonly #destroyRef = inject(DestroyRef);

    /** Loads a page of options; provided by the consumer (bound to the Tag/Category service). */
    readonly $loadPage = input.required<DotLazyMultiselectLoader>({ alias: 'loadPage' });
    /** Currently-selected values, used to highlight rows. */
    readonly $selectedValues = input<string[]>([], { alias: 'selectedValues' });
    /** Emits the selected options (value + label) whenever the selection changes. */
    readonly selectionChange = output<DotLazyMultiselectOption[]>();

    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;
    protected readonly ITEM_HEIGHT = ITEM_HEIGHT;

    readonly $state = signalState<State>({
        options: [],
        loading: false,
        filter: '',
        canLoadMore: true,
        currentPage: 1
    });

    /** Selected values bound to the listbox; re-seeds when the input changes. */
    protected readonly $model = linkedSignal<string[]>(() => [...this.$selectedValues()]);

    /** Cancels an in-flight load when a newer search supersedes it. */
    readonly #cancel$ = new Subject<void>();
    readonly #search$ = new Subject<string>();

    /**
     * Accumulated value → label across every page loaded this session. `onChange` resolves labels
     * from here rather than the current page only, so a value selected earlier keeps its label
     * after a search reset or paging (otherwise it would emit `label = value`, which for Category
     * is the raw inode and would overwrite the good cached label upstream).
     */
    readonly #labelByValue = new Map<string, string>();

    constructor() {
        this.#search$
            .pipe(debounceTime(DEBOUNCE_TIME), takeUntilDestroyed(this.#destroyRef))
            .subscribe((filter) => {
                this.#cancel$.next();
                patchState(this.$state, {
                    filter,
                    currentPage: 1,
                    canLoadMore: true,
                    options: []
                });
                this.#load();
            });
    }

    ngOnInit(): void {
        this.#load();
    }

    protected onSearch(value: string): void {
        this.#search$.next(value ?? '');
    }

    protected onLazyLoad(event: ScrollerLazyLoadEvent): void {
        const last = typeof event.last === 'number' ? event.last : NaN;
        if (!Number.isFinite(last)) {
            return;
        }

        // Prefetch the next page as soon as the user reaches any row on the current one.
        const nextPage = Math.ceil(last / PER_PAGE) + 1;
        if (
            !this.$state.canLoadMore() ||
            nextPage <= this.$state.currentPage() ||
            this.$state.loading()
        ) {
            return;
        }

        patchState(this.$state, { currentPage: nextPage });
        this.#load(true);
    }

    protected onChange(values: string[]): void {
        this.selectionChange.emit(
            (values ?? []).map((value) => ({
                label: this.#labelByValue.get(value) ?? value,
                value
            }))
        );
    }

    #load(append = false): void {
        patchState(this.$state, { loading: true });
        this.$loadPage()({
            page: this.$state.currentPage(),
            perPage: PER_PAGE,
            filter: this.$state.filter()
        })
            .pipe(
                take(1),
                takeUntil(this.#cancel$),
                // A failed page must not leave the list spinning forever; stop loading and stop
                // paging so the empty state shows instead of a permanent spinner.
                catchError(() => {
                    patchState(this.$state, { loading: false, canLoadMore: false });

                    return EMPTY;
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ options, hasMore }) => {
                for (const option of options) {
                    this.#labelByValue.set(option.value, option.label);
                }

                patchState(this.$state, {
                    options: append ? [...this.$state.options(), ...options] : options,
                    canLoadMore: hasMore,
                    loading: false
                });
            });
    }
}

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

import { DotMessagePipe } from '../../../../../dot-message/dot-message.pipe';
import { LISTBOX_OPTION_HEIGHT } from '../../../../../theme/theme.config';
import { DotFilterListItemComponent } from '../../../../dot-filter-list-item/dot-filter-list-item.component';
import { FIELD_FILTER_DEBOUNCE_TIME, FIELD_FILTER_PANEL_SCROLL_HEIGHT } from '../constants';

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
 * Row height (px) for the virtual scroller. Taken from the theme, which fixes every listbox
 * option to the same height, so this can never drift from what is actually rendered.
 */
const ITEM_HEIGHT = LISTBOX_OPTION_HEIGHT;
/** Page size requested from the loader. */
const PER_PAGE = 20;

interface State {
    options: DotLazyMultiselectOption[];
    loading: boolean;
    error: boolean;
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
    selector: 'dot-lazy-multiselect',
    imports: [
        FormsModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        ListboxModule,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-lazy-multiselect.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLazyMultiselectComponent implements OnInit {
    readonly #destroyRef = inject(DestroyRef);

    /** Loads a page of options; provided by the consumer (bound to the Tag/Category service). */
    readonly $loadPage = input.required<DotLazyMultiselectLoader>({ alias: 'loadPage' });
    /** Currently-selected values, used to highlight rows. */
    readonly $selectedValues = input<string[]>([], { alias: 'selectedValues' });
    /** Emits the selected options (value + label) whenever the selection changes. */
    readonly selectionChange = output<DotLazyMultiselectOption[]>();
    protected readonly SCROLL_HEIGHT = FIELD_FILTER_PANEL_SCROLL_HEIGHT;
    protected readonly ITEM_HEIGHT = ITEM_HEIGHT;

    readonly $state = signalState<State>({
        options: [],
        loading: false,
        error: false,
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
            .pipe(debounceTime(FIELD_FILTER_DEBOUNCE_TIME), takeUntilDestroyed(this.#destroyRef))
            .subscribe((filter) => {
                this.#cancel$.next();
                patchState(this.$state, {
                    filter,
                    currentPage: 1,
                    canLoadMore: true,
                    error: false,
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
        patchState(this.$state, { loading: true, error: false });
        this.$loadPage()({
            page: this.$state.currentPage(),
            perPage: PER_PAGE,
            filter: this.$state.filter()
        })
            .pipe(
                take(1),
                takeUntil(this.#cancel$),
                // A failed page must not leave the list spinning forever; stop loading and paging
                // and flag the error so the panel shows a distinct "failed" state rather than a
                // silent "no results" that looks like an empty search.
                catchError(() => {
                    patchState(this.$state, {
                        loading: false,
                        canLoadMore: false,
                        error: true
                    });

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

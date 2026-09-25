import { patchState, signalState } from '@ngrx/signals';
import { EMPTY, Observable, Subject, timer } from 'rxjs';

import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    input,
    linkedSignal,
    OnInit,
    output,
    TemplateRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { ListboxModule } from 'primeng/listbox';
import { ScrollerLazyLoadEvent } from 'primeng/scroller';

import { catchError, debounce, take, takeUntil } from 'rxjs/operators';

import { DotMessagePipe } from '../../../../../dot-message/dot-message.pipe';
import { LISTBOX_OPTION_HEIGHT } from '../../../../../theme/theme.config';
import { DotFilterListItemComponent } from '../../../../dot-filter-list-item/dot-filter-list-item.component';
import { FIELD_FILTER_DEBOUNCE_TIME, FIELD_FILTER_PANEL_SCROLL_HEIGHT } from '../constants';

export interface DotLazyMultiselectOption<T = unknown> {
    label: string;
    value: string;
    /**
     * Whatever the consumer wants a custom row template to reach — the full record behind the
     * option, say. Never read by this component.
     */
    data?: T;
}

/** Context handed to a custom `itemTemplate`: the option being rendered. */
export interface DotLazyMultiselectItemContext<T = unknown> {
    $implicit: DotLazyMultiselectOption<T>;
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
        NgTemplateOutlet,
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
    /**
     * Emits the selected options (value + label) whenever the selection changes. Always an array,
     * so a single-select consumer reads the one entry rather than a different type.
     */
    readonly selectionChange = output<DotLazyMultiselectOption[]>();

    /**
     * Whether several options can be ticked at once. Defaults to `true`, which is what every
     * filter built on this list wants; `false` turns it into a plain pick-one list with no
     * checkboxes.
     */
    readonly $multiple = input<boolean>(true, { alias: 'multiple' });

    /**
     * Replaces the default one-line row. Receives the option as `$implicit`; put the full record
     * in the option's `data` to render more than the label.
     *
     * A taller row must come with a matching {@link $itemSize}: the virtual scroller positions
     * rows by that number and cannot measure them.
     */
    readonly $itemTemplate = input<TemplateRef<DotLazyMultiselectItemContext> | null>(null, {
        alias: 'itemTemplate'
    });

    /** Rendered row height in px, for the virtual scroller. Defaults to the theme's listbox row. */
    readonly $itemSize = input<number>(LISTBOX_OPTION_HEIGHT, { alias: 'itemSize' });

    /** Message key for the search box placeholder and accessible name. */
    readonly $searchPlaceholderKey = input<string>('search', { alias: 'searchPlaceholderKey' });

    /**
     * Idle time before a typed term is searched, in ms. Defaults to the field-filter value, so
     * every existing consumer keeps the timing it has.
     *
     * Overridable because the pause belongs to the screen, not to this component: a consumer whose
     * own search box debounces differently would otherwise have two controls on one screen
     * reacting at different speeds (#37307 FR-013).
     */
    readonly $debounceMs = input<number>(FIELD_FILTER_DEBOUNCE_TIME, { alias: 'debounceMs' });

    /**
     * Message key shown when the search matched nothing. Defaults to the field-filter copy.
     *
     * Overridable because the default says "No searchable fields available", which is true of the
     * Tag and Category filters and wrong for anything else paging through this list — a directory
     * of people, say (#37307 FR-014).
     */
    readonly $emptyKey = input<string>('content-drive.field-filter.more.empty', {
        alias: 'emptyKey'
    });

    /** Message key shown when a page failed to load. Same reasoning as {@link $emptyKey}. */
    readonly $errorKey = input<string>('content-drive.field-filter.more.error', {
        alias: 'errorKey'
    });

    /**
     * A page failed to load.
     *
     * Optional for consumers — the panel already shows its own failed state, so ignoring this
     * costs the inline message and nothing else. A consumer that has somewhere to report errors
     * should route it there.
     */
    readonly loadFailed = output<HttpErrorResponse>();
    protected readonly SCROLL_HEIGHT = FIELD_FILTER_PANEL_SCROLL_HEIGHT;

    readonly $state = signalState<State>({
        options: [],
        loading: false,
        error: false,
        filter: '',
        canLoadMore: true,
        currentPage: 1
    });

    /**
     * Selected values bound to the listbox; re-seeds when the input changes. An array in multiple
     * mode and a single value otherwise, which is what `p-listbox` expects for each.
     */
    protected readonly $model = linkedSignal<string[] | string | null>(() =>
        this.$multiple() ? [...this.$selectedValues()] : (this.$selectedValues()[0] ?? null)
    );

    /** Cancels an in-flight load when a newer search supersedes it. */
    readonly #cancel$ = new Subject<void>();
    readonly #search$ = new Subject<string>();

    /**
     * Accumulated value → option across every page loaded this session. `onChange` resolves picks
     * from here rather than the current page only, so a value selected earlier keeps its label
     * (and its `data`) after a search reset or paging — otherwise it would emit `label = value`,
     * which for Category is the raw inode and would overwrite the good cached label upstream.
     */
    readonly #optionByValue = new Map<string, DotLazyMultiselectOption>();

    constructor() {
        this.#search$
            .pipe(
                // Read per emission, not once here. Signal inputs still hold their defaults while
                // the constructor runs — Angular binds them afterwards — and `debounceTime`
                // captures its argument when the pipe is built, so `debounceTime($debounceMs())`
                // pinned every consumer to the default however it was bound.
                debounce(() => timer(this.$debounceMs())),
                takeUntilDestroyed(this.#destroyRef)
            )
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

    protected onChange(selection: string[] | string | null): void {
        const values = Array.isArray(selection) ? selection : selection ? [selection] : [];

        this.selectionChange.emit(
            // The whole option, not just its label: a consumer that put its record in `data`
            // needs it back on the pick.
            values.map((value) => {
                const known = this.#optionByValue.get(value);

                return known ? { ...known } : { label: value, value };
            })
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
                catchError((error: HttpErrorResponse) => {
                    patchState(this.$state, {
                        loading: false,
                        canLoadMore: false,
                        error: true
                    });

                    // Also reported outwards. The inline state tells the person looking at the
                    // panel that this list failed; it does not tell the surface, which is the only
                    // thing that can route a 403 or a dead session to wherever that surface shows
                    // errors. This component cannot do that itself — `DotHttpErrorManagerService`
                    // transitively needs `Router` and `DotEventsSocket`, which the legacy Dojo
                    // host `@dotcms/ui` is bundled into does not have. Same reasoning, and the
                    // same shape, as `dot-field-filter-menu`'s `error` output.
                    this.loadFailed.emit(error);

                    return EMPTY;
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ options, hasMore }) => {
                for (const option of options) {
                    this.#optionByValue.set(option.value, option);
                }

                patchState(this.$state, {
                    options: append ? [...this.$state.options(), ...options] : options,
                    canLoadMore: hasMore,
                    loading: false
                });
            });
    }
}

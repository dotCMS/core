import { computed, effect, inject, input, output, signal, untracked, Component } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { PopoverModule } from 'primeng/popover';

import { map } from 'rxjs/operators';

import { DotUserSearchService, dotUserDisplayName } from '@dotcms/data-access';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotChipFilterComponent } from '../dot-chip-filter/dot-chip-filter.component';
import {
    DotLazyMultiselectComponent,
    DotLazyMultiselectLoader,
    DotLazyMultiselectOption
} from '../dot-filter-bar/chips/dot-field-filter/dot-lazy-multiselect/dot-lazy-multiselect.component';

/** Idle time before a typed term is searched. Matches the listing search boxes it sits beside. */
const SEARCH_DEBOUNCE_MS = 300;

/**
 * Chip that narrows a listing to one or more people from the dotCMS user directory.
 *
 * **It owns the catalogue; the consumer owns the selection.** Only user ids cross that boundary —
 * `selected` in, `selectionChange` out — because the selection is the consumer's view state, while
 * the directory is a paged resource with a lifecycle of its own that no screen wants to hold.
 *
 * Specific in what it picks, generic in what it says: the copy is handed in, because "Created By"
 * on an experiments list and "Owner" on a content list are the same control asking a different
 * question.
 *
 * Paging, virtual scrolling, search debouncing and cancelling a superseded load are **not** here:
 * {@link DotLazyMultiselectComponent} already solves them. This supplies the loader and the labels.
 */
@Component({
    selector: 'dot-user-filter',
    imports: [PopoverModule, DotChipFilterComponent, DotLazyMultiselectComponent, DotMessagePipe],
    templateUrl: './dot-user-filter.component.html'
})
export class DotUserFilterComponent {
    readonly #search = inject(DotUserSearchService);

    /** Chip label, already translated — what this filter is asking, e.g. "Created By". */
    readonly $title = input.required<string>({ alias: 'title' });

    /** What the chip reads while nothing is selected, already translated, e.g. "All". */
    readonly $emptyLabel = input.required<string>({ alias: 'emptyLabel' });

    /** Applied user ids. Owned by the consumer, usually backed by its address. */
    readonly $selected = input<string[]>([], { alias: 'selected' });

    /** Message key shown when a search matches nobody. */
    readonly $emptyKey = input<string>('users.filter.empty', { alias: 'emptyKey' });

    /** Message key shown when the directory cannot be read. */
    readonly $errorKey = input<string>('users.filter.error', { alias: 'errorKey' });

    /** Emits the selected ids on every toggle and on clear. */
    readonly selectionChange = output<string[]>();

    protected readonly DEBOUNCE_MS = SEARCH_DEBOUNCE_MS;

    /** Names for ids the chip has been asked to display, filled as they resolve. */
    readonly #namesById = signal<Record<string, string>>({});

    /**
     * Ids the chip has been asked to show but cannot yet name, or `undefined` when there are none.
     *
     * `undefined` rather than an empty array on purpose: that is what tells the resource below
     * there is nothing to fetch, so a selection made by clicking — which arrives already
     * labelled — never triggers a request.
     */
    readonly #unresolvedIds = computed<string[] | undefined>(() => {
        const namesById = this.#namesById();
        const unresolved = this.$selected().filter((userId) => !namesById[userId]);

        return unresolved.length ? unresolved : undefined;
    });

    /**
     * Turns ids that arrived from the consumer's address into names.
     *
     * A resource rather than an effect around `subscribe`, for two reasons that are correctness
     * rather than taste. It **cancels** a resolution the moment the selection changes, where two
     * overlapping subscriptions would race and let the older answer win; and its teardown is tied
     * to the component, where a bare `subscribe` would outlive it if the chip were destroyed
     * mid-flight.
     *
     * The accumulator above stays: a resource holds only its latest value, and a name once
     * resolved should survive the next selection rather than be fetched again.
     */
    readonly #resolved = rxResource({
        params: () => this.#unresolvedIds(),
        stream: ({ params }) => this.#search.resolveNames(params)
    });

    /** True while ids from the address are still being turned into names. */
    readonly $resolving = computed<boolean>(() => this.#resolved.isLoading());

    /**
     * Bound to the option list so a selected person stays ticked.
     *
     * Straight off the input rather than held locally: the consumer is the single writer, so a
     * selection restored from an address or cleared from an empty state lands here too.
     */
    readonly $selectedValues = computed<string[]>(() => this.$selected());

    /**
     * What the chip renders after its title, and what makes it read as active.
     *
     * Falls back to the id for anything not yet resolved, so the chip is never blank and a failed
     * resolution costs a name rather than the filter.
     */
    protected readonly $selectedLabels = computed<string[]>(() => {
        const namesById = this.#namesById();

        return this.$selected().map((userId) => namesById[userId] ?? userId);
    });

    /**
     * Loads a page of the directory and reports whether another remains.
     *
     * `hasMore` comes from the response total rather than from "a full page came back": the latter
     * asks for one extra empty page whenever the total is an exact multiple of the page size.
     *
     * Public because it is this component's whole contract with the option list, which is handed
     * it as `[loadPage]` and calls it once per page it wants. What that list then does — scrolling,
     * counting pages, cancelling a superseded load — belongs to the shared component and is tested
     * there, not here.
     *
     * A plain property rather than a `computed`: it reads no signal, so there is nothing for a
     * computed to recompute, and a `$` name would claim a reactivity it does not have.
     */
    readonly loadPage: DotLazyMultiselectLoader = ({ page, perPage, filter }) =>
        this.#search.searchPage({ page, perPage, filter }).pipe(
            map((response) => ({
                options: (response?.entity ?? []).map((row) => ({
                    // The id is the value because that is what a consumer narrows on and what an
                    // address carries; the name is only ever displayed.
                    value: row.userId,
                    label: dotUserDisplayName(row)
                })),
                hasMore: page * perPage < (response?.pagination?.totalEntries ?? 0)
            }))
        );

    constructor() {
        /**
         * Folds each resolution into the accumulator.
         *
         * The resource decides *when* to fetch and what to cancel; this only records what came
         * back. Writing to a signal read by the same resource's params would loop, hence the
         * `untracked` — the guard is that a name just recorded leaves `#unresolvedIds` smaller,
         * never larger, so the sequence terminates.
         */
        effect(() => {
            const resolved = this.#resolved.value();

            if (!resolved) {
                return;
            }

            untracked(() => this.#namesById.update((names) => ({ ...names, ...resolved })));
        });
    }

    /** Remembers the labels the list just showed, so a fresh pick never needs resolving. */
    onSelectionChange(options: DotLazyMultiselectOption[]): void {
        this.#namesById.update((names) => ({
            ...names,
            ...Object.fromEntries(options.map(({ value, label }) => [value, label]))
        }));

        this.selectionChange.emit(options.map(({ value }) => value));
    }

    onRemoveAll(): void {
        this.selectionChange.emit([]);
    }
}

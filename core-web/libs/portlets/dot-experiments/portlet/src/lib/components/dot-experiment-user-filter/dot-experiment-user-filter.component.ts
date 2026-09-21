import {
    Component,
    computed,
    effect,
    inject,
    input,
    output,
    signal,
    untracked
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { PopoverModule } from 'primeng/popover';

import { map } from 'rxjs/operators';

import { DotUserSearchService, dotUserDisplayName } from '@dotcms/data-access';
import {
    DotChipFilterComponent,
    DotLazyMultiselectComponent,
    DotLazyMultiselectLoader,
    DotLazyMultiselectOption
} from '@dotcms/ui';

import { SEARCH_DEBOUNCE_MS } from '../../shared/constants';

/**
 * Created By chip for the experiments list: pick one or more people and narrow the table to the
 * experiments they created (#37307).
 *
 * **It owns the catalogue; the parent owns the selection.** Only user ids cross the boundary —
 * `selected` in, `selectionChange` out — because the selection is view state that lives in the
 * address, while the directory is a paged resource with a lifecycle of its own that nothing else
 * on the screen needs. That is the opposite split from the Status and Goal chips, whose options
 * are derived from experiments the store already holds and are therefore handed in.
 *
 * Paging, cancellation and the stop at the last page are **not** here: they belong to the shared
 * lazy multi-select, which already solves them for the Tag and Category filters. This component
 * supplies the loader and nothing more.
 *
 * Kept local to the portlet deliberately. A shared "user picker chip" would be an abstraction over
 * a single consumer; promote it when a second one appears (a "last modified by" filter is the
 * likely first, per #37304).
 */
@Component({
    selector: 'dot-experiment-user-filter',
    imports: [PopoverModule, DotChipFilterComponent, DotLazyMultiselectComponent],
    templateUrl: './dot-experiment-user-filter.component.html'
})
export class DotExperimentUserFilterComponent {
    readonly #search = inject(DotUserSearchService);

    /** Chip label, already translated. */
    readonly $title = input.required<string>({ alias: 'title' });

    /** What the chip reads while nothing is selected — `All`, as Status and Goal do. */
    readonly $emptyLabel = input.required<string>({ alias: 'emptyLabel' });

    /** Applied user ids, owned by the parent's store and backed by the address. */
    readonly $selected = input<string[]>([], { alias: 'selected' });

    /** Message key for "the search matched nobody". */
    readonly $emptyKey = input<string>('experiments.list.filter.users.empty', {
        alias: 'emptyKey'
    });

    /** Message key for "the directory could not be read". */
    readonly $errorKey = input<string>('experiments.list.filter.users.error', {
        alias: 'errorKey'
    });

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
     * Turns ids that arrived from the address into names.
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
     * Straight off the input rather than held locally: the parent is the single writer, so a
     * selection restored from the address or cleared from the empty state lands here too.
     */
    readonly $selectedValues = computed<string[]>(() => this.$selected());

    /**
     * What the chip renders after its title, and what makes it read as active.
     *
     * Falls back to the id for anything not yet resolved, so the chip is never blank and a failed
     * resolution costs a name rather than the filter (FR-009f).
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
     * Public because it is this component's whole contract with the option list. What that list
     * then does with it — scrolling, counting pages, cancelling a superseded load — belongs to the
     * shared component and is tested there, not here.
     */
    readonly $loadPage = computed<DotLazyMultiselectLoader>(
        () =>
            ({ page, perPage, filter }) =>
                this.#search.searchPage({ page, perPage, filter }).pipe(
                    map((response) => ({
                        options: (response?.entity ?? []).map((row) => ({
                            // The id is the value because that is what narrowing compares and what
                            // the address carries; the name is only ever displayed.
                            value: row.userId,
                            label: dotUserDisplayName(row)
                        })),
                        hasMore: page * perPage < (response?.pagination?.totalEntries ?? 0)
                    }))
                )
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

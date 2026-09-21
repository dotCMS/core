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

    /** True while ids that arrived from the address are being turned into names. */
    readonly $resolving = signal(false);

    /** Names for ids the chip has been asked to display, filled as they resolve. */
    readonly #namesById = signal<Record<string, string>>({});

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
         * Resolves ids the chip cannot yet name.
         *
         * Only ids arriving from the address need this: a person picked from the list came with a
         * label. Hitting the service for a name already known would be a request per reload for
         * nothing, so the service's cache — seeded by the very pages this chip loads — usually
         * answers without going out at all.
         */
        effect(() => {
            const unresolved = this.$selected().filter(
                (userId) => !untracked(this.#namesById)[userId]
            );

            if (!unresolved.length) {
                return;
            }

            untracked(() => {
                this.$resolving.set(true);
                this.#search.resolveNames(unresolved).subscribe({
                    next: (resolved) => {
                        this.#namesById.update((names) => ({ ...names, ...resolved }));
                        this.$resolving.set(false);
                    },
                    // The service already falls back to the id per entry, so reaching here means
                    // something unexpected. Stop saying "working" either way.
                    error: () => this.$resolving.set(false)
                });
            });
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

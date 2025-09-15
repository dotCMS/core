import { patchState, signalState } from '@ngrx/signals';
import { of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    DestroyRef,
    OnInit,
    signal,
    computed,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { MultiSelectFilterEvent, MultiSelectModule } from 'primeng/multiselect';
import { SkeletonModule } from 'primeng/skeleton';

import { catchError, debounceTime, skip, switchMap, tap } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentType,
    DotPagination
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DEBOUNCE_TIME,
    MAP_NUMBERS_TO_BASE_TYPES,
    PANEL_SCROLL_HEIGHT
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

type DotContentDriveContentTypeFieldState = {
    filter: string;
    loading: boolean;
    canLoadMore: boolean;
    contentTypes: DotCMSContentType[];
};

@Component({
    selector: 'dot-content-drive-content-type-field',
    imports: [MultiSelectModule, FormsModule, SkeletonModule, DotMessagePipe],
    templateUrl: './dot-content-drive-content-type-field.component.html',
    styleUrl: './dot-content-drive-content-type-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveContentTypeFieldComponent implements OnInit {
    readonly #store = inject(DotContentDriveStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly #contentTypesService = inject(DotContentTypeService);
    readonly #searchSubject = new Subject<{ type?: string; filter: string }>();

    protected readonly MULTISELECT_SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;

    readonly $state = signalState<DotContentDriveContentTypeFieldState>({
        filter: '',
        loading: true,
        canLoadMore: true,
        contentTypes: []
    });

    readonly $selectedContentTypes = signal<DotCMSContentType[]>([]);

    // We need to map the numbers to the base types, ticket: https://github.com/dotCMS/core/issues/32991
    // This prevents the effect from being triggered when the base types are the same or filters changes
    private readonly $mappedBaseTypes = computed<string>(
        () => {
            const baseTypesString =
                this.#store
                    .filters()
                    .baseType?.map((item) => MAP_NUMBERS_TO_BASE_TYPES[item])
                    .join(',') ?? '';

            return baseTypesString.length > 0 ? baseTypesString : undefined;
        },
        {
            // This will trigger the effect if the base types are different
            equal: (a, b) => a?.length === b?.length && a === b
        }
    );

    readonly getContentTypesEffect = effect(() => {
        const type = this.$mappedBaseTypes();

        const filter = this.$state.filter();

        // Filter the selected content types based on the base types, if there are no base types, all content types are shown
        if (type?.length) {
            untracked(() => {
                this.$selectedContentTypes.update((selectedContentTypes) =>
                    selectedContentTypes.filter(({ baseType }) =>
                        type.split(',').includes(baseType)
                    )
                );
                this.onChange(); // Trigger a manual change to update the store
            });
        }

        // Push the request parameters to the debounced stream
        this.#searchSubject.next({ type, filter });
    });
    readonly fechingItems = computed(() => {
        return this.$state.loading() && this.$state.canLoadMore();
    });

    ngOnInit() {
        this.loadInitialContentTypes();
        this.setupFilterSubscription();
    }

    /**
     * Handles filter input changes from the multiselect component.
     *
     * Updates the filter state which triggers the reactive effect to fetch
     * filtered content types with debouncing.
     *
     * @param {MultiSelectFilterEvent} event - The filter event containing the search term
     * @protected
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    protected onFilter({ filter }: MultiSelectFilterEvent) {
        this.updateState({ filter });
    }

    /**
     * Handles selection changes in the multiselect component.
     *
     * Updates the store filters based on selected content types or removes
     * the filter if no content types are selected.
     *
     * @protected
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    protected onChange() {
        const value = this.$selectedContentTypes();

        if (value?.length) {
            this.#store.patchFilters({
                contentType: value.map((item) => item.variable)
            });
        } else {
            this.#store.removeFilter('contentType');
        }
    }

    /**
     * Resets the filter when the multiselect panel is hidden.
     *
     * Clears the search filter to ensure a clean state when the user
     * reopens the multiselect dropdown.
     *
     * @protected
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    protected onPanelHide() {
        this.updateState({ filter: '' });
    }

    /**
     * Utility method to update the component state.
     *
     * Provides a centralized way to patch the component state with type safety.
     *
     * @param {Partial<DotContentDriveContentTypeFieldState>} state - Partial state to update
     * @private
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    private updateState(state: Partial<DotContentDriveContentTypeFieldState>) {
        patchState(this.$state, state);
    }

    /**
     * Filters and deduplicates content types by:
     * - Removing duplicates based on ID
     * - Excluding system content types
     * - Excluding form content types
     *
     * @param contentTypes - Array of content types to process
     * @returns Filtered and deduplicated array of content types
     */
    private filterAndDeduplicateContentTypes(
        contentTypes: DotCMSContentType[]
    ): DotCMSContentType[] {
        const uniqueIds = new Set<string>();

        return contentTypes.filter((contentType) => {
            // Skip if already seen, is system type, or is form type
            if (
                uniqueIds.has(contentType.variable) ||
                contentType.system ||
                contentType.baseType === DotCMSBaseTypesContentTypes.FORM
            ) {
                return false;
            }

            uniqueIds.add(contentType.variable);
            return true;
        });
    }

    /**
     * Loads the initial set of content types on component initialization.
     *
     * This method:
     * - Fetches all available content types without any filter
     * - Processes URL query parameters to pre-select content types
     * - Sets the initial state for both available and selected content types
     * - Executes immediately without debounce for fast initial load
     *
     * @private
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    private loadInitialContentTypes() {
        this.#contentTypesService
            .getContentTypesWithPagination({ filter: '', type: this.$mappedBaseTypes() })
            .pipe(
                tap(() => this.updateState({ loading: true })),
                catchError(() =>
                    of({
                        contentTypes: [],
                        pagination: {}
                    })
                )
            )
            .subscribe(
                ({
                    contentTypes,
                    pagination
                }: {
                    contentTypes: DotCMSContentType[];
                    pagination: DotPagination;
                }) => {
                    const dotCMSContentTypes = this.filterAndDeduplicateContentTypes(contentTypes);
                    const storeVariables = this.getVariablesFromStore();
                    const canLoadMore = pagination.currentPage < pagination.totalEntries;
                    const selectedItems = dotCMSContentTypes.filter(({ variable }) =>
                        storeVariables.includes(variable)
                    );
                    this.updateState({
                        contentTypes: dotCMSContentTypes,
                        canLoadMore,
                        loading: false
                    });
                    this.$selectedContentTypes.set(selectedItems);
                }
            );
    }

    /**
     * Sets up the reactive subscription for handling user-initiated content type filtering.
     *
     * This method:
     * - Listens to filter changes triggered by user input in the multiselect component
     * - Applies debouncing to prevent excessive API calls during typing
     * - Skips the initial emission to avoid duplicate requests (initial load is handled separately)
     * - Merges filtered results with currently selected content types
     * - Updates the available content types list while preserving user selections
     *
     * @private
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    private setupFilterSubscription() {
        this.#searchSubject
            .pipe(
                skip(1), // Skip initial emission to avoid duplicate API call (handled by loadInitialContentTypes)
                tap(() => this.updateState({ loading: true })),
                debounceTime(DEBOUNCE_TIME),
                takeUntilDestroyed(this.#destroyRef),
                switchMap(({ filter, type }) =>
                    this.#contentTypesService
                        .getContentTypes({ filter, type })
                        .pipe(catchError(() => of([])))
                )
            )
            .subscribe((dotCMSContentTypes: DotCMSContentType[] = []) => {
                const selectedContentTypes = this.$selectedContentTypes();

                const allContentTypes = [
                    ...selectedContentTypes,
                    ...dotCMSContentTypes.sort((a, b) => a.variable.localeCompare(b.variable))
                ];
                const contentTypes = this.filterAndDeduplicateContentTypes(allContentTypes);

                this.updateState({ contentTypes, loading: false });
            });
    }

    /**
     * Retrieves the content type variables from the store.
     *
     * @returns The content type variables from the store.
     * @private
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    private getVariablesFromStore() {
        try {
            return (this.#store.getFilterValue('contentType') as string[]) ?? [];
        } catch (error) {
            console.warn('Error retrieving content type filters from store:', error);
            return [];
        }
    }
}

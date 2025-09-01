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
    computed
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

import { DEBOUNCE_TIME } from '../../../../shared/constants';
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

    readonly $state = signalState<DotContentDriveContentTypeFieldState>({
        filter: '',
        loading: true,
        canLoadMore: true,
        contentTypes: []
    });

    readonly $selectedContentTypes = signal<DotCMSContentType[]>([]);
    readonly getContentTypesEffect = effect(() => {
        const type = undefined;
        const filter = this.$state.filter();

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

        if (value.length) {
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
        const contentTypeVariables = (this.#store.getFilterValue('contentType') as string[]) ?? [];

        this.#contentTypesService
            .getContentTypesWithPagination({ filter: '' })
            .pipe(
                tap(() => this.updateState({ loading: true })),
                catchError(() => of([]))
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
                    const canLoadMore = pagination.currentPage < pagination.totalEntries;
                    const selectedItems = dotCMSContentTypes.filter(({ variable }) =>
                        contentTypeVariables.includes(variable)
                    );
                    this.updateState({ contentTypes, canLoadMore, loading: false });
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
                switchMap(({ filter }) =>
                    this.#contentTypesService
                        .getContentTypes({ filter })
                        .pipe(catchError(() => of([])))
                )
            )
            .subscribe((dotCMSContentTypes: DotCMSContentType[] = []) => {
                const selectedContentTypes = this.$selectedContentTypes();
                const allContentTypes = [...selectedContentTypes, ...dotCMSContentTypes];
                const contentTypes = this.filterAndDeduplicateContentTypes(allContentTypes);

                this.updateState({ contentTypes, loading: false });
            });
    }
}

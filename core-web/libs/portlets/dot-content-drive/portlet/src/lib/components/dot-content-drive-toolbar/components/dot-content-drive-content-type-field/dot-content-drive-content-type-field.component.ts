import { patchState, signalState } from '@ngrx/signals';
import { of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    DestroyRef,
    OnInit,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { MultiSelectFilterEvent, MultiSelectModule } from 'primeng/multiselect';
import { SkeletonModule } from 'primeng/skeleton';

import { catchError, debounceTime, switchMap, tap } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEBOUNCE_TIME } from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

type DotContentDriveContentTypeFieldState = {
    contentTypes: DotCMSContentType[];
    filterByKeyword: string;
    loading: boolean;
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
    readonly #contentTypesService = inject(DotContentTypeService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #apiRequestSubject = new Subject<{ type?: string; filter: string }>();
    readonly $state = signalState<DotContentDriveContentTypeFieldState>({
        contentTypes: [],
        filterByKeyword: '',
        loading: true
    });

    readonly $selectedContentTypes = signal<DotCMSContentType[]>([]);
    readonly getContentTypesEffect = effect(() => {
        // TODO: We need to improve this when this ticket is done: https://github.com/dotCMS/core/issues/32991
        // After that remove the uncommented code and add the line below
        // const type = this.$mappedBaseTypes().join(',')
        const type = undefined;
        const filter = this.$state.filterByKeyword();

        // Push the request parameters to the debounced stream
        this.#apiRequestSubject.next({ type, filter });
    });

    ngOnInit() {
        // Set up debounced API request stream with switchMap
        this.#apiRequestSubject
            .pipe(
                tap(() =>
                    patchState(this.$state, {
                        loading: true
                    })
                ),
                debounceTime(DEBOUNCE_TIME),
                takeUntilDestroyed(this.#destroyRef),
                switchMap(({ type, filter }) =>
                    this.#contentTypesService
                        .getContentTypes({ type, filter })
                        .pipe(catchError(() => of([])))
                )
            )
            .subscribe((dotCMSContentTypes: DotCMSContentType[] = []) => {
                const selectedContentTypes = this.$selectedContentTypes() ?? [];
                const allContentTypes = [...selectedContentTypes, ...dotCMSContentTypes];
                const contentTypes = this.filterAndDeduplicateContentTypes(allContentTypes);

                patchState(this.$state, {
                    contentTypes,
                    loading: false
                });
            });
    }

    /**
     * This function is used to filter the content types by keyword
     *
     * @param {MultiSelectFilterEvent} event
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    onFilter(event: MultiSelectFilterEvent) {
        patchState(this.$state, {
            filterByKeyword: event.filter
        });
    }

    /**
     * This function is used to handle the change event of the multiselect
     *
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    onChange() {
        const value = this.$selectedContentTypes() ?? [];

        if (value.length) {
            this.#store.patchFilters({
                contentType: value.map((item) => item.variable)
            });
        } else {
            this.#store.removeFilter('contentType');
        }
    }

    /**
     * Reset the filter by keyword when the multiselect is hidden
     *
     * @memberof DotContentDriveContentTypeFieldComponent
     */
    onHidePanel() {
        patchState(this.$state, {
            filterByKeyword: ''
        });
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
                uniqueIds.has(contentType.id) ||
                contentType.system ||
                contentType.baseType === DotCMSBaseTypesContentTypes.FORM
            ) {
                return false;
            }

            uniqueIds.add(contentType.id);
            return true;
        });
    }
}

import { patchState, signalState } from '@ngrx/signals';
import { of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
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

import { DEBOUNCE_TIME, MAP_NUMBERS_TO_BASE_TYPES } from '../../../../shared/constants';
import { DotContentDriveContentType } from '../../../../shared/models';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

type DotContentDriveContentTypeFieldState = {
    contentTypes: DotContentDriveContentType[];
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
    #store = inject(DotContentDriveStore);

    #contentTypesService = inject(DotContentTypeService);
    #destroyRef = inject(DestroyRef);

    readonly $state = signalState<DotContentDriveContentTypeFieldState>({
        contentTypes: [],
        filterByKeyword: '',
        loading: true
    });

    readonly $selectedContentTypes = signal<DotContentDriveContentType[]>([]);

    #apiRequestSubject = new Subject<{ type?: string; filter: string }>();

    // We need to map the numbers to the base types, ticket: https://github.com/dotCMS/core/issues/32991
    // This prevents the effect from being triggered when the base types are the same or filters changes
    private readonly $mappedBaseTypes = computed(
        () => this.#store.filters().baseType?.map((item) => MAP_NUMBERS_TO_BASE_TYPES[item]) ?? [],
        {
            // This will trigger the effect if the base types are different
            equal: (a, b) => a?.length === b?.length && a?.every((item) => b.includes(item))
        }
    );

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
            .subscribe((contentTypes: DotCMSContentType[]) => {
                const selectedContentTypes = this.$selectedContentTypes() ?? [];
                const contentTypeFilters = this.#store.getFilterValue('contentType');

                // Preserve the selected content types
                const allContentTypes = [...selectedContentTypes, ...(contentTypes ?? [])];

                // Remove duplicates
                const cleanedContentTypes = allContentTypes.reduce<DotContentDriveContentType[]>(
                    (acc, current) => {
                        const exists = acc.find((item) => item.id === current.id);

                        // We want to filter out forms and system types and also remove duplicates
                        if (
                            !exists &&
                            !current.system &&
                            current.baseType !== DotCMSBaseTypesContentTypes.FORM
                        ) {
                            // If the item is selected in the multiselect
                            // or is in the filters we have it in the store
                            // we want to set the selected property to true
                            const isSelected =
                                selectedContentTypes.some((item) => item.id === current.id) ||
                                contentTypeFilters?.includes(current.variable);

                            acc.push({
                                ...current,
                                selected: !!isSelected
                            });
                        }

                        return acc;
                    },
                    []
                );

                patchState(this.$state, {
                    contentTypes: cleanedContentTypes,
                    loading: false
                });

                this.$selectedContentTypes.set(cleanedContentTypes.filter((item) => item.selected));
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
}

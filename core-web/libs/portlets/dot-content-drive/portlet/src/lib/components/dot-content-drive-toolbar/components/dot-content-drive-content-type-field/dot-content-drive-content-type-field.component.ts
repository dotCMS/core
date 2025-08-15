import { patchState, signalState } from '@ngrx/signals';
import { of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { MultiSelectFilterEvent, MultiSelectModule } from 'primeng/multiselect';
import { SkeletonModule } from 'primeng/skeleton';

import { catchError, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEBOUNCE_TIME, MAP_NUMBERS_TO_BASE_TYPES } from '../../../../shared/constants';
import { BASE_TYPES } from '../../../../shared/models';
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
export class DotContentDriveContentTypeFieldComponent {
    #store = inject(DotContentDriveStore);

    #contentTypesService = inject(DotContentTypeService);

    readonly $state = signalState<DotContentDriveContentTypeFieldState>({
        contentTypes: [],
        filterByKeyword: '',
        loading: false
    });

    readonly $selectedContentTypes = signal<DotCMSContentType[]>([]);

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

    constructor() {
        // Set up debounced API request stream with switchMap
        this.#apiRequestSubject
            .pipe(
                tap(() => patchState(this.$state, { loading: true, contentTypes: [] })),
                debounceTime(DEBOUNCE_TIME),
                distinctUntilChanged(
                    (prev, curr) => prev.type === curr.type && prev.filter === curr.filter
                ),
                switchMap(({ type, filter }) =>
                    this.#contentTypesService
                        .getContentTypes({ type, filter })
                        .pipe(catchError(() => of([])))
                ),
                takeUntilDestroyed()
            )
            .subscribe((contentTypes: DotCMSContentType[]) => {
                const contentTypeFallback = contentTypes ?? [];

                patchState(this.$state, {
                    contentTypes: contentTypeFallback?.filter(
                        (item) => item.baseType !== BASE_TYPES.form && !item.system
                    ),
                    loading: false
                });

                const contentTypeFilters = this.#store.getFilterValue('contentType');

                this.$selectedContentTypes.set(
                    contentTypeFallback.filter((item) =>
                        contentTypeFilters?.includes(item.variable)
                    )
                );
            });
    }

    readonly getContentTypesEffect = effect(() => {
        // TODO: We need to improve this when this ticket is done: https://github.com/dotCMS/core/issues/32991
        // After that remove the uncommented code and
        // const type = this.$mappedBaseTypes().join(',')
        const type = undefined;
        const filter = this.$state.filterByKeyword();

        // Push the request parameters to the debounced stream
        this.#apiRequestSubject.next({ type, filter });
    });

    onFilter(event: MultiSelectFilterEvent) {
        patchState(this.$state, {
            filterByKeyword: event.filter
        });
    }

    onChange() {
        const value = this.$selectedContentTypes() ?? [];

        if (value.length > 0) {
            this.#store.patchFilters({
                contentType: value.map((item) => item.variable)
            });
        } else {
            this.#store.removeFilter('contentType');
        }
    }
}

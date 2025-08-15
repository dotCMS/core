import { patchState, signalState } from '@ngrx/signals';
import { of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectFilterEvent, MultiSelectModule } from 'primeng/multiselect';

import { catchError, debounceTime, distinctUntilChanged, map, take } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DEBOUNCE_TIME, MAP_NUMBERS_TO_BASE_TYPES } from '../../../../shared/constants';
import { BASE_TYPES } from '../../../../shared/models';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

type DotContentDriveContentTypeFieldState = {
    contentTypes: DotCMSContentType[];
    filterByKeyword: string;
};

@Component({
    selector: 'dot-content-drive-content-type-field',
    imports: [MultiSelectModule, FormsModule],
    templateUrl: './dot-content-drive-content-type-field.component.html',
    styleUrl: './dot-content-drive-content-type-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveContentTypeFieldComponent {
    #store = inject(DotContentDriveStore);

    #contentTypesService = inject(DotContentTypeService);

    readonly $state = signalState<DotContentDriveContentTypeFieldState>({
        contentTypes: [],
        filterByKeyword: ''
    });

    readonly $selectedContentTypes = signal<DotCMSContentType[]>([]);

    // We need to map the numbers to the base types, ticket: https://github.com/dotCMS/core/issues/32991
    private readonly $mappedBaseTypes = computed(
        () => this.#store.filters().baseType?.map((item) => MAP_NUMBERS_TO_BASE_TYPES[item]) ?? [],
        {
            // This will trigger the effect if the base types are different
            equal: (a, b) => a?.length === b?.length && a?.every((item) => b.includes(item))
        }
    );

    readonly getContentTypesEffect = effect(() => {
        // TODO: We need to improve this when this ticket is done: https://github.com/dotCMS/core/issues/32991
        // After that remove the `&& undefined`
        const type = this.$mappedBaseTypes().join(',') && undefined;
        const filter = this.$state.filterByKeyword();

        this.#contentTypesService
            .getContentTypes({ type, filter })
            .pipe(
                take(1),
                catchError(() => of([]))
            )
            .subscribe((contentTypes: DotCMSContentType[]) => {
                patchState(this.$state, {
                    contentTypes: contentTypes.filter(
                        (item) => item.baseType !== BASE_TYPES.form && !item.system
                    )
                });

                const contentTypeFilters = this.#store.getFilterValue('contentType');

                this.$selectedContentTypes.set(
                    contentTypes.filter((item) => contentTypeFilters?.includes(item.variable))
                );
            });
    });

    onFilter(event: MultiSelectFilterEvent) {
        // We need to debounce the filter to avoid too many requests
        of(event.filter)
            .pipe(debounceTime(DEBOUNCE_TIME), distinctUntilChanged())
            .subscribe((value) => {
                patchState(this.$state, {
                    filterByKeyword: value
                });
            });
    }

    onChange() {
        of(this.$selectedContentTypes() ?? [])
            .pipe(
                debounceTime(DEBOUNCE_TIME), // Debounce to avoid spamming the server
                distinctUntilChanged(),
                map((value) => {
                    return value.map((item) => item.variable);
                })
            )
            .subscribe((value) => {
                if (value.length > 0) {
                    this.#store.patchFilters({
                        contentType: value
                    });
                } else {
                    this.#store.removeFilter('contentType');
                }
            });
    }
}

import { patchState, signalState } from '@ngrx/signals';

import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotUVEPaletteListType } from '../../../../../shared/models';
import {
    DotPageContentTypeParams,
    DotPageContentTypeService,
    DotPagination
} from '../../service/dot-page-contenttype.service';
import { DotUvePaletteItemComponent } from '../dot-uve-palette-item/dot-uve-palette-item.component';

interface DotUVEPaletteListState {
    contentTypes: DotCMSContentType[];
    pagination?: Omit<DotPagination, 'totalEntries'>;
    sort: {
        orderby: 'name' | 'usage' | 'modified';
        direction: 'ASC' | 'DESC';
    };
    filter?: string;
    totalEntries: number;
    loading: boolean;
}

const DEFAULT_PER_PAGE = 30;

@Component({
    selector: 'dot-uve-palette-list',
    imports: [
        DotUvePaletteItemComponent,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        PaginatorModule,
        DotMessagePipe,
        SkeletonModule
    ],
    providers: [DotPageContentTypeService],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent {
    $type = input.required<DotUVEPaletteListType>({ alias: 'type' });
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });

    readonly LOADING_ROWS_MOCK = Array.from({ length: DEFAULT_PER_PAGE }, (_, index) => index + 1);

    // Computed signal for paginated content types
    readonly $start = computed(() => {
        return (this.$pagination().currentPage - 1) * this.$pagination().perPage;
    });
    readonly $rowsPerPage = computed(() => {
        return this.$pagination().perPage;
    });

    readonly #pageContentTypeService = inject(DotPageContentTypeService);

    readonly $state = signalState<DotUVEPaletteListState>({
        contentTypes: [],
        pagination: {
            currentPage: 1,
            perPage: DEFAULT_PER_PAGE
        },
        sort: {
            orderby: 'name',
            direction: 'ASC'
        },
        filter: '',
        totalEntries: 0,
        loading: true
    });

    readonly $contentTypes = this.$state.contentTypes;
    readonly $pagination = this.$state.pagination;
    readonly $totalRecords = this.$state.totalEntries;
    readonly $loading = this.$state.loading;

    readonly fetchEffect = effect(() => {
        const filter = this.$state.filter();
        const sort = this.$state.sort();
        const pagination = this.$state.pagination();

        const params: DotPageContentTypeParams = {
            pagePathOrId: this.$pagePath(),
            language: this.$languageId().toString(),
            type: this.$type(),
            filter: filter,
            page: pagination.currentPage,
            per_page: pagination.perPage,
            orderby: sort.orderby,
            direction: sort.direction
        };

        patchState(this.$state, {
            loading: true
        });

        this.#pageContentTypeService.get(params).subscribe(({ contenttypes, pagination }) => {
            patchState(this.$state, {
                contentTypes: contenttypes,
                totalEntries: pagination.totalEntries,
                loading: false
            });
        });
    });

    onPageChange(event: PaginatorState) {
        patchState(this.$state, {
            pagination: {
                currentPage: event.page + 1, // PrimeNG uses 0-based indexing, but API expects 1-based
                perPage: DEFAULT_PER_PAGE
            }
        });
    }

    onSearch(event: Event) {
        const value = (event.target as HTMLInputElement).value;
        patchState(this.$state, {
            filter: value
        });
    }
}

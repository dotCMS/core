import { patchState, signalState } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessagePipe } from '@dotcms/ui';

import { DotUVEPaletteListState, SortOption, ViewOption } from './model';

import { DotUVEPaletteListType } from '../../../../../shared/models';
import {
    DotPageContentTypeParams,
    DotPageContentTypeService
} from '../../service/dot-page-contenttype.service';
import { DotUvePaletteItemComponent } from '../dot-uve-palette-item/dot-uve-palette-item.component';

/** Default number of items per page */
const DEFAULT_PER_PAGE = 30;

const DEFAULT_STATE: DotUVEPaletteListState = {
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
};

/**
 * Component for displaying and managing a list of content types in the UVE palette.
 * Supports grid/list view modes, sorting, filtering, and pagination.
 *
 * @example
 * ```html
 * <dot-uve-palette-list
 *   [type]="'content'"
 *   [languageId]="1"
 *   [pagePath]="'/home'">
 * </dot-uve-palette-list>
 * ```
 */
@Component({
    selector: 'dot-uve-palette-list',
    imports: [
        DotUvePaletteItemComponent,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        MenuModule,
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

    readonly #pageContentTypeService = inject(DotPageContentTypeService);

    readonly menuItems = signal<MenuItem[]>([
        {
            label: 'Sort by',
            items: [
                {
                    label: 'Most Popular',
                    id: 'most-popular',
                    command: () => this.onSortSelect({ orderby: 'usage', direction: 'ASC' })
                },
                {
                    label: 'A to Z',
                    id: 'a-to-z',
                    command: () => this.onSortSelect({ orderby: 'name', direction: 'ASC' })
                },
                {
                    label: 'Z to A',
                    id: 'z-to-a',
                    command: () => this.onSortSelect({ orderby: 'name', direction: 'DESC' })
                }
            ]
        },
        {
            separator: true
        },
        {
            label: 'View',
            items: [
                {
                    label: 'Grid',
                    id: 'grid',
                    command: () => this.onViewSelect('grid')
                },
                {
                    label: 'List',
                    id: 'list',
                    command: () => this.onViewSelect('list')
                }
            ]
        }
    ]);

    /** Component state */
    readonly $state = signalState<DotUVEPaletteListState>(DEFAULT_STATE);

    readonly $contentTypes = this.$state.contentTypes;
    readonly $pagination = this.$state.pagination;
    readonly $totalRecords = this.$state.totalEntries;
    readonly $loading = this.$state.loading;

    readonly LOADING_ROWS_MOCK = Array.from({ length: DEFAULT_PER_PAGE }, (_, index) => index + 1);
    readonly $view = signal<ViewOption>('grid');

    /** Starting index for current page (0-based) */
    readonly $start = computed(
        () => (this.$pagination().currentPage - 1) * this.$pagination().perPage
    );
    /** Number of rows per page */
    readonly $rowsPerPage = computed(() => this.$pagination().perPage);

    /**
     * Effect that fetches content types whenever filter, sort, or pagination changes.
     * Automatically runs on component initialization and when dependencies update.
     */
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

    /**
     * Handles pagination page change events.
     * Converts PrimeNG's 0-based page index to API's 1-based index.
     *
     * @param event - PrimeNG paginator state event
     */
    onPageChange(event: PaginatorState) {
        patchState(this.$state, {
            pagination: {
                currentPage: event.page + 1, // PrimeNG uses 0-based indexing, but API expects 1-based
                perPage: DEFAULT_PER_PAGE
            }
        });
    }

    /**
     * Handles search input changes.
     * Updates the filter state which triggers a new fetch via the effect.
     *
     * @param event - Input change event
     */
    onSearch(event: Event) {
        const value = (event.target as HTMLInputElement).value;
        patchState(this.$state, {
            filter: value
        });
    }

    /**
     * Handles sort option selection from the options menu.
     * Updates the sort state which triggers a new fetch via the effect.
     *
     * @param sortOption - Selected sort configuration
     */
    onSortSelect({ orderby, direction }: SortOption) {
        patchState(this.$state, {
            sort: { orderby, direction }
        });
    }

    /**
     * Handles view mode selection (grid/list) from the options menu.
     * Updates the view signal to toggle between grid and list layouts.
     *
     * @param viewOption - Selected view mode ('grid' or 'list')
     */
    onViewSelect(viewOption: ViewOption) {
        this.$view.set(viewOption);
    }
}

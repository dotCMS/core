import { patchState, signalState } from '@ngrx/signals';
import { of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import { catchError } from 'rxjs/operators';

import { DotESContentService, DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSBaseTypesContentTypes, ESContent } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotUVEPaletteListState, SortOption, ViewOption } from './model';

import { DotUVEPaletteListType } from '../../../../../shared/models';
import {
    DotPageContentTypeParams,
    DotPageContentTypeService
} from '../../service/dot-page-contenttype.service';
import { BASETYPES_FOR_CONTENT, BASETYPES_FOR_WIDGET } from '../../utils';
import { DotUvePaletteContentletComponent } from '../dot-uve-palette-contentlet/dot-uve-palette-contentlet.component';
import { DotUvePaletteItemComponent } from '../dot-uve-palette-item/dot-uve-palette-item.component';

/** Default number of items per page */
const DEFAULT_PER_PAGE = 30;

const DEFAULT_STATE: DotUVEPaletteListState = {
    contentTypes: [],
    contentlets: [],
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
    loading: true,
    selectedContentType: undefined
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
        DotUvePaletteContentletComponent,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        MenuModule,
        PaginatorModule,
        DotMessagePipe,
        SkeletonModule
    ],
    providers: [DotPageContentTypeService, DotESContentService],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent {
    $type = input.required<DotUVEPaletteListType>({ alias: 'type' });
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });

    readonly #pageContentTypeService = inject(DotPageContentTypeService);
    readonly #dotESContentService = inject(DotESContentService);
    readonly #dotMessageService = inject(DotMessageService);

    /** Component state */
    readonly $state = signalState<DotUVEPaletteListState>(DEFAULT_STATE);

    readonly $contentTypes = this.$state.contentTypes;
    readonly $pagination = this.$state.pagination;
    readonly $totalRecords = this.$state.totalEntries;
    readonly $loading = this.$state.loading;
    readonly $selectedContentType = this.$state.selectedContentType;
    readonly $contentlets = this.$state.contentlets;

    readonly LOADING_ROWS_MOCK = Array.from({ length: DEFAULT_PER_PAGE }, (_, index) => index + 1);
    readonly $view = signal<ViewOption>('grid');

    /** Starting index for current page (0-based) */
    readonly $start = computed(
        () => (this.$pagination().currentPage - 1) * this.$pagination().perPage
    );
    /** Number of rows per page */
    readonly $rowsPerPage = computed(() => this.$pagination().perPage);

    /** Computed menu items with active state based on current sort and view */
    readonly $menuItems = computed(() => {
        const currentView = this.$view();

        return [
            {
                label: this.#dotMessageService.get('uve.palette.menu.sort.title'),
                items: [
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.popular'),
                        id: 'most-popular',
                        command: () => this.onSortSelect({ orderby: 'usage', direction: 'ASC' }),
                        styleClass: this.isSortActive({ orderby: 'usage', direction: 'ASC' })
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.a-to-z'),
                        id: 'a-to-z',
                        command: () => this.onSortSelect({ orderby: 'name', direction: 'ASC' }),
                        styleClass: this.isSortActive({ orderby: 'name', direction: 'ASC' })
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.z-to-a'),
                        id: 'z-to-a',
                        command: () => this.onSortSelect({ orderby: 'name', direction: 'DESC' }),
                        styleClass: this.isSortActive({ orderby: 'name', direction: 'DESC' })
                    }
                ]
            },
            {
                separator: true
            },
            {
                label: this.#dotMessageService.get('uve.palette.menu.view.title'),
                items: [
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.view.option.grid'),
                        id: 'grid',
                        command: () => this.onViewSelect('grid'),
                        styleClass: currentView === 'grid' ? 'active-menu-item' : ''
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.view.option.list'),
                        id: 'list',
                        command: () => this.onViewSelect('list'),
                        styleClass: currentView === 'list' ? 'active-menu-item' : ''
                    }
                ]
            }
        ];
    });

    /**
     * Effect that fetches content types whenever filter, sort, or pagination changes.
     * Automatically runs on component initialization and when dependencies update.
     */
    readonly fetchEffect = effect(() => {
        const filter = this.$state.filter();
        const sort = this.$state.sort();
        const pagination = this.$state.pagination();
        const isWidget = this.$type() === DotCMSBaseTypesContentTypes.WIDGET;

        const selectedContentType = this.$state.selectedContentType();

        const params: DotPageContentTypeParams = {
            pagePathOrId: this.$pagePath(),
            language: this.$languageId().toString(),
            types: isWidget ? BASETYPES_FOR_WIDGET : BASETYPES_FOR_CONTENT,
            filter: filter,
            page: pagination.currentPage,
            per_page: pagination.perPage,
            orderby: sort.orderby,
            direction: sort.direction
        };

        patchState(this.$state, {
            loading: true
        });

        if (selectedContentType) {
            this.fetchContentlets(
                filter,
                selectedContentType,
                this.$languageId().toString(),
                DEFAULT_VARIANT_ID,
                pagination.currentPage,
                pagination.perPage
            ).subscribe((contentlets) => {
                patchState(this.$state, {
                    contentlets: contentlets.jsonObjectView.contentlets,
                    totalEntries: contentlets.resultsSize,
                    loading: false
                });
            });
        } else {
            this.#pageContentTypeService.get(params).subscribe(({ contenttypes, pagination }) => {
                patchState(this.$state, {
                    contentTypes: contenttypes,
                    totalEntries: pagination.totalEntries,
                    loading: false
                });
            });
        }
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

    /**
     * Determines the CSS class for sort menu items based on current sort state.
     * Returns 'active-menu-item' if the item matches the current sort configuration.
     *
     * @param orderby - Sort field to check
     * @param direction - Sort direction to check
     * @param currentSort - Current sort state
     * @returns CSS class string for the menu item
     */
    private isSortActive(itemSort: SortOption): string {
        const activeSort = this.$state.sort();
        const sameOrderby = activeSort.orderby === itemSort.orderby;
        const sameDirection = activeSort.direction === itemSort.direction;
        const isActive = sameOrderby && sameDirection;

        return isActive ? 'active-menu-item' : '';
    }

    /**
     * Handles the selection of a content type.
     *
     * @param {string} contentType
     * @memberof DotUvePaletteListComponent
     */
    onSelectContentType(contentType: string) {
        patchState(this.$state, {
            selectedContentType: contentType
        });
    }

    /**
     * Handles the back to content types action.
     * Resets the state to the initial state.
     */
    onBackToContentTypes() {
        patchState(this.$state, {
            selectedContentType: undefined,
            contentlets: [],
            totalEntries: 0,
            loading: true
        });
    }

    private fetchContentlets(
        filter: string,
        contenttypeName: string,
        languageId: string,
        variantId: string,
        page: number,
        perPage: number
    ) {
        const variantTerm = variantId
            ? `+variant:(${DEFAULT_VARIANT_ID} OR ${variantId})`
            : `+variant:${DEFAULT_VARIANT_ID}`;

        return this.#dotESContentService
            .get({
                itemsPerPage: perPage,
                lang: languageId || '1',
                filter: filter || '',
                offset: ((page - 1) * perPage).toString(),
                query: `+contentType:${contenttypeName} +deleted:false ${variantTerm}`.trim()
            })
            .pipe(
                catchError((error) => {
                    console.error(error);
                    return of({
                        jsonObjectView: {
                            contentlets: []
                        },
                        resultsSize: 0,
                        queryTook: 0,
                        contentTook: 0
                    } as ESContent);
                })
            );
    }
}

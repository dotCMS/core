import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    OnInit,
    signal,
    OnDestroy,
    DestroyRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotESContentService, DotMessageService } from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DotCMSBaseTypesContentTypes,
    DotCMSContentType
} from '@dotcms/dotcms-models';

import { SortOption, ViewOption } from './model';
import {
    DotUVEPaletteListView,
    DotPaletteListStore,
    DEFAULT_PER_PAGE,
    DotPaletteListStatus
} from './store/store';

import { DotUVEPaletteListType } from '../../../../../shared/models';
import {
    DotPageContentTypeParams,
    DotPageContentTypeService
} from '../../service/dot-page-contenttype.service';
import { DotPageFavoriteContentTypeService } from '../../service/dot-page-favorite-contentType.service';
import { BASETYPES_FOR_CONTENT, BASETYPES_FOR_WIDGET, isSortActive } from '../../utils';
import { DotFavoriteSelectorComponent } from '../dot-favorite-selector/dot-favorite-selector.component';
import { DotUvePaletteContentletComponent } from '../dot-uve-palette-contentlet/dot-uve-palette-contentlet.component';
import { DotUVEPaletteContenttypeComponent } from '../dot-uve-palette-contenttype/dot-uve-palette-contenttype.component';

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
        DotUVEPaletteContenttypeComponent,
        DotUvePaletteContentletComponent,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        MenuModule,
        PaginatorModule,
        SkeletonModule,
        OverlayPanelModule,
        DotFavoriteSelectorComponent
    ],
    providers: [
        DotPaletteListStore,
        DotESContentService,
        DotPageContentTypeService,
        DotPageFavoriteContentTypeService
    ],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent implements OnInit, OnDestroy {
    $type = input.required<DotUVEPaletteListType>({ alias: 'type' });
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    readonly #paletteListStore = inject(DotPaletteListStore);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #messageService = inject(MessageService);
    readonly #destroyRef = inject(DestroyRef);

    readonly $start = this.#paletteListStore.$start;
    readonly $contenttypes = this.#paletteListStore.contenttypes;
    readonly $contentlets = this.#paletteListStore.contentlets;
    readonly $pagination = this.#paletteListStore.pagination;
    readonly $rowsPerPage = this.#paletteListStore.$rowsPerPage;
    readonly $currentView = this.#paletteListStore.currentView;
    readonly $status = this.#paletteListStore.$status;
    readonly DotUVEPaletteListView = DotUVEPaletteListView;
    readonly DotPaletteListStatus = DotPaletteListStatus;

    readonly $viewMode = signal<ViewOption>('grid');
    readonly $favoriteMenuItems = signal<MenuItem[]>([]);
    readonly $contextMenuItems = signal<MenuItem[]>([]);
    readonly #searchSubject = new Subject<string>();

    readonly $skeletonHeight = computed(() => (this.$showViewList() ? '4rem' : '6.875rem'));
    readonly $showViewList = computed(
        () =>
            this.$viewMode() === 'list' || this.$currentView() === DotUVEPaletteListView.CONTENTLETS
    );
    readonly allowedBaseTypes = computed(() =>
        this.$type() === DotCMSBaseTypesContentTypes.CONTENT
            ? BASETYPES_FOR_CONTENT
            : BASETYPES_FOR_WIDGET
    );

    readonly $showPaginator = computed(
        () => this.$pagination() && this.$status() !== DotPaletteListStatus.EMPTY
    );
    readonly $paginatorTemplate = computed(() => {
        return `{first} - {last} ${this.#dotMessageService.get('uve.palette.pagination.of')} {totalRecords}`;
    });
    readonly LOADING_ROWS_MOCK = Array.from({ length: DEFAULT_PER_PAGE }, (_, index) => index + 1);

    ngOnInit() {
        this.getContentTypes();
        this.setupDebouncedSearch();
    }

    ngOnDestroy() {
        this.#searchSubject.complete();
    }

    /**
     * Handles pagination page change events.
     * Converts PrimeNG's 0-based page index to API's 1-based index.
     *
     * @param event - PrimeNG paginator state event
     */
    onPageChange(event: PaginatorState) {
        const page = event.page + 1;
        if (this.$currentView() === DotUVEPaletteListView.CONTENT_TYPES) {
            this.getContentTypes({ page });
        } else {
            this.getContentlets({
                contentTypeName: this.#paletteListStore.currentContentType(),
                page,
                filter: ''
            });
        }
    }

    /**
     * Handles search input changes.
     * Emits the search value to the debounced search subject.
     *
     * @param event - Input change event
     */
    onSearch(event: Event) {
        const value = (event.target as HTMLInputElement).value;
        this.#searchSubject.next(value);
    }

    /**
     * Handles sort option selection from the options menu.
     * Updates the sort state which triggers a new fetch via the effect.
     *
     * @param sortOption - Selected sort configuration
     */
    onSortSelect({ orderby, direction }: SortOption) {
        this.getContentTypes({ orderby, direction });
    }

    /**
     * Handles view mode selection (grid/list) from the options menu.
     * Updates the view signal to toggle between grid and list layouts.
     *
     * @param viewOption - Selected view mode ('grid' or 'list')
     */
    onViewSelect(viewOption: ViewOption) {
        this.$viewMode.set(viewOption);
    }

    /**
     * Handles the selection of a content type.
     *
     * @param {string} contentType
     * @memberof DotUvePaletteListComponent
     */
    onSelectContentType(contentTypeName: string) {
        this.getContentlets({ contentTypeName, page: 1, filter: '' });
    }

    /**
     * Handles the back to content types action.
     * Resets the state to the initial state.
     */
    onBackToContentTypes() {
        this.getContentTypes();
    }

    /**
     * Get the search parameters for the content types.
     * @returns DotPageContentTypeParams
     */
    private getSearchParams(): DotPageContentTypeParams {
        return {
            types: this.allowedBaseTypes(),
            pagePathOrId: this.$pagePath(),
            language: this.$languageId().toString(),
            filter: '',
            per_page: DEFAULT_PER_PAGE,
            page: 1,
            orderby: 'name',
            direction: 'ASC'
        };
    }

    /**
     * Get the content types.
     * @param params - The parameters for the content types.
     */
    private getContentTypes(params: Partial<DotPageContentTypeParams> = {}) {
        if (this.$type() === 'FAVORITES') {
            this.#paletteListStore.getFavoriteContentTypes(this.$pagePath(), params.filter || '');
        } else if (this.$type() === DotCMSBaseTypesContentTypes.WIDGET) {
            this.#paletteListStore.getWidgets({ ...this.getSearchParams(), ...params });
        } else {
            this.#paletteListStore.getContentTypes({ ...this.getSearchParams(), ...params });
        }
    }

    /**
     * Get the content lets.
     * @param params - The parameters for the content lets.
     */
    private getContentlets({ contentTypeName, filter = '', page = 1 }) {
        const variantTerm = this.$variantId()
            ? `+variant:(${DEFAULT_VARIANT_ID} OR ${this.$variantId()})`
            : `+variant:${DEFAULT_VARIANT_ID}`;

        this.#paletteListStore.getContentlets({
            filter,
            itemsPerPage: DEFAULT_PER_PAGE,
            lang: this.$languageId().toString(),
            offset: ((page - 1) * DEFAULT_PER_PAGE).toString(),
            query: `+contentType:${contentTypeName} +deleted:false ${variantTerm}`.trim()
        });
        this.#paletteListStore.setCurrentContentType(contentTypeName);
    }

    /**
     * Sets up the debounced search functionality.
     * Listens to search input changes and triggers search after 500ms delay.
     */
    private setupDebouncedSearch() {
        this.#searchSubject
            .pipe(debounceTime(500), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((searchTerm) => {
                if (this.$currentView() === DotUVEPaletteListView.CONTENT_TYPES) {
                    this.getContentTypes({ filter: searchTerm });
                } else {
                    this.getContentlets({
                        contentTypeName: this.#paletteListStore.currentContentType(),
                        filter: searchTerm,
                        page: 1
                    });
                }
            });
    }

    protected setSortMenuItems() {
        const currentView = this.$viewMode();
        const currentSort = this.#paletteListStore.sort();
        const items = [
            {
                label: this.#dotMessageService.get('uve.palette.menu.sort.title'),
                items: [
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.popular'),
                        id: 'most-popular',
                        command: () => this.onSortSelect({ orderby: 'usage', direction: 'ASC' }),
                        styleClass: isSortActive(
                            { orderby: 'usage', direction: 'ASC' },
                            currentSort
                        )
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.a-to-z'),
                        id: 'a-to-z',
                        command: () => this.onSortSelect({ orderby: 'name', direction: 'ASC' }),
                        styleClass: isSortActive({ orderby: 'name', direction: 'ASC' }, currentSort)
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.z-to-a'),
                        id: 'z-to-a',
                        command: () => this.onSortSelect({ orderby: 'name', direction: 'DESC' }),
                        styleClass: isSortActive(
                            { orderby: 'name', direction: 'DESC' },
                            currentSort
                        )
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
        this.$contextMenuItems.set(items);
    }

    protected setFavoriteMenuItems(contentType: DotCMSContentType) {
        const isFavorite = this.#paletteListStore.isFavoriteContentType(
            this.$pagePath(),
            contentType.id
        );
        const label = isFavorite
            ? 'uve.palette.menu.favorite.option.remove'
            : 'uve.palette.menu.favorite.option.add';
        const command = isFavorite
            ? () => this.removeFavoriteItems(contentType)
            : () => this.addFavoriteItems(contentType);
        this.$contextMenuItems.set([{ label: this.#dotMessageService.get(label), command }]);
    }

    /**
     * Remove a content type from favorites.
     * @param contentType - The content type to remove.
     */
    private removeFavoriteItems(contentType: DotCMSContentType) {
        this.#paletteListStore.removeFavoriteContentType(this.$pagePath(), contentType.id);

        this.#messageService.add({
            severity: 'success',
            summary: 'Removed from favorites',
            detail: 'You have removed it from your favorites',
            life: 3000
        });

        if (this.$type() === 'FAVORITES') {
            this.getContentTypes();
        }
    }

    /**
     * Add a content type to favorites.
     * @param contentType - The content type to add.
     */
    private addFavoriteItems(contentType: DotCMSContentType) {
        this.#paletteListStore.addFavoriteContentType(this.$pagePath(), contentType);

        this.#messageService.add({
            severity: 'success',
            summary: 'Added to favorites',
            detail: 'You have added it to your favorites',
            life: 3000
        });

        if (this.$type() === 'FAVORITES') {
            this.getContentTypes();
        }
    }
}

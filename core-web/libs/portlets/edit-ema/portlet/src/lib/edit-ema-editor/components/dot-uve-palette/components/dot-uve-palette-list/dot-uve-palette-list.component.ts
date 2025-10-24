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

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotESContentService, DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { SortOption, ViewOption } from './model';
import { DotUVEPaletteListView, PaletteListStore, DEFAULT_PER_PAGE } from './store/store';

import { DotUVEPaletteListType } from '../../../../../shared/models';
import {
    DotPageContentTypeParams,
    DotPageContentTypeService
} from '../../service/dot-page-contenttype.service';
import { BASETYPES_FOR_CONTENT, BASETYPES_FOR_WIDGET } from '../../utils';
import { DotUvePaletteContentletComponent } from '../dot-uve-palette-contentlet/dot-uve-palette-contentlet.component';
import { DotUvePaletteItemComponent } from '../dot-uve-palette-item/dot-uve-palette-item.component';

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
    providers: [DotPageContentTypeService, DotESContentService, PaletteListStore],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent implements OnInit, OnDestroy {
    $type = input.required<DotUVEPaletteListType>({ alias: 'type' });
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });

    readonly #paletteListStore = inject(PaletteListStore);
    readonly #dotMessageService = inject(DotMessageService);

    readonly $start = this.#paletteListStore.$start;
    readonly $contenttypes = this.#paletteListStore.contenttypes;
    readonly $contentlets = this.#paletteListStore.contentlets;
    readonly $pagination = this.#paletteListStore.pagination;
    readonly $rowsPerPage = this.#paletteListStore.$rowsPerPage;
    readonly $currentView = this.#paletteListStore.currentView;
    readonly DotUVEPaletteListView = DotUVEPaletteListView;

    readonly $showViewList = computed(() => {
        return this.$view() === 'list' || this.$currentView() === DotUVEPaletteListView.CONTENTLETS;
    });
    readonly allowedBaseTypes = computed(() => {
        return this.$type() === DotCMSBaseTypesContentTypes.CONTENT
            ? BASETYPES_FOR_CONTENT
            : BASETYPES_FOR_WIDGET;
    });

    readonly LOADING_ROWS_MOCK = Array.from({ length: DEFAULT_PER_PAGE }, (_, index) => index + 1);
    readonly $view = signal<ViewOption>('grid');

    // Subject for debounced search
    readonly #searchSubject = new Subject<string>();
    readonly #destroyRef = inject(DestroyRef);

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
        this.$view.set(viewOption);
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
     * Determines the CSS class for sort menu items based on current sort state.
     * Returns 'active-menu-item' if the item matches the current sort configuration.
     *
     * @param orderby - Sort field to check
     * @param direction - Sort direction to check
     * @param currentSort - Current sort state
     * @returns CSS class string for the menu item
     */
    private isSortActive(itemSort: SortOption): string {
        const activeSort = this.#paletteListStore.sort();
        const sameOrderby = activeSort.orderby === itemSort.orderby;
        const sameDirection = activeSort.direction === itemSort.direction;
        const isActive = sameOrderby && sameDirection;

        return isActive ? 'active-menu-item' : '';
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
        this.#paletteListStore.getContentTypes({ ...this.getSearchParams(), ...params });
    }

    /**
     * Get the content lets.
     * @param params - The parameters for the content lets.
     */
    private getContentlets({ contentTypeName, filter = '', page = 1 }) {
        this.#paletteListStore.getContentlets({
            filter,
            itemsPerPage: DEFAULT_PER_PAGE,
            lang: this.$languageId().toString(),
            offset: ((page - 1) * DEFAULT_PER_PAGE).toString(),
            query: `+contentType:${contentTypeName} +deleted:false`.trim()
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
}

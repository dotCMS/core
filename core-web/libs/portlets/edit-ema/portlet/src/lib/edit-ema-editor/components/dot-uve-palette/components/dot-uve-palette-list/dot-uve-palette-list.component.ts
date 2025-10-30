import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    input,
    OnInit,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

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
import { DEFAULT_VARIANT_ID, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPaletteListStore } from './store/store';

import {
    DotPaletteListStatus,
    DotPaletteSortOption,
    DotPaletteViewMode,
    DotUVEPaletteListTypes,
    DotUVEPaletteListView
} from '../../models';
import { DotPageFavoriteContentTypeService } from '../../service/dot-page-favorite-contentType.service';
import { buildContentletsQuery, getSortActiveClass, LOADING_ROWS_MOCK } from '../../utils';
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
        NgTemplateOutlet,
        ReactiveFormsModule,
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
        DotFavoriteSelectorComponent,
        DotMessagePipe
    ],
    providers: [DotPaletteListStore, DotESContentService],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent implements OnInit {
    $listType = input.required<DotUVEPaletteListTypes>({ alias: 'listType' });
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    readonly #paletteListStore = inject(DotPaletteListStore);
    readonly #dotPageFavoriteContentTypeService = inject(DotPageFavoriteContentTypeService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #messageService = inject(MessageService);
    readonly #destroyRef = inject(DestroyRef);

    searchControl = new FormControl('', { nonNullable: true });

    readonly $start = this.#paletteListStore.$start;
    readonly $contenttypes = this.#paletteListStore.contenttypes;
    readonly $contentlets = this.#paletteListStore.contentlets;
    readonly $pagination = this.#paletteListStore.pagination;
    readonly $currentView = this.#paletteListStore.currentView;
    readonly $status = this.#paletteListStore.$status;
    readonly $isLoading = this.#paletteListStore.$isLoading;

    readonly DotUVEPaletteListView = DotUVEPaletteListView;
    readonly DotPaletteListStatus = DotPaletteListStatus;

    readonly $viewMode = signal<DotPaletteViewMode>('grid');
    readonly $contextMenuItems = signal<MenuItem[]>([]);
    readonly $currentContentType = signal<string>('');

    readonly $isListLayout = computed(() => {
        return this.$viewMode() === 'list' || this.#paletteListStore.$isContentletsView();
    });

    readonly $emptyStateMessage = computed(() => {
        const currentView = this.$currentView();
        const listType = this.$listType();

        if (listType === 'FAVORITES') {
            return 'uve.palette.empty.favorites.message';
        }

        if (currentView === DotUVEPaletteListView.CONTENT_TYPES) {
            return 'uve.palette.empty.content-types.message';
        }

        return 'uve.palette.empty.contentlets.message';
    });

    readonly $showAddButton = computed(() => this.$listType() === 'FAVORITES');

    readonly LOADING_ROWS = LOADING_ROWS_MOCK;

    ngOnInit() {
        this.#paletteListStore.setSearchParams({
            pagePathOrId: this.$pagePath(),
            language: this.$languageId(),
            variantId: this.$variantId()
        });
        this.#paletteListStore.getContentTypes(this.$listType());

        // Set up debounced search with distinctUntilChanged to avoid duplicate calls
        this.searchControl.valueChanges
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((searchTerm) => {
                if (!this.$currentContentType()) {
                    this.#paletteListStore.getContentTypes(this.$listType(), {
                        filter: searchTerm
                    });
                } else {
                    const query = buildContentletsQuery(
                        this.$currentContentType(),
                        this.$variantId()
                    );
                    this.#paletteListStore.getContentlets({
                        query,
                        offset: '0',
                        filter: searchTerm
                    });
                }
            });
    }

    /**
     * Handles pagination page change events.
     * Preserves the current search term when paginating.
     *
     * @param event - PrimeNG paginator state event
     */
    onPageChange(event: PaginatorState) {
        const currentFilter = this.searchControl.value;
        if (!this.$currentContentType()) {
            this.#paletteListStore.getContentTypes(this.$listType(), {
                page: event.page,
                filter: currentFilter
            });
        } else {
            const query = buildContentletsQuery(this.$currentContentType(), this.$variantId());
            this.#paletteListStore.getContentlets({
                query,
                offset: '0',
                filter: currentFilter
            });
        }
    }

    /**
     * Handles sort option selection from the options menu.
     * Preserves the current search term when sorting.
     *
     * @param sortOption - Selected sort configuration
     */
    onSortSelect(sortOption: DotPaletteSortOption) {
        const currentFilter = this.searchControl.value;
        this.#paletteListStore.getContentTypes(this.$listType(), {
            orderby: sortOption.orderby,
            direction: sortOption.direction,
            filter: currentFilter
        });
    }

    /**
     * Handles view mode selection (grid/list) from the options menu.
     * Updates the view signal to toggle between grid and list layouts.
     *
     * @param viewOption - Selected view mode ('grid' or 'list')
     */
    onViewSelect(viewOption: DotPaletteViewMode) {
        this.$viewMode.set(viewOption);
    }

    /**
     * Handles the selection of a content type to view its contentlets.
     * Resets the search term when drilling into a content type.
     *
     * @param contentTypeName - The name of the content type to drill into
     */
    onSelectContentType(contentTypeName: string) {
        this.$currentContentType.set(contentTypeName);
        this.searchControl.setValue('', { emitEvent: false });
        const query = buildContentletsQuery(contentTypeName, this.$variantId());
        this.#paletteListStore.getContentlets({ query, offset: '0' });
    }

    /**
     * Handles returning to the content types view from contentlets drill-down.
     * Resets the search term when going back.
     */
    onBackToContentTypes() {
        this.$currentContentType.set('');
        this.searchControl.setValue('', { emitEvent: false });
        this.#paletteListStore.getContentTypes(this.$listType());
    }

    protected setSortMenuItems() {
        const currentView = this.$viewMode();
        const currentSort = this.#paletteListStore.$currentSort();
        const items = [
            {
                label: this.#dotMessageService.get('uve.palette.menu.sort.title'),
                items: [
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.popular'),
                        command: () => this.onSortSelect({ orderby: 'usage', direction: 'ASC' }),
                        styleClass: getSortActiveClass(
                            { orderby: 'usage', direction: 'ASC' },
                            currentSort
                        )
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.a-to-z'),
                        command: () => this.onSortSelect({ orderby: 'name', direction: 'ASC' }),
                        styleClass: getSortActiveClass(
                            { orderby: 'name', direction: 'ASC' },
                            currentSort
                        )
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.sort.option.z-to-a'),
                        command: () => this.onSortSelect({ orderby: 'name', direction: 'DESC' }),
                        styleClass: getSortActiveClass(
                            { orderby: 'name', direction: 'DESC' },
                            currentSort
                        )
                    }
                ]
            },
            {
                label: this.#dotMessageService.get('uve.palette.menu.view.title'),
                items: [
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.view.option.grid'),
                        command: () => this.onViewSelect('grid'),
                        styleClass: currentView === 'grid' ? 'active-menu-item' : ''
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.view.option.list'),
                        command: () => this.onViewSelect('list'),
                        styleClass: currentView === 'list' ? 'active-menu-item' : ''
                    }
                ]
            }
        ];
        this.$contextMenuItems.set(items);
    }

    protected setFavoriteMenuItems(contentType: DotCMSContentType) {
        const isFavorite = this.#dotPageFavoriteContentTypeService.isFavorite(contentType.id);
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
        const contenttypes = this.#dotPageFavoriteContentTypeService.remove(contentType.id);

        if (this.$listType() === DotUVEPaletteListTypes.FAVORITES) {
            this.#paletteListStore.setContentTypesFromFavorite(contenttypes);
        }
        // this.#paletteListStore.setFavorites(contenttypes);

        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('uve.palette.favorite.remove.success.summary'),
            detail: this.#dotMessageService.get('uve.palette.favorite.remove.success.detail'),
            life: 3000
        });
    }

    /**
     * Add a content type to favorites.
     * @param contentType - The content type to add.
     */
    private addFavoriteItems(contentType: DotCMSContentType) {
        const contenttypes = this.#dotPageFavoriteContentTypeService.add(contentType);

        if (this.$listType() === DotUVEPaletteListTypes.FAVORITES) {
            this.#paletteListStore.setContentTypesFromFavorite(contenttypes);
        }
        // this.#paletteListStore.setFavorites(contenttypes);

        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('uve.palette.favorite.add.success.summary'),
            detail: this.#dotMessageService.get('uve.palette.favorite.add.success.detail'),
            life: 3000
        });
    }
}

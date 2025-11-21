import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    input,
    OnInit,
    signal,
    untracked,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ContextMenuModule } from 'primeng/contextmenu';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { PopoverModule } from 'primeng/popover';
import { SkeletonModule } from 'primeng/skeleton';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import {
    DotESContentService,
    DotFavoriteContentTypeService,
    DotMessageService
} from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPaletteListStore } from './store/store';

import {
    DotPaletteSortOption,
    DotPaletteViewMode,
    DotUVEPaletteListTypes,
    DotUVEPaletteListView
} from '../../models';
import { getSortActiveClass, LOADING_ROWS_MOCK } from '../../utils';
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
        PopoverModule,
        DotFavoriteSelectorComponent,
        DotMessagePipe,
        ContextMenuModule
    ],
    providers: [DotPaletteListStore, DotESContentService],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent implements OnInit {
    @ViewChild('favoritesPanel') favoritesPanel?: DotFavoriteSelectorComponent;

    $listType = input.required<DotUVEPaletteListTypes>({ alias: 'listType' });
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    readonly #paletteListStore = inject(DotPaletteListStore);
    readonly #dotFavoriteContentTypeService = inject(DotFavoriteContentTypeService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #messageService = inject(MessageService);
    readonly #destroyRef = inject(DestroyRef);

    readonly searchControl = new FormControl('', { nonNullable: true });

    protected readonly DotUVEPaletteListView = DotUVEPaletteListView;
    protected readonly LOADING_ROWS = LOADING_ROWS_MOCK;

    protected readonly $contenttypes = this.#paletteListStore.contenttypes;
    protected readonly $contentlets = this.#paletteListStore.contentlets;
    protected readonly $pagination = this.#paletteListStore.pagination;
    protected readonly $currentView = this.#paletteListStore.currentView;
    protected readonly $isLoading = this.#paletteListStore.$isLoading;
    protected readonly $isEmpty = this.#paletteListStore.$isEmpty;
    protected readonly $layoutMode = this.#paletteListStore.layoutMode;
    protected readonly $showListLayout = this.#paletteListStore.$showListLayout;
    protected readonly $emptyStateMessage = this.#paletteListStore.$emptyStateMessage;

    protected readonly $contextMenuItems = signal<MenuItem[]>([]);

    readonly $start = computed(
        () => (this.$pagination().currentPage - 1) * this.$pagination().perPage
    );
    readonly $isFavoritesList = computed(
        () => this.$listType() === DotUVEPaletteListTypes.FAVORITES
    );
    readonly $showSortButton = computed(
        () =>
            this.$currentView() === DotUVEPaletteListView.CONTENT_TYPES && !this.$isFavoritesList()
    );

    protected $menuItems = computed(() => {
        const viewMode = this.$layoutMode();
        const currentSort = this.#paletteListStore.$currentSort();
        return [
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
                        styleClass: viewMode === 'grid' ? 'active-menu-item' : ''
                    },
                    {
                        label: this.#dotMessageService.get('uve.palette.menu.view.option.list'),
                        command: () => this.onViewSelect('list'),
                        styleClass: viewMode === 'list' ? 'active-menu-item' : ''
                    }
                ]
            }
        ];
    });

    constructor() {
        // React to input changes and fetch content types
        effect(() => {
            const pagePathOrId = this.$pagePath();
            const language = this.$languageId();
            const variantId = this.$variantId();
            const listType = this.$listType();

            // Use untracked to prevent writes during effect
            untracked(() => {
                this.#paletteListStore.getContentTypes({
                    pagePathOrId,
                    language,
                    variantId,
                    listType
                });
            });
        });
    }

    ngOnInit() {
        // Set up debounced search with distinctUntilChanged to avoid duplicate calls
        this.searchControl.valueChanges
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((searchTerm) => {
                if (this.#paletteListStore.$isContentTypesView()) {
                    this.#paletteListStore.getContentTypes({
                        filter: searchTerm,
                        page: 1 // Reset to first page on search
                    });
                } else {
                    this.#paletteListStore.getContentlets({
                        filter: searchTerm,
                        page: 1
                    });
                }
            });
    }

    /**
     * Handles pagination page change events.
     * Store automatically preserves the current search term and builds queries.
     *
     * @param event - PrimeNG paginator state event
     */
    protected onPageChange(event: PaginatorState) {
        const page = (event.page ?? 0) + 1; // PrimeNG uses 0-based pages

        if (this.#paletteListStore.$isContentTypesView()) {
            this.#paletteListStore.getContentTypes({ page });
        } else {
            this.#paletteListStore.getContentlets({ page });
        }
    }

    /**
     * Handles sort option selection from the options menu.
     * Store automatically preserves the current search term.
     *
     * @param sortOption - Selected sort configuration
     */
    protected onSortSelect(sortOption: DotPaletteSortOption) {
        this.#paletteListStore.getContentTypes({
            orderby: sortOption.orderby,
            direction: sortOption.direction,
            page: 1 // Reset to first page on sort change
        });
    }

    /**
     * Handles view mode selection (grid/list) from the options menu.
     * Updates the view signal to toggle between grid and list layouts.
     *
     * @param viewOption - Selected view mode ('grid' or 'list')
     */
    protected onViewSelect(viewOption: DotPaletteViewMode) {
        this.#paletteListStore.setLayoutMode(viewOption);
    }

    /**
     * Handles the selection of a content type to view its contentlets.
     * Store handles query building, filter reset, and page reset automatically.
     *
     * @param contentTypeName - The name of the content type to drill into
     */
    protected onSelectContentType(contentTypeName: string) {
        this.#paletteListStore.getContentlets({
            selectedContentType: contentTypeName,
            filter: '',
            page: 1
        });
        this.searchControl.setValue('', { emitEvent: false });
    }

    /**
     * Handles returning to the content types view from contentlets drill-down.
     * Store handles filter reset and page reset automatically.
     */
    protected onBackToContentTypes() {
        this.#paletteListStore.getContentTypes({
            selectedContentType: '',
            filter: '',
            page: 1
        });
        this.searchControl.setValue('', { emitEvent: false });
    }

    protected onContextMenu(contentType: DotCMSContentType) {
        const isFavorite = this.#dotFavoriteContentTypeService.isFavorite(contentType.id);
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
        const contenttypes = this.#dotFavoriteContentTypeService.remove(contentType.id);

        if (this.$listType() === DotUVEPaletteListTypes.FAVORITES) {
            this.#paletteListStore.setContentTypesFromFavorite(contenttypes);
        }

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
        const contenttypes = this.#dotFavoriteContentTypeService.add(contentType);

        if (this.$listType() === DotUVEPaletteListTypes.FAVORITES) {
            this.#paletteListStore.setContentTypesFromFavorite(contenttypes);
        }

        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('uve.palette.favorite.add.success.summary'),
            detail: this.#dotMessageService.get('uve.palette.favorite.add.success.detail'),
            life: 3000
        });
    }

    /**
     * Handles clicks on the empty state message.
     * Opens the favorites panel when a span element is clicked.
     * @param event - The click event
     */
    protected onEmptyStateClick(event: Event) {
        const target = event.target as HTMLElement;

        // Check if the clicked element is a span (or its parent is)
        if (target.tagName === 'SPAN' || target.closest('span')) {
            event.preventDefault();
            this.favoritesPanel?.toggle(event);
        }
    }
}

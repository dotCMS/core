import { signalMethod } from '@ngrx/signals';

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
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ContextMenuModule } from 'primeng/contextmenu';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import { debounceTime, distinctUntilChanged, filter, take } from 'rxjs/operators';

import {
    DotESContentService,
    DotFavoriteContentTypeService,
    DotMessageService
} from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPaletteListStore } from './store/store';

import {
    DotCMSContentTypePalette,
    DotPaletteListStatus,
    DotPaletteSearchParams,
    DotPaletteSortOption,
    DotPaletteViewMode,
    DotUVEPaletteListTypes,
    DotUVEPaletteListView
} from '../../models';
import {
    buildPaletteMenuItems,
    EMPTY_MESSAGE_CONTENTLETS,
    EMPTY_MESSAGE_SEARCH,
    EMPTY_MESSAGES,
    LOADING_ROWS_MOCK
} from '../../utils';
import { DotFavoriteSelectorComponent } from '../dot-favorite-selector/dot-favorite-selector.component';
import { DotUvePaletteContentletComponent } from '../dot-uve-palette-contentlet/dot-uve-palette-contentlet.component';
import { DotUVEPaletteContenttypeComponent } from '../dot-uve-palette-contenttype/dot-uve-palette-contenttype.component';

const EMPTY_SEARCH_PARAMS: Partial<DotPaletteSearchParams> = {
    selectedContentType: '',
    filter: '',
    page: 1
};

const DEBOUNCE_TIME = 300;

/**
 * Component for displaying and managing a list of content types in the UVE palette.
 * Supports grid/list view modes, sorting, filtering, and pagination.
 *
 * @example
 * ```html
 * <dot-uve-palette-list
 *   [type]="'content'"
 *   [languageId]="1"
 *   [pagePath]="'/home'"
 *   [variantId]="'1'" />
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
        DotMessagePipe,
        ContextMenuModule
    ],
    providers: [DotPaletteListStore, DotESContentService],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent implements OnInit {
    @ViewChild('menu') menu!: { toggle: (event: Event) => void };
    @ViewChild('favoritesPanel') favoritesPanel?: DotFavoriteSelectorComponent;

    $type = input.required<DotUVEPaletteListTypes>({ alias: 'listType' });
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    readonly #globalStore = inject(GlobalStore);
    readonly #paletteListStore = inject(DotPaletteListStore);
    readonly #dotFavoriteContentTypeService = inject(DotFavoriteContentTypeService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #messageService = inject(MessageService);
    readonly #destroyRef = inject(DestroyRef);

    readonly searchControl = new FormControl('', { nonNullable: true });
    protected readonly DotUVEPaletteListView = DotUVEPaletteListView;
    protected readonly LOADING_ROWS = LOADING_ROWS_MOCK;

    protected readonly $skipNextSearch = signal(false);
    protected readonly $contextMenuItems = signal<MenuItem[]>([]);
    protected readonly $isSearching = signal<boolean>(false);
    protected readonly $shouldHideControls = signal<boolean>(true);
    protected readonly $siteId = this.#globalStore.currentSiteId;
    protected readonly $contenttypes = this.#paletteListStore.contenttypes;
    protected readonly $contentlets = this.#paletteListStore.contentlets;
    protected readonly $pagination = this.#paletteListStore.pagination;
    protected readonly $currentView = this.#paletteListStore.currentView;
    protected readonly $isLoading = this.#paletteListStore.$isLoading;
    protected readonly $isEmpty = this.#paletteListStore.$isEmpty;
    protected readonly $layoutMode = this.#paletteListStore.layoutMode;
    protected readonly $showListLayout = this.#paletteListStore.$showListLayout;
    protected readonly $isContentletsView = this.#paletteListStore.$isContentletsView;
    protected readonly $isContentTypesView = this.#paletteListStore.$isContentTypesView;
    protected readonly $isFavoritesList = this.#paletteListStore.$isFavoritesList;
    protected readonly status$ = toObservable(this.#paletteListStore.status);

    /**
     * Computed signal to determine the start index for the pagination.
     * @returns The start index for the pagination.
     */
    protected readonly $start = computed(() => {
        const currentPage = this.$pagination().currentPage;
        const perPage = this.$pagination().perPage;
        return (currentPage - 1) * perPage;
    });

    /**
     * Computed signal to determine the action button object.
     * @returns The action button object.
     */
    protected readonly $actionButton = computed(() => {
        if (this.$isFavoritesList()) {
            return {
                testId: 'add-favorites-button',
                icon: 'pi pi-plus',
                onClick: (event: Event) => this.favoritesPanel?.toggle(event)
            };
        }

        return {
            testId: 'sort-menu-button',
            icon: 'pi pi-arrow-right-arrow-left',
            onClick: (event: Event) => this.menu.toggle(event)
        };
    });

    /**
     * Computed signal to determine the menu items.
     * @returns The menu items.
     */
    protected $menuItems = computed(() => {
        const currentSort = this.#paletteListStore.$currentSort();
        const viewMode = this.$layoutMode();
        const onSortSelect = (sortOption: DotPaletteSortOption) => this.onSortSelect(sortOption);
        const onViewSelect = (viewOption: DotPaletteViewMode) => this.onViewSelect(viewOption);

        return buildPaletteMenuItems({ viewMode, currentSort, onSortSelect, onViewSelect });
    });

    /**
     * Getter to determine the empty message object.
     * @returns The empty message object.
     */
    protected readonly $emptyState = computed(() => {
        if (this.$isSearching()) {
            return EMPTY_MESSAGE_SEARCH;
        }

        if (this.$isContentletsView()) {
            return EMPTY_MESSAGE_CONTENTLETS;
        }

        return EMPTY_MESSAGES[this.$type()];
    });

    /**
     * Updates controls visibility whenever the current view changes between content types and contentlets.
     *
     * Automatically triggered when `$currentView` signal changes to ensure controls are only shown
     * when the palette has loaded items, following UX/UI design requirements.
     */
    protected readonly $handleViewChange = signalMethod<() => void>((_view) => {
        this.#updateControlsVisibility();
    });

    constructor() {
        // React to input changes and fetch content types
        effect(() => {
            const params = {
                pagePathOrId: this.$pagePath(),
                language: this.$languageId(),
                variantId: this.$variantId(),
                listType: this.$type(),
                host: this.$siteId()
            };

            // Use untracked to prevent writes during effect
            untracked(() => this.#paletteListStore.getContentTypes(params));
        });

        this.$handleViewChange(this.$currentView);
    }

    ngOnInit() {
        this.searchControl.valueChanges
            .pipe(
                debounceTime(DEBOUNCE_TIME),
                distinctUntilChanged(),
                filter(() => this.#shouldRunSearch()),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((filter) => {
                this.$isSearching.set(filter.trim().length > 0);
                this.#loadItems({ filter, page: 1 });
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

        this.#loadItems({ page });
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
    protected onSelectContentType(selectedContentType: string) {
        this.#paletteListStore.getContentlets({ ...EMPTY_SEARCH_PARAMS, selectedContentType });
        this.#resetSearch();
    }

    /**
     * Handles returning to the content types view from contentlets drill-down.
     * Store handles filter reset and page reset automatically.
     */
    protected onBackToContentTypes() {
        this.#paletteListStore.getContentTypes(EMPTY_SEARCH_PARAMS);
        this.#resetSearch();
    }

    protected onContextMenu(contentType: DotCMSContentTypePalette) {
        const isFavorite = this.#dotFavoriteContentTypeService.isFavorite(contentType.id);
        const label = isFavorite
            ? 'uve.palette.menu.favorite.option.remove'
            : 'uve.palette.menu.favorite.option.add';
        const command = isFavorite
            ? () => this.#removeFavorite(contentType)
            : () => this.#addFavorite(contentType);
        this.$contextMenuItems.set([{ label: this.#dotMessageService.get(label), command }]);
    }

    /**
     * Handles clicks on the empty state message.
     * Opens the favorites panel when a span element is clicked.
     * @param event - The click event
     */
    protected onEmptyStateClick(event: Event) {
        const target = event.target as HTMLElement;
        const isTargetSpan = target.tagName === 'SPAN' || target.closest('span');

        if (!isTargetSpan) {
            return;
        }

        this.favoritesPanel?.toggle(event);
    }

    /**
     * Remove a content type from favorites.
     * @param contentType - The content type to remove.
     */
    #removeFavorite(contentType: DotCMSContentTypePalette) {
        this.#paletteListStore.removeFavorite(contentType.id);
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
    #addFavorite(contentType: DotCMSContentTypePalette) {
        this.#paletteListStore.addFavorite(contentType);
        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('uve.palette.favorite.add.success.summary'),
            detail: this.#dotMessageService.get('uve.palette.favorite.add.success.detail'),
            life: 3000
        });
    }

    /**
     * Clears the search input without triggering another request.
     * Keeps debounced listeners quiet when switching views manually.
     */
    #resetSearch() {
        if (!this.$isSearching()) {
            // Search is already empty, nothing to do
            return;
        }

        // Search has text, clear it and skip the debounced search trigger
        this.$skipNextSearch.set(true);
        this.$isSearching.set(false);
        this.searchControl.setValue('');
    }

    /**
     * Dispatches the appropriate store fetch based on the current view.
     * Accepts any partial search params so pagination and filtering can reuse it.
     *
     * @param params - Partial palette search params (filter/page/order/etc.)
     */
    #loadItems(params: Partial<DotPaletteSearchParams>) {
        if (this.$isContentTypesView()) {
            this.#paletteListStore.getContentTypes(params);
            return;
        }

        this.#paletteListStore.getContentlets(params);
    }

    /**
     * Determines if the search should be run.
     * @returns True if the search should be run, false otherwise.
     */
    #shouldRunSearch() {
        if (this.$skipNextSearch()) {
            this.$skipNextSearch.set(false);
            return false;
        }
        return true;
    }

    /**
     * Updates the visibility of palette controls based on the current load status.
     *
     * This method listens for status changes (EMPTY or LOADED) and toggles the controls visibility:
     * - Hides controls when the palette is empty (no items to manage)
     * - Shows controls when the palette has loaded items
     *
     * Called automatically whenever the view changes between content types and contentlets views,
     * ensuring controls are only displayed when they make sense from a UX/UI perspective.
     *
     * @private
     */
    #updateControlsVisibility() {
        this.status$
            .pipe(
                filter(
                    (status) =>
                        status === DotPaletteListStatus.EMPTY ||
                        status === DotPaletteListStatus.LOADED
                ),
                take(1)
            )
            .subscribe((status) => {
                this.$shouldHideControls.set(status === DotPaletteListStatus.EMPTY);
            });
    }
}

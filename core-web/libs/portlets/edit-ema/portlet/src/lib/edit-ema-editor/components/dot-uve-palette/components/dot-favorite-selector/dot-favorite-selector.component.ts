import { Subject } from 'rxjs';

import { Component, DestroyRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { Listbox, ListboxModule } from 'primeng/listbox';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';

import { debounceTime, finalize, switchMap } from 'rxjs/operators';

import {
    DotContentTypeQueryParams,
    DotFavoriteContentTypeService,
    DotPageContentTypeService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPaletteListStore } from '../dot-uve-palette-list/store/store';

/**
 * Debounce time in milliseconds used to throttle filter requests
 * to the content types endpoint while the user types.
 */
const FILTER_DEBOUNCE_TIME = 300;
/**
 * Set of base content type categories included in the selector.
 */
const CONTENT_TYPE_CATEGORIES = [
    DotCMSBaseTypesContentTypes.CONTENT,
    DotCMSBaseTypesContentTypes.FILEASSET,
    DotCMSBaseTypesContentTypes.DOTASSET,
    DotCMSBaseTypesContentTypes.WIDGET
];

@Component({
    selector: 'dot-favorite-selector',
    imports: [FormsModule, ListboxModule, DotMessagePipe, OverlayPanelModule],
    templateUrl: './dot-favorite-selector.component.html',
    styleUrls: ['./dot-favorite-selector.component.scss']
})
/**
 * Favorite selector overlay used in the UVE palette.
 *
 * Provides a searchable, multi-select list of content types so users can
 * add or remove favorites. The list supports client-side filtering with
 * debouncing, and persists selections using the favorites service.
 */
export class DotFavoriteSelectorComponent implements OnInit {
    /** Reference to the overlay panel instance controlling visibility. */
    $overlayPanel = viewChild.required(OverlayPanel);
    /** Reference to the PrimeNG Listbox to manage its built-in filter. */
    $listbox = viewChild.required(Listbox);
    readonly #store = inject(DotPaletteListStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly #pageContentTypeService = inject(DotPageContentTypeService);
    readonly #favoriteContentTypeService = inject(DotFavoriteContentTypeService);

    /** Full list of content types matching the current filter. */
    readonly $contenttypes = signal<DotCMSContentType[]>([]);
    /** Currently selected favorite content types. */
    readonly $selectedContentTypes = signal<DotCMSContentType[]>([]);
    /** Loading indicator for filter requests. */
    readonly $fetchingItems = signal<boolean>(false);

    /**
     * Internal subject used to debounce the filter value before
     * querying the content types service.
     */
    readonly #filterSubject$ = new Subject<string>();

    /**
     * Initializes reactivity wiring for favorites synchronization and
     * debounced filtering behavior.
     */
    constructor() {
        this.#setupFavoritesSync();
        this.#setupFilterDebounce();
    }

    /**
     * Component init lifecycle hook. Loads the initial set of content types.
     */
    ngOnInit(): void {
        this.#loadAllContentTypes();
    }

    /**
     * Toggles the overlay panel visibility.
     * @param event Browser event originating from the trigger element.
     */
    toggle(event: Event): void {
        this.$overlayPanel().toggle(event);
    }

    /**
     * Resets the Listbox filter and triggers a fresh search
     * when the overlay panel is hidden.
     */
    protected onHide(): void {
        this.$listbox().resetFilter();
        this.onFilter({ filter: '' });
    }

    /**
     * Handles filter input changes. The value is debounced before
     * hitting the API to reduce requests while typing.
     * @param event Object containing the current filter string.
     */
    protected onFilter(event: { filter: string }): void {
        this.$fetchingItems.set(true);
        this.#filterSubject$.next(event.filter);
    }

    /**
     * Handles selection changes and persists favorites, then updates
     * the palette list store so the UI reflects the latest state.
     * @param value Selected content types from the Listbox.
     */
    protected onSelectionChange({ value }: { value: DotCMSContentType[] }): void {
        const contenttypes = this.#favoriteContentTypeService.set(value);
        this.#store.setContentTypesFromFavorite(contenttypes);
    }

    /**
     * Syncs the selected favorites whenever the store's content types change,
     * ensuring the selector reflects updates made elsewhere in the app.
     */
    #setupFavoritesSync(): void {
        toObservable(this.#store.contenttypes)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                this.$selectedContentTypes.set(this.#favoriteContentTypeService.getAll());
            });
    }

    /**
     * Wires the debounced filter stream to the content types service,
     * updating the visible options and managing the loading indicator.
     */
    #setupFilterDebounce(): void {
        this.#filterSubject$
            .pipe(
                debounceTime(FILTER_DEBOUNCE_TIME),
                switchMap((filter) => this.#getContentTypes(filter)),
                finalize(() => this.$fetchingItems.set(false)),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ contenttypes }) => {
                this.$contenttypes.set(contenttypes);
                this.$fetchingItems.set(false);
            });
    }

    /**
     * Loads all content types with an empty filter.
     * Invoked during initialization.
     */
    #loadAllContentTypes(): void {
        this.#getContentTypes('')
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(({ contenttypes }) => this.$contenttypes.set(contenttypes));
    }

    /**
     * Retrieves content types using the provided filter and default
     * sorting and category constraints.
     * @param filter Filter string for server-side matching.
     */
    #getContentTypes(filter: string) {
        const params: DotContentTypeQueryParams = {
            filter,
            orderby: 'name',
            direction: 'ASC',
            types: CONTENT_TYPE_CATEGORIES,
            per_page: 10
        };

        return this.#pageContentTypeService.getAllContentTypes(params);
    }
}

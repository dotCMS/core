import { Subject } from 'rxjs';

import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';

import { debounceTime, switchMap } from 'rxjs/operators';

import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentTypeQueryParams } from '../../models';
import { DotPageContentTypeService } from '../../service/dot-page-contenttype.service';
import { DotPageFavoriteContentTypeService } from '../../service/dot-page-favorite-contentType.service';
import { DotPaletteListStore } from '../dot-uve-palette-list/store/store';

const FILTER_DEBOUNCE_TIME = 500;
const CONTENT_TYPE_CATEGORIES = [
    DotCMSBaseTypesContentTypes.CONTENT,
    DotCMSBaseTypesContentTypes.FILEASSET,
    DotCMSBaseTypesContentTypes.DOTASSET,
    DotCMSBaseTypesContentTypes.WIDGET
];

@Component({
    selector: 'dot-favorite-selector',
    imports: [FormsModule, ListboxModule, DotMessagePipe],
    templateUrl: './dot-favorite-selector.component.html',
    styleUrls: ['./dot-favorite-selector.component.scss']
})
export class DotFavoriteSelectorComponent implements OnInit {
    readonly #store = inject(DotPaletteListStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly #pageContentTypeService = inject(DotPageContentTypeService);
    readonly #favoriteContentTypeService = inject(DotPageFavoriteContentTypeService);

    // Signals
    readonly $contenttypes = signal<DotCMSContentType[]>([]);
    readonly $selectedContentTypes = signal<DotCMSContentType[]>([]);

    // Filter subject for debouncing
    readonly #filterSubject$ = new Subject<string>();

    constructor() {
        this.#setupFavoritesSync();
        this.#setupFilterDebounce();
    }

    ngOnInit(): void {
        this.#loadAllContentTypes();
    }

    /**
     * Handle filter input changes with debounce
     */
    onFilter(event: { filter: string }): void {
        this.#filterSubject$.next(event.filter);
    }

    /**
     * Handle selection changes in the listbox
     */
    onSelectionChange({ value }: { value: DotCMSContentType[] }): void {
        const contenttypes = this.#favoriteContentTypeService.set(value);
        this.#store.setContentTypesFromFavorite(contenttypes);
    }

    /**
     * Sync selected favorites whenever store changes
     */
    #setupFavoritesSync(): void {
        toObservable(this.#store.contenttypes)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                this.$selectedContentTypes.set(this.#favoriteContentTypeService.getAll());
            });
    }

    /**
     * Setup filter with debounce
     */
    #setupFilterDebounce(): void {
        this.#filterSubject$
            .pipe(
                debounceTime(FILTER_DEBOUNCE_TIME),
                switchMap((filter) => this.#getContentTypes(filter)),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ contenttypes }) => {
                this.$contenttypes.set(contenttypes);
            });
    }

    /**
     * Load all content types on init
     */
    #loadAllContentTypes(): void {
        this.#getContentTypes('')
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(({ contenttypes }) => this.$contenttypes.set(contenttypes));
    }

    /**
     * Get content types with filter
     */
    #getContentTypes(filter: string) {
        const params: DotContentTypeQueryParams = {
            filter,
            orderby: 'name',
            direction: 'ASC',
            types: CONTENT_TYPE_CATEGORIES
        };

        return this.#pageContentTypeService.getAllContentTypes(params);
    }
}

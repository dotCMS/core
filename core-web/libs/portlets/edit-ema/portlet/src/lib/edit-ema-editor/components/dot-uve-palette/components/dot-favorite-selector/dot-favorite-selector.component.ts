import { CommonModule } from '@angular/common';
import { Component, inject, input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';

import { take } from 'rxjs/operators';

import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DotContentTypeParams } from '../../service/dot-page-contenttype.service';
import { DotPaletteListStore } from '../dot-uve-palette-list/store/store';

@Component({
    selector: 'dot-favorite-selector',
    standalone: true,
    imports: [CommonModule, FormsModule, ListboxModule],
    templateUrl: './dot-favorite-selector.component.html',
    styleUrls: ['./dot-favorite-selector.component.scss']
})
export class DotFavoriteSelectorComponent implements OnInit {
    readonly store = inject(DotPaletteListStore);

    // Input for the page path or ID to manage favorites
    $pagePath = input.required<string>({ alias: 'pagePath' });

    selectedContentTypes: DotCMSContentType[] = [];
    contenttypes: DotCMSContentType[] = [];
    filterValue = '';

    ngOnInit(): void {
        const pageId = this.$pagePath();
        if (pageId) {
            this.loadContentTypes();
        }
    }

    /**
     * Load content types with current filter and pagination
     */
    loadContentTypes() {
        const params: DotContentTypeParams = {
            filter: this.filterValue,
            orderby: 'name',
            direction: 'ASC'
        };

        this.store
            .getAllContentTypes(params)
            .pipe(take(1))
            .subscribe(({ contenttypes }) => {
                this.contenttypes = contenttypes;
                this.selectedContentTypes = this.getSelectedContentTypes();
            });
    }

    /**
     * Handle filter input changes
     */
    onFilter(event: { filter: string }) {
        this.filterValue = event.filter;
        this.loadContentTypes();
    }

    /**
     * Handle checkbox changes for favorites
     */
    onSelectionChange(event) {
        this.store.saveFavoriteContentTypes(this.$pagePath(), event.value);
    }

    /**
     * Check if a content type is marked as favorite
     */
    isContentTypeFavorite(contentTypeId: string): boolean {
        return this.store.getIsFavoriteContentType(this.$pagePath(), contentTypeId);
    }

    /**
     * Initialize selected content types based on favorites
     */
    getSelectedContentTypes() {
        return this.store.getAllFavoriteContentTypes(this.$pagePath(), this.filterValue)
            .contenttypes;
    }
}

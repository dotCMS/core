import { Component } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { DotPagesState, DotPageStore } from './dot-pages-store/dot-pages.store';

@Component({
    selector: 'dot-pages',
    templateUrl: './dot-pages.component.html',
    styleUrls: ['./dot-pages.component.scss'],
    providers: [DotPageStore]
})
export class DotPagesComponent {
    vm$: Observable<DotPagesState> = this.store.vm$;

    constructor(private store: DotPageStore) {
        this.store.setInitialStateData();
    }

    toggleFavoritePagesData(areAllFavoritePagesLoaded: boolean): void {
        if (areAllFavoritePagesLoaded) {
            this.store.limitFavoritePages();
        } else {
            this.store.loadAllFavoritePages();
        }
    }
}

import { Injectable } from '@angular/core';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { PaginatorService } from '@dotcms/app/api/services/paginator';
import { ESContent } from '@dotcms/app/shared/models/dot-es-content/dot-es-content.model';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { ComponentStore } from '@ngrx/component-store';
import { LazyLoadEvent } from 'primeng/api';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

export interface DotPaletteState {
    contentlets: DotCMSContentlet[] | DotCMSContentType[];
    contentTypes: DotCMSContentType[];
    filter: string;
    languageId: string;
    totalRecords: number;
    viewContentlet: string;
    loading: boolean;
}

export const enum LoadingState {
    INIT = 'INIT',
    LOADING = 'LOADING',
    LOADED = 'LOADED'
}

@Injectable()
export class DotPaletteStore extends ComponentStore<DotPaletteState> {
    constructor(
        public paginatorESService: DotESContentService,
        public paginationService: PaginatorService
    ) {
        super({
            contentlets: null,
            contentTypes: null,
            filter: '',
            languageId: '1',
            totalRecords: 0,
            viewContentlet: 'contentlet:out',
            loading: false
        });
    }

    /**
     * Request contentlets data with filter and pagination params.
     *
     * @param LazyLoadEvent [event]
     * @memberof DotPaletteStore
     */
    getContentletsData(event?: LazyLoadEvent): void {
        this.setLoading();

        this.state$.pipe(take(1)).subscribe(({ filter, languageId }) => {
            if (this.isFormContentType) {
                this.paginationService.setExtraParams('filter', filter);

                this.paginationService
                    .getWithOffset((event && event.first) || 0)
                    .pipe(take(1))
                    .subscribe((data: DotCMSContentlet[] | DotCMSContentType[]) => {
                        data.forEach((item) => (item.contentType = item.variable = 'FORM'));
                        this.setLoaded();
                        this.setContentlets(data);
                        this.setTotalRecords(this.paginationService.totalRecords);
                    });
            } else {
                this.paginatorESService
                    .get({
                        itemsPerPage: this.itemsPerPage,
                        lang: languageId || '1',
                        filter: filter || '',
                        offset: (event && event.first.toString()) || '0',
                        query: `+contentType: ${this.contentTypeVarName}`
                    })
                    .pipe(take(1))
                    .subscribe((response: ESContent) => {
                        this.setLoaded();
                        this.setTotalRecords(response.resultsSize);
                        this.setContentlets(response.jsonObjectView.contentlets);
                    });
            }
        });
    }

    /**
     * Sets value to show/hide components, clears filter value and starts loding data
     *
     * @param string [variableName]
     * @memberof DotPaletteContentletsComponent
     */
    switchView(variableName?: string): void {
        const viewContentlet = variableName ? 'contentlet:in' : 'contentlet:out';
        this.setViewContentlet(viewContentlet);
        this.setFilter('');
        this.loadContentlets(variableName);
    }

    private isFormContentType: boolean;
    private itemsPerPage = 25;
    private contentTypeVarName: string;

    readonly vm$ = this.state$;

    // UPDATERS
    private readonly setContentlets = this.updater(
        (state: DotPaletteState, data: DotCMSContentlet[] | DotCMSContentType[]) => {
            return { ...state, contentlets: data };
        }
    );

    private readonly setContentTypes = this.updater(
        (state: DotPaletteState, data: DotCMSContentType[]) => {
            return { ...state, contentTypes: data };
        }
    );

    private readonly setTotalRecords = this.updater((state: DotPaletteState, data: number) => {
        return { ...state, totalRecords: data };
    });

    readonly setFilter = this.updater((state: DotPaletteState, data: string) => {
        return { ...state, filter: data };
    });

    readonly setLanguageId = this.updater((state: DotPaletteState, data: string) => {
        return { ...state, languageId: data };
    });

    readonly setViewContentlet = this.updater((state: DotPaletteState, data: string) => {
        return { ...state, viewContentlet: data };
    });

    readonly setLoading = this.updater((state: DotPaletteState) => {
        return {
            ...state,
            loading: LoadingState.LOADING === LoadingState.LOADING
        };
    });

    readonly setLoaded = this.updater((state: DotPaletteState) => {
        return {
            ...state,
            loading: !(LoadingState.LOADED === LoadingState.LOADED)
        };
    });

    // EFFECTS
    readonly loadContentTypes = this.effect((data$: Observable<DotCMSContentType[]>) => {
        return data$.pipe(
            map((data) => {
                this.setContentTypes(data);
            })
        );
    });

    readonly filterContentlets = this.effect((filterValue$: Observable<string>) => {
        return filterValue$.pipe(
            map((value: string) => {
                this.setFilter(value);

                if (this.isFormContentType) {
                    this.paginationService.searchParam = 'variable';
                    this.paginationService.filter = value;
                }
                this.getContentletsData({ first: 0 });
            })
        );
    });

    readonly loadContentlets = this.effect((contentTypeVariable$: Observable<string>) => {
        return contentTypeVariable$.pipe(
            map((contentTypeVariable: string) => {
                this.contentTypeVarName = contentTypeVariable;
                this.isFormContentType = contentTypeVariable === 'forms';
                if (this.isFormContentType) {
                    this.paginationService.url = `v1/contenttype`;
                    this.paginationService.paginationPerPage = this.itemsPerPage;
                    this.paginationService.sortField = 'modDate';
                    this.paginationService.setExtraParams('type', 'Form');
                    this.paginationService.sortOrder = 1;
                }

                this.getContentletsData();
            })
        );
    });
}

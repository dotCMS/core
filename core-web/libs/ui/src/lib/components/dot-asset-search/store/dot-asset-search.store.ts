import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';

import { Injectable, inject } from '@angular/core';

import { Observable } from 'rxjs/internal/Observable';
import { map, switchMap, tap } from 'rxjs/operators';

import {
    DotContentSearchService,
    ESOrderDirectionSearch,
    EsQueryParamsSearch,
    DotLanguagesService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotLanguage } from '@dotcms/dotcms-models';

export interface DotAssetSearch {
    loading: boolean;
    preventScroll: boolean;
    contentlets: DotCMSContentlet[];
}

interface DotAssetSeachQuery {
    search: string;
    assetType: string;
    offset?: number;
    languageId?: number | string;
}

const defaultState: DotAssetSearch = {
    loading: true,
    preventScroll: false,
    contentlets: []
};

@Injectable()
export class DotAssetSearchStore extends ComponentStore<DotAssetSearch> {
    private dotContentSearchService = inject(DotContentSearchService);
    private dotLanguagesService = inject(DotLanguagesService);

    // Selectors
    readonly vm$ = this.select((state) => state);

    readonly updateContentlets = this.updater<DotCMSContentlet[]>((_state, contentlets) => ({
        contentlets,
        preventScroll: !contentlets?.length,
        loading: false
    }));

    readonly mergeContentlets = this.updater<DotCMSContentlet[]>((state, contentlets) => ({
        contentlets: [...state.contentlets, ...contentlets],
        preventScroll: !contentlets?.length,
        loading: false
    }));

    readonly updateLoading = this.updater<boolean>((state, loading) => {
        return {
            ...state,
            loading
        };
    });

    private languages: { [key: string]: DotLanguage } = {};

    constructor() {
        super(defaultState);

        this.dotLanguagesService.get().subscribe((languages) => {
            languages.forEach((lang) => {
                this.languages[lang.id] = lang;
            });
        });
    }

    /**
     * Search for contentlets
     *
     * @memberof DotAssetSearchStore
     */
    readonly searchContentlet = this.effect((params$: Observable<DotAssetSeachQuery>) => {
        return params$.pipe(
            tap(() => this.updateLoading(true)),
            switchMap((params) => {
                return this.searchContentletsRequest(params).pipe(
                    tapResponse(
                        (contentlets) => this.updateContentlets(contentlets),
                        (_error) => {
                            /* */
                        }
                    )
                );
            })
        );
    });

    /**
     * Load more contentlets
     *
     * @memberof DotAssetSearchStore
     */
    readonly nextBatch = this.effect((params$: Observable<DotAssetSeachQuery>) => {
        return params$.pipe(
            switchMap((params) =>
                this.searchContentletsRequest(params).pipe(
                    tapResponse(
                        (contentlets) => this.mergeContentlets(contentlets),
                        (_error) => {
                            /* */
                        }
                    )
                )
            )
        );
    });

    private searchContentletsRequest(params): Observable<DotCMSContentlet[]> {
        const query = this.queryParams(params);

        return this.dotContentSearchService.get(query).pipe(
            map(({ jsonObjectView: { contentlets } }) => {
                return this.setContentletLanguage(contentlets);
            })
        );
    }

    private queryParams(data): EsQueryParamsSearch {
        const { search, assetType, offset = 0, languageId = '' } = data;
        const filter = search.includes('-') ? search : `${search}*`;

        return {
            query: `+catchall:${filter} title:'${search}'^15 +languageId:${languageId} +baseType:(4 OR 9) +metadata.contenttype:${
                assetType || ''
            }/* +deleted:false +working:true`,
            sortOrder: ESOrderDirectionSearch.ASC,
            limit: 20,
            offset
        };
    }

    /**
     * This method add the Language to the contentets based on their languageId
     *
     * @private
     * @param {DotCMSContentlet[]} contentlets
     * @return {*}
     * @memberof ImageTabviewFormComponent
     */
    private setContentletLanguage(contentlets: DotCMSContentlet[]) {
        return contentlets.map((contentlet) => {
            return {
                ...contentlet,
                language: this.getLanguageBadge(contentlet.languageId)
            };
        });
    }

    private getLanguageBadge(languageId: number): string {
        const { languageCode, countryCode } = this.languages[languageId] || {};

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}

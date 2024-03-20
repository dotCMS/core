import { ComponentStore } from '@ngrx/component-store';

import { Injectable } from '@angular/core';

import { Observable } from 'rxjs/internal/Observable';
import { map, mergeMap, switchMap, tap, withLatestFrom } from 'rxjs/operators';

import {
    DotContentSearchService,
    ESOrderDirectionSearch,
    EsQueryParamsSearch,
    DotLanguagesService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotLanguage, EditorAssetTypes } from '@dotcms/dotcms-models';

export interface DotImageSearchState {
    loading: boolean;
    preventScroll: boolean;
    contentlets: DotCMSContentlet[];
    languageId: number | string;
    search: string;
    assetType: EditorAssetTypes;
}

const defaultState: DotImageSearchState = {
    loading: true,
    preventScroll: false,
    contentlets: [],
    languageId: '*',
    search: '',
    assetType: 'image'
};

@Injectable()
export class DotAssetSearchStore extends ComponentStore<DotImageSearchState> {
    // Selectors
    readonly vm$ = this.select(({ contentlets, loading, preventScroll }) => ({
        contentlets,
        loading,
        preventScroll
    }));

    // Setters
    readonly updateContentlets = this.updater<DotCMSContentlet[]>((state, contentlets) => {
        return {
            ...state,
            contentlets
        };
    });

    readonly updateAssetType = this.updater<EditorAssetTypes>((state, assetType) => {
        return {
            ...state,
            assetType
        };
    });

    readonly updatelanguageId = this.updater<number>((state, languageId) => {
        return {
            ...state,
            languageId
        };
    });

    readonly updateLoading = this.updater<boolean>((state, loading) => {
        return {
            ...state,
            loading
        };
    });

    readonly updatePreventScroll = this.updater<boolean>((state, preventScroll) => {
        return {
            ...state,
            preventScroll
        };
    });

    readonly updateSearch = this.updater<string>((state, search) => {
        return {
            ...state,
            loading: true,
            search
        };
    });

    // Effects
    readonly init = this.effect((origin$: Observable<EditorAssetTypes>) => {
        return origin$.pipe(
            tap((assetType) => this.updateAssetType(assetType)),
            switchMap(() =>
                this.dotLanguagesService.get().pipe(
                    tap((languages) => {
                        languages.forEach((lang) => {
                            this.languages[lang.id] = lang;
                        });
                    })
                )
            ),
            withLatestFrom(this.state$),
            switchMap(([_, state]) => this.searchContentletsRequest(this.params(state), []))
        );
    });

    readonly searchContentlet = this.effect((origin$: Observable<string>) => {
        return origin$.pipe(
            tap((search) => this.updateSearch(search)),
            withLatestFrom(this.state$),
            mergeMap(([search, state]) =>
                this.searchContentletsRequest(this.params({ ...state, search }), [])
            )
        );
    });

    readonly nextBatch = this.effect((origin$: Observable<number>) => {
        return origin$.pipe(
            withLatestFrom(this.state$),
            map(([offset, state]) => ({ ...state, offset })),
            mergeMap(({ contentlets, ...data }) =>
                this.searchContentletsRequest(this.params(data), contentlets)
            )
        );
    });

    private languages: { [key: string]: DotLanguage } = {};

    constructor(
        private DotContentSearchService: DotContentSearchService,
        private dotLanguagesService: DotLanguagesService
    ) {
        super(defaultState);
    }

    private searchContentletsRequest(params, prev: DotCMSContentlet[]) {
        return this.DotContentSearchService.get(params).pipe(
            map(({ jsonObjectView: { contentlets } }) => {
                const items = this.setContentletLanguage(contentlets);
                this.updateLoading(false);
                this.updatePreventScroll(!contentlets?.length);

                return this.updateContentlets([...prev, ...items]);
            })
        );
    }

    private params(data): EsQueryParamsSearch {
        const { search, assetType, offset = 0, languageId = '' } = data;
        const filter = search.includes('-') ? search : `${search}*`;
        const languageQuery = languageId ? `+languageId:${languageId}` : '';

        return {
            query: `+catchall:${filter} +title:'${search}'^15 ${languageQuery} +baseType:(4 OR 9) +metadata.contenttype:${
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
                language: this.getLanguage(contentlet.languageId)
            };
        });
    }

    private getLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.languages[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}

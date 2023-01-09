import { Injectable } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { ComponentStore } from '@ngrx/component-store';

import { Languages, queryEsParams } from '@dotcms/block-editor';
import { mergeMap, tap, map, withLatestFrom } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';
import { SearchService } from '../../../../../shared/services/search/search.service';

import { DEFAULT_LANG_ID } from '../../../../bubble-menu/models/index';
import { ESOrderDirection } from '@dotcms/block-editor';
import { DotLanguageService } from '../../../../../shared/services/dot-language/dot-language.service';

export interface DotImageSearchState {
    loading: boolean;
    preventScroll: boolean;
    contentlets: DotCMSContentlet[];
    languageId: number;
    search: string;
}

const defaultState: DotImageSearchState = {
    loading: true,
    preventScroll: false,
    contentlets: [],
    languageId: DEFAULT_LANG_ID,
    search: ''
};

@Injectable()
export class DotImageSearchStore extends ComponentStore<DotImageSearchState> {
    private languages: Languages;

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
            search
        };
    });

    // Effects
    readonly searchContentlet = this.effect((origin$: Observable<string>) => {
        return origin$.pipe(
            tap((search) => {
                this.updateLoading(true);
                this.updateSearch(search);
            }),
            withLatestFrom(this.state$),
            map(([search, state]) => ({ ...state, search })),
            mergeMap((data) => this.searchContentletsRequest(this.params({ ...data }), []))
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

    constructor(
        private searchService: SearchService,
        private dotLanguageService: DotLanguageService
    ) {
        super(defaultState);

        this.dotLanguageService.getLanguages().subscribe((languages) => {
            this.languages = languages;
            this.searchContentlet('');
        });
    }

    private searchContentletsRequest(params, prev: DotCMSContentlet[]) {
        return this.searchService.get(params).pipe(
            map(({ jsonObjectView: { contentlets } }) => {
                const items = this.setContentletLanguage(contentlets);
                this.updateLoading(false);
                this.updatePreventScroll(!contentlets?.length);

                return this.updateContentlets([...prev, ...items]);
            })
        );
    }

    private params({ search, offset = 0, languageId }): queryEsParams {
        return {
            query: ` +catchall:${search}* title:'${search}'^15 +languageId:${languageId} +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
            sortOrder: ESOrderDirection.ASC,
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

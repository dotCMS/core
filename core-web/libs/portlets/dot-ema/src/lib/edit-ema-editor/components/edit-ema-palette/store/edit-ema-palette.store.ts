import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, forkJoin } from 'rxjs';

import { Injectable } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { map, switchMap } from 'rxjs/operators';

import { DotContentTypeService, DotESContentService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { PALETTE_TYPES } from '../../../../shared/enums';

export interface DotPaletteState {
    contentlets: {
        filter: {
            query: string;
            contentTypeVarName: string;
        };
        items: DotCMSContentlet[];
        totalRecords: number;
    };
    contenttypes: {
        filter: string;
        items: DotCMSContentType[];
    };
    loading: boolean;
    currentPaletteType: PALETTE_TYPES;
    itemsPerPage: number;
}

@Injectable()
export class DotPaletteStore extends ComponentStore<DotPaletteState> {
    readonly vm$ = this.state$;
    private itemsPerPage = 4; //Search how to integrate this
    private contentTypeVarName: string; //Same

    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotESContentService: DotESContentService,
        private paginatorESService: DotESContentService
    ) {
        super({
            contentlets: {
                items: [],
                filter: { query: '', contentTypeVarName: '' },
                totalRecords: 0
            },
            contenttypes: { items: [], filter: '' },
            loading: false,
            currentPaletteType: PALETTE_TYPES.CONTENTTYPE,
            itemsPerPage: 4
        });
    }

    readonly loadContenttypesEffect = this.effect(
        (data$: Observable<{ filter: string; allowedContent: string[] }>) => {
            return data$.pipe(
                switchMap(({ allowedContent, filter }) => {
                    // const obs$ = allowedContent?.length
                    //     ? this.getFilteredContenttypes(filter, allowedContent)
                    //     : this.getWidgets(filter);
                    const obs$ = this.getFilteredContenttypes(filter, allowedContent);

                    return obs$.pipe(
                        tapResponse(
                            (contentTypes) => {
                                this.patchState({ contenttypes: { items: contentTypes, filter } });
                            },
                            // eslint-disable-next-line no-console
                            (err) => console.log(err)
                        )
                    );
                })
            );
        }
    );

    readonly loadContentletsEffect = this.effect(
        (
            data$: Observable<{
                filter: string;
                contenttypeName: string;
                languageId: string;
                page?: number;
            }>
        ) => {
            return data$.pipe(
                switchMap(({ filter, contenttypeName, languageId, page }) => {
                    return this.dotESContentService
                        .get({
                            itemsPerPage: this.itemsPerPage,
                            lang: languageId || '1',
                            filter: filter || '',
                            offset: ((page || 0) * this.itemsPerPage).toString() || '0',
                            query: `+contentType: ${contenttypeName} +deleted: false`.trim()
                        })
                        .pipe(
                            tapResponse(
                                (contentlets) => {
                                    this.patchState({
                                        contentlets: {
                                            items: contentlets.jsonObjectView.contentlets,
                                            filter: {
                                                query: filter,
                                                contentTypeVarName: contenttypeName
                                            },
                                            totalRecords: contentlets.resultsSize
                                        }
                                    });
                                },
                                // eslint-disable-next-line no-console
                                (err) => console.log(err)
                            )
                        );
                })
            );
        }
    );

    getFilteredContenttypes(filter: string, allowedContent: string[]) {
        return forkJoin([
            this.dotContentTypeService.filterContentTypes(filter, allowedContent.join(',')),
            this.dotContentTypeService.getContentTypes({ filter, page: 40, type: 'WIDGET' })
        ]).pipe(
            map((results) => {
                const [filteredContentTypes, widgets] = results;

                // Some pages bring widgets in the CONTENT_PALETTE_HIDDEN_CONTENT_TYPES, and others don't.
                // However, all pages allow widgets, so we make a request just to get them.
                // Full comment here: https://github.com/dotCMS/core/pull/22573#discussion_r921263060
                // This filter is used to prevent widgets from being repeated.
                const contentLets = filteredContentTypes.filter(
                    (item) => item.baseType !== 'WIDGET'
                );

                // Merge both array and order them by name
                const contentTypes = [...contentLets, ...widgets]
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .slice(0, 40);

                return contentTypes;
            })
        );
    }

    getWidgets(filter: string) {
        return this.dotContentTypeService.getContentTypes({ filter, page: 40, type: 'WIDGET' });
    }

    getContentlets(event?: LazyLoadEvent, languageId?: string, filter?: string) {
        return this.paginatorESService.get({
            itemsPerPage: this.itemsPerPage,
            lang: languageId || '1',
            filter: filter || '',
            offset: (event && event.first.toString()) || '0',
            query: `+contentType: ${this.contentTypeVarName} +deleted: false `.trim()
        });
    }
}

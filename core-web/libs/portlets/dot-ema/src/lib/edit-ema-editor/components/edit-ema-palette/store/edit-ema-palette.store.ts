import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, forkJoin } from 'rxjs';

import { Injectable } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotContentTypeService, DotESContentService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { PAGINATOR_ITEMS_PER_PAGE } from '../../../../shared/consts';
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
    private itemsPerPage = PAGINATOR_ITEMS_PER_PAGE;

    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotESContentService: DotESContentService
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
            itemsPerPage: PAGINATOR_ITEMS_PER_PAGE
        });
    }

    readonly changeView = this.updater((state, view: PALETTE_TYPES) => ({
        ...state,
        currentPaletteType: view
    }));

    readonly loadContentTypes = this.effect(
        (data$: Observable<{ filter: string; allowedContent: string[] }>) => {
            return data$.pipe(
                switchMap(({ allowedContent, filter }) => {
                    //     ? this.getFilteredContenttypes(filter, allowedContent)
                    //     : this.getWidgets(filter);
                    const obs$ = this.getFilteredContentTypes(filter, allowedContent);

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

    readonly loadContentlets = this.effect(
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

    private getFilteredContentTypes(filter: string, allowedContent: string[]) {
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
}

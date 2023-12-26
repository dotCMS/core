import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

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

    readonly setLoading = this.updater((state, loading: boolean) => ({
        ...state,
        loading
    }));

    readonly resetContentlets = this.updater((state) => ({
        ...state,
        contentlets: { items: [], filter: { query: '', contentTypeVarName: '' }, totalRecords: 0 }
    }));

    /**
     * Loads content types based on the provided filter and allowed content.
     * @param data$ An observable that emits an object containing the filter and allowed content.
     * @returns An observable that emits the loaded content types.
     */
    readonly loadContentTypes = this.effect(
        (data$: Observable<{ filter: string; allowedContent: string[] }>) => {
            this.setLoading(true);
            this.resetContentlets();

            return data$.pipe(
                switchMap(({ allowedContent, filter }) => {
                    const obs$ = this.getFilteredContentTypes(filter, allowedContent);

                    return obs$.pipe(
                        tapResponse(
                            (contentTypes) => {
                                this.patchState({ contenttypes: { items: contentTypes, filter } });
                                this.setLoading(false);
                            },
                            // eslint-disable-next-line no-console
                            (err) => console.log(err)
                        )
                    );
                })
            );
        }
    );

    /**
     * Loads contentlets based on the provided filter, content type name, language ID, and page number.
     * @param data$ An observable that emits an object containing the filter, content type name, language ID, and page number.
     * @returns An observable that emits the loaded contentlets.
     */
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
                tap(() => this.setLoading(true)),
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
                                    this.setLoading(false);
                                },
                                // eslint-disable-next-line no-console
                                (err) => console.log(err)
                            )
                        );
                })
            );
        }
    );

    /**
     * Retrieves filtered content types based on the provided filter and allowed content.
     * @param filter - The filter to apply to the content types.
     * @param allowedContent - An array of allowed content types.
     * @returns An Observable that emits the filtered content types.
     */
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
            }),
            catchError(() => EMPTY)
        );
    }
}

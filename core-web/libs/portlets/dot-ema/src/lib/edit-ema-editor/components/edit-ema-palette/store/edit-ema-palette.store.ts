import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { DotContentTypeService, DotESContentService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { PALETTE_PAGINATOR_ITEMS_PER_PAGE } from '../shared/edit-ema-palette.const';
import { EditEmaPaletteStoreStatus, PALETTE_TYPES } from '../shared/edit-ema-palette.enums';

export interface DotPaletteState {
    contentlets: {
        filter: {
            query: string;
            contentTypeVarName: string;
        };
        items: DotCMSContentlet[];
        totalRecords: number;
        itemsPerPage: number;
    };
    contenttypes: {
        filter: string;
        items: DotCMSContentType[];
    };
    status: EditEmaPaletteStoreStatus;
    currentPaletteType: PALETTE_TYPES;
}

@Injectable()
export class DotPaletteStore extends ComponentStore<DotPaletteState> {
    readonly vm$ = this.state$;

    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotESContentService: DotESContentService
    ) {
        super({
            contentlets: {
                items: [],
                filter: { query: '', contentTypeVarName: '' },
                totalRecords: 0,
                itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
            },
            contenttypes: { items: [], filter: '' },
            status: EditEmaPaletteStoreStatus.LOADING,
            currentPaletteType: PALETTE_TYPES.CONTENTTYPE
        });
    }

    readonly changeView = this.updater((state, view: PALETTE_TYPES) => ({
        ...state,
        currentPaletteType: view
    }));

    readonly setStatus = this.updater((state, status: EditEmaPaletteStoreStatus) => ({
        ...state,
        status
    }));

    readonly resetContentlets = this.updater((state) => ({
        ...state,
        contentlets: {
            items: [],
            filter: { query: '', contentTypeVarName: '' },
            totalRecords: 0,
            itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
        }
    }));

    /**
     * Loads content types based on the provided filter and allowed content.
     * @param data$ An observable that emits an object containing the filter and allowed content.
     * @returns An observable that emits the loaded content types.
     */
    readonly loadContentTypes = this.effect(
        (data$: Observable<{ filter: string; allowedContent: string[] }>) => {
            this.resetContentlets();

            return data$.pipe(
                switchMap(({ allowedContent, filter }) => {
                    return this.getFilteredContentTypes(filter, allowedContent).pipe(
                        tapResponse(
                            (contentTypes) => {
                                this.patchState({
                                    contenttypes: { items: contentTypes, filter },
                                    status: EditEmaPaletteStoreStatus.LOADED
                                });
                            },
                            (err) => {
                                this.setStatus(EditEmaPaletteStoreStatus.ERROR);
                                // eslint-disable-next-line no-console
                                console.log(err);
                            }
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
                tap(() => this.setStatus(EditEmaPaletteStoreStatus.LOADING)),
                switchMap(({ filter, contenttypeName, languageId, page }) => {
                    return this.dotESContentService
                        .get({
                            itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE,
                            lang: languageId || '1',
                            filter: filter || '',
                            offset:
                                ((page || 0) * PALETTE_PAGINATOR_ITEMS_PER_PAGE).toString() || '0',
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
                                            totalRecords: contentlets.resultsSize,
                                            itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
                                        },
                                        status: EditEmaPaletteStoreStatus.LOADED
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

    setInitialState() {
        this.setState({
            contentlets: {
                items: [],
                filter: { query: '', contentTypeVarName: '' },
                totalRecords: 0,
                itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
            },
            contenttypes: { items: [], filter: '' },
            status: EditEmaPaletteStoreStatus.LOADED,
            currentPaletteType: PALETTE_TYPES.CONTENTTYPE
        });
    }
}

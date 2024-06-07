import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotESContentService,
    DotPropertiesService
} from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotCMSContentType,
    DotContainerStructure,
    DotPageContainerStructure
} from '@dotcms/dotcms-models';

export enum PALETTE_TYPES {
    CONTENTTYPE = 'CONTENTTYPE',
    CONTENTLET = 'CONTENTLET'
}

export enum EditEmaPaletteStoreStatus {
    LOADING = 'LOADING',
    LOADED = 'LOADED',
    ERROR = 'ERROR'
}

export const PALETTE_PAGINATOR_ITEMS_PER_PAGE = 20;

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
    allowedTypes: string[];
    currentContentType?: string;
}

@Injectable()
export class DotPaletteStore extends ComponentStore<DotPaletteState> {
    private readonly dotConfigurationService = inject(DotPropertiesService);

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
            currentPaletteType: PALETTE_TYPES.CONTENTTYPE,
            allowedTypes: []
        });
    }

    readonly vm$ = this.select((state) => state);

    readonly setStatus = this.updater((state, status: EditEmaPaletteStoreStatus) => ({
        ...state,
        status
    }));

    readonly setAllowedTypes = this.updater((state, allowedTypes: string[]) => ({
        ...state,
        allowedTypes
    }));

    readonly setCurrentContentType = this.updater((state, currentContentType: string) => ({
        ...state,
        currentContentType
    }));

    readonly resetContentlets = this.updater((state) => ({
        ...state,
        contentlets: {
            items: [],
            filter: { query: '', contentTypeVarName: '' },
            totalRecords: 0,
            itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
        },
        currentPaletteType: PALETTE_TYPES.CONTENTTYPE
    }));

    /**
     *
     *
     * @memberof DotPaletteStore
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
     *
     *
     * @memberof DotPaletteStore
     */
    readonly loadContentlets = this.effect(
        (
            data$: Observable<{
                filter: string;
                contenttypeName: string;
                languageId: string;
                page?: number;
                variantId?: string;
            }>
        ) => {
            return data$.pipe(
                tap(() => this.setStatus(EditEmaPaletteStoreStatus.LOADING)),
                switchMap(({ filter, contenttypeName, languageId, variantId, page = 0 }) => {
                    this.setCurrentContentType(contenttypeName);

                    const variantTerm = variantId
                        ? `+variant:(${DEFAULT_VARIANT_ID} OR ${variantId})`
                        : `+variant:${DEFAULT_VARIANT_ID}`;

                    return this.dotESContentService
                        .get({
                            itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE,
                            lang: languageId || '1',
                            filter: filter || '',
                            offset: (page * PALETTE_PAGINATOR_ITEMS_PER_PAGE).toString(),
                            query: `+contentType:${contenttypeName} +deleted:false ${variantTerm}`.trim()
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
                                        status: EditEmaPaletteStoreStatus.LOADED,
                                        currentPaletteType: PALETTE_TYPES.CONTENTLET
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
     *
     *
     * @memberof DotPaletteStore
     */
    loadAllowedContentTypes = this.effect(
        (data$: Observable<{ containers: DotPageContainerStructure }>) => {
            return data$.pipe(
                switchMap(({ containers }) => {
                    return this.dotConfigurationService
                        .getKeyAsList('CONTENT_PALETTE_HIDDEN_CONTENT_TYPES')
                        .pipe(
                            tapResponse(
                                (results) => {
                                    const allowedContentTypes =
                                        this.filterAllowedContentTypes(containers, results) || [];

                                    this.setAllowedTypes(allowedContentTypes);
                                    this.loadContentTypes({
                                        filter: '',
                                        allowedContent: allowedContentTypes
                                    });
                                },
                                (err) => {
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
     * Retrieves filtered content types based on the provided filter and allowed content.
     * @param filter - The filter to apply to the content types.
     * @param allowedContent - An array of allowed content types.
     * @returns An Observable that emits the filtered content types.
     */
    private getFilteredContentTypes(filter: string, allowedContent: string[]) {
        return forkJoin([
            this.dotContentTypeService.filterContentTypes(filter, allowedContent.join(',')),
            this.dotContentTypeService.getContentTypes({
                filter,
                page: 40,
                type: 'WIDGET'
            })
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

    private filterAllowedContentTypes(
        containers: DotPageContainerStructure,
        blackList: string[] = []
    ): string[] {
        const allowedContent = new Set();
        Object.values(containers).forEach((container) => {
            Object.values(container.containerStructures).forEach(
                (containerStructure: DotContainerStructure) => {
                    allowedContent.add(containerStructure.contentTypeVar.toLocaleLowerCase());
                }
            );
        });
        blackList.forEach((content) => allowedContent.delete(content.toLocaleLowerCase()));

        return [...allowedContent] as string[];
    }

    //Unused for now, have a strange issue when try lazy init.
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
            currentPaletteType: PALETTE_TYPES.CONTENTTYPE,
            allowedTypes: []
        });
    }
}

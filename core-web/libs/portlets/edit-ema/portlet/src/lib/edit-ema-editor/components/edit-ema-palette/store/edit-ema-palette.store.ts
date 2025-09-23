import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
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
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    DotConfigurationVariables,
    DotContainerStructure
} from '@dotcms/dotcms-models';
import { DotCMSPageAssetContainers } from '@dotcms/types';

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
    private dotContentTypeService = inject(DotContentTypeService);
    private dotESContentService = inject(DotESContentService);

    private readonly dotConfigurationService = inject(DotPropertiesService);

    constructor() {
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
    readonly isContentType$ = this.select(
        (state) => state.currentPaletteType === PALETTE_TYPES.CONTENTTYPE
    );

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

                    return this.fetchContentlets(
                        filter,
                        contenttypeName,
                        languageId,
                        variantId,
                        page
                    );
                })
            );
        }
    );

    /**
     * Refreshes the contentlets when the language or variant changes.
     * @param data$ - The observable that emits the language and variant.
     * @returns An Observable that emits the contentlets.
     */
    readonly refreshContentlets = this.effect(
        (data$: Observable<{ languageId: number; variantId: string }>) => {
            return data$.pipe(
                tap(() => this.setStatus(EditEmaPaletteStoreStatus.LOADING)),
                switchMap(({ languageId, variantId }) => {
                    const { query, contentTypeVarName } = this.state().contentlets.filter;
                    return this.fetchContentlets(
                        query,
                        contentTypeVarName,
                        languageId.toString(),
                        variantId,
                        0
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
        (data$: Observable<{ containers: DotCMSPageAssetContainers }>) => {
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
        return forkJoin({
            contentTypes: this.dotContentTypeService.filterContentTypes(
                filter,
                allowedContent.join(',')
            ),
            widgets: this.dotContentTypeService.getContentTypes({
                filter,
                page: 40,
                type: 'WIDGET'
            }),
            hiddenContentTypes: this.dotConfigurationService.getKeyAsList(
                DotConfigurationVariables.CONTENT_PALETTE_HIDDEN_CONTENT_TYPES
            )
        }).pipe(
            map(({ contentTypes, widgets, hiddenContentTypes }) => {
                /**
                 * This filter is used to prevent widgets from being repeated.
                 * More information here: https://github.com/dotCMS/core/pull/22573#discussion_r921263060
                 */
                const filteredContentTypes = contentTypes.filter(
                    (item) => item.baseType !== DotCMSBaseTypesContentTypes.WIDGET
                );
                const mergedContentAndWidgets = [...filteredContentTypes, ...widgets];

                return mergedContentAndWidgets
                    .filter((item) => !hiddenContentTypes.includes(item.variable))
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .slice(0, 40);
            }),
            catchError(() => EMPTY)
        );
    }

    private filterAllowedContentTypes(
        containers: DotCMSPageAssetContainers,
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

    /**
     * Fetches the contentlets from the ES.
     * @param filter - The filter to apply to the contentlets.
     * @param contenttypeName - The name of the content type.
     * @param languageId - The id of the language.
     * @param variantId - The id of the variant.
     * @param page - The page number.
     * @returns An Observable that emits the contentlets.
     */
    private fetchContentlets(
        filter: string,
        contenttypeName: string,
        languageId: string,
        variantId: string,
        page: number
    ) {
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
    }
}

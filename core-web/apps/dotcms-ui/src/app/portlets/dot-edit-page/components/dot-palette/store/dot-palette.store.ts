import { ComponentStore } from '@ngrx/component-store';
import { forkJoin, Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { debounceTime, map, take } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotESContentService,
    DotPropertiesService,
    DotSessionStorageService,
    PaginatorService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DEFAULT_VARIANT_ID,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    DotConfigurationVariables,
    ESContent
} from '@dotcms/dotcms-models';

export interface DotPaletteState {
    contentlets: DotCMSContentlet[] | DotCMSContentType[];
    contentTypes: DotCMSContentType[];
    allowedContent: string[];
    filter: string;
    languageId: string;
    totalRecords: number;
    viewContentlet: string;
    loading: boolean;
}

const CONTENTLET_VIEW_IN = 'contentlet:in';
const CONTENTLET_VIEW_OUT = 'contentlet:out';

@Injectable()
export class DotPaletteStore extends ComponentStore<DotPaletteState> {
    private dotContentTypeService = inject(DotContentTypeService);
    private paginatorESService = inject(DotESContentService);
    private paginationService = inject(PaginatorService);
    private dotSessionStorageService = inject(DotSessionStorageService);
    private dotConfigurationService = inject(DotPropertiesService);

    readonly vm$ = this.state$;

    readonly setFilter = this.updater((state: DotPaletteState, data: string) => {
        return { ...state, filter: data };
    });

    readonly setLanguage = this.updater((state: DotPaletteState, languageId: string) => {
        return { ...state, languageId, viewContentlet: CONTENTLET_VIEW_OUT, filter: '' };
    });

    readonly setViewContentlet = this.updater((state: DotPaletteState, data: string) => {
        return { ...state, viewContentlet: data };
    });

    readonly setLoading = this.updater((state: DotPaletteState) => {
        return {
            ...state,
            loading: ComponentStatus.LOADING === ComponentStatus.LOADING
        };
    });

    readonly setLoaded = this.updater((state: DotPaletteState) => {
        return {
            ...state,
            loading: !(ComponentStatus.LOADED === ComponentStatus.LOADED)
        };
    });

    readonly setAllowedContent = this.updater((state: DotPaletteState, data: string[]) => {
        return { ...state, allowedContent: data };
    });

    // EFFECTS
    readonly loadContentTypes = this.effect((data$: Observable<string[]>) => {
        return data$.pipe(
            map((allowedContent) => {
                this.setAllowedContent(allowedContent);
                this.getContenttypesData();
            })
        );
    });

    private isFormContentType: boolean;

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

    private itemsPerPage = 25;

    private contentTypeVarName: string;

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

    private initialContent: DotCMSContentType[];

    // UPDATERS
    private readonly setContentlets = this.updater(
        (state: DotPaletteState, data: DotCMSContentlet[] | DotCMSContentType[]) => {
            return { ...state, contentlets: data, totalRecords: data.length };
        }
    );

    private readonly setContentTypes = this.updater(
        (state: DotPaletteState, data: DotCMSContentType[]) => {
            return { ...state, contentTypes: data };
        }
    );

    readonly filterContentTypes = this.effect((filterValue$: Observable<string>) => {
        return filterValue$.pipe(
            debounceTime(400),
            map((value: string) => {
                const query = value ? value.trim() : '';

                if (query && query.length < 3) {
                    return;
                }

                this.setFilter(query);

                // If it's empty, set the inital contentTypes;
                if (!query) {
                    this.setContentTypes(this.initialContent);
                } else {
                    this.getContenttypesData();
                }
            })
        );
    });

    private readonly setTotalRecords = this.updater((state: DotPaletteState, data: number) => {
        return { ...state, totalRecords: data };
    });

    constructor() {
        super({
            contentlets: null,
            contentTypes: null,
            allowedContent: [],
            filter: '',
            languageId: '1',
            totalRecords: 0,
            viewContentlet: CONTENTLET_VIEW_OUT,
            loading: false
        });
    }

    /**
     * Switch language and request Content Types data.
     * @param languageId
     *
     * @memberof DotPaletteStore
     */
    switchLanguage(languageId: string): void {
        this.setLanguage(languageId);
        this.getContenttypesData();
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
                        query: `+contentType: ${
                            this.contentTypeVarName
                        } +deleted: false ${this.getExperimentVariantQueryField()}`.trim()
                    })
                    .pipe(take(1))
                    .subscribe((response: ESContent) => {
                        this.setLoaded();
                        if (this.dotSessionStorageService.getVariationId() !== DEFAULT_VARIANT_ID) {
                            // GH issue: https://github.com/dotCMS/core/issues/26363
                            // This is a workaround to remove the original (variant: DEFAULT) when exist a modified contentlet inside a
                            // variant (it make a copy of the original) the endpoint return the original and the derivated/duplicated.
                            // We need to discus about create or not a new endpoint to get the contentlets taking
                            // in consideration the variant contentlets, if you remove this, the contentlets will show the duplicated and the original contentlet
                            const contentlets = this.removeOriginalContentletsDuplicated(
                                response.jsonObjectView.contentlets
                            );

                            this.setContentlets(contentlets);
                        } else {
                            this.setContentlets(response.jsonObjectView.contentlets);
                        }
                    });
            }
        });
    }

    /**
     * Request contenttypes data with filter params.
     *
     * @memberof DotPaletteStore
     */
    getContenttypesData(): void {
        this.setLoading();
        this.state$.pipe(take(1)).subscribe(({ filter, allowedContent = [] }) => {
            // Note: This store needs to be refactored
            const hasAllowedContent = allowedContent && allowedContent.length > 0;
            const contentTypes$ = hasAllowedContent
                ? this.dotContentTypeService.filterContentTypes(filter, allowedContent.join(','))
                : of([]);

            forkJoin({
                contentTypes: contentTypes$,
                widgets: this.dotContentTypeService.getContentTypes({
                    filter,
                    page: 40,
                    type: 'WIDGET'
                }),
                hiddenContentTypes: this.dotConfigurationService.getKeyAsList(
                    DotConfigurationVariables.CONTENT_PALETTE_HIDDEN_CONTENT_TYPES
                )
            })
                .pipe(take(1))
                .subscribe(({ contentTypes, widgets, hiddenContentTypes }) => {
                    /**
                     * This filter is used to prevent widgets from being repeated.
                     * More information here: https://github.com/dotCMS/core/pull/22573#discussion_r921263060
                     */
                    const filteredContentTypes = contentTypes.filter(
                        (item) => item.baseType !== DotCMSBaseTypesContentTypes.WIDGET
                    );
                    const mergedContentAndWidgets = [...filteredContentTypes, ...widgets];
                    const data = mergedContentAndWidgets
                        .filter((item) => !hiddenContentTypes.includes(item.variable))
                        .sort((a, b) => a.name.localeCompare(b.name))
                        .slice(0, 40);

                    this.loadContentypes(data);
                });
        });
    }

    /**
     *
     *
     * @param {DotCMSContentType[]} contentTypes
     * @memberof DotPaletteStore
     */
    loadContentypes(contentTypes: DotCMSContentType[]): void {
        this.setLoaded();
        this.setContentTypes(contentTypes);
        if (!this.initialContent) {
            this.initialContent = contentTypes;
        }
    }

    /**
     * Sets value to show/hide components, clears filter value and starts loding data
     *
     * @param string [variableName]
     * @memberof DotPaletteContentletsComponent
     */
    switchView(variableName?: string): void {
        const viewContentlet = variableName ? CONTENTLET_VIEW_IN : CONTENTLET_VIEW_OUT;
        this.setViewContentlet(viewContentlet);
        this.setFilter('');
        this.loadContentlets(variableName);
        this.setContentTypes(this.initialContent);
    }

    /**
     * Retrieves the experiment variant query field.
     *
     * @private
     *
     * @returns {string} The query field for the experiment variant.
     */
    private getExperimentVariantQueryField(): string {
        return this.dotSessionStorageService.getVariationId() !== DEFAULT_VARIANT_ID
            ? `+(variant:default OR variant:${this.dotSessionStorageService.getVariationId()})`
            : '';
    }

    /**
     * If the contentlets have a derivated/duplicated contentlet, remove the original (variant: DEFAULT).
     *
     * @param {DotCMSContentlet[]} contentlets - The array of contentlets to remove derived contentlets from.
     * @return {DotCMSContentlet[]} - The modified array of contentlets without the original contentlets.
     */
    private removeOriginalContentletsDuplicated(contentlets: DotCMSContentlet[]) {
        const currentVariationId = this.dotSessionStorageService.getVariationId();
        const uniqueIdentifiersFromVariantContentlet = new Set();
        const iNodesOfOriginalContentletToDelete = [];

        contentlets.reduce((acc, item) => {
            if (item.variant === currentVariationId) {
                uniqueIdentifiersFromVariantContentlet.add(item.identifier);
            }

            if (
                uniqueIdentifiersFromVariantContentlet.has(item.identifier) &&
                item.variant !== currentVariationId
            ) {
                iNodesOfOriginalContentletToDelete.push(item.inode);
            }

            return acc;
        }, {});

        return contentlets.filter(
            (item) => !iNodesOfOriginalContentletToDelete.includes(item.inode)
        );
    }
}

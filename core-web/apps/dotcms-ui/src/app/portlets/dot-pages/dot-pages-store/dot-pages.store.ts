import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MenuItem, SelectItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { catchError, filter, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';

import {
    DotCMSPageWorkflowState,
    DotCurrentUserService,
    DotESContentService,
    DotEventsService,
    DotFavoritePageService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotLicenseService,
    DotLocalstorageService,
    DotMessageService,
    DotPageTypesService,
    DotPageWorkflowsActionsService,
    DotPropertiesService,
    DotRenderMode,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    ESOrderDirection,
    PushPublishService
} from '@dotcms/data-access';
import { DotPushPublishDialogService, SiteService } from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSWorkflowAction,
    DotCMSWorkflowActionEvent,
    DotCurrentUser,
    DotEnvironment,
    DotLanguage,
    DotPermissionsType,
    ESContent,
    FeaturedFlags,
    PermissionsType,
    UserPermissions
} from '@dotcms/dotcms-models';
import { DotFavoritePageComponent } from '@dotcms/portlets/dot-ema/ui';
import { generateDotFavoritePageUrl } from '@dotcms/utils';

import { DotPagesCreatePageDialogComponent } from '../dot-pages-create-page-dialog/dot-pages-create-page-dialog.component';

export interface DotPagesInfo {
    actionMenuDomId?: string;
    addToBundleCTId?: string;
    archived?: boolean;
    items: DotCMSContentlet[];
    keyword?: string;
    languageId?: string;
    menuActions?: MenuItem[];
    status?: ComponentStatus;
    total?: number;
}

export interface DotFavoritePagesInfo {
    collapsed?: boolean;
    items: DotCMSContentlet[];
    showLoadMoreButton: boolean;
    total: number;
}

export interface DotPagesState {
    favoritePages: DotFavoritePagesInfo;
    environments: boolean;
    isEnterprise: boolean;
    languages: DotLanguage[];
    loggedUser: {
        canRead: { contentlets: boolean; htmlPages: boolean };
        canWrite: { contentlets: boolean; htmlPages: boolean };
        id: string;
    };
    pages?: DotPagesInfo;
    pageTypes?: DotCMSContentType[];
    portletStatus: ComponentStatus;
}

export interface DotSessionStorageFilter {
    archived: boolean;
    keyword: string;
    languageId: string;
}

interface UserPagePermission {
    canUserReadPage: boolean;
    canUserReadContent: boolean;
    canUserWritePage: boolean;
    canUserWriteContent: boolean;
}

export const FAVORITE_PAGE_LIMIT = 500;

export const LOCAL_STORAGE_FAVORITES_PANEL_KEY = 'FavoritesPanelCollapsed';

export const SESSION_STORAGE_FAVORITES_KEY = 'FavoritesSearchTerms';

@Injectable()
export class DotPageStore extends ComponentStore<DotPagesState> {
    readonly getStatus$ = this.select((state) => state.pages.status);

    readonly getFilterParams$: Observable<DotSessionStorageFilter> = this.select((state) => {
        return {
            languageId: state.pages.languageId,
            keyword: state.pages.keyword,
            archived: state.pages.archived
        };
    });

    readonly isFavoritePanelCollaped$: Observable<boolean> = this.select((state) => {
        return state.favoritePages.collapsed;
    });

    readonly isPagesLoading$: Observable<boolean> = this.select(
        (state) =>
            state.pages.status === ComponentStatus.LOADING ||
            state.pages.status === ComponentStatus.INIT
    );

    readonly isPortletLoading$: Observable<boolean> = this.select(
        (state) =>
            state.portletStatus === ComponentStatus.LOADING ||
            state.portletStatus === ComponentStatus.INIT
    );

    readonly actionMenuDomId$: Observable<string> = this.select(
        ({ pages }) => pages.actionMenuDomId
    ).pipe(filter((i) => i !== null));

    readonly languageOptions$: Observable<SelectItem[]> = this.select(
        ({ languages }: DotPagesState) => {
            const languageOptions: SelectItem[] = [];
            languageOptions.push({
                label: this.dotMessageService.get('All'),
                value: null
            });

            if (languages?.length) {
                languages.forEach((language) => {
                    const countryCode = language.countryCode.length
                        ? ` (${language.countryCode})`
                        : '';

                    languageOptions.push({
                        label: `${language.language}${countryCode}`,
                        value: language.id
                    });
                });
            }

            return languageOptions;
        }
    );

    readonly keywordValue$: Observable<string> = this.select(({ pages }) => pages.keyword);

    readonly languageIdValue$: Observable<number> = this.select(({ pages }) =>
        parseInt(pages.languageId, 10)
    );

    readonly showArchivedValue$: Observable<boolean> = this.select(({ pages }) => pages.archived);

    readonly languageLabels$: Observable<{ [id: string]: string }> = this.select(
        ({ languages }: DotPagesState) => {
            const langLabels = {};
            if (languages?.length) {
                languages.forEach((language) => {
                    const countryCode = language.countryCode.length
                        ? `-${language.countryCode}`
                        : '';

                    langLabels[language.id] = `${language.languageCode}${countryCode}`;
                });
            }

            return langLabels;
        }
    );

    readonly pageTypes$ = this.select(({ pageTypes }) => pageTypes);

    readonly setFavoritePages = this.updater<Partial<DotFavoritePagesInfo>>(
        (state: DotPagesState, favoritePages: DotFavoritePagesInfo) => {
            return {
                ...state,
                favoritePages: {
                    ...state.favoritePages,
                    ...favoritePages
                }
            };
        }
    );

    readonly setPages = this.updater<Partial<DotPagesInfo>>(
        (state: DotPagesState, pagesInfo: DotPagesInfo) => {
            return {
                ...state,
                pages: {
                    ...state.pages,
                    status: ComponentStatus.LOADED,
                    ...pagesInfo
                }
            };
        }
    );

    readonly setKeyword = this.updater<string>((state: DotPagesState, keyword: string) => {
        return {
            ...state,
            pages: {
                ...state.pages,
                items: [],
                keyword
            }
        };
    });

    readonly setLanguageId = this.updater<string>((state: DotPagesState, languageId: string) => {
        return {
            ...state,
            pages: {
                ...state.pages,
                items: [],
                languageId
            }
        };
    });

    readonly setArchived = this.updater<string>((state: DotPagesState, archived: string) => {
        return {
            ...state,
            pages: {
                ...state.pages,
                items: [],
                archived: !!archived
            }
        };
    });

    readonly setPortletStatus = this.updater<ComponentStatus>(
        (state: DotPagesState, portletStatus: ComponentStatus) => {
            return {
                ...state,
                portletStatus
            };
        }
    );

    readonly setPagesStatus = this.updater<ComponentStatus>(
        (state: DotPagesState, status: ComponentStatus) => {
            return {
                ...state,
                pages: {
                    ...state.pages,
                    status
                }
            };
        }
    );

    readonly clearMenuActions = this.updater((state: DotPagesState) => {
        return {
            ...state,
            pages: {
                ...state.pages,
                items: [...state.pages.items],
                actionMenuDomId: null,
                menuActions: []
            }
        };
    });

    readonly setMenuActions = this.updater(
        (
            state: DotPagesState,
            { actions, actionMenuDomId }: { actions: MenuItem[]; actionMenuDomId: string }
        ) => {
            return {
                ...state,
                pages: {
                    ...state.pages,
                    menuActions: actions,
                    actionMenuDomId
                }
            };
        }
    );

    readonly showAddToBundle = this.updater((state: DotPagesState, addToBundleCTId: string) => {
        return {
            ...state,
            pages: {
                ...state.pages,
                addToBundleCTId
            }
        };
    });
    readonly getPageTypes = this.effect<void>((trigger$) =>
        trigger$.pipe(
            switchMap(() => {
                return this.dotPageTypesService.getPages().pipe(
                    take(1),
                    tapResponse(
                        (pageTypes: DotCMSContentType[]) => {
                            this.patchState({
                                pageTypes
                            });
                            this.dialogService.open(DotPagesCreatePageDialogComponent, {
                                header: this.dotMessageService.get('create.page'),
                                width: '58rem',
                                data: {
                                    pageTypes
                                }
                            });
                        },
                        (error: HttpErrorResponse) => {
                            return this.httpErrorManagerService.handle(error);
                        }
                    )
                );
            })
        )
    );
    readonly updateSinglePageData = this.effect(
        (
            params$: Observable<{
                identifier: string;
                isFavoritePage: boolean;
            }>
        ) => {
            return params$.pipe(
                mergeMap((params) => {
                    const { identifier, isFavoritePage } = params;
                    const sortOrderValue = this.getSortOrderValue();

                    if (!isFavoritePage) this.setPagesStatus(ComponentStatus.LOADING);

                    return this.getPagesDataFn(isFavoritePage, sortOrderValue, identifier).pipe(
                        tap(
                            (items) => {
                                // Finished fetch loop and will proceed to set data on store
                                if (isFavoritePage) {
                                    const pagesData = this.get().favoritePages.items.map((page) => {
                                        return page?.identifier === identifier
                                            ? items.jsonObjectView.contentlets[0]
                                            : page;
                                    });

                                    this.setFavoritePages({ items: pagesData });
                                } else {
                                    let pagesData = this.get().pages.items;

                                    if (items.jsonObjectView.contentlets[0] === undefined) {
                                        pagesData = pagesData.filter((page) => {
                                            return page?.identifier !== identifier;
                                        });

                                        // Add undefined to keep the same length of the array,
                                        // otherwise the pagination(endless scroll) will break
                                        pagesData.push(undefined);
                                    } else {
                                        pagesData = pagesData.map((page) => {
                                            return page?.identifier === identifier
                                                ? items.jsonObjectView.contentlets[0]
                                                : page;
                                        });
                                    }

                                    this.setPages({ items: pagesData });
                                }
                            },
                            (error: HttpErrorResponse) => {
                                return this.httpErrorManagerService.handle(error);
                            }
                        )
                    );
                })
            );
        }
    );
    readonly vm$: Observable<DotPagesState> = this.select(
        this.state$,
        this.isFavoritePanelCollaped$,
        this.isPagesLoading$,
        this.isPortletLoading$,
        this.languageOptions$,
        this.languageLabels$,
        this.keywordValue$,
        this.languageIdValue$,
        this.showArchivedValue$,
        this.pageTypes$,
        (
            {
                favoritePages,
                isEnterprise,
                environments,
                languages,
                loggedUser,
                pages,
                portletStatus
            },
            isFavoritePanelCollaped,
            isPagesLoading,
            isPortletLoading,
            languageOptions,
            languageLabels,
            keywordValue,
            languageIdValue,
            showArchivedValue,
            pageTypes
        ) => ({
            favoritePages,
            isEnterprise,
            environments,
            languages,
            loggedUser,
            pages,
            portletStatus,
            isFavoritePanelCollaped,
            isPagesLoading,
            isPortletLoading,
            languageOptions,
            languageLabels,
            keywordValue,
            languageIdValue,
            showArchivedValue,
            pageTypes
        })
    );

    constructor(
        private dotCurrentUser: DotCurrentUserService,
        private dotRouterService: DotRouterService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotESContentService: DotESContentService,
        private dotPageTypesService: DotPageTypesService,
        private dotMessageService: DotMessageService,
        private dialogService: DialogService,
        private dotLanguagesService: DotLanguagesService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotPageWorkflowsActionsService: DotPageWorkflowsActionsService,
        private dotWorkflowsActionsService: DotWorkflowsActionsService,
        private dotWorkflowEventHandlerService: DotWorkflowEventHandlerService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotLicenseService: DotLicenseService,
        private dotEventsService: DotEventsService,
        private pushPublishService: PushPublishService,
        private siteService: SiteService,
        private dotFavoritePageService: DotFavoritePageService,
        private dotLocalstorageService: DotLocalstorageService,
        private dotPropertiesService: DotPropertiesService
    ) {
        super(null);
    }

    /**
     * Sets initial state data from props, roles and current logged user data
     * @param number initialFavoritePagesLimit
     * @memberof DotFavoritePageStore
     */
    setInitialStateData(initialFavoritePagesLimit: number): void {
        forkJoin([
            this.getFavoritePagesData({ limit: initialFavoritePagesLimit }),
            this.dotCurrentUser.getCurrentUser(),
            this.dotLanguagesService.get(),
            this.dotLicenseService.isEnterprise(),
            this.pushPublishService
                .getEnvironments()
                .pipe(map((environments: DotEnvironment[]) => !!environments.length)),
            this.getSessionStorageFilterParams(),
            this.getLocalStorageFavoritePanelParams(),
            this.dotPropertiesService.getFeatureFlag(
                FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
            ),
            this.dotPropertiesService
                .getKey(FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_CONTENT_TYPE)
                .pipe(map((contentTypes) => contentTypes.split(',')))
        ])
            .pipe(
                take(1),
                mergeMap(
                    ([
                        favoritePages,
                        currentUser,
                        languages,
                        isEnterprise,
                        environments,
                        filterParams,
                        collapsedParam
                    ]: [
                        ESContent,
                        DotCurrentUser,
                        DotLanguage[],
                        boolean,
                        boolean,
                        DotSessionStorageFilter,
                        boolean
                    ]) => {
                        return this.dotCurrentUser
                            .getUserPermissions(
                                currentUser.userId,
                                [UserPermissions.READ, UserPermissions.WRITE],
                                [PermissionsType.CONTENTLETS, PermissionsType.HTMLPAGES]
                            )
                            .pipe(
                                take(1),
                                map((permissionsType: DotPermissionsType) => {
                                    return [
                                        favoritePages,
                                        currentUser,
                                        languages,
                                        isEnterprise,
                                        environments,
                                        permissionsType,
                                        filterParams,
                                        collapsedParam
                                    ];
                                })
                            );
                    }
                )
            )
            .subscribe(
                ([
                    favoritePages,
                    currentUser,
                    languages,
                    isEnterprise,
                    environments,
                    permissions,
                    filterParams,
                    collapsedParam
                ]: [
                    ESContent,
                    DotCurrentUser,
                    DotLanguage[],
                    boolean,
                    boolean,
                    DotPermissionsType,
                    DotSessionStorageFilter,
                    boolean
                ]): void => {
                    this.setState({
                        favoritePages: {
                            collapsed: collapsedParam,
                            items: favoritePages?.jsonObjectView.contentlets,
                            showLoadMoreButton:
                                favoritePages.jsonObjectView.contentlets.length <
                                favoritePages.resultsSize,
                            total: favoritePages.resultsSize
                        },
                        isEnterprise,
                        environments,
                        languages,
                        loggedUser: {
                            id: currentUser.userId,
                            canRead: {
                                contentlets: permissions.CONTENTLETS.canRead,
                                htmlPages: permissions.HTMLPAGES.canRead
                            },
                            canWrite: {
                                contentlets: permissions.CONTENTLETS.canWrite,
                                htmlPages: permissions.HTMLPAGES.canWrite
                            }
                        },
                        pages: {
                            items: [],
                            keyword: filterParams?.keyword || '',
                            languageId: filterParams?.languageId?.toString() || '',
                            archived: filterParams?.archived || false,
                            status: ComponentStatus.INIT
                        },
                        portletStatus: ComponentStatus.LOADED
                    });
                },
                () => {
                    this.setState({
                        favoritePages: {
                            collapsed: true,
                            items: [],
                            showLoadMoreButton: false,
                            total: 0
                        },
                        isEnterprise: false,
                        environments: false,
                        languages: null,
                        loggedUser: {
                            id: null,
                            canRead: {
                                contentlets: null,
                                htmlPages: null
                            },
                            canWrite: {
                                contentlets: null,
                                htmlPages: null
                            }
                        },
                        pages: {
                            items: [],
                            keyword: '',
                            status: ComponentStatus.INIT
                        },
                        portletStatus: ComponentStatus.LOADED
                    });
                }
            );
    }

    /**
     * Sets on LocalStorage Favorite Page panel collapsed state
     * @param boolean collapsed
     * @memberof DotFavoritePageStore
     */
    setLocalStorageFavoritePanelCollapsedParams(collapsed: boolean): void {
        this.dotLocalstorageService.setItem(
            LOCAL_STORAGE_FAVORITES_PANEL_KEY,
            collapsed.toString()
        );
    }

    /**
     * Sets on Session Storage Page's table filter params
     * @memberof DotFavoritePageStore
     */
    setSessionStorageFilterParams(): void {
        const { keyword, languageId, archived } = this.get().pages;

        sessionStorage.setItem(
            SESSION_STORAGE_FAVORITES_KEY,
            JSON.stringify({ keyword, languageId, archived })
        );
    }

    private getSortOrderValue(sortField?: string, sortOrder?: number) {
        let sortOrderValue: ESOrderDirection;
        if ((sortOrder === 1 && !sortField) || (!sortOrder && !sortField)) {
            sortOrderValue = ESOrderDirection.DESC;
        } else {
            sortOrderValue = sortOrder === -1 ? ESOrderDirection.DESC : ESOrderDirection.ASC;
        }

        return sortOrderValue;
    }

    private getESQuery(identifier?: string) {
        const { keyword, languageId, archived } = this.get().pages;
        const hostIdQuery = this.siteService.currentSite
            ? `+conhost:${this.siteService.currentSite?.identifier}`
            : '';
        const langQuery = languageId ? `+languageId:${languageId}` : '';
        const archivedQuery = archived ? `+deleted:true` : '+deleted:false';
        const identifierQuery = identifier ? `+identifier:${identifier}` : '';
        const keywordQuery = keyword
            ? `+(title:${keyword}* OR path:*${keyword}* OR urlmap:*${keyword}*)`
            : '';

        return `${hostIdQuery} +working:true  +(urlmap:* OR basetype:5) ${langQuery} ${archivedQuery} ${keywordQuery} ${identifierQuery}`;
    }

    private getWorflowActionsFn = (item: DotCMSContentlet): Observable<DotCMSPageWorkflowState> => {
        let obs$: Observable<DotCMSPageWorkflowState>;

        if (item?.contentType === 'dotFavoritePage') {
            obs$ = this.getFavoritePageWorflowActions(item);
        } else {
            obs$ = this.dotWorkflowsActionsService
                .getByInode(item.inode, DotRenderMode.LISTING)
                .pipe(
                    map((workflowActions: DotCMSWorkflowAction[]) => {
                        return {
                            actions: workflowActions,
                            page: item
                        };
                    })
                );
        }

        return obs$.pipe(
            catchError((error: HttpErrorResponse) => {
                console.warn(error);

                return of({
                    actions: [],
                    page: item
                });
            })
        );
    };

    private getFavoritePageWorflowActions(
        item: DotCMSContentlet
    ): Observable<DotCMSPageWorkflowState> {
        const urlParams: { [key: string]: string } = {
            url: item.url.split('?')[0]
        };
        const searchParams = new URLSearchParams(item.url.split('?')[1]);
        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        const { host_id, language_id, url } = urlParams;

        return this.dotPageWorkflowsActionsService.getByUrl({
            host_id,
            language_id,
            url
        });
    }

    private getPagesDataFn(
        isFavoritePage: boolean,
        sortOrderValue: ESOrderDirection,
        identifier: string
    ) {
        return isFavoritePage
            ? this.getFavoritePagesData({ limit: 1, identifier })
            : this.getPagesData(0, sortOrderValue, '', identifier);
    }

    private getPagesData = (
        offset: number,
        sortOrderValue?: ESOrderDirection,
        sortField?: string,
        identifier?: string
    ) => {
        return this.dotESContentService.get({
            itemsPerPage: 40,
            offset: offset.toString(),
            query: this.getESQuery(identifier),
            sortField: sortField || 'modDate',
            sortOrder: sortOrderValue
        });
    };

    readonly getPages = this.effect(
        (
            params$: Observable<{
                offset: number;
                sortField?: string;
                sortOrder?: number;
            }>
        ) => {
            return params$.pipe(
                mergeMap((params) => {
                    const { offset, sortField, sortOrder } = params;
                    const sortOrderValue = this.getSortOrderValue(sortField, sortOrder);

                    return this.getPagesData(offset, sortOrderValue, sortField).pipe(
                        tapResponse(
                            (items) => {
                                this.setPages({
                                    items: items.jsonObjectView.contentlets as DotCMSContentlet[],
                                    total: items.resultsSize
                                });
                            },
                            (error: HttpErrorResponse) => {
                                this.setPagesStatus(ComponentStatus.LOADED);

                                // Set message to throw a custom Favorite Page error message
                                error.error.message = this.dotMessageService.get(
                                    'favoritePage.error.fetching.data'
                                );

                                return this.httpErrorManagerService.handle(error, true);
                            }
                        )
                    );
                })
            );
        }
    );

    private getFavoritePagesData = (params: {
        limit: number;
        identifier?: string;
        url?: string;
    }) => {
        const { limit, identifier, url } = params;

        return this.dotCurrentUser.getCurrentUser().pipe(
            switchMap(({ userId }) => {
                return this.dotFavoritePageService.get({
                    limit,
                    userId,
                    identifier,
                    url,
                    sortOrder: 'ASC, identifier'
                });
            })
        );
    };

    // EFFECTS
    readonly getFavoritePages = this.effect((itemsPerPage$: Observable<number>) => {
        return itemsPerPage$.pipe(
            switchMap((itemsPerPage: number) =>
                this.getFavoritePagesData({ limit: itemsPerPage }).pipe(
                    tapResponse(
                        (items) => {
                            const favoritePages = this.getNewFavoritePages(items);
                            this.patchState({ favoritePages });
                        },
                        (error: HttpErrorResponse) => this.httpErrorManagerService.handle(error)
                    )
                )
            )
        );
    });
    readonly deleteFavoritePage = this.effect((data$: Observable<string>) => {
        return data$.pipe(
            switchMap((inode: string) =>
                this.dotWorkflowActionsFireService
                    .deleteContentlet({
                        inode
                    })
                    .pipe(take(1))
            ),
            switchMap(() => {
                return this.getFavoritePagesData({ limit: FAVORITE_PAGE_LIMIT }).pipe(
                    take(1),
                    tapResponse(
                        (items) => {
                            const favoritePages = this.getNewFavoritePages(items);
                            this.patchState({ favoritePages });
                        },
                        (error: HttpErrorResponse) => this.httpErrorManagerService.handle(error)
                    )
                );
            }),
            catchError((error: HttpErrorResponse) => this.httpErrorManagerService.handle(error))
        );
    });
    readonly showActionsMenu = this.effect(
        (params$: Observable<{ item: DotCMSContentlet; actionMenuDomId: string }>) => {
            return params$.pipe(
                switchMap(({ item, actionMenuDomId }) => {
                    return forkJoin({
                        workflowsData: this.getWorflowActionsFn(item),
                        dotFavorite: this.getFavoritePagesData({
                            limit: 1,
                            url:
                                item?.contentType === 'dotFavoritePage'
                                    ? item.url
                                    : generateDotFavoritePageUrl({
                                          pageURI: item.urlMap || item.url.split('?')[0],
                                          languageId: item.languageId,
                                          siteId: item.host
                                      })
                        })
                    }).pipe(
                        take(1),
                        tapResponse(
                            ({ workflowsData, dotFavorite }) => {
                                if (workflowsData) {
                                    this.setMenuActions({
                                        actions: this.getSelectActions(
                                            workflowsData?.actions,
                                            workflowsData?.page,
                                            dotFavorite.jsonObjectView.contentlets[0]
                                        ),
                                        actionMenuDomId
                                    });
                                }
                            },
                            (error: HttpErrorResponse) => this.httpErrorManagerService.handle(error)
                        )
                    );
                })
            );
        }
    );

    private getSessionStorageFilterParams(): Observable<DotSessionStorageFilter> {
        const params = JSON.parse(sessionStorage.getItem(SESSION_STORAGE_FAVORITES_KEY));

        return of(params);
    }

    private getLocalStorageFavoritePanelParams(): Observable<boolean> {
        const collapsed =
            JSON.parse(this.dotLocalstorageService.getItem(LOCAL_STORAGE_FAVORITES_PANEL_KEY)) ??
            true;

        return of(collapsed);
    }

    private getSelectActions(
        actions: DotCMSWorkflowAction[],
        item: DotCMSContentlet,
        favoritePage?: DotCMSContentlet
    ): MenuItem[] {
        const actionsMenu: MenuItem[] = [];

        const favoritePageUrl = favoritePage
            ? favoritePage.url
            : generateDotFavoritePageUrl({
                  pageURI: item.urlMap || item.url,
                  languageId: item.languageId,
                  siteId: item.host
              });

        // Adding DotFavorite actions
        if (!item.archived) {
            actionsMenu.push({
                label: favoritePage
                    ? this.dotMessageService.get('favoritePage.contextMenu.action.edit')
                    : this.dotMessageService.get('favoritePage.contextMenu.action.add'),
                command: () => {
                    this.dialogService.open(DotFavoritePageComponent, {
                        header: this.dotMessageService.get('favoritePage.dialog.header'),
                        width: '80rem',
                        data: {
                            page: {
                                favoritePageUrl,
                                favoritePage
                            },
                            onSave: () => {
                                this.getFavoritePages(FAVORITE_PAGE_LIMIT);
                            },
                            onDelete: () => {
                                this.getFavoritePages(FAVORITE_PAGE_LIMIT);
                            }
                        }
                    });
                }
            });
        }

        if (favoritePage) {
            actionsMenu.push({
                label: this.dotMessageService.get('favoritePage.dialog.delete.button'),
                command: () => this.deleteFavoritePage(favoritePage.inode)
            });
        }

        if (!actions && !item) {
            return actionsMenu;
        }

        if (actionsMenu?.length > 0) {
            actionsMenu.push({ separator: true });
        }

        // Adding Edit & View actions
        const { isEnterprise, environments } = this.get();

        const isEditable = (item.live || item.working) && !item.deleted;
        const { canUserReadPage, canUserReadContent, canUserWritePage, canUserWriteContent } =
            this.getUserPagePermissions(item);

        // Adding Edit & View actions
        if (isEditable && (canUserReadPage || canUserReadContent)) {
            actionsMenu.push({
                label:
                    canUserWritePage || canUserWriteContent
                        ? this.dotMessageService.get('Edit')
                        : this.dotMessageService.get('View'),
                command: () => {
                    this.dotRouterService.goToEditContentlet(item.inode);
                }
            });
        }

        // Adding Workflow actions
        actions.forEach((action: DotCMSWorkflowAction) => {
            actionsMenu.push({
                label: `${this.dotMessageService.get(action.name)}`,
                command: () => {
                    if (!(action.actionInputs?.length > 0)) {
                        this.fireWorkflowAction(item.inode, action.id);

                        return;
                    }

                    const wfActionEvent: DotCMSWorkflowActionEvent = {
                        workflow: action,
                        callback: 'ngWorkflowEventCallback',
                        inode: item.inode,
                        selectedInodes: null
                    };

                    this.dotWorkflowEventHandlerService.open(wfActionEvent);
                }
            });
        });

        // Adding Push Publish action
        if (isEnterprise && environments) {
            actionsMenu.push({
                label: this.dotMessageService.get('contenttypes.content.push_publish'),
                command: () =>
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: item.identifier,
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    })
            });
        }

        // Adding Add To Bundle action
        if (isEnterprise) {
            actionsMenu.push({
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle'),
                command: () => this.showAddToBundle(item.identifier)
            });
        }

        return actionsMenu;
    }

    private getNewFavoritePages(items: ESContent): DotFavoritePagesInfo {
        return {
            items: [...items.jsonObjectView.contentlets],
            showLoadMoreButton: items.jsonObjectView.contentlets.length <= items.resultsSize,
            total: items.resultsSize,
            collapsed:
                JSON.parse(
                    this.dotLocalstorageService.getItem(LOCAL_STORAGE_FAVORITES_PANEL_KEY)
                ) ?? true
        };
    }

    /**
     * Get user permission for a page or contentlet
     *
     * @private
     * @param {DotCMSContentlet} item
     * @return {*}  {UserPagePermission}
     * @memberof DotPageStore
     */
    private getUserPagePermissions(item: DotCMSContentlet): UserPagePermission {
        // Logged user
        const { loggedUser } = this.get();
        const { canRead, canWrite } = loggedUser;

        // Item types
        const isPage = item.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE;
        const isContent = item.baseType === DotCMSBaseTypesContentTypes.CONTENT;

        // Page permissions
        const canUserReadPage = isPage && canRead.htmlPages;
        const canUserWritePage = isPage && canWrite.htmlPages;

        // Contentlet permissions
        const canUserReadContent = isContent && canRead.contentlets;
        const canUserWriteContent = isContent && canWrite.contentlets;

        return {
            canUserReadPage,
            canUserReadContent,
            canUserWritePage,
            canUserWriteContent
        };
    }

    /**
     * Fire workflow action
     *
     * @private
     * @param {string} contentletInode
     * @param {string} actionId
     * @memberof DotPageStore
     */
    private fireWorkflowAction(contentletInode: string, actionId: string): void {
        const value = this.dotMessageService.get('Workflow-executed');
        this.dotWorkflowActionsFireService.fireTo({ actionId, inode: contentletInode }).subscribe(
            (payload) => this.dotEventsService.notify('save-page', { payload, value }),
            (error) => this.httpErrorManagerService.handle(error, true)
        );
    }
}

import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MenuItem, SelectItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { delay, filter, map, mergeMap, retryWhen, switchMap, take, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { DotEnvironment } from '@dotcms/app/shared/models/dot-environment/dot-environment';
import {
    DotCMSPageWorkflowState,
    DotCurrentUserService,
    DotESContentService,
    DotEventsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPageTypesService,
    DotPageWorkflowsActionsService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    ESOrderDirection
} from '@dotcms/data-access';
import { DotPushPublishDialogService, SiteService } from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSWorkflowAction,
    DotCMSWorkflowActionEvent,
    DotCurrentUser,
    DotLanguage,
    DotPermissionsType,
    ESContent,
    PermissionsType,
    UserPermissions
} from '@dotcms/dotcms-models';

import { DotPagesCreatePageDialogComponent } from '../dot-pages-create-page-dialog/dot-pages-create-page-dialog.component';

export interface DotPagesState {
    favoritePages: {
        items: DotCMSContentlet[];
        showLoadMoreButton: boolean;
        total: number;
    };
    environments: boolean;
    isEnterprise: boolean;
    languages: DotLanguage[];
    loggedUser: {
        canRead: { contentlets: boolean; htmlPages: boolean };
        canWrite: { contentlets: boolean; htmlPages: boolean };
        id: string;
    };
    pages?: {
        actionMenuDomId?: string;
        addToBundleCTId?: string;
        archived?: boolean;
        items: DotCMSContentlet[];
        keyword?: string;
        languageId?: string;
        menuActions?: MenuItem[];
        status: ComponentStatus;
    };
    pageTypes?: DotCMSContentType[];
    portletStatus: ComponentStatus;
}

const FAVORITE_PAGES_ES_QUERY = `+contentType:dotFavoritePage +deleted:false +working:true`;

@Injectable()
export class DotPageStore extends ComponentStore<DotPagesState> {
    readonly getStatus$ = this.select((state) => state.pages.status);

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
                    languageOptions.push({
                        label: `${language.language} (${language.countryCode})`,
                        value: language.id
                    });
                });
            }

            return languageOptions;
        }
    );

    readonly languageLabels$: Observable<{ [id: string]: string }> = this.select(
        ({ languages }: DotPagesState) => {
            const langLabels = {};
            if (languages?.length) {
                languages.forEach((language) => {
                    langLabels[language.id] = `${language.languageCode}-${language.countryCode}`;
                });
            }

            return langLabels;
        }
    );

    readonly pageTypes$ = this.select(({ pageTypes }) => pageTypes);

    readonly setFavoritePages = this.updater<DotCMSContentlet[]>(
        (state: DotPagesState, favoritePages: DotCMSContentlet[]) => {
            return {
                ...state,
                favoritePages: {
                    ...state.favoritePages,
                    items: [...favoritePages]
                }
            };
        }
    );

    readonly setPages = this.updater<DotCMSContentlet[]>(
        (state: DotPagesState, pages: DotCMSContentlet[]) => {
            return {
                ...state,
                pages: {
                    ...state.pages,
                    items: [...pages],
                    status: ComponentStatus.LOADED
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

    // EFFECTS
    readonly getFavoritePages = this.effect((itemsPerPage$: Observable<number>) => {
        return itemsPerPage$.pipe(
            switchMap((itemsPerPage: number) =>
                this.getFavoritePagesData(itemsPerPage).pipe(
                    tapResponse(
                        (items) => {
                            this.patchState({
                                favoritePages: {
                                    items: [...items.jsonObjectView.contentlets],
                                    showLoadMoreButton:
                                        items.jsonObjectView.contentlets.length <=
                                        items.resultsSize,
                                    total: items.resultsSize
                                }
                            });
                        },
                        (error: HttpErrorResponse) => {
                            return this.httpErrorManagerService.handle(error);
                        }
                    )
                )
            )
        );
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
                                data: pageTypes
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
                    let localPageData: DotCMSContentlet;
                    let retries = 0;
                    const { identifier, isFavoritePage } = params;
                    const sortOrderValue = this.getSortOrderValue();

                    if (isFavoritePage) {
                        localPageData = this.get().favoritePages.items.filter(
                            (item) => item?.identifier === identifier
                        )[0];
                    } else {
                        localPageData = this.get().pages.items.filter(
                            (item) => item?.identifier === identifier
                        )[0];
                        this.setPagesStatus(ComponentStatus.LOADING);
                    }

                    return this.getPagesDataFn(isFavoritePage, sortOrderValue, identifier)
                        .pipe(
                            tap(
                                (items) => {
                                    retries++;

                                    // Will continue repeating fetch until data has changed or limit fetch reached
                                    if (
                                        localPageData?.modDate ===
                                            items.jsonObjectView.contentlets[0]?.modDate &&
                                        retries < 10
                                    ) {
                                        throw false;
                                    } else {
                                        // Finished fetch loop and will proceed to set data on store
                                        if (isFavoritePage) {
                                            const pagesData = this.get().favoritePages.items.map(
                                                (page) => {
                                                    return page?.identifier === identifier
                                                        ? items.jsonObjectView.contentlets[0]
                                                        : page;
                                                }
                                            );

                                            this.setFavoritePages(pagesData);
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

                                            this.setPages(pagesData);
                                        }
                                    }
                                },
                                (error: HttpErrorResponse) => {
                                    return this.httpErrorManagerService.handle(error);
                                }
                            )
                        )
                        .pipe(retryWhen((errors) => errors.pipe(delay(1000))));
                })
            );
        }
    );

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
                                let currentPages = this.get().pages.items;

                                if (currentPages.length === 0) {
                                    currentPages = Array.from({ length: items.resultsSize });
                                }

                                Array.prototype.splice.apply(currentPages, [
                                    ...[offset, 40],
                                    ...items.jsonObjectView.contentlets
                                ]);
                                this.setPages(currentPages);
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

    readonly showActionsMenu = this.effect(
        (params$: Observable<{ item: DotCMSContentlet; actionMenuDomId: string }>) => {
            return params$.pipe(
                switchMap(({ item, actionMenuDomId }) => {
                    return this.getWorflowActionsFn(item).pipe(
                        take(1),
                        map((data: DotCMSPageWorkflowState) => {
                            return {
                                actions: this.getSelectActions(data.actions, data.page),
                                actionMenuDomId
                            };
                        })
                    );
                }),
                tap(
                    ({
                        actions,
                        actionMenuDomId
                    }: {
                        actions: MenuItem[];
                        actionMenuDomId: string;
                    }) => {
                        this.setMenuActions({ actions, actionMenuDomId });
                    }
                )
            );
        }
    );

    readonly vm$: Observable<DotPagesState> = this.select(
        this.state$,
        this.isPagesLoading$,
        this.isPortletLoading$,
        this.languageOptions$,
        this.languageLabels$,
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
            isPagesLoading,
            isPortletLoading,
            languageOptions,
            languageLabels,
            pageTypes
        ) => ({
            favoritePages,
            isEnterprise,
            environments,
            languages,
            loggedUser,
            pages,
            portletStatus,
            isPagesLoading,
            isPortletLoading,
            languageOptions,
            languageLabels,
            pageTypes
        })
    );

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
        if (item?.contentType === 'dotFavoritePage') {
            return this.getFavoritePageWorflowActions(item);
        } else {
            return this.dotWorkflowsActionsService
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
    };

    private getFavoritePageWorflowActions(
        item: DotCMSContentlet
    ): Observable<DotCMSPageWorkflowState> {
        const urlParams: { [key: string]: string } = { url: item.url.split('?')[0] };
        const searchParams = new URLSearchParams(item.url.split('?')[1]);
        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        const { host_id, language_id, url } = urlParams;

        return this.dotPageWorkflowsActionsService.getByUrl({ host_id, language_id, url });
    }

    private getWorflowActionsFn = (item: DotCMSContentlet): Observable<DotCMSPageWorkflowState> => {
        if (item?.contentType === 'dotFavoritePage') {
            return this.getFavoritePageWorflowActions(item);
        } else {
            return this.dotWorkflowsActionsService
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
    };

    private getFavoritePageWorflowActions(
        item: DotCMSContentlet
    ): Observable<DotCMSPageWorkflowState> {
        const urlParams: { [key: string]: string } = { url: item.url.split('?')[0] };
        const searchParams = new URLSearchParams(item.url.split('?')[1]);
        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        const { host_id, language_id, url } = urlParams;

        return this.dotPageWorkflowsActionsService.getByUrl({ host_id, language_id, url });
    }

    private getPagesDataFn(
        isFavoritePage: boolean,
        sortOrderValue: ESOrderDirection,
        identifier: string
    ) {
        return isFavoritePage
            ? this.getFavoritePagesData(1, identifier)
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

    private getFavoritePagesData = (limit: number, identifier?: string) => {
        const identifierQuery = identifier ? `+identifier:${identifier}` : '';

        return this.dotCurrentUser.getCurrentUser().pipe(
            switchMap(({ userId }) => {
                return this.dotESContentService.get({
                    itemsPerPage: limit,
                    offset: '0',
                    query: `${FAVORITE_PAGES_ES_QUERY} +owner:${userId} ${identifierQuery}`,
                    sortField: 'dotFavoritePage.order',
                    sortOrder: ESOrderDirection.ASC
                });
            })
        );
    };

    private getSelectActions(actions: DotCMSWorkflowAction[], item: DotCMSContentlet): MenuItem[] {
        const selectItems: MenuItem[] = [];

        // Adding Edit & View actions
        const { loggedUser, isEnterprise, environments } = this.get();

        if (
            (item.live || item.working) &&
            ((item.baseType === 'HTMLPAGE' && loggedUser.canRead.htmlPages == true) ||
                (item.baseType === 'CONTENT' && loggedUser.canRead.contentlets == true)) &&
            !item.deleted
        ) {
            selectItems.push({
                label:
                    (item.baseType === 'HTMLPAGE' && loggedUser.canWrite.htmlPages == true) ||
                    (item.baseType === 'CONTENT' && loggedUser.canWrite.contentlets == true)
                        ? this.dotMessageService.get('Edit')
                        : this.dotMessageService.get('View'),
                command: () => {
                    this.dotRouterService.goToEditContentlet(item.inode);
                }
            });
        }

        // Adding Workflow actions
        actions.forEach((action: DotCMSWorkflowAction) => {
            selectItems.push({
                label: `${this.dotMessageService.get(action.name)}`,
                command: () => {
                    if (action.actionInputs?.length > 0) {
                        const wfActionEvent: DotCMSWorkflowActionEvent = {
                            workflow: action,
                            callback: 'ngWorkflowEventCallback',
                            inode: item.inode,
                            selectedInodes: null
                        };
                        this.dotWorkflowEventHandlerService.open(wfActionEvent);
                    } else {
                        this.dotWorkflowActionsFireService
                            .fireTo(item.inode, action.id)
                            .subscribe((item) => {
                                this.dotEventsService.notify('save-page', {
                                    payload: item,
                                    value: this.dotMessageService.get('Workflow-executed')
                                });
                            });
                    }
                }
            });
        });

        // Adding Push Publish action
        if (isEnterprise && environments) {
            selectItems.push({
                label: this.dotMessageService.get('contenttypes.content.push_publish'),
                command: (item) =>
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: item.identifier,
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    })
            });
        }

        // Adding Add To Bundle action
        if (isEnterprise) {
            selectItems.push({
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle'),
                command: () => this.showAddToBundle(item.identifier)
            });
        }

        return selectItems;
    }

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
        private siteService: SiteService
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
            this.getFavoritePagesData(initialFavoritePagesLimit),
            this.dotCurrentUser.getCurrentUser(),
            this.dotLanguagesService.get(),
            this.dotLicenseService.isEnterprise(),
            this.pushPublishService
                .getEnvironments()
                .pipe(map((environments: DotEnvironment[]) => !!environments.length))
        ])
            .pipe(
                take(1),
                mergeMap(([favoritePages, currentUser, languages, isEnterprise, environments]) => {
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
                                    permissionsType
                                ];
                            })
                        );
                })
            )
            .subscribe(
                ([favoritePages, currentUser, languages, isEnterprise, environments, permissions]: [
                    ESContent,
                    DotCurrentUser,
                    DotLanguage[],
                    boolean,
                    boolean,
                    DotPermissionsType
                ]): void => {
                    this.setState({
                        favoritePages: {
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
                            keyword: '',
                            status: ComponentStatus.INIT
                        },
                        portletStatus: ComponentStatus.LOADED
                    });
                },
                () => {
                    this.setState({
                        favoritePages: {
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
     * Limit Favorite page data
     * @param number limit
     * @memberof DotFavoritePageStore
     */
    limitFavoritePages(limit: number): void {
        const favoritePages = this.get().favoritePages.items;
        this.setFavoritePages(favoritePages.slice(0, limit));
    }
}

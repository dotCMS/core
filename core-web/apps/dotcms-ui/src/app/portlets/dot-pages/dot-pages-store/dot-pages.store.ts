import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MenuItem, SelectItem } from 'primeng/api';

import { delay, filter, map, mergeMap, retryWhen, switchMap, take, tap } from 'rxjs/operators';

import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { DotEnvironment } from '@dotcms/app/shared/models/dot-environment/dot-environment';
import {
    DotCurrentUserService,
    DotESContentService,
    DotEventsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPageTypesService,
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
            languages.forEach((language) => {
                languageOptions.push({
                    label: `${language.language} (${language.countryCode})`,
                    value: language.id
                });
            });

            return languageOptions;
        }
    );

    readonly languageLabels$: Observable<{ [id: string]: string }> = this.select(
        ({ languages }: DotPagesState) => {
            const langLabels = {};
            languages.forEach((language) => {
                langLabels[language.id] = `${language.languageCode}-${language.countryCode}`;
            });

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
                        },
                        (error: HttpErrorResponse) => {
                            return this.httpErrorManagerService.handle(error);
                        }
                    )
                );
            })
        )
    );

    readonly getPages = this.effect(
        (params$: Observable<{ offset: number; sortField?: string; sortOrder?: number }>) => {
            return params$.pipe(
                mergeMap((params) => {
                    const { offset, sortField, sortOrder } = params;
                    const sortOrderValue = this.getSortOrderValue(sortField, sortOrder);

                    return this.getPagesData(offset, sortOrderValue, sortField).pipe(
                        tapResponse(
                            (items) => {
                                const formatedPages = this.formatPagesData(items, offset);
                                this.setPages(formatedPages);
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

    readonly getPagesRetry = this.effect((params$: Observable<{ offset: number }>) => {
        return params$.pipe(
            mergeMap((params) => {
                const { offset } = params;
                const sortOrderValue = this.getSortOrderValue();

                let initialData = '';
                let retries = 0;

                return this.getPagesData(offset, sortOrderValue)
                    .pipe(
                        tap(
                            (items) => {
                                retries++;

                                // First fetch to grab initial data to compare
                                if (!initialData) {
                                    initialData = JSON.stringify(items.jsonObjectView);
                                    this.setPagesStatus(ComponentStatus.LOADING);
                                    throw false;
                                } else if (
                                    // Will continue repeating fetch until data has changed or limit fetch reached
                                    initialData === JSON.stringify(items.jsonObjectView) &&
                                    retries < 10
                                ) {
                                    throw false;
                                } else {
                                    // Terminated fetch loop and will proceed to set data on store
                                    const formatedPages = this.formatPagesData(items, offset);
                                    this.setPages(formatedPages);
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
    });

    readonly showActionsMenu = this.effect(
        (params$: Observable<{ item: DotCMSContentlet; actionMenuDomId: string }>) => {
            return params$.pipe(
                switchMap(({ item, actionMenuDomId }) => {
                    return this.dotWorkflowsActionsService
                        .getByInode(item.inode, DotRenderMode.LISTING)
                        .pipe(
                            take(1),
                            map((actions: DotCMSWorkflowAction[]) => {
                                return {
                                    actions: this.getSelectActions(actions, item),
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
        this.languageOptions$,
        this.languageLabels$,
        this.pageTypes$,
        (
            { favoritePages, isEnterprise, environments, languages, loggedUser, pages },
            isPagesLoading,
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
            isPagesLoading,
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

    private getESQuery() {
        const { keyword, languageId, archived } = this.get().pages;
        const hostId = this.siteService.currentSite.identifier;
        const langQuery = languageId ? `+languageId:${languageId}` : '';
        const archivedQuery = archived ? `+archived:${archived}` : '';
        const keywordQuery = keyword
            ? `+(title:${keyword}* OR path:*${keyword}* OR urlmap:*${keyword}*)`
            : '';

        return `+conhost:${hostId} +deleted:false  +(urlmap:* OR basetype:5) ${langQuery} ${archivedQuery} ${keywordQuery} `;
    }

    private formatRelativeDates(pages: DotCMSContentlet[]): DotCMSContentlet[] {
        const data = pages.map((page: DotCMSContentlet) => {
            return page
                ? {
                      ...page,
                      modDate: this.dotFormatDateService.getRelative(
                          new Date(page.modDate).getTime().toString(),
                          new Date()
                      )
                  }
                : undefined;
        });

        return data;
    }

    private getPagesData = (
        offset: number,
        sortOrderValue?: ESOrderDirection,
        sortField?: string
    ) => {
        return this.dotESContentService.get({
            itemsPerPage: 40,
            offset: offset.toString(),
            query: this.getESQuery(),
            sortField: sortField || 'modDate',
            sortOrder: sortOrderValue
        });
    };

    private formatPagesData = (items: ESContent, offset: number) => {
        const dateFormattedPages = this.formatRelativeDates(items.jsonObjectView.contentlets);

        let currentPages = this.get().pages.items;

        if (currentPages.length === 0) {
            currentPages = Array.from({ length: items.resultsSize });
        }

        Array.prototype.splice.apply(currentPages, [...[offset, 40], ...dateFormattedPages]);

        return currentPages;
    };

    private getFavoritePagesData = (limit: number) => {
        return this.dotESContentService.get({
            itemsPerPage: limit,
            offset: '0',
            query: FAVORITE_PAGES_ES_QUERY,
            sortField: 'dotFavoritePage.order',
            sortOrder: ESOrderDirection.ASC
        });
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
                            .subscribe(() => {
                                this.dotEventsService.notify('save-page', {
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
        private dotCurrentUserService: DotCurrentUserService,
        private dotRouterService: DotRouterService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotESContentService: DotESContentService,
        private dotPageTypesService: DotPageTypesService,
        private dotFormatDateService: DotFormatDateService,
        private dotMessageService: DotMessageService,
        private dotLanguagesService: DotLanguagesService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
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
                    return this.dotCurrentUserService
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
                        }
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

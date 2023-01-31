/* eslint-disable no-console */
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { MenuItem, SelectItem } from 'primeng/api';

import { filter, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';

import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { DotEnvironment } from '@dotcms/app/shared/models/dot-environment/dot-environment';
import {
    DotCurrentUserService,
    DotESContentService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    ESOrderDirection
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotCMSWorkflowAction,
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
        id: string;
        canRead: { contentlets: boolean; htmlPages: boolean };
        canWrite: { contentlets: boolean; htmlPages: boolean };
    };
    pages?: {
        addToBundleCTId?: string;
        rowActionMenuIndex?: number;
        menuActions?: MenuItem[];
        items: DotCMSContentlet[];
        keyword?: string;
        languageId?: string;
        archived?: boolean;
    };
}

const FAVORITE_PAGES_ES_QUERY = `+contentType:dotFavoritePage +deleted:false +working:true`;

@Injectable()
export class DotPageStore extends ComponentStore<DotPagesState> {
    readonly rowActionMenuIndex$: Observable<number> = this.select(
        ({ pages }) => pages.rowActionMenuIndex
    ).pipe(filter((i) => i !== null));

    readonly languageOptions$: Observable<SelectItem[]> = this.select(
        ({ languages }: DotPagesState) => {
            const languageOptions: SelectItem[] = [];
            languageOptions.push({
                // label: `${language.languageCode}_${language.countryCode}`,
                label: this.dotMessageService.get('All'),
                value: null
            });
            languages.forEach((language) => {
                languageOptions.push({
                    label: `${language.language} (${language.countryCode})`,
                    value: language.id
                });
            });
            console.log('***lang', languageOptions);

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

    /** A function that updates the Favorite Pages Items in the state of the store.
     * @param DotPagesState state
     * @param DotCMSContentlet[] favoritePages
     * @memberof DotPageStore
     */
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
            console.log('***set pages', {
                ...state,
                pages: {
                    ...state.pages,
                    items: [...pages]
                }
            });

            return {
                ...state,
                pages: {
                    ...state.pages,
                    items: [...pages]
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

    readonly setMenuActions = this.updater(
        (
            state: DotPagesState,
            { actions, rowIndex }: { actions: MenuItem[]; rowIndex: number }
        ) => {
            console.log('===setMenuActions', {
                ...state,
                pages: {
                    ...state.pages,
                    menuActions: actions,
                    rowActionMenuIndex: rowIndex
                }
            });

            return {
                ...state,
                pages: {
                    ...state.pages,
                    menuActions: actions,
                    rowActionMenuIndex: rowIndex
                }
            };
        }
    );

    readonly showAddToBundle = this.updater((state: DotPagesState, addToBundleCTId: string) => {
        console.log('***showAddToBundle', addToBundleCTId, {
            ...state,
            pages: {
                ...state.pages,
                addToBundleCTId
            }
        });

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

    // TODO: ver si
    // - meter template para rows al mostrar data
    // - meter doc
    // - hacer lo del hamburguer button
    // - meter const para LIMIT de 40
    // - meter const para QUERY de getPages()
    readonly getPages = this.effect(
        (params$: Observable<{ offset: number; sortField?: string; sortOrder?: number }>) => {
            return params$.pipe(
                switchMap((params) => {
                    const { offset, sortField, sortOrder } = params;

                    return this.dotESContentService
                        .get({
                            itemsPerPage: 40,
                            offset: offset.toString(),
                            query: this.getESQuery(),
                            sortField: sortField || 'modDate',
                            sortOrder:
                                sortOrder === -1 ? ESOrderDirection.DESC : ESOrderDirection.ASC
                        })
                        .pipe(
                            tapResponse(
                                (items) => {
                                    console.log('===items.resultsSize', items.resultsSize);

                                    const dateFormattedPages = this.formatRelativeDates(
                                        items.jsonObjectView.contentlets
                                    );
                                    console.log(
                                        '**tempPages format',
                                        this.formatRelativeDates(items.jsonObjectView.contentlets)
                                        // tempPages
                                    );

                                    let currentPages = this.get().pages.items;

                                    if (currentPages.length === 0) {
                                        currentPages = Array.from({ length: items.resultsSize });
                                    }

                                    Array.prototype.splice.apply(currentPages, [
                                        ...[offset, 40],
                                        ...dateFormattedPages
                                    ]);

                                    // console.log('**tempPages', tempPages);

                                    this.setPages(currentPages);
                                    // this.patchState({
                                    //     pages: {
                                    //         items: [...items.jsonObjectView.contentlets],
                                    //         total: items.resultsSize
                                    //     }
                                    // });
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

    readonly showActionsMenu = this.effect(
        (params$: Observable<{ item: DotCMSContentlet; rowIndex: number }>) => {
            return params$.pipe(
                switchMap(({ item, rowIndex }) => {
                    return this.dotWorkflowsActionsService
                        .getByInode(item.inode, DotRenderMode.LISTING)
                        .pipe(
                            take(1),
                            map((actions: DotCMSWorkflowAction[]) => {
                                console.log(actions);

                                return { actions: this.getSelectActions(actions, item), rowIndex };
                            })
                        );
                }),
                tap(({ actions, rowIndex }: { actions: MenuItem[]; rowIndex: number }) => {
                    this.setMenuActions({ actions, rowIndex });
                })
            );
        }
    );

    readonly vm$: Observable<DotPagesState> = this.select(
        this.state$,
        this.languageOptions$,
        this.languageLabels$,
        (
            { favoritePages, isEnterprise, environments, languages, loggedUser, pages },
            languageOptions,
            languageLabels
        ) => ({
            favoritePages,
            isEnterprise,
            environments,
            languages,
            loggedUser,
            pages,
            languageOptions,
            languageLabels
        })
    );

    private getESQuery() {
        const { keyword, languageId, archived } = this.get().pages;
        const langQuery = languageId ? `+languageId:${languageId}` : '';
        const archivedQuery = archived ? `+archived:${archived}` : '';
        const keywordQuery = keyword
            ? `+(title:${keyword}* OR path:${keyword}* OR urlmap:${keyword}*)`
            : '';

        return `+conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 +deleted:false  +(urlmap:* OR basetype:5) ${langQuery} ${archivedQuery} ${keywordQuery} `;
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
                    console.log('===EDIT', item);
                    this.dotRouterService.goToEditContentlet(item.inode);
                }
            });
        }

        // Adding Workflow actions
        actions.forEach((action: DotCMSWorkflowAction) => {
            selectItems.push({
                label: `${this.dotMessageService.get(action.name)}`,
                command: () => {
                    console.log('===getSelectActions coommand', item);
                    this.dotWorkflowActionsFireService
                        .fireTo(item.inode, action.id)
                        .subscribe((data) => {
                            console.log('===fireTo', data);
                            this.dotMessageDisplayService.push({
                                life: 3000,
                                message: this.dotMessageService.get('Workflow-executed'),
                                severity: DotMessageSeverity.SUCCESS,
                                type: DotMessageType.SIMPLE_MESSAGE
                            });
                        });
                }
            });
        });

        // Adding Add To Bundle action
        if (isEnterprise) {
            selectItems.push({
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle'),
                // command: (item) => this.addToBundleContentType(item)
                command: () => this.showAddToBundle(item.identifier)
            });
        }

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

        return selectItems;
    }

    constructor(
        private dotCurrentUser: DotCurrentUserService,
        private dotCurrentUserService: DotCurrentUserService,
        private dotRouterService: DotRouterService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotESContentService: DotESContentService,
        private dotFormatDateService: DotFormatDateService,
        private dotMessageService: DotMessageService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dotLanguagesService: DotLanguagesService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotWorkflowsActionsService: DotWorkflowsActionsService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotLicenseService: DotLicenseService,
        private pushPublishService: PushPublishService
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
                            keyword: ''
                            // items: Array.from({ length: 124 }),
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

/* eslint-disable @typescript-eslint/member-ordering */
/* eslint-disable no-console */
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { SelectItem } from 'primeng/api';

import { map, switchMap, take } from 'rxjs/operators';

import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotActionMenuItem } from '@dotcms/app/shared/models/dot-action-menu/dot-action-menu-item.model';
import {
    DotCurrentUserService,
    DotESContentService,
    DotLanguagesService,
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    ESOrderDirection
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSWorkflowAction,
    DotCurrentUser,
    DotLanguage,
    ESContent
} from '@dotcms/dotcms-models';

export interface DotPagesState {
    favoritePages: {
        items: DotCMSContentlet[];
        showLoadMoreButton: boolean;
        total: number;
    };
    languages: DotLanguage[];
    loggedUserId: string;
    pages?: {
        items: DotCMSContentlet[];
        keyword?: string;
        languageId?: string;
        archived?: boolean;
    };
}

export interface DotPagesViewModel extends DotPagesState {
    languageOptions: SelectItem[];
}

const FAVORITE_PAGES_ES_QUERY = `+contentType:dotFavoritePage +deleted:false +working:true`;

@Injectable()
export class DotPageStore extends ComponentStore<DotPagesState> {
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
                langLabels[language.id] = `${language.languageCode}_${language.countryCode}`;
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

    readonly vm$: Observable<DotPagesViewModel> = this.select(
        this.state$,
        this.languageOptions$,
        this.languageLabels$,
        ({ favoritePages, languages, loggedUserId, pages }, languageOptions, languageLabels) => ({
            favoritePages,
            languages,
            loggedUserId,
            pages,
            languageOptions,
            languageLabels
        })
    );

    constructor(
        private dotCurrentUser: DotCurrentUserService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotESContentService: DotESContentService,
        private dotFormatDateService: DotFormatDateService,
        private dotMessageService: DotMessageService,
        private dotLanguagesService: DotLanguagesService,
        private dotWorkflowsActionsService: DotWorkflowsActionsService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService
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
            this.dotLanguagesService.get()
        ])
            .pipe(take(1))
            .subscribe(
                ([favoritePages, currentUser, languages]: [
                    ESContent,
                    DotCurrentUser,
                    DotLanguage[]
                ]): void => {
                    this.setState({
                        favoritePages: {
                            items: favoritePages?.jsonObjectView.contentlets,
                            showLoadMoreButton:
                                favoritePages.jsonObjectView.contentlets.length <
                                favoritePages.resultsSize,
                            total: favoritePages.resultsSize
                        },
                        languages,
                        loggedUserId: currentUser.userId,
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

    getContentletActions(item: DotCMSContentlet): Observable<DotActionMenuItem[]> {
        return this.dotWorkflowsActionsService.getByInode(item.inode, DotRenderMode.LISTING).pipe(
            take(1),
            map((actions: DotCMSWorkflowAction[]) => {
                console.log(actions);

                return this.getSelectActions(actions);
            })
        );
    }

    private getSelectActions(actions: DotCMSWorkflowAction[]): DotActionMenuItem[] {
        const selectItems: DotActionMenuItem[] = [];

        actions.forEach((action: DotCMSWorkflowAction) => {
            selectItems.push({
                menuItem: {
                    label: `${this.dotMessageService.get(action.name)}`,
                    command: (item) => {
                        console.log(item);
                        this.dotWorkflowActionsFireService
                            .fireTo(item.inode, action.id)
                            .subscribe((data) => {
                                console.log('===fireTo', data);
                            });
                    }
                }
            });
        });

        return selectItems;
    }

    // getLanguageOptions(): SelectItem[] {
    //     const languageOptions = [];
    //     languageOptions.push({
    //         // label: `${language.languageCode}_${language.countryCode}`,
    //         label: `All`,
    //         value: null
    //     });
    //     this.get().languages.forEach((language) => {
    //         languageOptions.push({
    //             // label: `${language.languageCode}_${language.countryCode}`,
    //             label: `${language.language} (${language.countryCode})`,
    //             value: language.id
    //         });
    //     });

    //     return languageOptions;
    // }
}

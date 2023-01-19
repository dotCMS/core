/* eslint-disable no-console */
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap, take } from 'rxjs/operators';

import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import {
    DotCurrentUserService,
    DotESContentService,
    DotLanguagesService,
    ESOrderDirection
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCurrentUser, DotLanguage, ESContent } from '@dotcms/dotcms-models';

export interface DotPagesState {
    favoritePages: {
        items: DotCMSContentlet[];
        showLoadMoreButton: boolean;
        total: number;
    };
    languageLabels: { [id: string]: string };
    loggedUserId: string;
    pages?: {
        items: DotCMSContentlet[];
        filter: string;
    };
}

const FAVORITE_PAGES_ES_QUERY = `+contentType:dotFavoritePage +deleted:false +working:true`;

@Injectable()
export class DotPageStore extends ComponentStore<DotPagesState> {
    readonly vm$ = this.state$;

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

    readonly setFilter = this.updater<string>((state: DotPagesState, keyword: string) => {
        return {
            ...state,
            pages: {
                items: [],
                filter: keyword
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
    // - hacer lo de SORT
    // - hacer lo del hamburguer button
    // - meter const para LIMIT de 40
    // - meter const para QUERY de getPages()
    readonly getPages = this.effect(
        (params$: Observable<{ offset: number; sortField?: string; sortOrder?: number }>) => {
            return params$.pipe(
                switchMap((params) => {
                    const filter = this.get().pages.filter;
                    const { offset, sortField, sortOrder } = params;

                    return this.dotESContentService
                        .get({
                            itemsPerPage: 40,
                            offset: offset.toString(),
                            query: '+conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 +deleted:false  +(urlmap:* OR basetype:5)',
                            sortField: sortField || 'modDate',
                            filter,
                            sortOrder:
                                sortOrder === -1 ? ESOrderDirection.DESC : ESOrderDirection.ASC
                        })
                        .pipe(
                            tapResponse(
                                (items) => {
                                    console.log('===items.resultsSize', items.resultsSize);

                                    let tempPages = this.get().pages.items;

                                    if (tempPages.length === 0) {
                                        tempPages = Array.from({ length: items.resultsSize });
                                    }

                                    Array.prototype.splice.apply(tempPages, [
                                        ...[offset, 40],
                                        ...items.jsonObjectView.contentlets
                                    ]);

                                    // console.log('**tempPages', tempPages);
                                    /*
                                    tempPages = this.formatRelativeDates(tempPages);
                                    console.log(
                                        '**tempPages format',
                                        // this.formatRelativeDates(tempPages)
                                        tempPages
                                    );
*/
                                    this.setPages(tempPages);
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

    private formatRelativeDates(pages: DotCMSContentlet[]): DotCMSContentlet[] {
        this.dotFormatDateService.setLang('es_ES');

        const data = pages.map((page: DotCMSContentlet) => {
            return page
                ? {
                      ...page,
                      modDate: this.dotFormatDateService.getRelative(page.modDate)
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

    constructor(
        private dotCurrentUser: DotCurrentUserService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotESContentService: DotESContentService,
        private dotFormatDateService: DotFormatDateService,
        private dotLanguagesService: DotLanguagesService
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
                    const langLabels = {};
                    languages.forEach((language) => {
                        langLabels[
                            language.id
                        ] = `${language.languageCode}_${language.countryCode}`;
                    });

                    this.setState({
                        favoritePages: {
                            items: favoritePages?.jsonObjectView.contentlets,
                            showLoadMoreButton:
                                favoritePages.jsonObjectView.contentlets.length <
                                favoritePages.resultsSize,
                            total: favoritePages.resultsSize
                        },
                        languageLabels: langLabels,
                        loggedUserId: currentUser.userId,
                        pages: {
                            items: [],
                            filter: ''
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

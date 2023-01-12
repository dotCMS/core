/* eslint-disable no-console */
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotCurrentUserService, DotESContentService, ESOrderDirection } from '@dotcms/data-access';
import { DotCMSContentlet, DotCurrentUser, ESContent } from '@dotcms/dotcms-models';

export interface DotPagesState {
    favoritePages: {
        items: DotCMSContentlet[];
        showLoadMoreButton: boolean;
        total: number;
    };
    loggedUserId: string;
    pages?: {
        items: DotCMSContentlet[];
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
            console.log('*** state pages', state.pages.items);
            console.log('*** rrecibe pages', pages);

            return {
                ...state,
                pages: {
                    ...state.pages,
                    items: [...pages]
                }
            };
        }
    );

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
    // - meto un parametro extra aparte de OFFSET para el filtro
    // - meter INPUT para filtro
    // - meter template para rows al mostrar data
    // - meter doc
    // - meter const para LIMIT de 40
    // - meter const para QUERY de getPages()
    readonly getPages = this.effect((offset$: Observable<number>) => {
        return offset$.pipe(
            switchMap((offset: number) => {
                return this.dotESContentService
                    .get({
                        itemsPerPage: 40,
                        offset: offset.toString(),
                        query: '+conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 +deleted:false  +(urlmap:* OR basetype:5)',
                        sortField: 'modDate',
                        // filter:'surf camp',
                        sortOrder: ESOrderDirection.ASC
                    })
                    .pipe(
                        tapResponse(
                            (items) => {
                                console.log('**resp', items.jsonObjectView.contentlets);

                                let tempPages = this.get().pages.items;

                                if (tempPages.length === 0) {
                                    tempPages = Array.from({ length: items.resultsSize });
                                }

                                Array.prototype.splice.apply(tempPages, [
                                    ...[offset, 40],
                                    ...items.jsonObjectView.contentlets
                                ]);

                                console.log('**tempPages', tempPages);

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
    });

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
        private dotESContentService: DotESContentService
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
            this.dotCurrentUser.getCurrentUser()
        ])
            .pipe(take(1))
            .subscribe(([favoritePages, currentUser]: [ESContent, DotCurrentUser]): void => {
                this.setState({
                    favoritePages: {
                        items: favoritePages?.jsonObjectView.contentlets,
                        showLoadMoreButton:
                            favoritePages.jsonObjectView.contentlets.length <
                            favoritePages.resultsSize,
                        total: favoritePages.resultsSize
                    },
                    loggedUserId: currentUser.userId,
                    pages: {
                        items: []
                        // items: Array.from({ length: 124 }),
                    }
                });
            });
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

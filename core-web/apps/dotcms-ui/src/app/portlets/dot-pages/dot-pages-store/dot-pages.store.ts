import { Injectable } from '@angular/core';
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';
import { DotCMSContentlet, DotCurrentUser, ESContent } from '@dotcms/dotcms-models';

import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { HttpErrorResponse } from '@angular/common/http';
import { DotCurrentUserService, DotESContentService, ESOrderDirection } from '@dotcms/data-access';

export interface DotPagesState {
    favoritePages: {
        items: DotCMSContentlet[];
        showLoadMoreButton: boolean;
        total: number;
    };
    loggedUserId: string;
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
                    loggedUserId: currentUser.userId
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

import { Injectable } from '@angular/core';
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable, of } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotCurrentUserService } from '@dotcms/app/api/services/dot-current-user/dot-current-user.service';
import { DotCurrentUser } from '@dotcms/app/shared/models/dot-current-user/dot-current-user';
import {
    DotESContentService,
    ESOrderDirection
} from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { ESContent } from '@dotcms/app/shared/models/dot-es-content/dot-es-content.model';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { HttpErrorResponse } from '@angular/common/http';

export const enum DotFavoritePageActionState {
    SAVED = 'SAVED',
    DELETED = 'DELETED'
}

export interface DotPagesState {
    favoritePages: {
        items: DotCMSContentlet[];
        total: number;
    };
    loggedUserId: string;
}

const FAVORITE_PAGES_ES_QUERY = `+contentType:dotFavoritePage +deleted:false +working:true`;

@Injectable()
export class DotPageStore extends ComponentStore<DotPagesState> {
    private initialFavoritePagesLimit = 5;
    private favoritePagesTotalSize = 0;
    private favoritePages = [];

    readonly vm$ = this.state$;

    /** A function that updates the Favorite Pages Items in the state of the store.
     * @param state DotPagesState
     * @param categories DotCMSContentlet[]
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
    readonly loadAllFavoritePages = this.effect<void>((trigger$: Observable<unknown>) =>
        trigger$.pipe(
            switchMap(() =>
                this.dotESContentService
                    .get({
                        itemsPerPage: this.favoritePagesTotalSize - this.initialFavoritePagesLimit,
                        offset: this.initialFavoritePagesLimit.toString(),
                        query: FAVORITE_PAGES_ES_QUERY,
                        sortField: 'dotFavoritePage.order',
                        sortOrder: ESOrderDirection.ASC
                    })
                    .pipe(
                        tapResponse(
                            (items) => {
                                this.favoritePages = [
                                    ...this.favoritePages.slice(0, this.initialFavoritePagesLimit),
                                    ...items.jsonObjectView.contentlets
                                ];
                                this.setFavoritePages(this.favoritePages);
                            },
                            (error: HttpErrorResponse) => {
                                return this.httpErrorManagerService.handle(error);
                            }
                        )
                    )
            )
        )
    );

    constructor(
        private dotCurrentUser: DotCurrentUserService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotESContentService: DotESContentService
    ) {
        super(null);
    }

    /**
     * Sets initial state data from props, roles and current logged user data
     *
     * @memberof DotFavoritePageStore
     */
    setInitialStateData(): void {
        forkJoin([
            this.dotESContentService.get({
                itemsPerPage: this.initialFavoritePagesLimit,
                offset: '0',
                query: FAVORITE_PAGES_ES_QUERY,
                sortField: 'dotFavoritePage.order',
                sortOrder: ESOrderDirection.ASC
            }),
            this.dotCurrentUser.getCurrentUser()
        ])
            .pipe(take(1))
            .subscribe(([favoritePages, currentUser]: [ESContent, DotCurrentUser]): void => {
                this.favoritePages = favoritePages?.jsonObjectView.contentlets;
                this.favoritePagesTotalSize = favoritePages?.resultsSize;

                this.setState({
                    favoritePages: {
                        items: favoritePages?.jsonObjectView.contentlets,
                        total: this.favoritePagesTotalSize
                    },
                    loggedUserId: currentUser.userId
                });
            });
    }

    limitFavoritePages(): void {
        this.setFavoritePages(this.favoritePages.slice(0, this.initialFavoritePagesLimit));
    }
}

import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotCurrentUserService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCurrentUser } from '@dotcms/dotcms-models';

export interface UserState {
    currentUser: DotCurrentUser;
}

export const userInitialState: UserState = {
    currentUser: null
};

/**
 * Feature that manages the current user state
 * Used to determine if the current user is the one who locked the content
 */
export function withUser() {
    return signalStoreFeature(
        withState(userInitialState),
        withMethods(
            (
                store,
                dotCurrentUserService = inject(DotCurrentUserService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
            ) => ({
                /**
                 * Loads the current user data
                 */
                loadCurrentUser: rxMethod<void>(
                    pipe(
                        switchMap(() =>
                            dotCurrentUserService.getCurrentUser().pipe(
                                tapResponse({
                                    next: (currentUser) => {
                                        patchState(store, {
                                            currentUser
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            currentUser: null
                                        });
                                        dotHttpErrorManagerService.handle(error);
                                    }
                                })
                            )
                        )
                    )
                )
            })
        )
    );
}

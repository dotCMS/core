import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotCurrentUserService, DotHttpErrorManagerService } from '@dotcms/data-access';

import { EditContentState } from '../../edit-content.store';

/**
 * Feature that manages the current user state
 * Used to determine if the current user is the one who locked the content
 */
export function withUser() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
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

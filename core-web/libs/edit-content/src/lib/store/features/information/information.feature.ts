import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, inject, untracked } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EditContentState } from '../../edit-content.store';

/**
 * Signal store feature that manages the information component state in the edit content sidebar
 * Handles loading states, error handling, and related content count for the current contentlet
 */
export function withInformation() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withComputed(({ information }) => ({
            isLoadingInformation: computed(() => information().status === ComponentStatus.LOADING)
        })),

        withMethods(
            (
                store,
                dotEditContentService = inject(DotEditContentService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
            ) => ({
                /**
                 * Fetches the number of reference pages for the current contentlet and updates the application state.
                 */
                getReferencePages: rxMethod<string>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                information: {
                                    ...store.information(),
                                    status: ComponentStatus.LOADING,
                                    error: null
                                }
                            })
                        ),
                        switchMap((identifier: string) => {
                            return dotEditContentService.getReferencePages(identifier).pipe(
                                tapResponse({
                                    next: (value) =>
                                        patchState(store, {
                                            information: {
                                                ...store.information(),
                                                relatedContent: value.toString(),
                                                status: ComponentStatus.LOADED
                                            }
                                        }),

                                    error: (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            information: {
                                                ...store.information(),
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                        dotHttpErrorManagerService.handle(error);
                                    }
                                })
                            );
                        })
                    )
                )
            })
        ),
        withHooks({
            onInit(store) {
                /**
                 * Reloads reference pages whenever the contentlet changes, store-owned
                 * so it works regardless of which component (or host — dialog vs
                 * full-screen route) happens to be mounted at the time. Depends on the
                 * whole contentlet (not just the identifier) so it also refreshes after
                 * a save/publish, which mints a NEW inode under the SAME identifier.
                 * Gated on `isSidebarOpen` to avoid firing on every edit-content load
                 * when the user never opens the sidebar.
                 *
                 * Reads the `isSidebarOpen` leaf rather than `uiState()`: every writer
                 * replaces that slice wholesale, so depending on the object would refetch
                 * on unrelated UI changes — including the `view` flip that `loadVersions`
                 * itself performs.
                 */
                effect(() => {
                    const contentlet = store.contentlet();
                    const identifier = contentlet?.identifier;
                    const isSidebarOpen = store.uiState.isSidebarOpen();

                    untracked(() => {
                        if (identifier && isSidebarOpen) {
                            store.getReferencePages(identifier);
                        }
                    });
                });
            }
        })
    );
}

import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withHooks, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { effect, inject, untracked } from '@angular/core';

import { MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { ContentletIdentifier } from '../../../models/dot-edit-content-field.type';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EditContentState } from '../../edit-content.store';

/**
 * Feature store for managing activities state
 */
export function withActivities() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withMethods(
            (
                store,
                dotEditContentService = inject(DotEditContentService),
                errorManager = inject(DotHttpErrorManagerService),
                messageService = inject(MessageService),
                dotMessageService = inject(DotMessageService)
            ) => ({
                /**
                 * Sets the activity view state for the Activity Sidebar Component
                 * @param state The new state to set
                 */
                setActivityViewState: (state: 'idle' | 'create') => {
                    patchState(store, {
                        activityViewState: state
                    });
                },

                /**
                 * Loads activities for a given content identifier
                 * @param identifier Content identifier
                 */
                loadActivities: rxMethod<ContentletIdentifier>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                activitiesStatus: {
                                    status: ComponentStatus.LOADING,
                                    error: null
                                }
                            });
                        }),
                        switchMap((identifier) =>
                            dotEditContentService.getActivities(identifier).pipe(
                                tapResponse({
                                    next: (activities) => {
                                        patchState(store, {
                                            // Sort activities by createdDate, the latest activity should be first. Endpoint returns them in descending order.
                                            activities: activities.sort(
                                                (a, b) => a.createdDate - b.createdDate
                                            ),
                                            activitiesStatus: {
                                                status: ComponentStatus.LOADED,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        errorManager.handle(error);
                                        patchState(store, {
                                            activitiesStatus: {
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                    }
                                })
                            )
                        )
                    )
                ),

                /**
                 * Creates a new activity (comment) for a given content identifier
                 * @param params Object containing identifier and comment
                 */
                addComment: rxMethod<{ identifier: ContentletIdentifier; comment: string }>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                activitiesStatus: {
                                    status: ComponentStatus.SAVING,
                                    error: null
                                }
                            });
                        }),
                        switchMap(({ identifier, comment }) =>
                            dotEditContentService.createActivity(identifier, comment).pipe(
                                tapResponse({
                                    next: (activity) => {
                                        messageService.clear();
                                        messageService.add({
                                            severity: 'success',
                                            summary: dotMessageService.get(
                                                'edit.content.sidebar.activities.comment.success.title'
                                            ),
                                            detail: dotMessageService.get(
                                                'edit.content.sidebar.activities.comment.success.message'
                                            )
                                        });
                                        patchState(store, (state) => ({
                                            activities: [...(state.activities || []), activity],
                                            activitiesStatus: {
                                                status: ComponentStatus.IDLE,
                                                error: null
                                            }
                                        }));
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        errorManager.handle(error);
                                        patchState(store, {
                                            activitiesStatus: {
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                    }
                                })
                            )
                        )
                    )
                )
            })
        ),
        withHooks({
            onInit(store) {
                /**
                 * Reloads activities whenever the contentlet changes, store-owned so
                 * it works regardless of which component (or host — dialog vs
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
                            store.loadActivities(identifier);
                        }
                    });
                });
            }
        })
    );
}

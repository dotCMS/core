import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

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
        )
    );
}

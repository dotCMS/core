import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { tapResponse } from '@ngrx/component-store';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';
import { delay, switchMap, tap } from 'rxjs/operators';
import { DotHttpErrorManagerService } from '../../../../../../data-access/src/lib/dot-http-error-manager/dot-http-error-manager.service';
import { ComponentStatus } from '../../../../../../dotcms-models/src/lib/shared-models';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EditContentState } from './edit-content.store';

interface InformationState {
    information: {
        status: ComponentStatus;
        error: string | null;
        relatedContent: number;
    };
}

const initialState: InformationState = {
    information: {
        status: ComponentStatus.INIT,
        error: null,
        relatedContent: 0
    }
};

/**
 * Signal store feature that manages the information component state in the edit content sidebar
 * Handles loading states, error handling, and related content count for the current contentlet
 */
export function withInformation() {
    return signalStoreFeature(
        {
            state: type<EditContentState>()
        },
        withState(initialState),
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
                                delay(3000),
                                tapResponse({
                                    next: (value) =>
                                        patchState(store, {
                                            information: {
                                                ...store.information(),
                                                relatedContent: Number(value),
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

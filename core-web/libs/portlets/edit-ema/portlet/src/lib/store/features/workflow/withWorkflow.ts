import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { RxMethod, rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { UVE_STATUS } from '../../../shared/enums';
import { UVEState } from '../../models';

interface WithWorkflowState {
    workflowActions: DotCMSWorkflowAction[];
    workflowIsLoading: boolean;
}

/**
 * Interface defining the methods provided by withWorkflow
 * Use this as props type in dependent features
 *
 * @export
 * @interface WithWorkflowMethods
 */
export interface WithWorkflowMethods {
    // Methods
    workflowFetch: RxMethod<string>;
    setWorkflowActionLoading: (workflowIsLoading: boolean) => void;
}

/**
 * Add load and reload method to the store
 *
 * @export
 * @return {*}
 */
export function withWorkflow() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<WithWorkflowState>({
            workflowActions: [],
            workflowIsLoading: true
        }),
        withMethods((store) => {
            const dotWorkflowsActionsService = inject(DotWorkflowsActionsService);

            return {
                /**
                 * Load workflow actions
                 */
                workflowFetch: rxMethod<string>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                workflowIsLoading: true
                            });
                        }),
                        switchMap((pageInode) => {
                            return dotWorkflowsActionsService.getByInode(pageInode).pipe(
                                tapResponse({
                                    next: (workflowActions = []) => {
                                        patchState(store, {
                                            workflowActions,
                                            workflowIsLoading: false
                                        });
                                    },
                                    error: ({ status: errorStatus }: HttpErrorResponse) => {
                                        patchState(store, {
                                            errorCode: errorStatus,
                                            status: UVE_STATUS.ERROR
                                        });
                                    }
                                })
                            );
                        })
                    )
                ),
                setWorkflowActionLoading: (workflowIsLoading: boolean) => {
                    patchState(store, { workflowIsLoading });
                }
            };
        })
    );
}

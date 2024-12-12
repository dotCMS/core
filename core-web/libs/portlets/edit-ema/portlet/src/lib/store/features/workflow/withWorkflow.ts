import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
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
    workflowLoading: boolean;
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
            workflowLoading: false
        }),
        withMethods((store) => {
            const dotWorkflowsActionsService = inject(DotWorkflowsActionsService);

            return {
                /**
                 * Load workflow actions
                 */
                getWorkflowActions: rxMethod<void | string>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                workflowLoading: false
                            });
                        }),
                        switchMap((inode) => {
                            const pageInode = inode || store.pageAPIResponse()?.page.inode;

                            return dotWorkflowsActionsService.getByInode(pageInode).pipe(
                                tapResponse({
                                    next: (workflowActions = []) => {
                                        patchState(store, {
                                            workflowActions,
                                            workflowLoading: true
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
                )
            };
        })
    );
}

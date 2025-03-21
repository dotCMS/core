import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { ContentletIdentifier } from '../../../models/dot-edit-content-field.type';

import { Activity } from '../../../models/dot-edit-content-file.model';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EditContentState } from '../../edit-content.store';

/**
 * Interface representing the activities state
 */
export interface ActivitiesState {
    /** List of activities */
    activities: Activity[];

    activitiesStatus: {
        status: ComponentStatus;
        error: string | null;
    };
}

/**
 * Feature store for managing activities state
 */
export function withActivities() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withMethods(
            (
                store,
                service = inject(DotEditContentService),
                errorManager = inject(DotHttpErrorManagerService)
            ) => ({
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
                            service.getActivities(identifier).pipe(
                                tapResponse({
                                    next: (activities) => {
                                        patchState(store, {
                                            activities,
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
                 * Creates a new activity for a given content identifier
                 * @param params Parameters for creating an activity
                 */
                createActivity: rxMethod<{ identifier: ContentletIdentifier; comment: string }>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                activitiesStatus: {
                                    status: ComponentStatus.LOADING,
                                    error: null
                                }
                            });
                        }),
                        switchMap((params) =>
                            service.createActivity(params.identifier, params.comment).pipe(
                                switchMap(() => service.getActivities(params.identifier)),
                                tapResponse({
                                    next: (activities) => {
                                        patchState(store, {
                                            activities,
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
                )
            })
        )
    );
}

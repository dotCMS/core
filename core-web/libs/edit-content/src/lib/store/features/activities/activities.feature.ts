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
                            dotEditContentService.getActivities(identifier).pipe(
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

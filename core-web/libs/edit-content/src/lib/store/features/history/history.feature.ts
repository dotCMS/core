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
 * Feature store for managing content versions state
 */
export function withHistory() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withMethods(
            (
                store,
                dotEditContentService = inject(DotEditContentService),
                errorManager = inject(DotHttpErrorManagerService)
            ) => ({
                /**
                 * Loads versions for a given content identifier
                 * Retrieves all versions of the content including live, working, and archived versions
                 * @param identifier Content identifier
                 */
                loadVersions: rxMethod<ContentletIdentifier>(
                    pipe(
                        tap(() => {
                            patchState(store, {
                                versionsStatus: {
                                    status: ComponentStatus.LOADING,
                                    error: null
                                }
                            });
                        }),
                        switchMap((identifier) =>
                            dotEditContentService.getVersions(identifier).pipe(
                                tapResponse({
                                    next: (versions) => {
                                        patchState(store, {
                                            versions,
                                            versionsStatus: {
                                                status: ComponentStatus.LOADED,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        errorManager.handle(error);
                                        patchState(store, {
                                            versionsStatus: {
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
                 * Clears the versions data and resets status to initial state
                 */
                clearVersions: () => {
                    patchState(store, {
                        versions: [],
                        versionsStatus: {
                            status: ComponentStatus.INIT,
                            error: null
                        }
                    });
                }
            })
        )
    );
}

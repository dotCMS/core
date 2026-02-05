import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { delay, tap } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { FiltersState } from './with-filters.feature';

import { RequestState } from '../../types';
import { EngagementData } from '../../types/engagement.types';
import { createInitialRequestState } from '../../utils/data/analytics-data.utils';
import { MOCK_ENGAGEMENT_DATA } from '../../utils/mock-engagement-data';

/**
 * State interface for the Engagement feature.
 */
export interface EngagementState {
    engagementData: RequestState<EngagementData>;
}

/**
 * Initial state for the Engagement feature.
 */
const initialEngagementState: EngagementState = {
    engagementData: createInitialRequestState()
};

/**
 * Signal Store Feature for managing engagement analytics data.
 */
export function withEngagement() {
    return signalStoreFeature(
        { state: type<FiltersState>() },
        withState(initialEngagementState),
        withMethods((store) => ({
            loadEngagementData: rxMethod<void>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            engagementData: {
                                status: ComponentStatus.LOADING,
                                data: null,
                                error: null
                            }
                        })
                    ),
                    delay(500), // Simulate network delay
                    tap(() => {
                        patchState(store, {
                            engagementData: {
                                status: ComponentStatus.LOADED,
                                data: MOCK_ENGAGEMENT_DATA,
                                error: null
                            }
                        });
                    })
                )
            )
        }))
    );
}

import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsService } from '../services/dot-analytics.service';

export interface AnalyticsDashboardData {
    metrics: Record<string, unknown>[];
    summary: {
        totalViews: number;
        totalSessions: number;
    };
}

export interface DotAnalyticsDashboardState {
    data: AnalyticsDashboardData | null;
    status: ComponentStatus;
    timeRange: 'day' | 'week' | 'month';
}

const initialState: DotAnalyticsDashboardState = {
    data: null,
    status: ComponentStatus.INIT,
    timeRange: 'day'
};

export const DotAnalyticsDashboardStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withComputed(({ data, status }) => ({
        isLoading: computed(() => status() === ComponentStatus.LOADING),
        isLoaded: computed(() => status() === ComponentStatus.LOADED),
        hasData: computed(() => !!data())
    })),
    withMethods((store) => {
        const analyticsService = inject(DotAnalyticsService);
        const dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

        return {
            setTimeRange: (timeRange: 'day' | 'week' | 'month') => {
                patchState(store, { timeRange });
            },

            loadDashboardData: rxMethod<string>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap((pageId) =>
                        analyticsService.getDashboardData(pageId, store.timeRange()).pipe(
                            tapResponse(
                                (data: AnalyticsDashboardData) => {
                                    patchState(store, {
                                        data,
                                        status: ComponentStatus.LOADED
                                    });
                                },
                                (error: HttpErrorResponse) => {
                                    patchState(store, { status: ComponentStatus.ERROR });
                                    dotHttpErrorManagerService.handle(error);
                                }
                            )
                        )
                    )
                )
            )
        };
    })
);

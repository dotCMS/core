import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@openng/spectator/jest';
import { of, Subject } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAnalyticsDashboardStore } from './dot-analytics-dashboard.store';

import { DotAnalyticsService } from '../services/dot-analytics.service';
import { SessionEngagementByDayData } from '../types';

describe('DotAnalyticsDashboardStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAnalyticsDashboardStore>>;
    let store: InstanceType<typeof DotAnalyticsDashboardStore>;
    let analyticsService: SpyObject<DotAnalyticsService>;
    let globalStore: SpyObject<InstanceType<typeof GlobalStore>>;

    const createService = createServiceFactory({
        service: DotAnalyticsDashboardStore,
        providers: [
            mockProvider(DotAnalyticsService),
            mockProvider(GlobalStore, {
                currentSiteId: jest.fn().mockReturnValue(''),
                addNewBreadcrumb: jest.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { queryParams: {} } }
            },
            mockProvider(Router)
        ]
    });

    describe('$isReportLoading', () => {
        // Regression test for the case flagged in PR review: when there is no current site,
        // loadEngagementData()/loadAllPageviewData()/loadConversionsData() all return without
        // firing any request, leaving their slices at ComponentStatus.INIT forever — nothing will
        // ever resolve that status. $isReportLoading must not treat that as "still loading", or
        // the full-report overlay would stay up indefinitely with no site selected.
        it('should stay false when there is no current site, since nothing was asked to load', () => {
            spectator = createService();
            store = spectator.service;
            spectator.flushEffects();

            expect(store.$isReportLoading()).toBe(false);
        });

        it("should be true while the active tab's requests are in flight, and false once settled", () => {
            const engagement$ = new Subject<SessionEngagementByDayData[]>();

            spectator = createService();
            store = spectator.service;
            globalStore = spectator.inject(GlobalStore);
            analyticsService = spectator.inject(DotAnalyticsService);

            (globalStore.currentSiteId as jest.Mock).mockReturnValue('site-1');
            analyticsService.getSessionEngagement.mockReturnValue(engagement$);
            analyticsService.getSessionEngagementGroupBy.mockReturnValue(of([]));

            // Default landing tab is 'engagement' — flushing effects fires loadEngagementData().
            spectator.flushEffects();

            expect(store.$isReportLoading()).toBe(true);

            engagement$.next([
                {
                    day: '2026-01-01',
                    avgEngagedSessionTimeSeconds: 0,
                    avgInteractionsPerEngagedSession: 0,
                    avgSessionTimeSeconds: 0,
                    conversionRate: 0,
                    engagedConversionSessions: 0,
                    engagedSessions: 0,
                    engagementRate: 0,
                    totalSessions: 0
                }
            ]);
            engagement$.complete();

            expect(store.$isReportLoading()).toBe(false);
        });
    });
});

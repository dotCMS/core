import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { BehaviorSubject, Subject } from 'rxjs';

import { take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';

import { DotCDNStats } from './dot-cdn.models';
import { DotCDNService, StatsRequest } from './dot-cdn.service';
import { DotCDNStore } from './dot-cdn.store';

const stats = (cdnDomain: string, chart: { [key: string]: number } = {}): DotCDNStats => ({
    stats: {
        bandwidthPretty: '0 B',
        bandwidthUsedChart: chart,
        requestsServedChart: chart,
        cacheHitRateChart: {},
        originResponseTimeChart: {},
        error4xxChart: {},
        error5xxChart: {},
        cacheHitRate: 0,
        averageOriginResponseTime: 0,
        dateFrom: '',
        dateTo: '',
        geographicDistribution: null,
        totalBandwidthUsed: 0,
        totalRequestsServed: 0,
        cdnDomain
    }
});

describe('DotCDNStore', () => {
    let spectator: SpectatorService<DotCDNStore>;
    let site$: BehaviorSubject<{ identifier: string }>;
    let responses: { request: StatsRequest; response: Subject<DotCDNStats> }[];

    const createService = createServiceFactory({
        service: DotCDNStore,
        providers: [
            mockProvider(DotHttpErrorManagerService),
            {
                provide: SiteService,
                useFactory: () => ({ getCurrentSite: () => site$ })
            },
            {
                provide: DotCDNService,
                useFactory: () => ({
                    requestStats: (request: StatsRequest) => {
                        const response = new Subject<DotCDNStats>();
                        responses.push({ request, response });

                        return response.pipe(take(1));
                    }
                })
            }
        ]
    });

    const state = () => {
        let value: { isChartLoading: boolean; cdnDomain: string } | undefined;
        spectator.service.vm$.pipe(take(1)).subscribe((vm) => (value = vm));

        return value;
    };

    beforeEach(() => {
        site$ = new BehaviorSubject({ identifier: 'site-1' });
        responses = [];
        spectator = createService();
    });

    it('clears the loading flag once the stats response arrives', () => {
        expect(state()?.isChartLoading).toBe(true);

        responses[0].response.next(stats('one.example'));

        expect(state()?.isChartLoading).toBe(false);
        expect(state()?.cdnDomain).toBe('one.example');
    });

    it('ignores an older response that arrives after a newer filter', () => {
        spectator.service.loadStats({ dateFrom: '2026-11-01', dateTo: '2026-11-02', hourly: true });

        responses[0].response.next(stats('stale.example'));
        expect(state()?.isChartLoading).toBe(true);
        expect(state()?.cdnDomain).toBe('');

        responses[1].response.next(stats('fresh.example'));
        expect(state()?.cdnDomain).toBe('fresh.example');
        expect(state()?.isChartLoading).toBe(false);
    });

    it('reloads the stats for the new site when the selected site changes', () => {
        responses[0].response.next(stats('one.example'));

        site$.next({ identifier: 'site-2' });

        expect(responses).toHaveLength(2);
        expect(responses[1].request.hostId).toBe('site-2');
        expect(state()?.isChartLoading).toBe(true);
    });

    it('labels daily buckets by their UTC date', () => {
        responses[0].response.next(stats('one.example', { '2026-11-01T00:00:00Z': 1 }));

        let labels: string[] = [];
        spectator.service.vm$
            .pipe(take(1))
            .subscribe((vm) => (labels = vm.chartBandwidthData.labels));

        expect(labels).toEqual(['Nov 1']);
    });
});

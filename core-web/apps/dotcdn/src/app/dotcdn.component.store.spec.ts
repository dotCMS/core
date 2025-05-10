import { describe, expect, it, jest } from '@jest/globals';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { SpiedClass } from 'jest-mock';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { distinctUntilChanged, map, take, toArray } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { mockResponseView } from '@dotcms/utils-testing';

import { ChartPeriod, DotCDNStats, Loader } from './app.models';
import { DotCDNStore } from './dotcdn.component.store';
import { DotCDNService } from './dotcdn.service';

const fakeResponseData: DotCDNStats = {
    stats: {
        bandwidthPretty: '114.42 MB',
        cdnDomain: 'demo.dotcms.com',
        bandwidthUsedChart: {
            '2021-04-15T15:00:00-05:00': 78554075,
            '2021-04-16T15:00:00-05:00': 15506849,
            '2021-04-17T15:00:00-05:00': 0,
            '2021-04-18T15:00:00-05:00': 0,
            '2021-04-19T15:00:00-05:00': 15301470,
            '2021-04-20T15:00:00-05:00': 5061682,
            '2021-04-21T15:00:00-05:00': 0,
            '2021-04-22T15:00:00-05:00': 0,
            '2021-04-23T15:00:00-05:00': 0
        },
        requestsServedChart: {
            '2021-04-15T15:00:00-05:00': 78554075,
            '2021-04-16T15:00:00-05:00': 15506849,
            '2021-04-17T15:00:00-05:00': 0,
            '2021-04-18T15:00:00-05:00': 0,
            '2021-04-19T15:00:00-05:00': 15301470,
            '2021-04-20T15:00:00-05:00': 5061682,
            '2021-04-21T15:00:00-05:00': 0,
            '2021-04-22T15:00:00-05:00': 0,
            '2021-04-23T15:00:00-05:00': 0
        },
        cacheHitRate: 14.615384615384617,
        dateFrom: '2021-03-23T15:00:00-05:00',
        dateTo: '2021-04-22T15:00:00-05:00',
        geographicDistribution: {
            NA: {
                ' Miami, FL': 114424076
            }
        },
        totalBandwidthUsed: 114424076,
        totalRequestsServed: 130
    }
};

const fakeStateViewModel = {
    chartBandwidthData: {
        labels: ['15/04', '16/04', '17/04', '18/04', '19/04', '20/04', '21/04', '22/04', '23/04'],
        datasets: [
            {
                label: 'Bandwidth Used',
                data: ['78.55', '15.51', '0.00', '0.00', '15.30', '5.06', '0.00', '0.00', '0.00'],
                borderColor: '#6f5fa3',
                fill: false
            }
        ]
    },
    chartRequestsData: {
        labels: ['15/04', '16/04', '17/04', '18/04', '19/04', '20/04', '21/04', '22/04', '23/04'],
        datasets: [
            {
                label: 'Requests Served',
                data: ['78554075', '15506849', '0', '0', '15301470', '5061682', '0', '0', '0'],
                borderColor: '#FFA726',
                fill: false
            }
        ]
    },
    statsData: [
        {
            label: 'Bandwidth Used',
            value: '114.42 MB',
            icon: 'insert_chart_outlined'
        },
        {
            label: 'Requests Served',
            value: '130',
            icon: 'file_download'
        },
        {
            label: 'Cache Hit Rate',
            value: '14.62%',
            icon: 'file_download'
        }
    ],
    isChartLoading: false,
    cdnDomain: 'demo.dotcms.com'
};

describe('DotCDNComponentStore', () => {
    let spectator: SpectatorService<DotCDNStore>;
    let store: DotCDNStore;
    let dotCDNService: SpyObject<DotCDNService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;
    let dotMessageService: SpyObject<DotMessageService>;
    let dateSpy: SpiedClass<DateConstructor>;
    const PrevDateConstructor: DateConstructor = Date;

    const createStoreService = createServiceFactory({
        service: DotCDNStore,
        providers: [
            mockProvider(DotCDNService, {
                requestStats: jest.fn(() => of(fakeResponseData))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotMessageService)
        ]
    });

    beforeEach(() => {
        spectator = createStoreService();
        store = spectator.service;
        dotCDNService = spectator.inject(DotCDNService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        dotMessageService = spectator.inject(DotMessageService);
        expect(dotMessageService.init).toBeCalled();
        expect(dotCDNService.requestStats).toHaveBeenCalledWith(ChartPeriod.Last15Days);

        dateSpy = jest.spyOn(global, 'Date').mockImplementation(function (
            this: Date,
            ...args: ConstructorParameters<typeof Date>
        ) {
            if (args.length === 1 && typeof args[0] === 'string') {
                const dateString = args[0];
                if (dateString.indexOf('T') >= 0) {
                    return new PrevDateConstructor(dateString);
                } else {
                    return new PrevDateConstructor(dateString + 'T00:00:00');
                }
            }

            return new PrevDateConstructor(...args);
        });
    });

    afterEach(() => {
        dateSpy.mockRestore();
    });

    // Check if the loading state is set correctly when purging cache or getting stats
    // and that the loading state is reset after the operation is completed
    function checkLoadingState(loaderType: Loader) {
        const storeLoadingState$ =
            loaderType == Loader.CHART
                ? store.vm$.pipe(map((state) => state.isChartLoading))
                : loaderType == Loader.PURGE_URLS
                  ? store.vmPurgeLoaders$.pipe(map((state) => state.isPurgeUrlsLoading))
                  : store.vmPurgeLoaders$.pipe(map((state) => state.isPurgeZoneLoading));
        storeLoadingState$
            .pipe(distinctUntilChanged(), take(3), toArray())
            .subscribe((loadingStates) => {
                expect(loadingStates).toEqual([false, true, false]);
            });
    }

    describe('DotCDN Component Store', () => {
        it('should set chart state', (done) => {
            checkLoadingState(Loader.CHART);

            const testPeriod = '30';
            spectator.service.getChartStats(of(testPeriod));

            store.vm$.subscribe((state) => {
                expect(dotCDNService.requestStats).toHaveBeenCalledWith(testPeriod);
                expect(state).toEqual(fakeStateViewModel);
                done();
            });
        });

        it('should purge cdn with urls', (done) => {
            const urls = ['url1, url2'];

            dotCDNService.purgeCache.mockReturnValue(
                of({
                    entity: {
                        'All Urls Sent Purged: ': true
                    },
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    permissions: []
                })
            );

            checkLoadingState(Loader.PURGE_URLS);

            spectator.service.purgeCDNCache(urls).subscribe(() => {
                expect(dotCDNService.purgeCache).toHaveBeenCalledWith(urls);
                done();
            });
        });

        it('should purge all the cache', (done) => {
            dotCDNService.purgeCacheAll.mockReturnValue(
                of({
                    entity: {
                        'Entire Cache Purged: ': true
                    },
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    permissions: []
                })
            );

            checkLoadingState(Loader.PURGE_PULL_ZONE);

            spectator.service.purgeCDNCacheAll();

            expect(dotCDNService.purgeCacheAll).toHaveBeenCalled();
            done();
        });

        it('should handle error when getting stats', (done) => {
            const error500 = mockResponseView(500);
            dotCDNService.requestStats.mockReturnValue(throwError(error500));

            checkLoadingState(Loader.CHART);

            spectator.service.getChartStats('30');

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                error500 as HttpErrorResponse
            );
            done();
        });

        it('should handle error when purging urls from cache', (done) => {
            const error500 = mockResponseView(500);
            dotCDNService.purgeCache.mockReturnValue(throwError(error500));

            checkLoadingState(Loader.PURGE_URLS);

            spectator.service.purgeCDNCache(['url1']);

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                error500 as HttpErrorResponse
            );
            done();
        });

        it('should handle error when purging all cache', (done) => {
            const error500 = mockResponseView(500);
            dotCDNService.purgeCacheAll.mockReturnValue(throwError(error500));

            checkLoadingState(Loader.PURGE_PULL_ZONE);

            spectator.service.purgeCDNCacheAll();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                error500 as HttpErrorResponse
            );
            done();
        });
    });
});

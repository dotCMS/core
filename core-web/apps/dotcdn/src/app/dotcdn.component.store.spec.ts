import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SiteService } from '@dotcms/dotcms-js';
import { SiteServiceMock } from '@dotcms/utils-testing';

import { DotCDNStats } from './app.models';
import { DotCDNStore } from './dotcdn.component.store';
import { DotCDNService } from './dotcdn.service';

const fakeResponseData: DotCDNStats = {
    stats: {
        bandwidthPretty: '114.42 MB',
        cdnDomain: 'demo.dotcms.com',
        bandwidthUsedChart: {
            '2021-04-15T00:00:00Z': 78554075,
            '2021-04-16T00:00:00Z': 15506849,
            '2021-04-17T00:00:00Z': 0,
            '2021-04-18T00:00:00Z': 0,
            '2021-04-19T00:00:00Z': 15301470,
            '2021-04-20T00:00:00Z': 5061682,
            '2021-04-21T00:00:00Z': 0,
            '2021-04-22T00:00:00Z': 0,
            '2021-04-23T00:00:00Z': 0
        },
        requestsServedChart: {
            '2021-04-15T00:00:00Z': 78554075,
            '2021-04-16T00:00:00Z': 15506849,
            '2021-04-17T00:00:00Z': 0,
            '2021-04-18T00:00:00Z': 0,
            '2021-04-19T00:00:00Z': 15301470,
            '2021-04-20T00:00:00Z': 5061682,
            '2021-04-21T00:00:00Z': 0,
            '2021-04-22T00:00:00Z': 0,
            '2021-04-23T00:00:00Z': 0
        },
        cacheHitRate: 14.615384615384617,
        dateFrom: '2021-03-23T23:00:00Z',
        dateTo: '2021-04-22T22:00:00Z',
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
        labels: ['14/04', '15/04', '16/04', '17/04', '18/04', '19/04', '20/04', '21/04', '22/04'],
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
        labels: ['14/04', '15/04', '16/04', '17/04', '18/04', '19/04', '20/04', '21/04', '22/04'],
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
    let store: DotCDNStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotCDNStore,
                { provide: SiteService, useClass: SiteServiceMock },
                {
                    provide: DotCDNService,
                    useValue: {
                        requestStats() {
                            return of(fakeResponseData);
                        },

                        purgeCacheAll() {
                            return of({
                                entity: {
                                    'Entire Cache Purged: ': true
                                },
                                errors: [],
                                i18nMessagesMap: {},
                                messages: [],
                                permissions: []
                            });
                        },

                        purgeCache() {
                            return of({
                                entity: {
                                    'All Urls Sent Purged: ': true
                                },
                                errors: [],
                                i18nMessagesMap: {},
                                messages: [],
                                permissions: []
                            });
                        }
                    }
                }
            ]
        });
        store = TestBed.inject(DotCDNStore);
    });

    describe('DotCDN Component Store', () => {
        beforeEach(() => {
            jest.restoreAllMocks();
            jest.clearAllMocks();
        });

        xit('should set chart state', (done) => {
            store.vm$.subscribe((state) => {
                expect(state).toStrictEqual(fakeStateViewModel);
                done();
            });

            store.getChartStats('30');
        });

        it('should purge cdn with urls', (done) => {
            const urls = ['url1, url2'];

            store.state$.subscribe((state) => {
                expect(state.isPurgeUrlsLoading).toBe(false);
            });

            store.purgeCDNCache(urls);

            store.state$.subscribe((state) => {
                expect(state.isPurgeUrlsLoading).toBe(true);
                done();
            });
        });

        it('should purge all the cache', (done) => {
            store.state$.subscribe((state) => {
                expect(state.isPurgeUrlsLoading).toBe(true);
            });

            store.purgeCDNCacheAll();

            store.state$.subscribe((state) => {
                expect(state.isPurgeUrlsLoading).toBe(false);
                done();
            });
        });
    });
});

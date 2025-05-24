import { describe, expect, it, jest } from '@jest/globals';
import {
    createHttpFactory,
    mockProvider,
    HttpMethod,
    SpectatorHttp,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { take } from 'rxjs/operators';

import { DotSiteService } from '@dotcms/data-access';

import { DotCDNService } from './dotcdn.service';

const fakeDotCDNViewData = {
    resp: {
        headers: {
            normalizedNames: {},
            lazyUpdate: null
        },
        status: 200,
        statusText: 'OK',
        url: 'http://localhost:4200/api/v1/dotcdn/stats?hostId=48190c8c-42c4-46af-8d1a-0cd5db894797&dateFrom=2021-04-16&dateTo=2021-05-01',
        ok: true,
        type: 4,
        body: {
            entity: {
                stats: {
                    bandwidthPretty: '238.85 MB',
                    bandwidthUsedChart: {
                        '2021-04-16T00:00:00Z': 15506849,
                        '2021-04-17T00:00:00Z': 0,
                        '2021-04-18T00:00:00Z': 0,
                        '2021-04-19T00:00:00Z': 15301470,
                        '2021-04-20T00:00:00Z': 5061682,
                        '2021-04-21T00:00:00Z': 0,
                        '2021-04-22T00:00:00Z': 0,
                        '2021-04-23T00:00:00Z': 0,
                        '2021-04-24T00:00:00Z': 0,
                        '2021-04-25T00:00:00Z': 0,
                        '2021-04-26T00:00:00Z': 0,
                        '2021-04-27T00:00:00Z': 0,
                        '2021-04-28T00:00:00Z': 110186951,
                        '2021-04-29T00:00:00Z': 92793786,
                        '2021-04-30T00:00:00Z': 0,
                        '2021-05-01T00:00:00Z': 0
                    },
                    cacheHitRate: 64.58333333333334,
                    cdnDomain: 'https://erick-demo.b-cdn.net',
                    dateFrom: '2021-04-15T22:00:00Z',
                    dateTo: '2021-04-30T22:00:00Z',
                    geographicDistribution: {
                        NA: {
                            ' Miami, FL': 238850738
                        }
                    },
                    requestsServedChart: {
                        '2021-04-16T00:00:00Z': 3,
                        '2021-04-17T00:00:00Z': 0,
                        '2021-04-18T00:00:00Z': 0,
                        '2021-04-19T00:00:00Z': 6,
                        '2021-04-20T00:00:00Z': 3,
                        '2021-04-21T00:00:00Z': 0,
                        '2021-04-22T00:00:00Z': 0,
                        '2021-04-23T00:00:00Z': 0,
                        '2021-04-24T00:00:00Z': 0,
                        '2021-04-25T00:00:00Z': 0,
                        '2021-04-26T00:00:00Z': 0,
                        '2021-04-27T00:00:00Z': 0,
                        '2021-04-28T00:00:00Z': 19,
                        '2021-04-29T00:00:00Z': 17,
                        '2021-04-30T00:00:00Z': 0,
                        '2021-05-01T00:00:00Z': 0
                    },
                    totalBandwidthUsed: 238850738,
                    totalRequestsServed: 48
                }
            },
            errors: [],
            i18nMessagesMap: {},
            messages: [],
            permissions: []
        }
    },
    bodyJsonObject: {
        entity: {
            stats: {
                bandwidthPretty: '238.85 MB',
                bandwidthUsedChart: {
                    '2021-04-16T00:00:00Z': 15506849,
                    '2021-04-17T00:00:00Z': 0,
                    '2021-04-18T00:00:00Z': 0,
                    '2021-04-19T00:00:00Z': 15301470,
                    '2021-04-20T00:00:00Z': 5061682,
                    '2021-04-21T00:00:00Z': 0,
                    '2021-04-22T00:00:00Z': 0,
                    '2021-04-23T00:00:00Z': 0,
                    '2021-04-24T00:00:00Z': 0,
                    '2021-04-25T00:00:00Z': 0,
                    '2021-04-26T00:00:00Z': 0,
                    '2021-04-27T00:00:00Z': 0,
                    '2021-04-28T00:00:00Z': 110186951,
                    '2021-04-29T00:00:00Z': 92793786,
                    '2021-04-30T00:00:00Z': 0,
                    '2021-05-01T00:00:00Z': 0
                },
                cacheHitRate: 64.58333333333334,
                cdnDomain: 'https://erick-demo.b-cdn.net',
                dateFrom: '2021-04-15T22:00:00Z',
                dateTo: '2021-04-30T22:00:00Z',
                geographicDistribution: {
                    NA: {
                        ' Miami, FL': 238850738
                    }
                },
                requestsServedChart: {
                    '2021-04-16T00:00:00Z': 3,
                    '2021-04-17T00:00:00Z': 0,
                    '2021-04-18T00:00:00Z': 0,
                    '2021-04-19T00:00:00Z': 6,
                    '2021-04-20T00:00:00Z': 3,
                    '2021-04-21T00:00:00Z': 0,
                    '2021-04-22T00:00:00Z': 0,
                    '2021-04-23T00:00:00Z': 0,
                    '2021-04-24T00:00:00Z': 0,
                    '2021-04-25T00:00:00Z': 0,
                    '2021-04-26T00:00:00Z': 0,
                    '2021-04-27T00:00:00Z': 0,
                    '2021-04-28T00:00:00Z': 19,
                    '2021-04-29T00:00:00Z': 17,
                    '2021-04-30T00:00:00Z': 0,
                    '2021-05-01T00:00:00Z': 0
                },
                totalBandwidthUsed: 238850738,
                totalRequestsServed: 48
            }
        },
        errors: [],
        i18nMessagesMap: {},
        messages: [],
        permissions: []
    },
    headers: {
        normalizedNames: {},
        lazyUpdate: null
    }
};

const fakePurgeUrlsResp = {
    resp: {
        headers: {
            normalizedNames: {},
            lazyUpdate: null
        },
        status: 200,
        statusText: 'OK',
        url: 'http://localhost:4200/api/v1/dotcdn',
        ok: true,
        type: 4,
        body: {
            entity: {
                'All Urls Sent Purged: ': true
            },
            errors: [],
            i18nMessagesMap: {},
            messages: [],
            permissions: []
        }
    },
    bodyJsonObject: {
        entity: {
            'All Urls Sent Purged: ': true
        },
        errors: [],
        i18nMessagesMap: {},
        messages: [],
        permissions: []
    },
    headers: {
        normalizedNames: {},
        lazyUpdate: null
    }
};

const fakePurgeAllResp = {
    resp: {
        headers: {
            normalizedNames: {},
            lazyUpdate: null
        },
        status: 200,
        statusText: 'OK',
        url: 'http://localhost:4200/api/v1/dotcdn',
        ok: true,
        type: 4,
        body: {
            entity: {
                'Entire Cache Purged: ': true
            },
            errors: [],
            i18nMessagesMap: {},
            messages: [],
            permissions: []
        }
    },
    bodyJsonObject: {
        entity: {
            'Entire Cache Purged: ': true
        },
        errors: [],
        i18nMessagesMap: {},
        messages: [],
        permissions: []
    },
    headers: {
        normalizedNames: {},
        lazyUpdate: null
    }
};

describe('DotcdnService', () => {
    let spectator: SpectatorHttp<DotCDNService>;
    let dotSiteService: SpyObject<DotSiteService>;

    const createService = createHttpFactory({
        service: DotCDNService,
        providers: [mockProvider(DotSiteService)]
    });

    beforeEach(() => {
        spectator = createService();
        dotSiteService = spectator.inject(DotSiteService);
        dotSiteService.getCurrentSite.mockReturnValue(
            of({
                identifier: '123-xyz-567-xxl',
                hostname: 'test.dotcms.com',
                type: 'primary',
                archived: false
            })
        );

        jest.useFakeTimers();
        jest.setSystemTime(new Date('2021-05-03T15:00:00-05:00'));
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    // Skipping because the `useFakeTimers` and `setSystemTime` is not pickinh up win GHA
    it('should return the stats', (done) => {
        const {
            bodyJsonObject: { entity }
        } = fakeDotCDNViewData;

        spectator.service
            .requestStats('30')
            .pipe(take(1))
            .subscribe((value) => {
                expect(value).toStrictEqual(entity);
                done();
            });

        const req = spectator.expectOne(
            '/api/v1/dotcdn/stats?hostId=123-xyz-567-xxl&dateFrom=2021-04-03&dateTo=2021-05-03',
            HttpMethod.GET
        );
        req.flush({ entity });
    });

    it('should purge cache with URLs', (done) => {
        spectator.service
            .purgeCache(['url1', 'url2'])
            .pipe(take(1))
            .subscribe((value) => {
                expect(value).toStrictEqual({
                    entity: {
                        'All Urls Sent Purged: ': true
                    },
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    permissions: []
                });
                done();
            });

        const req = spectator.expectOne('/api/v1/dotcdn', HttpMethod.DELETE);
        expect(req.request.body).toEqual({
            urls: ['url1', 'url2'],
            invalidateAll: false,
            hostId: '123-xyz-567-xxl'
        });
        req.flush(fakePurgeUrlsResp);
    });

    it('should purge all cache', (done) => {
        spectator.service
            .purgeCacheAll()
            .pipe(take(1))
            .subscribe((value) => {
                expect(value).toStrictEqual({
                    entity: { 'Entire Cache Purged: ': true },
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    permissions: []
                });
                done();
            });

        const req = spectator.expectOne('/api/v1/dotcdn', HttpMethod.DELETE);
        expect(req.request.body).toEqual({
            urls: [],
            invalidateAll: true,
            hostId: '123-xyz-567-xxl'
        });
        req.flush(fakePurgeAllResp);
    });
});

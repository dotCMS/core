import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@ngneat/spectator/jest';

import { SiteService } from '@dotcms/dotcms-js';

import { DotCDNService } from './dotcdn.service';

const MOCK_SITE_IDENTIFIER = '123-xyz-567-xxl';

const fakeDotCDNStats = {
    stats: {
        bandwidthPretty: '238.85 MB',
        bandwidthUsedChart: {
            '2021-04-16T00:00:00Z': 15506849,
            '2021-04-17T00:00:00Z': 0,
            '2021-04-18T00:00:00Z': 0,
            '2021-04-19T00:00:00Z': 15301470,
            '2021-04-20T00:00:00Z': 5061682
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
            '2021-04-20T00:00:00Z': 3
        },
        totalBandwidthUsed: 238850738,
        totalRequestsServed: 48
    }
};

const fakePurgeUrlsResponse = {
    entity: {
        'All Urls Sent Purged: ': true
    },
    errors: [],
    i18nMessagesMap: {},
    messages: [],
    permissions: []
};

const fakePurgeAllResponse = {
    entity: {
        'Entire Cache Purged: ': true
    },
    errors: [],
    i18nMessagesMap: {},
    messages: [],
    permissions: []
};

describe('DotCDNService', () => {
    let spectator: SpectatorHttp<DotCDNService>;
    let siteService: SpyObject<SiteService>;

    const createHttp = createHttpFactory({
        service: DotCDNService,
        providers: [mockProvider(SiteService)]
    });

    beforeEach(() => {
        spectator = createHttp();
        siteService = spectator.inject(SiteService);

        jest.useFakeTimers();
        jest.setSystemTime(new Date('2021-05-03'));
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('requestStats', () => {
        beforeEach(() => {
            const { of } = jest.requireActual('rxjs');
            siteService.getCurrentSite.mockReturnValue(of({ identifier: MOCK_SITE_IDENTIFIER }));
        });

        it('should return the stats', () => {
            spectator.service.requestStats('30').subscribe((value) => {
                expect(value).toStrictEqual(fakeDotCDNStats);
            });

            const req = spectator.expectOne(
                `/api/v1/dotcdn/stats?hostId=${MOCK_SITE_IDENTIFIER}&dateFrom=2021-04-02&dateTo=2021-05-02`,
                HttpMethod.GET
            );

            req.flush({ entity: fakeDotCDNStats });
        });
    });

    describe('purgeCache', () => {
        beforeEach(() => {
            const { of } = jest.requireActual('rxjs');
            siteService.getCurrentSite.mockReturnValue(of({ identifier: MOCK_SITE_IDENTIFIER }));
        });

        it('should purge cache with URLs', () => {
            spectator.service.purgeCache(['url1', 'url2']).subscribe((value) => {
                expect(value).toStrictEqual(fakePurgeUrlsResponse);
            });

            const req = spectator.expectOne('/api/v1/dotcdn', HttpMethod.DELETE);
            expect(req.request.body).toBe(
                `{"urls":["url1","url2"],"invalidateAll":false,"hostId":"${MOCK_SITE_IDENTIFIER}"}`
            );

            req.flush({ bodyJsonObject: fakePurgeUrlsResponse });
        });

        it('should purge cache with empty URLs array when no URLs provided', () => {
            spectator.service.purgeCache([]).subscribe((value) => {
                expect(value).toStrictEqual(fakePurgeUrlsResponse);
            });

            const req = spectator.expectOne('/api/v1/dotcdn', HttpMethod.DELETE);
            expect(req.request.body).toBe(
                `{"urls":[],"invalidateAll":false,"hostId":"${MOCK_SITE_IDENTIFIER}"}`
            );

            req.flush({ bodyJsonObject: fakePurgeUrlsResponse });
        });
    });

    describe('purgeCacheAll', () => {
        beforeEach(() => {
            const { of } = jest.requireActual('rxjs');
            siteService.getCurrentSite.mockReturnValue(of({ identifier: MOCK_SITE_IDENTIFIER }));
        });

        it('should purge all cache', () => {
            spectator.service.purgeCacheAll().subscribe((value) => {
                expect(value).toStrictEqual(fakePurgeAllResponse);
            });

            const req = spectator.expectOne('/api/v1/dotcdn', HttpMethod.DELETE);
            expect(req.request.body).toBe(
                `{"urls":[],"invalidateAll":true,"hostId":"${MOCK_SITE_IDENTIFIER}"}`
            );

            req.flush({ bodyJsonObject: fakePurgeAllResponse });
        });
    });
});

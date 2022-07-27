import { TestBed } from '@angular/core/testing';
import { CoreWebService, SiteService } from '@dotcms/dotcms-js';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DotCDNService } from './dotcdn.service';
import { SiteServiceMock, CoreWebServiceMock } from '@dotcms/dotcms-js';
import MockDate from 'mockdate';

const fakeDotCDNViewData = {
    resp: {
        headers: {
            normalizedNames: {},
            lazyUpdate: null
        },
        status: 200,
        statusText: 'OK',
        url:
            'http://localhost:4200/api/v1/dotcdn/stats?hostId=48190c8c-42c4-46af-8d1a-0cd5db894797&dateFrom=2021-04-16&dateTo=2021-05-01',
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
    let service: DotCDNService;
    let dotSiteService: SiteService;
    let dotCoreWebService: CoreWebService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: SiteService, useClass: SiteServiceMock }
            ]
        });
        service = TestBed.inject(DotCDNService);
        dotSiteService = TestBed.inject(SiteService);
        dotCoreWebService = TestBed.inject(CoreWebService);
        httpMock = TestBed.inject(HttpTestingController);
        jest.spyOn(dotSiteService, 'getCurrentSite');
        jest.restoreAllMocks();
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('should return the stats', (done) => {
        jest.spyOn(dotCoreWebService, 'requestView');

        MockDate.set('2021-05-03');

        const {
            bodyJsonObject: { entity }
        } = fakeDotCDNViewData;

        service.requestStats('30').subscribe((value) => {
            expect(value).toStrictEqual(entity);
            done();
        });

        const req = httpMock.expectOne(
            `/api/v1/dotcdn/stats?hostId=123-xyz-567-xxl&dateFrom=2021-04-03&dateTo=2021-05-03`
        );

        req.flush({ entity });
    });

    it('should purge cache with URLs', (done) => {
        const requestViewSpy = jest.spyOn(dotCoreWebService, 'requestView');
        service.purgeCache(['url1, url2']).subscribe((value) => {
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
        const req = httpMock.expectOne('/api/v1/dotcdn');
        expect(req.request.method).toBe('DELETE');

        expect(requestViewSpy).toHaveBeenCalledWith({
            body: '{"urls":["url1, url2"],"invalidateAll":false,"hostId":"123-xyz-567-xxl"}',
            method: 'DELETE',
            url: '/api/v1/dotcdn'
        });

        req.flush(fakePurgeUrlsResp.bodyJsonObject);
    });

    it('should purge all cache', (done) => {
        const requestViewSpy = jest.spyOn(dotCoreWebService, 'requestView');
        service.purgeCacheAll().subscribe((value) => {
            expect(value).toStrictEqual({
                entity: { 'Entire Cache Purged: ': true },
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                permissions: []
            });
            done();
        });
        const req = httpMock.expectOne('/api/v1/dotcdn');
        expect(req.request.method).toBe('DELETE');

        expect(requestViewSpy).toHaveBeenCalledWith({
            body: '{"urls":[],"invalidateAll":true,"hostId":"123-xyz-567-xxl"}',
            method: 'DELETE',
            url: '/api/v1/dotcdn'
        });

        req.flush(fakePurgeAllResp.bodyJsonObject);
    });
});

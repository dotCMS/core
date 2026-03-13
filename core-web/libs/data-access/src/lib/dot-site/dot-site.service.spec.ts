import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotSiteService, SiteParams, BASE_SITE_URL } from './dot-site.service';

// Mock API response entities (as returned by the backend)
const mockSiteEntities = [
    {
        name: 'demo.dotcms.com',
        identifier: '123-xyz-567-xxl',
        aliases: null,
        archived: false,
        parentPath: '/'
    },
    {
        name: 'hello.dotcms.com',
        identifier: '456-xyz-789-xxl',
        aliases: null,
        archived: false,
        parentPath: '/en/'
    }
];

// Expected normalized sites (after service processes the API response)
const expectedNormalizedSites = [
    {
        hostname: 'demo.dotcms.com',
        identifier: '123-xyz-567-xxl',
        aliases: null,
        archived: false,
        parentPath: '/'
    },
    {
        hostname: 'hello.dotcms.com',
        identifier: '456-xyz-789-xxl',
        aliases: null,
        archived: false,
        parentPath: '/en/'
    }
];

describe('DotSiteService', () => {
    let spectator: SpectatorHttp<DotSiteService>;
    let service: DotSiteService;

    const createHttp = createHttpFactory(DotSiteService);

    beforeEach(() => {
        spectator = createHttp();
        service = spectator.service;
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getSites()', () => {
        it('should return a list of sites', (doneFn) => {
            service.getSites().subscribe(({ sites }) => {
                expect(sites.length).toBe(2);
                expect(sites).toEqual(expectedNormalizedSites);
                doneFn();
            });

            const url = `${BASE_SITE_URL}?per_page=10&page=1&filter=*&archive=false&live=true&system=true`;
            const req = spectator.expectOne(url, HttpMethod.GET);
            spectator.flushAll([req], [{ entity: mockSiteEntities }]);
        });

        it('should set the query params correctly', (doneFn) => {
            const searchParams: SiteParams = {
                archived: true,
                live: false,
                system: true
            };

            service.searchParam = searchParams;

            const url = `${BASE_SITE_URL}?per_page=15&page=1&filter=demo&archive=true&live=false&system=true`;

            service.getSites({ filter: 'demo', per_page: 15 }).subscribe(() => doneFn());

            const req = spectator.expectOne(url, HttpMethod.GET);
            spectator.flushAll([req], [{ entity: mockSiteEntities }]);
        });
    });

    describe('getCurrentSite()', () => {
        it('should return a list of sites', (doneFn) => {
            const expectedSite = expectedNormalizedSites[0];
            const mockSiteEntity = mockSiteEntities[0];

            service.getCurrentSite().subscribe((site) => {
                expect(site).toEqual(expectedSite);
                doneFn();
            });

            const url = `${BASE_SITE_URL}/currentSite`;
            const req = spectator.expectOne(url, HttpMethod.GET);
            spectator.flushAll([req], [{ entity: mockSiteEntity }]);
        });
    });

    describe('archiveSite()', () => {
        const siteId = '123-xyz-567-xxl';

        it('should call archive endpoint with cascade=true by default and return normalized site', (doneFn) => {
            const mockDetailEntity = {
                siteName: 'demo.dotcms.com',
                identifier: siteId,
                aliases: null,
                archived: true,
                parentPath: '/'
            };
            const expected = {
                hostname: 'demo.dotcms.com',
                identifier: siteId,
                aliases: null,
                archived: true,
                parentPath: '/'
            };

            service.archiveSite(siteId).subscribe((site) => {
                expect(site).toEqual(expected);
                doneFn();
            });

            const url = `${BASE_SITE_URL}/${siteId}/_archive?cascade=true`;
            const req = spectator.expectOne(url, HttpMethod.PUT);
            spectator.flushAll([req], [{ entity: mockDetailEntity }]);
        });

        it('should call archive endpoint without cascade when cascade=false', (doneFn) => {
            const mockDetailEntity = {
                siteName: 'demo.dotcms.com',
                identifier: siteId,
                aliases: null,
                archived: true,
                parentPath: '/'
            };

            service.archiveSite(siteId, false).subscribe(() => doneFn());

            const url = `${BASE_SITE_URL}/${siteId}/_archive`;
            const req = spectator.expectOne(url, HttpMethod.PUT);
            spectator.flushAll([req], [{ entity: mockDetailEntity }]);
        });

        it('should unwrap cascade response entity when cascade=true returns wrapper object', (doneFn) => {
            const mockDetailEntity = {
                siteName: 'demo.dotcms.com',
                identifier: siteId,
                aliases: null,
                archived: true,
                parentPath: '/'
            };
            const cascadeResponse = {
                entity: {
                    site: mockDetailEntity,
                    cascade: true,
                    descendantsArchived: 3
                }
            };

            service.archiveSite(siteId).subscribe((site) => {
                expect(site.hostname).toBe('demo.dotcms.com');
                expect(site.archived).toBe(true);
                doneFn();
            });

            const url = `${BASE_SITE_URL}/${siteId}/_archive?cascade=true`;
            const req = spectator.expectOne(url, HttpMethod.PUT);
            req.flush(cascadeResponse);
        });
    });

    describe('unarchiveSite()', () => {
        const siteId = '123-xyz-567-xxl';

        it('should call unarchive endpoint without cascade by default', (doneFn) => {
            const mockDetailEntity = {
                siteName: 'demo.dotcms.com',
                identifier: siteId,
                aliases: null,
                archived: false,
                parentPath: '/'
            };

            service.unarchiveSite(siteId).subscribe((site) => {
                expect(site.archived).toBe(false);
                doneFn();
            });

            const url = `${BASE_SITE_URL}/${siteId}/_unarchive`;
            const req = spectator.expectOne(url, HttpMethod.PUT);
            spectator.flushAll([req], [{ entity: mockDetailEntity }]);
        });

        it('should call unarchive endpoint with cascade=true when requested', (doneFn) => {
            const mockDetailEntity = {
                siteName: 'demo.dotcms.com',
                identifier: siteId,
                aliases: null,
                archived: false,
                parentPath: '/'
            };

            service.unarchiveSite(siteId, true).subscribe(() => doneFn());

            const url = `${BASE_SITE_URL}/${siteId}/_unarchive?cascade=true`;
            const req = spectator.expectOne(url, HttpMethod.PUT);
            spectator.flushAll([req], [{ entity: mockDetailEntity }]);
        });
    });

    describe('countSiteDescendants()', () => {
        it('should return the descendant count for a given site', (doneFn) => {
            const siteId = '123-xyz-567-xxl';

            service.countSiteDescendants(siteId).subscribe((count) => {
                expect(count).toBe(5);
                doneFn();
            });

            const url = `${BASE_SITE_URL}/${siteId}/descendants/count`;
            const req = spectator.expectOne(url, HttpMethod.GET);
            spectator.flushAll([req], [{ entity: 5 }]);
        });

        it('should return 0 when site has no descendants', (doneFn) => {
            const siteId = '456-xyz-789-xxl';

            service.countSiteDescendants(siteId).subscribe((count) => {
                expect(count).toBe(0);
                doneFn();
            });

            const url = `${BASE_SITE_URL}/${siteId}/descendants/count`;
            const req = spectator.expectOne(url, HttpMethod.GET);
            spectator.flushAll([req], [{ entity: 0 }]);
        });
    });
});

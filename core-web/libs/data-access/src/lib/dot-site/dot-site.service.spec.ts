import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotSiteService, SiteParams, BASE_SITE_URL } from './dot-site.service';

// Mock API response entities (as returned by the backend)
const mockSiteEntities = [
    {
        name: 'demo.dotcms.com',
        identifier: '123-xyz-567-xxl',
        aliases: null,
        archived: false
    },
    {
        name: 'hello.dotcms.com',
        identifier: '456-xyz-789-xxl',
        aliases: null,
        archived: false
    }
];

// Expected normalized sites (after service processes the API response)
const expectedNormalizedSites = [
    {
        hostname: 'demo.dotcms.com',
        identifier: '123-xyz-567-xxl',
        aliases: null,
        archived: false
    },
    {
        hostname: 'hello.dotcms.com',
        identifier: '456-xyz-789-xxl',
        aliases: null,
        archived: false
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
});

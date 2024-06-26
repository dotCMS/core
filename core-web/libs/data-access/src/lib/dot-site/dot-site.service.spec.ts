import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { mockSites } from '@dotcms/utils-testing';

import { DotSiteService, SiteParams, BASE_SITE_URL } from './dot-site.service';

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
            service.getSites().subscribe((sites) => {
                expect(sites.length).toBe(2);
                expect(sites).toEqual(mockSites);
                doneFn();
            });

            const url = `${BASE_SITE_URL}?filter=*&per_page=10&archived=false&live=true&system=true`;
            const req = spectator.expectOne(url, HttpMethod.GET);
            spectator.flushAll([req], [{ entity: mockSites }]);
        });

        it('should set the query params correctly', (doneFn) => {
            const searchParams: SiteParams = {
                archived: true,
                live: false,
                system: true
            };

            service.searchParam = searchParams;

            const url = `${BASE_SITE_URL}?filter=demo&per_page=15&archived=true&live=false&system=true`;

            service.getSites('demo', 15).subscribe(() => doneFn());

            const req = spectator.expectOne(url, HttpMethod.GET);
            spectator.flushAll([req], [{ entity: mockSites }]);
        });
    });

    describe('getCurrentSite()', () => {
        it('should return a list of sites', (doneFn) => {
            const mockSite = mockSites[0];

            service.getCurrentSite().subscribe((site) => {
                expect(site).toEqual(mockSite);
                doneFn();
            });

            const url = `${BASE_SITE_URL}/currentSite`;
            const req = spectator.expectOne(url, HttpMethod.GET);
            spectator.flushAll([req], [{ entity: mockSite }]);
        });
    });
});

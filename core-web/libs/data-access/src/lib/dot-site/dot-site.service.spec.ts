import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { mockSites } from '@dotcms/utils-testing';

import { DotSiteService, SiteParams } from './dot-site.service';

describe('DotSiteService', () => {
    let service: DotSiteService;
    let httpTestingController: HttpTestingController;

    const BASE_SITE_URL = '/api/v1/site';

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotSiteService]
        });

        service = TestBed.inject(DotSiteService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getSites()', () => {
        it('should return a list of sites', () => {
            // Call the getSites() method
            service.getSites().subscribe((sites) => {
                expect(sites.length).toBe(2);
                expect(sites).toEqual(mockSites);
            });

            const req = httpTestingController.expectOne(
                `${BASE_SITE_URL}?filter=*&perPage=10&archived=false&live=true&system=true`
            );
            expect(req.request.method).toBe('GET');
            req.flush({ entity: mockSites });
        });

        it('should set the query params correctly', () => {
            const searchParams: SiteParams = {
                archived: true,
                live: false,
                system: true
            };
            const queryParams = Object.keys(searchParams)
                .map((key: string) => `${key}=${searchParams[key as keyof SiteParams]}`)
                .join('&');

            service.searchParam = searchParams;

            // Call the getSites() method
            service.getSites('demo', 15).subscribe();

            const req = httpTestingController.expectOne(
                `${BASE_SITE_URL}?filter=demo&perPage=15&${queryParams}`
            );
            expect(req.request.method).toBe('GET');
            req.flush({ entity: mockSites });
        });
    });
});

import { DotPageSelectorService } from './dot-page-selector.service';
import {
    mockDotPageSelectorResults,
    mockDotSiteSelectorResults
} from '../dot-page-selector.component.spec';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

const MAX_RESULTS_SIZE = 20;

const emptyHostQuery = {
    query: {
        query_string: {
            query: `+contenttype:Host -identifier:SYSTEM_HOST +host.hostName:**`
        }
    },
    size: MAX_RESULTS_SIZE
};

const hostQuery = {
    query: {
        query_string: {
            query: `+contenttype:Host -identifier:SYSTEM_HOST +host.hostName:*demo.dotcms.com*`
        }
    }
};

const hostSpecificQuery = {
    query: {
        query_string: {
            query: `+contenttype:Host -identifier:SYSTEM_HOST +host.hostName:demo.dotcms.com`
        }
    }
};

export const mockEmptyHostDotSiteSelectorResults = Object.assign({}, mockDotSiteSelectorResults);
mockEmptyHostDotSiteSelectorResults.query = '';

describe('DotPageSelectorService', () => {
    let injector: TestBed;
    let dotPageSelectorService: DotPageSelectorService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPageSelectorService
            ]
        });
        injector = getTestBed();
        dotPageSelectorService = injector.get(DotPageSelectorService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get a page by identifier', () => {
        const searchParam = 'fdeb07ff-6fc3-4237-91d9-728109bc621d';
        const query = {
            query: {
                query_string: {
                    query: `+basetype:5 +identifier:*${searchParam}*`
                }
            }
        };

        dotPageSelectorService.getPageById(searchParam).subscribe((res: any) => {
            expect(res).toEqual({
                label: '//demo.dotcms.com/about-us',
                payload: mockDotPageSelectorResults.data[0].payload
            });
        });

        const req = httpMock.expectOne('es/search');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(query);
        req.flush({ contentlets: [mockDotPageSelectorResults.data[0].payload] });
    });

    it('should make page search', () => {
        dotPageSelectorService.setCurrentHost({
            hostname: 'random',
            type: 'n/a',
            identifier: '1',
            archived: false
        });

        dotPageSelectorService.search('about-us').subscribe((res: any) => {
            expect(res).toEqual(mockDotPageSelectorResults);
        });

        const req = httpMock.expectOne(
            `v1/page/search?path=about-us&onlyLiveSites=true&live=false`
        );
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [mockDotPageSelectorResults.data[0].payload] });
    });

    it('should make a host search', () => {
        dotPageSelectorService.search('//demo.dotcms.com').subscribe((res: any) => {
            expect(res).toEqual(mockDotSiteSelectorResults);
        });

        const req = httpMock.expectOne(`es/search`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(hostQuery);
        req.flush({ contentlets: [mockDotSiteSelectorResults.data[0].payload] });
    });

    it('should make a host search but limit to MAX_RESULTS_SIZE if string is empty', () => {
        dotPageSelectorService.search('//').subscribe((res: any) => {
            expect(res).toEqual(mockEmptyHostDotSiteSelectorResults);
        });

        const req = httpMock.expectOne(`es/search`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(emptyHostQuery);
        req.flush({ contentlets: [mockDotSiteSelectorResults.data[0].payload] });
    });

    it('should make host and page search (Full Search)', () => {
        dotPageSelectorService.search('//demo.dotcms.com/about-us').subscribe((res: any) => {
            expect(res).toEqual(mockDotPageSelectorResults);
        });

        const req = httpMock.expectOne(`es/search`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(hostSpecificQuery);
        req.flush({ contentlets: [mockDotSiteSelectorResults.data[0].payload] });

        const req2 = httpMock.expectOne(
            'v1/page/search?path=//demo.dotcms.com/about-us&onlyLiveSites=true&live=false'
        );
        expect(req2.request.method).toBe('GET');
        req2.flush({ entity: [mockDotPageSelectorResults.data[0].payload] });
    });

    it('should return empty results on Full Search if host is invalid', () => {
        dotPageSelectorService.search('//demo.dotcms.com/about-us').subscribe((res: any) => {
            expect(res).toEqual({
                data: [],
                query: 'demo.dotcms.com',
                type: 'site'
            });
        });

        const req = httpMock.expectOne(`es/search`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(hostSpecificQuery);
        req.flush({ contentlets: [] });
    });

    it('should return empty results when host is invalid', () => {
        dotPageSelectorService.search('//demo.dotcms.com').subscribe((res: any) => {
            expect(res).toEqual({
                data: [],
                query: 'demo.dotcms.com',
                type: 'site'
            });
        });

        const req = httpMock.expectOne(`es/search`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(hostQuery);
        req.flush({ contentlets: [] });
    });

    it('should return empty results when page is invalid', () => {
        const searchParam = 'invalidPage';
        dotPageSelectorService.search(searchParam).subscribe((res: any) => {
            expect(res).toEqual({
                data: [],
                query: 'invalidPage',
                type: 'page'
            });
        });

        const req = httpMock.expectOne(
            'v1/page/search?path=invalidPage&onlyLiveSites=true&live=false'
        );
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [] });
    });

    afterEach(() => {
        httpMock.verify();
    });
});

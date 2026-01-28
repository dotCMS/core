import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OrderDirection, PaginatorService } from './paginator.service';

describe('PaginatorService', () => {
    let paginatorService: PaginatorService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), PaginatorService]
        });
        paginatorService = TestBed.inject(PaginatorService);
        httpMock = TestBed.inject(HttpTestingController);
        paginatorService.url = '/api/v1/urldemo';
    });

    it('should do a request with basic params', () => {
        paginatorService.get().subscribe();
        const req = httpMock.expectOne(() => true);
        expect(req.request.method).toBe('GET');
        expect(req.request.url).toBe('/api/v1/urldemo');
    });

    it('should do a request with basic pagination params', () => {
        paginatorService.filter = 'test';
        paginatorService.sortField = 'name';
        paginatorService.sortOrder = OrderDirection.DESC;
        paginatorService.get().subscribe();
        httpMock.expectOne('/api/v1/urldemo?filter=test&orderby=name&direction=DESC&per_page=40');
    });

    it('should do a request with extra params', () => {
        paginatorService.setExtraParams('archive', 'false');
        paginatorService.setExtraParams('system', 'true');
        paginatorService.setExtraParams('live', null);
        paginatorService.get().subscribe();
        httpMock.expectOne('/api/v1/urldemo?per_page=40&archive=false&system=true');
    });

    it('should remove extra parameters', () => {
        paginatorService.setExtraParams('name', 'John');
        paginatorService.deleteExtraParams('name');

        expect(paginatorService.extraParams.get('name')).toBeUndefined();
    });

    it('should remove all extra parameters', () => {
        paginatorService.setExtraParams('name', 'John');
        paginatorService.setExtraParams('fullnam', 'John Doe');
        paginatorService.setExtraParams('age', '21');
        paginatorService.resetExtraParams();

        expect(paginatorService.extraParams.size).toBe(0);
    });

    afterEach(() => {
        httpMock.verify();
    });
});

describe('PaginatorService getting', () => {
    let fakeEntity: { items: { id: number; name: string }[]; totalRecords: number };
    let headerLink;
    let paginatorService: PaginatorService;
    let httpMock: HttpTestingController;
    let req;
    let result: unknown;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), PaginatorService]
        });
        paginatorService = TestBed.inject(PaginatorService);
        httpMock = TestBed.inject(HttpTestingController);
        paginatorService.url = '/api/v1/urldemo';

        headerLink = `/baseURL?filter=filter&page=1>;rel="first",
            /baseURL?filter=filter&page=5>;rel="last",
            /baseURL?filter=filter&page=pageValue>;rel="x-page",
            /baseURL?filter=filter&page=4>;rel="next",
            /baseURL?filter=filter&page=2>;rel="prev"`;

        fakeEntity = {
            items: [
                { id: 0, name: 'Name 0' },
                { id: 1, name: 'Name 1' },
                { id: 2, name: 'Name 2' },
                { id: 3, name: 'Name 3' }
            ],
            totalRecords: 5
        };

        let headers = new HttpHeaders();
        headers = headers.set('Link', headerLink);
        headers = headers.set('X-Pagination-Current-Page', '3');
        headers = headers.set('X-Pagination-Link-Pages', '5');
        headers = headers.set('X-Pagination-Per-Page', '10');
        headers = headers.set('X-Pagination-Total-Entries', '38');

        paginatorService.get().subscribe((items) => {
            result = items;
        });

        req = httpMock.expectOne(() => true);
        req.flush({ entity: fakeEntity }, { headers });
    });

    it('should get entity results', () => {
        expect(result).toEqual(fakeEntity);
    });

    it('links should has the right values', () => {
        expect(paginatorService.links.first).toEqual('baseURL?filter=filter&page=1');
        expect(paginatorService.links.last).toEqual('/baseURL?filter=filter&page=5');
        expect(paginatorService.links.next).toEqual('/baseURL?filter=filter&page=4');
        expect(paginatorService.links.prev).toEqual('/baseURL?filter=filter&page=2');
        expect(paginatorService.links['x-page']).toEqual('/baseURL?filter=filter&page=pageValue');
    });

    it('clean links after set any extra params', () => {
        paginatorService.setExtraParams('any_param', 'any_value');
        expect({}).toEqual(paginatorService.links);
    });

    it('should set basic pagination information', () => {
        expect(paginatorService.currentPage).toEqual(3);
        expect(paginatorService.maxLinksPage).toEqual(5);
        expect(paginatorService.totalRecords).toEqual(38);
        expect(paginatorService.paginationPerPage).toEqual(10);
    });

    it('should get first page', () => {
        paginatorService.getFirstPage().subscribe();
        req = httpMock.expectOne(() => true);
        expect(req.request.url).toBe('baseURL?filter=filter&page=1');
    });

    it('should get last page', () => {
        paginatorService.getLastPage().subscribe();
        req = httpMock.expectOne(() => true);
        expect(req.request.url).toBe('/baseURL?filter=filter&page=5');
    });

    it('should get next page', () => {
        paginatorService.getNextPage().subscribe();
        req = httpMock.expectOne(() => true);
        expect(req.request.url).toBe('/baseURL?filter=filter&page=4');
    });

    it('should get prev page', () => {
        paginatorService.getPrevPage().subscribe();
        req = httpMock.expectOne(() => true);
        expect(req.request.url).toBe('/baseURL?filter=filter&page=2');
    });

    it('should get page 6', () => {
        paginatorService.getPage(6).subscribe();
        req = httpMock.expectOne(() => true);
        expect(req.request.url).toBe('/baseURL?filter=filter&page=6');
    });

    it('should remove duplicated query params', () => {
        let headers = new HttpHeaders();
        paginatorService.url = '/api/v1/urldemo';
        headerLink = `/baseURL?filter=filter&page=1&system=true>;rel="first",
        /baseURL?filter=filter&page=2&system=true>;rel="prev"`;

        headers = headers.set('Link', headerLink);
        paginatorService.setExtraParams('system', true);
        paginatorService.get().subscribe();

        // Set new headers
        req = httpMock.expectOne(() => true);
        req.flush({}, { headers });

        paginatorService.getPrevPage().subscribe();
        httpMock.expectOne('/baseURL?filter=filter&page=2&system=true');
    });

    afterEach(() => {
        httpMock.verify();
    });
});

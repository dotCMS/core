import { fakeAsync, tick } from '@angular/core/testing';
import {
  Response,
  ResponseOptions,
  ConnectionBackend,
  Headers
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { PaginatorService, OrderDirection } from './';
import { DOTTestBed } from '../../../test/dot-test-bed';

describe('PaginatorService setting', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            PaginatorService
        ]);

        this.paginatorService = this.injector.get(PaginatorService);
        this.paginatorService.url = 'v1/urldemo';
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => this.lastConnection = connection);
    });

    it('should do a request with basic params', () => {
        this.paginatorService.get().subscribe(items => this.result = items);
        expect(this.lastConnection.request.url).toContain('v1/urldemo');
    });

    it('should do a request with basic pagination params', () => {
        this.paginatorService.filter = 'test';
        this.paginatorService.sortField = 'name';
        this.paginatorService.sortOrder = OrderDirection.DESC;
        this.paginatorService.get().subscribe(items => this.result = items);
        expect(this.lastConnection.request.url).toContain('v1/urldemo?filter=test&orderby=name&direction=DESC');
    });

    it('should do a request with extra params', () => {
        this.paginatorService.extraParams.append('hello', 'world');
        this.paginatorService.extraParams.append('hola', 'mundo');
        this.paginatorService.get().subscribe(items => this.result = items);
        expect(this.lastConnection.request.url).toContain('v1/urldemo?hello=world&hola=mundo');
    });
});

describe('PaginatorService getting', () => {
    let fakeEntity;
    let headerLink;

    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            PaginatorService
        ]);

        this.paginatorService = this.injector.get(PaginatorService);
        this.paginatorService.url = 'v1/urldemo';
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => this.lastConnection = connection);

        headerLink = `</baseURL?filter=filter&page=1>;rel="first",
            </baseURL?filter=filter&page=5>;rel="last",
            </baseURL?filter=filter&page={pageValue}>;rel="x-page",
            </baseURL?filter=filter&page=4>;rel="next",
            </baseURL?filter=filter&page=2>;rel="prev"`;

        fakeEntity = {
            items: [
                { id: 0, name: 'Name 0' },
                { id: 1, name: 'Name 1' },
                { id: 2, name: 'Name 2' },
                { id: 3, name: 'Name 3' }
            ],
            totalRecords: 5
        };

        this.paginatorService.get().subscribe(items => {
            this.result = items;
        });

        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify({
                entity: fakeEntity
            }),
            headers: new Headers({
                'Link': headerLink,
                'X-Pagination-Current-Page': '3',
                'X-Pagination-Link-Pages': '5',
                'X-Pagination-Per-Page': '10',
                'X-Pagination-Total-Entries': '38'
            }),
        })));
    });

    it('should get entity results', () => {
        expect(this.result).toEqual(fakeEntity);
    });

    it('should set basic pagination information', () => {
        expect(this.paginatorService.currentPage).toEqual(3);
        expect(this.paginatorService.maxLinksPage).toEqual(5);
        expect(this.paginatorService.totalRecords).toEqual(38);
        expect(this.paginatorService.paginationPerPage).toEqual(10);
    });

    it('should get first page', () => {
        this.paginatorService.getFirstPage().subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=1');
    });

    it('should get last page', () => {
        this.paginatorService.getLastPage().subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=5');
    });

    it('should get next page', () => {
        this.paginatorService.getNextPage().subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=4');
    });

    it('should get prev page', () => {
        this.paginatorService.getPrevPage().subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=2');
    });

    it('should get page 6', () => {
        this.paginatorService.getPage(6).subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=6');
    });
});
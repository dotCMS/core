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

describe('PaginatorService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            PaginatorService
        ]);

        this.paginatorService = this.injector.get(PaginatorService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => this.lastConnection = connection);
    });

    it('should return a entity with totalRecords and items properties', fakeAsync(() => {
        let headerLink = '</baseURL?filter=filter&page=1>;rel="first",' +
                         '</baseURL?filter=filter&page=5>;rel="last",' +
                         '</baseURL?filter=filter&page={pageValue}>;rel="x-page",' +
                         '</baseURL?filter=filter&page=4>;rel="next",' +
                         '</baseURL?filter=filter&page=2>;rel="prev"';

        let ITEM1 = { id: 0, name: 'Name 0' };
        let ITEM2 = { id: 1, name: 'Name 1' };
        let ITEM3 = { id: 2, name: 'Name 2' };
        let ITEM4 = { id: 3, name: 'Name 3' };

        const mockResponse = {
            entity: {
                items: [ITEM1, ITEM2, ITEM3, ITEM4],
                totalRecords: 5
            }
        };

        this.paginatorService.query = 'name';
        this.paginatorService.url = 'v1/urldemo';
        this.paginatorService.sortField = OrderDirection.ASC;
        this.paginatorService.sortOrder = 'name';

        this.paginatorService.get().subscribe( items => this.result = items);
        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify(mockResponse),
            headers: new Headers({
                    'Link': headerLink,
                    'X-Pagination-Current-Page': '3',
                    'X-Pagination-Link-Pages': '5',
                    'X-Pagination-Per-Page': '10',
                    'X-Pagination-Total-Entries': '38'
            }),
        })));

        tick();

        expect(this.lastConnection.request.url).toContain('/api/v1/urldemo');
        expect(this.result).toBeDefined('result is not defined');
        expect(this.result.totalRecords).toEqual(5);
        expect(this.result.items.length).toEqual(4);
        expect(this.result.items[0]).toEqual(ITEM1);
        expect(this.result.items[1]).toEqual(ITEM2);
        expect(this.result.items[2]).toEqual(ITEM3);
        expect(this.result.items[3]).toEqual(ITEM4);

        expect(3).toEqual(this.paginatorService.currentPage);
        expect(5).toEqual(this.paginatorService.maxLinksPage);
        expect(38).toEqual(this.paginatorService.totalRecords);
        expect(10).toEqual(this.paginatorService.paginationPerPage);

        this.paginatorService.getLastPage().subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=5');

        this.paginatorService.getFirstPage().subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=1');

        this.paginatorService.getNextPage().subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=4');

        this.paginatorService.getPrevPage().subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=2');

        this.paginatorService.getPage(6).subscribe();
        expect(this.lastConnection.request.url).toContain('/baseURL?filter=filter&page=6');
    }));
});
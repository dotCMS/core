import { fakeAsync, tick } from '@angular/core/testing';
import {
  Response,
  ResponseOptions,
  ConnectionBackend,
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { ListingService } from './listing-service';
import { DOTTestBed } from '../util/test/dot-test-bed';

describe('ListingService', () => {

  beforeEach(() => {

    this.injector = DOTTestBed.resolveAndCreate([
        ListingService
    ]);

    this.listingService = this.injector.get(ListingService);
    this.backend = this.injector.get(ConnectionBackend) as MockBackend;
    this.backend.connections.subscribe((connection: any) => this.lastConnection = connection);
  });

  it('should return a entity with totalRecords and items properties', fakeAsync(() => {
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

        let result: any;
        this.listingService.loadData().subscribe(entity => result = entity);
        this.lastConnection.mockRespond(new Response(new ResponseOptions({
          body: JSON.stringify(mockResponse),
        })));
        tick();
        expect(result).toBeDefined('result is not defined');
        expect(result.totalRecords).toEqual(5);
        expect(result.items.length).toEqual(4);
        expect(result.items[0]).toEqual(ITEM1);
        expect(result.items[1]).toEqual(ITEM2);
        expect(result.items[2]).toEqual(ITEM3);
        expect(result.items[3]).toEqual(ITEM4);
    }));
});
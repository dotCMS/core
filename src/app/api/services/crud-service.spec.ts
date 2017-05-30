import { fakeAsync, tick } from '@angular/core/testing';
import {
  Response,
  ResponseOptions,
  ConnectionBackend,
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { CrudService } from './crud-service';
import { DOTTestBed } from '../../test/dot-test-bed';

describe('CrudService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            CrudService
        ]);

        this.crudService = this.injector.get(CrudService);
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
        this.crudService.loadData('v1/urldemo', 10, 99).subscribe(entity => result = entity);
        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify(mockResponse),
        })));
        tick();
        expect(this.lastConnection.request.url).toEqual('http://localhost:9876/api/v1/urldemo?limit=10&offset=99');
        expect(result).toBeDefined('result is not defined');
        expect(result.totalRecords).toEqual(5);
        expect(result.items.length).toEqual(4);
        expect(result.items[0]).toEqual(ITEM1);
        expect(result.items[1]).toEqual(ITEM2);
        expect(result.items[2]).toEqual(ITEM3);
        expect(result.items[3]).toEqual(ITEM4);
    }));

    it('should post data and return an entity', fakeAsync(() => {
        let body = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            defaultType: false,
            description: 'This is the content type description',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: '12345-host',
            name: 'A content type',
            owner: 'user.id.1',
            system: false,
            variable: 'aContentType'
        };

        const mockResponse = {
            entity: [
                Object.assign({}, body, {
                    'fields': [],
                    'iDate': 1495670226000,
                    'id': '1234-id-7890-entifier',
                    'modDate': 1495670226000,
                    'multilingualable': false,
                    'system': false,
                    'versionable': true
                })
            ]
        };

        let result: any;
        this.crudService.postData('v1/urldemo', body).subscribe(res => {
            result = res;
        });
        let a = this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify(mockResponse)
        })));

        tick();
        expect(this.lastConnection.request.url).toContain('v1/urldemo');
        expect(JSON.parse(this.lastConnection.request._body)).toEqual(body);
        expect(result[0]).toEqual(mockResponse.entity[0]);
    }));
});
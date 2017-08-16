import { fakeAsync, tick } from '@angular/core/testing';
import {
  Response,
  ResponseOptions,
  ConnectionBackend,
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { CrudService } from './';
import { DOTTestBed } from '../../../test/dot-test-bed';

describe('CrudService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            CrudService
        ]);

        this.crudService = this.injector.get(CrudService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => this.lastConnection = connection);
    });

    it('should post data and return an entity', fakeAsync(() => {
        const body = {
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
        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify(mockResponse)
        })));

        tick();
        expect(this.lastConnection.request.url).toContain('v1/urldemo');
        expect(JSON.parse(this.lastConnection.request._body)).toEqual(body);
        expect(result[0]).toEqual(mockResponse.entity[0]);
    }));
});

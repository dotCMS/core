import { fakeAsync, tick } from '@angular/core/testing';
import {
  Response,
  ResponseOptions,
  ConnectionBackend,
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { FieldService } from './';
import { DOTTestBed } from '../../../../test/dot-test-bed';

describe('FieldService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            FieldService
        ]);

        this.fieldService = this.injector.get(FieldService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => this.lastConnection = connection);
    });

    it('should load field types', fakeAsync(() => {

        const mockResponse = {
            entity: [{
                clazz: 'TextField',
                helpText: 'helpText',
                id: 'text',
                label: 'Text',
                properties: [],
            }]
        };

        this.fieldService.loadFieldTypes().subscribe(res => this.response = res);

        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify(mockResponse)
        })));

        tick();

        expect(this.response).toEqual(mockResponse.entity);
    }));
});
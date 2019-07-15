import { Response, ResponseOptions, ConnectionBackend } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotFieldVariablesService } from './dot-field-variables.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotFieldVariable } from '../models/dot-field-variable.interface';
import { DotCMSContentTypeField } from '@dotcms/models';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';

describe('DotFieldVariablesService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotFieldVariablesService]);
        this.fieldVariableService = this.injector.get(DotFieldVariablesService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should load field variables', () => {
        const mockResponse = {
            entity: [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                    fieldId: '1',
                    id: '1b',
                    key: 'test1',
                    value: 'value1'
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                    fieldId: '2',
                    id: '2b',
                    key: 'test2',
                    value: 'value2'
                }
            ]
        };

        const field: DotCMSContentTypeField = {
            ...dotcmsContentTypeFieldBasicMock,
            contentTypeId: '1b',
            id: '1'
        };

        this.fieldVariableService.load(field).subscribe((variables: DotFieldVariable[]) => {
            expect(variables).toEqual(mockResponse.entity);
            expect(0).toBe(this.lastConnection.request.method); // 2 is GET method
            expect(this.lastConnection.request.url)
                .toContain(`v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables`);
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: JSON.stringify(mockResponse)
                })
            )
        );
    });

    it('should save field variables', () => {
        const mockResponse = {
            entity: {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                    fieldId: '1',
                    id: '1b',
                    key: 'test3',
                    value: 'value3'
                }
        };

        const field: DotCMSContentTypeField = {
            ...dotcmsContentTypeFieldBasicMock,
            contentTypeId: '1b',
            id: '1'
        };

        const variable: DotFieldVariable = {
            key: 'test3',
            value: 'value3'
        }

        this.fieldVariableService.save(field, variable).subscribe((variables: DotFieldVariable) => {
            expect(variables).toEqual(mockResponse.entity);
            expect(1).toBe(this.lastConnection.request.method); // 1 is POST method
            expect(this.lastConnection.request.url)
                .toContain(`v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables`);

        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: JSON.stringify(mockResponse)
                })
            )
        );
    });

    it('should delete field variables', () => {
        const mockResponse = {
            entity: []
        };

        const field: DotCMSContentTypeField = {
            ...dotcmsContentTypeFieldBasicMock,
            contentTypeId: '1b',
            id: '1'
        };

        const variable: DotFieldVariable = {
            id: 'code1',
            key: 'test3',
            value: 'value3'
        };

        this.fieldVariableService.delete(field, variable).subscribe((_variables: DotFieldVariable) => {
            expect(3).toBe(this.lastConnection.request.method); // 3 is DELETE method
            expect(this.lastConnection.request.url)
                .toContain(`v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables/id/${variable.id}`);

        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: JSON.stringify(mockResponse)
                })
            )
        );
    });
});

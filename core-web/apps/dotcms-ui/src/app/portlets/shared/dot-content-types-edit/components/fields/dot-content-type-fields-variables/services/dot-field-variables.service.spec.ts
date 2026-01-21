/* eslint-disable @typescript-eslint/no-explicit-any */

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';
import { CoreWebServiceMock, dotcmsContentTypeFieldBasicMock } from '@dotcms/utils-testing';

import { DotFieldVariablesService } from './dot-field-variables.service';

describe('DotFieldVariablesService', () => {
    let dotFieldVariablesService: DotFieldVariablesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotFieldVariablesService
            ]
        });
        dotFieldVariablesService = TestBed.inject(DotFieldVariablesService);
        httpMock = TestBed.inject(HttpTestingController);
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

        dotFieldVariablesService.load(field).subscribe((variables: DotFieldVariable[]) => {
            expect(variables).toEqual(mockResponse.entity);
        });

        const req = httpMock.expectOne(
            `v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables`
        );
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockResponse.entity });
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
        };

        dotFieldVariablesService.save(field, variable).subscribe((variables: DotFieldVariable) => {
            expect(variables).toEqual(mockResponse.entity);
        });

        const req = httpMock.expectOne(
            `v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables`
        );
        expect(req.request.method).toBe('POST');
        req.flush(mockResponse);
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

        dotFieldVariablesService.delete(field, variable).subscribe((variables: any) => {
            expect(variables).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(
            `v1/contenttype/${field.contentTypeId}/fields/id/${field.id}/variables/id/${variable.id}`
        );
        expect(req.request.method).toBe('DELETE');
        req.flush({ entity: mockResponse });
    });

    afterEach(() => {
        httpMock.verify();
    });
});

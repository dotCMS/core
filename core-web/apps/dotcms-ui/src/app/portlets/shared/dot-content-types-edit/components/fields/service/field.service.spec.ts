import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';
import { CoreWebServiceMock, dotcmsContentTypeFieldBasicMock } from '@dotcms/utils-testing';

import { FieldService } from '.';

import { FieldType } from '../../../components/fields';

export const mockFieldType: FieldType = {
    clazz: 'TextField',
    helpText: 'helpText',
    id: 'text',
    label: 'Text',
    properties: []
};

describe('FieldService', () => {
    let fieldService: FieldService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [{ provide: CoreWebService, useClass: CoreWebServiceMock }, FieldService]
        });
        fieldService = TestBed.inject(FieldService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should load field types', () => {
        const mockResponse = [mockFieldType];

        fieldService.loadFieldTypes().subscribe((res: FieldType[]) => {
            expect(res).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('v1/fieldTypes');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockResponse });
    });

    let mockData;

    describe('Save Fields', () => {
        it('should save field', () => {
            mockData = [
                {
                    divider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
                        name: 'Hello World'
                    }
                },
                {
                    divider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField'
                    }
                }
            ];

            const contentTypeId = '1';
            fieldService
                .saveFields(contentTypeId, mockData)
                .subscribe((res: DotCMSContentTypeLayoutRow[]) => {
                    expect(res).toEqual(mockData);
                });

            const req = httpMock.expectOne(`v3/contenttype/${contentTypeId}/fields/move`);
            expect(req.request.method).toBe('PUT');
            req.flush({ entity: mockData });
        });
    });

    describe('Delete Fields', () => {
        it('should delete field', () => {
            mockData = [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
                    name: 'Hello World',
                    id: '1'
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
                    id: '2'
                }
            ];

            const contentTypeId = '1';
            fieldService
                .deleteFields(contentTypeId, mockData)
                .subscribe(
                    (res: { fields: DotCMSContentTypeLayoutRow[]; deletedIds: string[] }) => {
                        expect(res).toEqual({ deletedIds: ['1', '2'], fields: mockData });
                    }
                );

            const req = httpMock.expectOne(`v3/contenttype/${contentTypeId}/fields`);
            expect(req.request.method).toBe('DELETE');
            req.flush({ entity: { deletedIds: ['1', '2'], fields: mockData } });
        });
    });

    describe('Update Field', () => {
        it('should update field', () => {
            const field: DotCMSContentTypeField = {
                ...dotcmsContentTypeFieldBasicMock,
                name: 'test field',
                id: '1',
                sortOrder: 1
            };

            const mockResponse: DotCMSContentTypeLayoutRow = {
                divider: field
            };

            const contentTypeId = '2';

            fieldService
                .updateField(contentTypeId, field)
                .subscribe((res: DotCMSContentTypeLayoutRow[]) => {
                    expect(res[0]).toEqual(mockResponse);
                });

            const req = httpMock.expectOne(`v3/contenttype/${contentTypeId}/fields/1`);
            expect(req.request.method).toBe('PUT');
            req.flush({ entity: [mockResponse] });
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});

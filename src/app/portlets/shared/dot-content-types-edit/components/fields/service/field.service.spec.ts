import { TestBed, getTestBed } from '@angular/core/testing';
import { FieldService } from '.';
import { FieldType } from '@portlets/shared/dot-content-types-edit/components/fields';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';
import { DotCMSContentTypeField } from 'dotcms-models';

export const mockFieldType: FieldType = {
    clazz: 'TextField',
    helpText: 'helpText',
    id: 'text',
    label: 'Text',
    properties: []
};

describe('FieldService', () => {
    let injector: TestBed;
    let fieldService: FieldService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [{ provide: CoreWebService, useClass: CoreWebServiceMock }, FieldService]
        });
        injector = getTestBed();
        fieldService = injector.get(FieldService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should load field types', () => {
        const mockResponse = {
            entity: [mockFieldType]
        };

        fieldService.loadFieldTypes().subscribe((res: any) => {
            expect(res).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('v1/fieldTypes');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockResponse });
    });

    describe('Save Fields', () => {
        it('should save field', () => {
            this.mockData = [
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
            fieldService.saveFields(contentTypeId, this.mockData).subscribe((res: any) => {
                expect(res).toEqual(this.mockData);
            });

            const req = httpMock.expectOne(`v3/contenttype/${contentTypeId}/fields/move`);
            expect(req.request.method).toBe('PUT');
            req.flush({ entity: this.mockData });
        });
    });

    describe('Delete Fields', () => {
        it('should delete field', () => {
            this.mockData = [
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
            fieldService.deleteFields(contentTypeId, this.mockData).subscribe((res: any) => {
                expect(res).toEqual({ deletedIds: ['1', '2'], fields: this.mockData });
            });

            const req = httpMock.expectOne(`v3/contenttype/${contentTypeId}/fields`);
            expect(req.request.method).toBe('DELETE');
            req.flush({ entity: { deletedIds: ['1', '2'], fields: this.mockData } });
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

            const contentTypeId = '2';
            fieldService.updateField(contentTypeId, field).subscribe((res: any) => {
                expect(res[0]).toEqual({
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'test field',
                    id: '1',
                    sortOrder: 1
                });
            });

            const req = httpMock.expectOne(`v3/contenttype/${contentTypeId}/fields/1`);
            expect(req.request.method).toBe('PUT');
            req.flush({ entity: [field] });
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});

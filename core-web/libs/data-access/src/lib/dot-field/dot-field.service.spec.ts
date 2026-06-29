import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    FieldType
} from '@dotcms/dotcms-models';
import { dotcmsContentTypeFieldBasicMock } from '@dotcms/utils-testing';

import { DotFieldService } from './dot-field.service';

const mockFieldType: FieldType = {
    clazz: 'TextField',
    helpText: 'helpText',
    id: 'text',
    label: 'Text',
    properties: []
};

describe('DotFieldService', () => {
    let spectator: SpectatorHttp<DotFieldService>;
    const createHttp = createHttpFactory(DotFieldService);

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('getFields', () => {
        const mockFields: DotCMSContentTypeField[] = [
            {
                fieldType: 'Text',
                name: 'title',
                required: true
            } as DotCMSContentTypeField
        ];

        it('should get all fields without filter', () => {
            spectator.service.getFields('Region').subscribe();

            const req = spectator.expectOne(
                '/api/v3/contenttype/Region/fields/allfields',
                HttpMethod.GET
            );
            expect(req.request.params.toString()).toEqual('');

            req.flush(mockFields);
        });

        it('should get fields with REQUIRED filter', () => {
            spectator.service.getFields('Region', 'REQUIRED').subscribe();

            const req = spectator.expectOne(
                '/api/v3/contenttype/Region/fields/allfields?filter=REQUIRED',
                HttpMethod.GET
            );
            expect(req.request.params.get('filter')).toBe('REQUIRED');

            req.flush(mockFields);
        });

        it('should get fields with SHOW_IN_LIST filter', () => {
            spectator.service.getFields('Region', 'SHOW_IN_LIST').subscribe();

            const req = spectator.expectOne(
                '/api/v3/contenttype/Region/fields/allfields?filter=SHOW_IN_LIST',
                HttpMethod.GET
            );
            expect(req.request.params.get('filter')).toBe('SHOW_IN_LIST');

            req.flush(mockFields);
        });

        it('should handle error response', () => {
            const errorResponse = { status: 404, statusText: 'Not Found' };

            spectator.service.getFields('InvalidType').subscribe({
                error: (error) => {
                    expect(error.status).toBe(404);
                }
            });

            const req = spectator.expectOne(
                '/api/v3/contenttype/InvalidType/fields/allfields',
                HttpMethod.GET
            );

            req.flush('Not Found', errorResponse);
        });
    });

    describe('loadFieldTypes', () => {
        it('should load field types', () => {
            const mockResponse = [mockFieldType];

            spectator.service.loadFieldTypes().subscribe((res: FieldType[]) => {
                expect(res).toEqual(mockResponse);
            });

            const req = spectator.expectOne('/api/v1/fieldTypes', HttpMethod.GET);
            req.flush({ entity: mockResponse });
        });
    });

    describe('saveFields', () => {
        it('should save fields', () => {
            const mockData: DotCMSContentTypeLayoutRow[] = [
                {
                    divider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
                        name: 'Hello World'
                    } as DotCMSContentTypeField
                },
                {
                    divider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField'
                    } as DotCMSContentTypeField
                }
            ];
            const contentTypeId = '1';

            spectator.service
                .saveFields(contentTypeId, mockData)
                .subscribe((res: DotCMSContentTypeLayoutRow[]) => {
                    expect(res).toEqual(mockData);
                });

            const req = spectator.expectOne(
                `/api/v3/contenttype/${contentTypeId}/fields/move`,
                HttpMethod.PUT
            );
            expect(req.request.body).toEqual({ layout: mockData });
            req.flush({ entity: mockData });
        });
    });

    describe('deleteFields', () => {
        it('should delete fields', () => {
            const mockData: DotCMSContentTypeField[] = [
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
                    name: 'Hello World',
                    id: '1'
                } as DotCMSContentTypeField,
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
                    id: '2'
                } as DotCMSContentTypeField
            ];
            const contentTypeId = '1';

            spectator.service.deleteFields(contentTypeId, mockData).subscribe((res) => {
                expect(res).toEqual({ deletedIds: ['1', '2'], fields: mockData });
            });

            const req = spectator.expectOne(
                `/api/v3/contenttype/${contentTypeId}/fields`,
                HttpMethod.DELETE
            );
            expect(req.request.body).toEqual({ fieldsID: ['1', '2'] });
            req.flush({ entity: { deletedIds: ['1', '2'], fields: mockData } });
        });
    });

    describe('updateField', () => {
        it('should update field', () => {
            const field: DotCMSContentTypeField = {
                ...dotcmsContentTypeFieldBasicMock,
                name: 'test field',
                id: '1',
                sortOrder: 1
            };
            const mockResponse: DotCMSContentTypeLayoutRow = { divider: field };
            const contentTypeId = '2';

            spectator.service
                .updateField(contentTypeId, field)
                .subscribe((res: DotCMSContentTypeLayoutRow[]) => {
                    expect(res[0]).toEqual(mockResponse);
                });

            const req = spectator.expectOne(
                `/api/v3/contenttype/${contentTypeId}/fields/1`,
                HttpMethod.PUT
            );
            expect(req.request.body).toEqual({ field });
            req.flush({ entity: [mockResponse] });
        });
    });
});

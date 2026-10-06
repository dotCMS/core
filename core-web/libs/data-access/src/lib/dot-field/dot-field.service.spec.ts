import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSFieldTypes,
    FieldType
} from '@dotcms/dotcms-models';
import { createFakeTextField, dotcmsContentTypeFieldBasicMock } from '@dotcms/utils-testing';

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

    /**
     * FR-013 (issue #37670): the content model is a closed union of the 28 field types the
     * frontend describes, but the backend's set is open — a customer plugin can contribute a
     * field type this build has never heard of. The union is a claim about runtime JSON, and
     * at this boundary that claim can be false.
     *
     * The requirement is that an unrecognised field type degrades rather than breaks: nothing
     * throws, the surrounding fields still arrive, and the odd one is still identifiable
     * instead of being silently dropped or mistyped as something it is not.
     */
    describe('an unmodelled field type (FR-013)', () => {
        const fieldFromAPluginWeDoNotModel = {
            fieldType: 'Com.acme.SignaturePad',
            clazz: 'com.acme.contenttype.model.field.ImmutableSignaturePadField',
            dataType: 'TEXT',
            name: 'signature',
            variable: 'signature'
        } as unknown as DotCMSContentTypeField;

        it('does not throw, and passes the field through untouched', () => {
            let received: DotCMSContentTypeField[] | undefined;
            let errored: unknown;

            spectator.service.getFields('Region').subscribe({
                next: (fields) => (received = fields),
                error: (e) => (errored = e)
            });

            spectator
                .expectOne('/api/v3/contenttype/Region/fields/allfields', HttpMethod.GET)
                .flush({
                    entity: [
                        createFakeTextField({ variable: 'title' }),
                        fieldFromAPluginWeDoNotModel
                    ]
                });

            expect(errored).toBeUndefined();
            expect(received).toHaveLength(2);
            expect(received?.[1].fieldType).toBe('Com.acme.SignaturePad');
        });

        it('still delivers the fields the model does know', () => {
            let received: DotCMSContentTypeField[] | undefined;

            spectator.service.getFields('Region').subscribe((fields) => (received = fields));

            spectator
                .expectOne('/api/v3/contenttype/Region/fields/allfields', HttpMethod.GET)
                .flush({
                    entity: [
                        fieldFromAPluginWeDoNotModel,
                        createFakeTextField({ variable: 'title' })
                    ]
                });

            // The unmodelled field must not take the rest of the content type down with it.
            expect(received?.[1].variable).toBe('title');
            expect(received?.[1].fieldType).toBe(DotCMSFieldTypes.TEXT);
        });
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
        it('should delete fields by id', () => {
            const fieldIds = ['1', '2'];
            const contentTypeId = '1';
            const mockFields: DotCMSContentTypeLayoutRow[] = [];

            spectator.service.deleteFields(contentTypeId, fieldIds).subscribe((res) => {
                expect(res).toEqual({ deletedIds: fieldIds, fields: mockFields });
            });

            const req = spectator.expectOne(
                `/api/v3/contenttype/${contentTypeId}/fields`,
                HttpMethod.DELETE
            );
            expect(req.request.body).toEqual({ fieldsID: fieldIds });
            req.flush({ entity: { deletedIds: fieldIds, fields: mockFields } });
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

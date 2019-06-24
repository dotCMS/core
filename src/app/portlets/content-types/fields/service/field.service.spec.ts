import { fakeAsync, tick } from '@angular/core/testing';
import { Response, ResponseOptions, ConnectionBackend } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { FieldService } from './';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { FieldType, DotContentTypeField } from '@portlets/content-types/fields';

export const mockFieldType: FieldType = {
    clazz: 'TextField',
    helpText: 'helpText',
    id: 'text',
    label: 'Text',
    properties: []
};

describe('FieldService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([FieldService]);

        this.fieldService = this.injector.get(FieldService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it(
        'should load field types',
        fakeAsync(() => {
            const mockResponse = {
                entity: [mockFieldType]
            };

            this.fieldService.loadFieldTypes().subscribe(res => (this.response = res));

            this.lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: JSON.stringify(mockResponse)
                    })
                )
            );

            tick();

            expect(this.response).toEqual(mockResponse.entity);
        })
    );

    describe('Save Fields', () => {
        beforeEach(() => {
            this.mockData = [
                {
                    divider: {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
                        name: 'Hello World'
                    }
                },
                {
                    divider:                 {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField'
                    }
                },
            ];

            this.fieldService
                .saveFields('1', this.mockData)
                .subscribe(res => (this.response = JSON.parse(res)));

            this.lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: {
                            entity: JSON.stringify(this.mockData)
                        }
                    })
                )
            );
        });

        it('should save field', () => {
            expect(this.mockData).toEqual(this.response);
            expect(this.lastConnection.request.url).toContain('v3/contenttype/1/fields');
            expect(2).toBe(this.lastConnection.request.method); // 2 is PUT method
        });
    });

    describe('Delete Fields', () => {
        beforeEach(() => {
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

            this.fieldService
                .deleteFields('1', this.mockData)
                .subscribe(res => (this.response = res));

            this.lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: {
                            entity: {
                                deletedIds: ['1', '2'],
                                fields: this.mockData
                            }
                        }
                    })
                )
            );
        });

        it('should delete field', () => {
            expect(['1', '2']).toEqual(this.response.deletedIds);
            expect(this.mockData).toEqual(this.response.fields);
            expect(3).toBe(this.lastConnection.request.method); // 3 is DELETE method
            expect(this.lastConnection.request.url).toContain('v3/contenttype/1/fields');
        });
    });

    describe('Update Field', () => {
        beforeEach(() => {
            const field: DotContentTypeField = {
                name: 'test field',
                id: '1',
                sortOrder: 1
            };

            this.fieldService
                .updateField('2', field)
                .subscribe(res => (this.response = JSON.parse(res)));

            this.lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: {
                            entity: JSON.stringify([field])
                        }
                    })
                )
            );
        });

        it('should update field', () => {
            expect(this.lastConnection.request._body.field).toEqual({
                name: 'test field',
                id: '1',
                sortOrder: 1
            });

            expect(this.lastConnection.request.url).toContain('v3/contenttype/2/fields/1');
            expect(2).toBe(this.lastConnection.request.method); // 2 is PUT method
        });
    });
});

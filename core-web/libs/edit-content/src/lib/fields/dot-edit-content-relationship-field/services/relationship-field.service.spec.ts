import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { formatDate } from '@angular/common';

import { DotContentSearchService, DotFieldService, DotLanguagesService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { RelationshipFieldService } from './relationship-field.service';

import {
    MANDATORY_FIRST_COLUMNS,
    MANDATORY_LAST_COLUMNS
} from '../dot-edit-content-relationship-field.constants';

describe('RelationshipFieldService', () => {
    let spectator: SpectatorService<RelationshipFieldService>;
    let dotFieldService: SpyObject<DotFieldService>;
    let dotContentSearchService: SpyObject<DotContentSearchService>;

    const createService = createServiceFactory({
        service: RelationshipFieldService,
        providers: [
            mockProvider(DotFieldService),
            mockProvider(DotContentSearchService),
            mockProvider(DotLanguagesService, {
                get: () =>
                    of([
                        { id: 1, language: 'English', isoCode: 'en' },
                        { id: 2, language: 'Spanish', isoCode: 'es' }
                    ])
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dotFieldService = spectator.inject(DotFieldService);
        dotContentSearchService = spectator.inject(DotContentSearchService);
    });

    describe('getColumns', () => {
        it('should map fields to columns', (done) => {
            const mockFields = [
                {
                    variable: 'field1',
                    name: 'Field 1'
                },
                {
                    variable: 'description',
                    name: 'Description'
                }
            ] as DotCMSContentTypeField[];

            dotFieldService.getFields.mockReturnValue(of(mockFields));

            const contentTypeId = '123';

            const expectedColumns = [
                ...MANDATORY_FIRST_COLUMNS.map((field) => ({ field, header: field })),
                ...mockFields.map((field) => ({ field: field.variable, header: field.name })),
                ...MANDATORY_LAST_COLUMNS.map((field) => ({ field, header: field }))
            ];

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual(expectedColumns);
                done();
            });
        });

        it('should return empty array when no fields are returned', (done) => {
            dotFieldService.getFields.mockReturnValue(of([]));

            const contentTypeId = '123';

            const expectedColumns = [
                ...MANDATORY_FIRST_COLUMNS.map((field) => ({ field, header: field })),
                ...MANDATORY_LAST_COLUMNS.map((field) => ({ field, header: field }))
            ];

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual(expectedColumns);
                done();
            });
        });

        it('should handle fields with missing properties', (done) => {
            const mockFields = [
                {
                    variable: 'field1',
                    name: 'Field 1'
                },
                {
                    variable: 'description' // missing name
                }
            ] as DotCMSContentTypeField[];

            dotFieldService.getFields.mockReturnValue(of(mockFields));

            const contentTypeId = '123';

            const expectedColumns = [
                ...MANDATORY_FIRST_COLUMNS.map((field) => ({ field, header: field })),
                { field: 'field1', header: 'Field 1' },
                ...MANDATORY_LAST_COLUMNS.map((field) => ({ field, header: field }))
            ];

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual(expectedColumns);
                done();
            });
        });
    });

    describe('getContent', () => {
        it('should get content with correct query parameters', (done) => {
            const contentTypeId = 'test123';
            const expectedQuery = `+contentType:${contentTypeId} +deleted:false +working:true`;

            const mockContentlets = [
                { identifier: '123', title: 'Test Content 1' },
                { identifier: '456', title: 'Test Content 2' }
            ] as DotCMSContentlet[];

            dotContentSearchService.get.mockReturnValue(
                of({
                    jsonObjectView: {
                        contentlets: mockContentlets
                    }
                })
            );

            spectator.service.getContent(contentTypeId).subscribe((result) => {
                expect(dotContentSearchService.get).toHaveBeenCalledWith({
                    query: expectedQuery,
                    limit: 100
                });
                expect(result).toEqual(mockContentlets);
                done();
            });
        });

        it('should return empty array when no contentlets are found', (done) => {
            const contentTypeId = 'test123';
            const emptyResponse = {
                jsonObjectView: {
                    contentlets: []
                }
            };

            dotContentSearchService.get.mockReturnValue(of(emptyResponse));

            spectator.service.getContent(contentTypeId).subscribe((result) => {
                expect(result).toEqual([]);
                done();
            });
        });

        it('should handle error case gracefully', (done) => {
            const contentTypeId = 'test123';
            const errorResponse = {
                jsonObjectView: null
            };

            dotContentSearchService.get.mockReturnValue(of(errorResponse));

            spectator.service.getContent(contentTypeId).subscribe((result) => {
                expect(result).toBeUndefined();
                done();
            });
        });
    });

    describe('getColumnsAndContent', () => {
        const mockContentTypeId = 'test123';

        const mockFields = [
            { variable: 'field', name: 'Field' },
            { variable: 'description', name: 'Description' }
        ] as DotCMSContentTypeField[];

        const expectedColumns = [
            ...MANDATORY_FIRST_COLUMNS.map((field) => ({ field, header: field })),
            { field: 'field', header: 'Field' },
            { field: 'description', header: 'Description' },
            ...MANDATORY_LAST_COLUMNS.map((field) => ({ field, header: field }))
        ];

        const mockContentlets = [
            {
                identifier: '123',
                title: 'Test Content 1',
                field: 'Field 1',
                description: 'Description 1',
                languageId: 1,
                modDate: '2024-01-01T00:00:00Z'
            },
            {
                identifier: '456',
                title: 'Test Content 2',
                field: 'Field 2',
                description: 'Description 2',
                languageId: 2,
                modDate: '2024-01-02T00:00:00Z'
            }
        ] as unknown as DotCMSContentlet[];

        beforeEach(() => {
            dotFieldService.getFields.mockReturnValue(of(mockFields));

            dotContentSearchService.get.mockReturnValue(
                of({
                    jsonObjectView: {
                        contentlets: mockContentlets
                    }
                })
            );
        });

        it('should combine columns and content correctly', (done) => {
            spectator.service
                .getColumnsAndContent(mockContentTypeId)
                .subscribe(([columns, items]) => {
                    // Verify columns
                    expect(columns.length).toBeGreaterThan(0);
                    expect(columns).toEqual(expectedColumns);

                    // Verify relationship items
                    expect(items.length).toBe(2);
                    expect(items[0]).toEqual({
                        id: '123',
                        title: 'Test Content 1',
                        field: 'Field 1',
                        description: 'Description 1',
                        language: 'English (en)',
                        modDate: formatDate(mockContentlets[0].modDate, 'short', 'en-US')
                    });
                    expect(items[1]).toEqual({
                        id: '456',
                        title: 'Test Content 2',
                        field: 'Field 2',
                        description: 'Description 2',
                        language: 'Spanish (es)',
                        modDate: formatDate(mockContentlets[1].modDate, 'short', 'en-US')
                    });

                    // Verify service calls
                    expect(dotFieldService.getFields).toHaveBeenCalledWith(
                        mockContentTypeId,
                        'SHOW_IN_LIST'
                    );
                    expect(dotContentSearchService.get).toHaveBeenCalledWith({
                        query: `+contentType:${mockContentTypeId} +deleted:false +working:true`,
                        limit: 100
                    });

                    done();
                });
        });

        it('should handle empty content correctly', (done) => {
            dotContentSearchService.get.mockReturnValue(
                of({
                    jsonObjectView: {
                        contentlets: []
                    }
                })
            );

            spectator.service
                .getColumnsAndContent(mockContentTypeId)
                .subscribe(([columns, items]) => {
                    expect(columns.length).toBeGreaterThan(0);
                    expect(items).toEqual([]);
                    done();
                });
        });

        it('should handle content with missing fields', (done) => {
            const contentWithMissingFields = [
                {
                    identifier: '789',
                    title: 'Test Content 3',
                    field: 'Field 3',
                    languageId: 1,
                    modDate: '2024-01-03T00:00:00Z'
                    // description is missing
                }
            ] as unknown as DotCMSContentlet[];

            dotContentSearchService.get.mockReturnValue(
                of({
                    jsonObjectView: {
                        contentlets: contentWithMissingFields
                    }
                })
            );

            spectator.service.getColumnsAndContent(mockContentTypeId).subscribe(([_, items]) => {
                expect(items[0]).toEqual({
                    id: '789',
                    title: 'Test Content 3',
                    field: 'Field 3',
                    description: '', // Should be empty string for missing field
                    language: 'English (en)',
                    modDate: formatDate(contentWithMissingFields[0].modDate, 'short', 'en-US')
                });
                done();
            });
        });

        it('should handle content without title', (done) => {
            const contentWithoutTitle = [
                {
                    identifier: '789',
                    description: 'Description 3',
                    field: 'Field 3',
                    languageId: 1,
                    modDate: '2024-01-03T00:00:00Z'
                }
            ] as unknown as DotCMSContentlet[];

            dotContentSearchService.get.mockReturnValue(
                of({
                    jsonObjectView: {
                        contentlets: contentWithoutTitle
                    }
                })
            );

            spectator.service.getColumnsAndContent(mockContentTypeId).subscribe(([_, items]) => {
                expect(items[0]).toEqual({
                    id: '789',
                    title: '789', // Should use identifier when title is null
                    field: 'Field 3',
                    description: 'Description 3',
                    language: 'English (en)',
                    modDate: formatDate(contentWithoutTitle[0].modDate, 'short', 'en-US')
                });
                done();
            });
        });
    });
});

import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import {
    DotContentSearchService,
    DotFieldService,
    DotHttpErrorManagerService,
    DotLanguagesService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeContentlet, mockLocales } from '@dotcms/utils-testing';

import { RelationshipFieldService } from './relationship-field.service';

describe('RelationshipFieldService', () => {
    let spectator: SpectatorService<RelationshipFieldService>;
    let dotFieldService: SpyObject<DotFieldService>;
    let dotContentSearchService: SpyObject<DotContentSearchService>;

    const createService = createServiceFactory({
        service: RelationshipFieldService,
        providers: [
            mockProvider(DotHttpErrorManagerService, {
                handle: () => of(null)
            }),
            mockProvider(DotFieldService),
            mockProvider(DotContentSearchService),
            mockProvider(DotLanguagesService, { get: () => of(mockLocales) })
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

            const expectedColumns = mockFields.map((field) => ({
                field: field.variable,
                header: field.name
            }));

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

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual([]);
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

            const expectedColumns = [{ field: 'field1', header: 'Field 1' }];

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
            { field: 'field', header: 'Field' },
            { field: 'description', header: 'Description' }
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
                    const item0 = {
                        identifier: items[0].identifier,
                        title: items[0].title,
                        field: items[0].field,
                        description: items[0].description,
                        language: items[0].language
                    };
                    const item1 = {
                        identifier: items[1].identifier,
                        title: items[1].title,
                        field: items[1].field,
                        description: items[1].description,
                        language: items[1].language
                    };
                    expect(item0).toEqual({
                        identifier: '123',
                        title: 'Test Content 1',
                        field: 'Field 1',
                        description: 'Description 1',
                        language: mockLocales[0]
                    });
                    expect(item1).toEqual({
                        identifier: '456',
                        title: 'Test Content 2',
                        field: 'Field 2',
                        description: 'Description 2',
                        language: mockLocales[1]
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

        it('should handle content without title', () => {
            const contentWithoutTitle = createFakeContentlet({
                identifier: '789',
                title: null,
                description: 'Description 3',
                field: 'Field 3',
                languageId: mockLocales[0].id,
                modDate: '2024-01-03T00:00:00Z'
            });

            dotContentSearchService.get.mockReturnValue(
                of({
                    jsonObjectView: {
                        contentlets: contentWithoutTitle
                    }
                })
            );

            spectator.service.getColumnsAndContent(mockContentTypeId).subscribe(([_, items]) => {
                const item = {
                    identifier: items[0].identifier,
                    title: items[0].title,
                    field: items[0].field,
                    description: items[0].description,
                    language: items[0].language
                };
                expect(item).toEqual({
                    identifier: '789',
                    title: '789', // Should use identifier when title is null
                    field: 'Field 3',
                    description: 'Description 3',
                    language: mockLocales[0]
                });
            });
        });
    });
});

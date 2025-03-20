import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import {
    DotContentSearchService,
    DotFieldService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotContentSearchParams
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeContentlet, mockLocales } from '@dotcms/utils-testing';

import { RelationshipFieldService } from './relationship-field.service';

describe('RelationshipFieldService', () => {
    let spectator: SpectatorService<RelationshipFieldService>;
    let dotFieldService: SpyObject<DotFieldService>;
    let dotContentSearchService: SpyObject<DotContentSearchService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

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
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
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

    describe('search', () => {
        const contentTypeId = 'test123';
        const searchTerm = 'query';
        const page = 1;
        const perPage = 10;
        const searchableFieldsByContentType = { [contentTypeId]: {} };

        const mockFakeContentlets = [
            createFakeContentlet({
                identifier: '123',
                title: 'Test Content 1',
                languageId: 1
            }),
            createFakeContentlet({
                identifier: '456',
                title: 'Test Content 2',
                languageId: 2
            })
        ];

        it('should search contentlets with correct parameters', (done) => {
            const expectedParams: DotContentSearchParams = {
                globalSearch: searchTerm,
                searchableFieldsByContentType,
                page,
                perPage
            };

            dotContentSearchService.search.mockReturnValue(of(mockFakeContentlets));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage,
                })
                .subscribe((results) => {
                    expect(dotContentSearchService.search).toHaveBeenCalledWith(expectedParams);
                    expect(results.length).toBe(2);

                    // Verify languages were applied to contentlets
                    expect(results[0].language).toEqual(mockLocales[0]);
                    expect(results[1].language).toEqual(mockLocales[1]);

                    done();
                });
        });

        it('should handle contentlets without title by using identifier', (done) => {
            const contentletsWithoutTitle = [
                createFakeContentlet({
                    identifier: '789',
                    title: null,
                    languageId: 1
                })
            ];

            dotContentSearchService.search.mockReturnValue(of(contentletsWithoutTitle));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage,
                })
                .subscribe((results) => {
                    expect(results[0].title).toBe('789'); // Should use identifier when title is null
                    expect(results[0].language).toEqual(mockLocales[0]);
                    done();
                });
        });

        it('should return empty array when no contentlets are found', (done) => {
            dotContentSearchService.search.mockReturnValue(of([]));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage,
                })
                .subscribe((results) => {
                    expect(dotContentSearchService.search).toHaveBeenCalled();
                    expect(results).toEqual([]);
                    done();
                });
        });

        it('should handle optional parameters correctly', () => {
            const expectedParams: DotContentSearchParams = {
                globalSearch: '',
                searchableFieldsByContentType,
                page,
                perPage
            };

            dotContentSearchService.search.mockReturnValue(of(mockFakeContentlets));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: '',
                    page,
                    perPage,
                })
                .subscribe(() => {
                    expect(dotContentSearchService.search).toHaveBeenCalledWith(expectedParams);
                });
        });

        it('should handle error case gracefully', () => {
            const errorResponse = new HttpErrorResponse({
                status: 500,
                statusText: 'Server Error'
            });
            dotContentSearchService.search.mockReturnValue(throwError(() => errorResponse));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage,
                })
                .subscribe((results) => {
                    expect(dotContentSearchService.search).toHaveBeenCalled();
                    expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(errorResponse);
                    expect(results).toEqual([]);
                });
        });

        it('should apply proper title and language to contentlets', (done) => {
            const customContentlets = [
                createFakeContentlet({
                    identifier: 'abc123',
                    title: 'Custom Title',
                    languageId: 1
                }),
                createFakeContentlet({
                    identifier: 'def456',
                    title: '', // Empty title
                    languageId: 2
                })
            ];

            dotContentSearchService.search.mockReturnValue(of(customContentlets));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage,
                })
                .subscribe((results) => {
                    expect(results[0].title).toBe('Custom Title');
                    expect(results[1].title).toBe('def456'); // Should use identifier for empty title

                    expect(results[0].language).toEqual(mockLocales[0]);
                    expect(results[1].language).toEqual(mockLocales[1]);

                    done();
                });
        });

        it('should handle additional system searchable fields', (done) => {
            const additionalFields = {
                someField: 'someValue',
                anotherField: 123
            };

            const expectedParams: DotContentSearchParams = {
                globalSearch: searchTerm,
                systemSearchableFields: { ...additionalFields },
                searchableFieldsByContentType,
                page,
                perPage
            };

            dotContentSearchService.search.mockReturnValue(of(mockFakeContentlets));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage,
                    systemSearchableFields: additionalFields
                })
                .subscribe((results) => {
                    expect(dotContentSearchService.search).toHaveBeenCalledWith(expectedParams);
                    expect(results.length).toBe(2);
                    done();
                });
        });

        it('should handle content with undefined title', (done) => {
            const contentletsWithUndefinedTitle = [
                createFakeContentlet({
                    identifier: 'undefined-title',
                    title: undefined,
                    languageId: 1
                })
            ];

            dotContentSearchService.search.mockReturnValue(of(contentletsWithUndefinedTitle));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage,
                })
                .subscribe((results) => {
                    expect(results[0].title).toBe('undefined-title'); // Should use identifier when title is undefined
                    expect(results[0].language).toEqual(mockLocales[0]);
                    done();
                });
        });
    });
});

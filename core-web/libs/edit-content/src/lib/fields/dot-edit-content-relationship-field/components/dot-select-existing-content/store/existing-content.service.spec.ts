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

import { ExistingContentService } from './existing-content.service';

describe('ExistingContentService', () => {
    let spectator: SpectatorService<ExistingContentService>;
    let dotFieldService: SpyObject<DotFieldService>;
    let dotContentSearchService: SpyObject<DotContentSearchService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    const createService = createServiceFactory({
        service: ExistingContentService,
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
        it('should map fields to columns excluding title, language and modDate', () => {
            const mockFields = [
                {
                    variable: 'field1',
                    name: 'Field 1'
                },
                {
                    variable: 'description',
                    name: 'Description'
                },
                {
                    variable: 'titleField',
                    name: 'Title'
                },
                {
                    variable: 'languageField',
                    name: 'Language'
                },
                {
                    variable: 'modDateField',
                    name: 'ModDate'
                }
            ] as DotCMSContentTypeField[];

            dotFieldService.getFields.mockReturnValue(of(mockFields));

            const contentTypeId = '123';

            // Only fields not in EXCLUDED_COLUMNS should be included
            const expectedColumns = [
                { field: 'field1', header: 'Field 1' },
                { field: 'description', header: 'Description' }
            ];

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual(expectedColumns);
            });
        });

        it('should handle case-insensitive exclusion of reserved columns', () => {
            const mockFields = [
                {
                    variable: 'field1',
                    name: 'Field 1'
                },
                {
                    variable: 'titleField',
                    name: 'TITLE' // uppercase
                },
                {
                    variable: 'languageField',
                    name: 'Language' // mixed case
                },
                {
                    variable: 'modDateField',
                    name: 'moddate' // lowercase
                }
            ] as DotCMSContentTypeField[];

            dotFieldService.getFields.mockReturnValue(of(mockFields));

            const contentTypeId = '123';

            // Only non-excluded fields should be included, regardless of case
            const expectedColumns = [{ field: 'field1', header: 'Field 1' }];

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual(expectedColumns);
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

        it('should filter out fields with missing variable or name', (done) => {
            const mockFields = [
                {
                    variable: 'field1',
                    name: 'Field 1'
                },
                {
                    variable: 'field2' // missing name
                },
                {
                    name: 'Field 3' // missing variable
                },
                {
                    variable: null,
                    name: 'Field 4'
                },
                {
                    variable: 'field5',
                    name: null
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

        const mockResponse = {
            jsonObjectView: {
                contentlets: mockFakeContentlets
            },
            resultsSize: mockFakeContentlets.length
        };

        it('should search contentlets with correct parameters', (done) => {
            const expectedParams: DotContentSearchParams = {
                globalSearch: searchTerm,
                searchableFieldsByContentType,
                page,
                perPage
            };

            dotContentSearchService.search.mockReturnValue(of(mockResponse));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage
                })
                .subscribe((results) => {
                    expect(dotContentSearchService.search).toHaveBeenCalledWith(expectedParams);
                    expect(results.contentlets.length).toBe(2);
                    expect(results.totalResults).toBe(2);

                    // Verify languages were applied to contentlets
                    expect(results.contentlets[0].language).toEqual(mockLocales[0]);
                    expect(results.contentlets[1].language).toEqual(mockLocales[1]);

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

            const responseWithoutTitle = {
                jsonObjectView: {
                    contentlets: contentletsWithoutTitle
                },
                resultsSize: contentletsWithoutTitle.length
            };

            dotContentSearchService.search.mockReturnValue(of(responseWithoutTitle));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage
                })
                .subscribe((results) => {
                    expect(results.contentlets[0].title).toBe('789'); // Should use identifier when title is null
                    expect(results.contentlets[0].language).toEqual(mockLocales[0]);
                    done();
                });
        });

        it('should return empty array when no contentlets are found', (done) => {
            const emptyResponse = {
                jsonObjectView: {
                    contentlets: []
                },
                resultsSize: 0
            };

            dotContentSearchService.search.mockReturnValue(of(emptyResponse));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage
                })
                .subscribe((results) => {
                    expect(dotContentSearchService.search).toHaveBeenCalled();
                    expect(results.contentlets).toEqual([]);
                    expect(results.totalResults).toBe(0);
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

            dotContentSearchService.search.mockReturnValue(of(mockResponse));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: '',
                    page,
                    perPage
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
                    perPage
                })
                .subscribe((results) => {
                    expect(dotContentSearchService.search).toHaveBeenCalled();
                    expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(errorResponse);
                    expect(results.contentlets).toEqual([]);
                    expect(results.totalResults).toBe(0);
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

            const customResponse = {
                jsonObjectView: {
                    contentlets: customContentlets
                },
                resultsSize: customContentlets.length
            };

            dotContentSearchService.search.mockReturnValue(of(customResponse));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage
                })
                .subscribe((results) => {
                    expect(results.contentlets[0].title).toBe('Custom Title');
                    expect(results.contentlets[1].title).toBe('def456'); // Should use identifier for empty title

                    expect(results.contentlets[0].language).toEqual(mockLocales[0]);
                    expect(results.contentlets[1].language).toEqual(mockLocales[1]);

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

            dotContentSearchService.search.mockReturnValue(of(mockResponse));

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
                    expect(results.contentlets.length).toBe(2);
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

            const undefinedTitleResponse = {
                jsonObjectView: {
                    contentlets: contentletsWithUndefinedTitle
                },
                resultsSize: contentletsWithUndefinedTitle.length
            };

            dotContentSearchService.search.mockReturnValue(of(undefinedTitleResponse));

            spectator.service
                .search({
                    contentTypeId,
                    globalSearch: searchTerm,
                    page,
                    perPage
                })
                .subscribe((results) => {
                    expect(results.contentlets[0].title).toBe('undefined-title'); // Should use identifier when title is undefined
                    expect(results.contentlets[0].language).toEqual(mockLocales[0]);
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

        const mockSearchResponse = {
            jsonObjectView: {
                contentlets: mockContentlets
            },
            resultsSize: mockContentlets.length
        };

        beforeEach(() => {
            dotFieldService.getFields.mockReturnValue(of(mockFields));

            dotContentSearchService.search.mockReturnValue(of(mockSearchResponse));
        });

        it('should combine columns and content correctly', (done) => {
            spectator.service
                .getColumnsAndContent(mockContentTypeId)
                .subscribe(([columns, response]) => {
                    // Verify columns
                    expect(columns.length).toBeGreaterThan(0);
                    expect(columns).toEqual(expectedColumns);

                    // Verify relationship items
                    expect(response.contentlets.length).toBe(2);
                    const item0 = {
                        identifier: response.contentlets[0].identifier,
                        title: response.contentlets[0].title,
                        field: response.contentlets[0].field,
                        description: response.contentlets[0].description,
                        language: response.contentlets[0].language
                    };
                    const item1 = {
                        identifier: response.contentlets[1].identifier,
                        title: response.contentlets[1].title,
                        field: response.contentlets[1].field,
                        description: response.contentlets[1].description,
                        language: response.contentlets[1].language
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

                    done();
                });
        });

        it('should handle empty content correctly', (done) => {
            const emptyResponse = {
                jsonObjectView: {
                    contentlets: []
                },
                resultsSize: 0
            };

            dotContentSearchService.search.mockReturnValue(of(emptyResponse));

            spectator.service
                .getColumnsAndContent(mockContentTypeId)
                .subscribe(([columns, response]) => {
                    expect(columns.length).toBeGreaterThan(0);
                    expect(response.contentlets).toEqual([]);
                    expect(response.totalResults).toBe(0);
                    done();
                });
        });

        it('should handle content without title', () => {
            const contentWithoutTitle = [
                createFakeContentlet({
                    identifier: '789',
                    title: null,
                    description: 'Description 3',
                    field: 'Field 3',
                    languageId: mockLocales[0].id,
                    modDate: '2024-01-03T00:00:00Z'
                })
            ];

            const responseWithoutTitle = {
                jsonObjectView: {
                    contentlets: contentWithoutTitle
                },
                resultsSize: contentWithoutTitle.length
            };

            dotContentSearchService.search.mockReturnValue(of(responseWithoutTitle));

            spectator.service.getColumnsAndContent(mockContentTypeId).subscribe(([_, response]) => {
                const item = {
                    identifier: response.contentlets[0].identifier,
                    title: response.contentlets[0].title,
                    field: response.contentlets[0].field,
                    description: response.contentlets[0].description,
                    language: response.contentlets[0].language
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

        it('should handle error case gracefully', () => {
            const errorResponse = new HttpErrorResponse({
                status: 500,
                statusText: 'Server Error'
            });

            dotContentSearchService.search.mockReturnValue(throwError(() => errorResponse));

            spectator.service.getColumnsAndContent(mockContentTypeId).subscribe((result) => {
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(errorResponse);
                expect(result).toBeNull();
            });
        });
    });
});

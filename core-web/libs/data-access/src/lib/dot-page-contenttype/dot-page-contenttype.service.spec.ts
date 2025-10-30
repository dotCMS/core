import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import {
    DotCMSAPIResponse,
    DotCMSBaseTypesContentTypes,
    DotCMSContentType
} from '@dotcms/dotcms-models';

import {
    DotPageContentTypeService,
    DotContentTypeQueryParams,
    DotPageContentTypeQueryParams
} from './dot-page-contenttype.service';

const DEFAULT_PER_PAGE = 30;

const MOCK_CONTENT_TYPE_1: DotCMSContentType = {
    id: 'content-type-1',
    name: 'Blog Post',
    variable: 'blogPost',
    description: 'A blog post content type',
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: 'default',
    owner: 'admin',
    system: false,
    baseType: 'CONTENT'
} as DotCMSContentType;

const MOCK_CONTENT_TYPE_2: DotCMSContentType = {
    id: 'content-type-2',
    name: 'News Article',
    variable: 'newsArticle',
    description: 'A news article content type',
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: 'default',
    owner: 'admin',
    system: false,
    baseType: 'CONTENT'
} as DotCMSContentType;

const MOCK_CONTENT_TYPE_3: DotCMSContentType = {
    id: 'content-type-3',
    name: 'Product',
    variable: 'product',
    description: 'A product content type',
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: 'default',
    owner: 'admin',
    system: false,
    baseType: 'CONTENT'
} as DotCMSContentType;

const MOCK_API_RESPONSE: DotCMSAPIResponse<DotCMSContentType[]> = {
    entity: [MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2, MOCK_CONTENT_TYPE_3],
    pagination: {
        currentPage: 1,
        perPage: DEFAULT_PER_PAGE,
        totalEntries: 3
    },
    errors: [],
    messages: [],
    permissions: [],
    i18nMessagesMap: {}
};

describe('DotPageContentTypeService', () => {
    let spectator: SpectatorService<DotPageContentTypeService>;
    let httpMock: HttpTestingController;

    const createService = createServiceFactory({
        service: DotPageContentTypeService,
        imports: [HttpClientTestingModule]
    });

    beforeEach(() => {
        spectator = createService();
        httpMock = spectator.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('get()', () => {
        const CONTENTTYPE_PAGE_API_URL = '/api/v1/contenttype/page';

        describe('Basic Functionality', () => {
            it('should fetch content types with required parameters', (done) => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page'
                };

                spectator.service.get(params).subscribe((response) => {
                    expect(response.contenttypes).toEqual(MOCK_API_RESPONSE.entity);
                    expect(response.pagination).toEqual(MOCK_API_RESPONSE.pagination);
                    done();
                });

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('pagePathOrId') === '/test-page' &&
                        request.params.get('per_page') === DEFAULT_PER_PAGE.toString()
                    );
                });

                expect(req.request.method).toBe('GET');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should make a GET request to the correct endpoint', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/home'
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_PAGE_API_URL)
                );
                expect(req.request.method).toBe('GET');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should complete the observable after one emission (take(1))', (done) => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page'
                };

                let emissionCount = 0;

                spectator.service.get(params).subscribe({
                    next: () => {
                        emissionCount++;
                    },
                    complete: () => {
                        expect(emissionCount).toBe(1);
                        done();
                    }
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_PAGE_API_URL)
                );
                req.flush(MOCK_API_RESPONSE);
            });
        });

        describe('Query Parameters', () => {
            it('should include language parameter when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    language: 1
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('language') === '1'
                    );
                });

                expect(req.request.params.get('language')).toBe('1');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include filter parameter when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    filter: 'blog'
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('filter') === 'blog'
                    );
                });

                expect(req.request.params.get('filter')).toBe('blog');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include page parameter when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    page: 2
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('page') === '2'
                    );
                });

                expect(req.request.params.get('page')).toBe('2');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include per_page parameter when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    per_page: 50
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('per_page') === '50'
                    );
                });

                expect(req.request.params.get('per_page')).toBe('50');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should use DEFAULT_PER_PAGE when per_page is not provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page'
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('per_page') === DEFAULT_PER_PAGE.toString()
                    );
                });

                expect(req.request.params.get('per_page')).toBe(DEFAULT_PER_PAGE.toString());
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include orderby parameter when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    orderby: 'name'
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('orderby') === 'name'
                    );
                });

                expect(req.request.params.get('orderby')).toBe('name');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include direction parameter when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    direction: 'ASC'
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('direction') === 'ASC'
                    );
                });

                expect(req.request.params.get('direction')).toBe('ASC');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include single type parameter when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    types: [DotCMSBaseTypesContentTypes.CONTENT]
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('type') === DotCMSBaseTypesContentTypes.CONTENT
                    );
                });

                expect(req.request.params.get('type')).toBe(DotCMSBaseTypesContentTypes.CONTENT);
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include multiple type parameters when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    types: [DotCMSBaseTypesContentTypes.CONTENT, DotCMSBaseTypesContentTypes.WIDGET]
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    const types = request.params.getAll('type') ?? [];

                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        types.includes(DotCMSBaseTypesContentTypes.CONTENT) &&
                        types.includes(DotCMSBaseTypesContentTypes.WIDGET)
                    );
                });

                const types = req.request.params.getAll('type');
                expect(types).toHaveLength(2);
                expect(types).toContain(DotCMSBaseTypesContentTypes.CONTENT);
                expect(types).toContain(DotCMSBaseTypesContentTypes.WIDGET);
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include all optional parameters when provided', () => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page',
                    language: 1,
                    filter: 'blog',
                    page: 2,
                    per_page: 50,
                    orderby: 'name',
                    direction: 'DESC',
                    types: [DotCMSBaseTypesContentTypes.CONTENT]
                };

                spectator.service.get(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_PAGE_API_URL &&
                        request.params.get('pagePathOrId') === '/test-page' &&
                        request.params.get('language') === '1' &&
                        request.params.get('filter') === 'blog' &&
                        request.params.get('page') === '2' &&
                        request.params.get('per_page') === '50' &&
                        request.params.get('orderby') === 'name' &&
                        request.params.get('direction') === 'DESC' &&
                        request.params.get('type') === DotCMSBaseTypesContentTypes.CONTENT
                    );
                });

                expect(req.request.params.keys().length).toBeGreaterThan(1);
                req.flush(MOCK_API_RESPONSE);
            });
        });

        describe('Response Mapping', () => {
            it('should correctly map API response to return format', (done) => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page'
                };

                spectator.service.get(params).subscribe((response) => {
                    expect(response).toHaveProperty('contenttypes');
                    expect(response).toHaveProperty('pagination');
                    expect(response.contenttypes).toEqual(MOCK_API_RESPONSE.entity);
                    expect(response.pagination).toEqual(MOCK_API_RESPONSE.pagination);
                    done();
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_PAGE_API_URL)
                );
                req.flush(MOCK_API_RESPONSE);
            });

            it('should return empty array when API returns empty entity', (done) => {
                const emptyResponse: DotCMSAPIResponse<DotCMSContentType[]> = {
                    entity: [],
                    pagination: {
                        currentPage: 1,
                        perPage: DEFAULT_PER_PAGE,
                        totalEntries: 0
                    },
                    errors: [],
                    messages: [],
                    permissions: [],
                    i18nMessagesMap: {}
                };

                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page'
                };

                spectator.service.get(params).subscribe((response) => {
                    expect(response.contenttypes).toEqual([]);
                    expect(response.contenttypes).toHaveLength(0);
                    expect(response.pagination.totalEntries).toBe(0);
                    done();
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_PAGE_API_URL)
                );
                req.flush(emptyResponse);
            });

            it('should preserve pagination metadata', (done) => {
                const customPagination = {
                    currentPage: 3,
                    perPage: 25,
                    totalEntries: 100
                };

                const customResponse: DotCMSAPIResponse<DotCMSContentType[]> = {
                    entity: [MOCK_CONTENT_TYPE_1],
                    pagination: customPagination,
                    errors: [],
                    messages: [],
                    permissions: [],
                    i18nMessagesMap: {}
                };

                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page'
                };

                spectator.service.get(params).subscribe((response) => {
                    expect(response.pagination).toEqual(customPagination);
                    expect(response.pagination.currentPage).toBe(3);
                    expect(response.pagination.perPage).toBe(25);
                    expect(response.pagination.totalEntries).toBe(100);
                    done();
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_PAGE_API_URL)
                );
                req.flush(customResponse);
            });
        });

        describe('Error Handling', () => {
            it('should handle HTTP error responses', (done) => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page'
                };

                spectator.service.get(params).subscribe({
                    next: () => fail('should have failed with 404 error'),
                    error: (error) => {
                        expect(error.status).toBe(404);
                        expect(error.statusText).toBe('Not Found');
                        done();
                    }
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_PAGE_API_URL)
                );
                req.flush('Not Found', { status: 404, statusText: 'Not Found' });
            });

            it('should handle server errors (500)', (done) => {
                const params: DotPageContentTypeQueryParams = {
                    pagePathOrId: '/test-page'
                };

                spectator.service.get(params).subscribe({
                    next: () => fail('should have failed with 500 error'),
                    error: (error) => {
                        expect(error.status).toBe(500);
                        done();
                    }
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_PAGE_API_URL)
                );
                req.flush('Internal Server Error', {
                    status: 500,
                    statusText: 'Internal Server Error'
                });
            });
        });
    });

    describe('getAllContentTypes()', () => {
        const CONTENTTYPE_API_URL = '/api/v1/contenttype';

        describe('Basic Functionality', () => {
            it('should fetch all content types', (done) => {
                const params: DotContentTypeQueryParams = {};

                spectator.service.getAllContentTypes(params).subscribe((response) => {
                    expect(response.contenttypes).toEqual(MOCK_API_RESPONSE.entity);
                    expect(response.pagination).toEqual(MOCK_API_RESPONSE.pagination);
                    done();
                });

                const req = httpMock.expectOne((request) => request.url === CONTENTTYPE_API_URL);

                expect(req.request.method).toBe('GET');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should make a GET request to the correct endpoint', () => {
                const params: DotContentTypeQueryParams = {};

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_API_URL)
                );
                expect(req.request.method).toBe('GET');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should complete the observable after one emission (take(1))', (done) => {
                const params: DotContentTypeQueryParams = {};

                let emissionCount = 0;

                spectator.service.getAllContentTypes(params).subscribe({
                    next: () => {
                        emissionCount++;
                    },
                    complete: () => {
                        expect(emissionCount).toBe(1);
                        done();
                    }
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_API_URL)
                );
                req.flush(MOCK_API_RESPONSE);
            });

            it('should work with empty parameters object', (done) => {
                const params: DotContentTypeQueryParams = {};

                spectator.service.getAllContentTypes(params).subscribe((response) => {
                    expect(response.contenttypes).toBeDefined();
                    expect(response.pagination).toBeDefined();
                    done();
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_API_URL)
                );
                req.flush(MOCK_API_RESPONSE);
            });
        });

        describe('Query Parameters', () => {
            it('should include language parameter when provided', () => {
                const params: DotContentTypeQueryParams = {
                    language: 2
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_API_URL &&
                        request.params.get('language') === '2'
                    );
                });

                expect(req.request.params.get('language')).toBe('2');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include filter parameter when provided', () => {
                const params: DotContentTypeQueryParams = {
                    filter: 'product'
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_API_URL &&
                        request.params.get('filter') === 'product'
                    );
                });

                expect(req.request.params.get('filter')).toBe('product');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include page parameter when provided', () => {
                const params: DotContentTypeQueryParams = {
                    page: 3
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_API_URL && request.params.get('page') === '3'
                    );
                });

                expect(req.request.params.get('page')).toBe('3');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include per_page parameter when provided', () => {
                const params: DotContentTypeQueryParams = {
                    per_page: 100
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_API_URL &&
                        request.params.get('per_page') === '100'
                    );
                });

                expect(req.request.params.get('per_page')).toBe('100');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include orderby parameter when provided', () => {
                const params: DotContentTypeQueryParams = {
                    orderby: 'usage'
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_API_URL &&
                        request.params.get('orderby') === 'usage'
                    );
                });

                expect(req.request.params.get('orderby')).toBe('usage');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include direction parameter when provided', () => {
                const params: DotContentTypeQueryParams = {
                    direction: 'DESC'
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_API_URL &&
                        request.params.get('direction') === 'DESC'
                    );
                });

                expect(req.request.params.get('direction')).toBe('DESC');
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include single type parameter when provided', () => {
                const params: DotContentTypeQueryParams = {
                    types: [DotCMSBaseTypesContentTypes.WIDGET]
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_API_URL &&
                        request.params.get('type') === DotCMSBaseTypesContentTypes.WIDGET
                    );
                });

                expect(req.request.params.get('type')).toBe(DotCMSBaseTypesContentTypes.WIDGET);
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include multiple type parameters when provided', () => {
                const params: DotContentTypeQueryParams = {
                    types: [
                        DotCMSBaseTypesContentTypes.CONTENT,
                        DotCMSBaseTypesContentTypes.WIDGET,
                        DotCMSBaseTypesContentTypes.FORM
                    ]
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    const types = request.params.getAll('type') ?? [];

                    return (
                        request.url === CONTENTTYPE_API_URL &&
                        types.includes(DotCMSBaseTypesContentTypes.CONTENT) &&
                        types.includes(DotCMSBaseTypesContentTypes.WIDGET) &&
                        types.includes(DotCMSBaseTypesContentTypes.FORM)
                    );
                });

                const types = req.request.params.getAll('type');
                expect(types).toHaveLength(3);
                expect(types).toContain(DotCMSBaseTypesContentTypes.CONTENT);
                expect(types).toContain(DotCMSBaseTypesContentTypes.WIDGET);
                expect(types).toContain(DotCMSBaseTypesContentTypes.FORM);
                req.flush(MOCK_API_RESPONSE);
            });

            it('should include all optional parameters when provided', () => {
                const params: DotContentTypeQueryParams = {
                    language: 1,
                    filter: 'test',
                    page: 2,
                    per_page: 50,
                    orderby: 'name',
                    direction: 'ASC',
                    types: [DotCMSBaseTypesContentTypes.CONTENT]
                };

                spectator.service.getAllContentTypes(params).subscribe();

                const req = httpMock.expectOne((request) => {
                    return (
                        request.url === CONTENTTYPE_API_URL &&
                        request.params.get('language') === '1' &&
                        request.params.get('filter') === 'test' &&
                        request.params.get('page') === '2' &&
                        request.params.get('per_page') === '50' &&
                        request.params.get('orderby') === 'name' &&
                        request.params.get('direction') === 'ASC' &&
                        request.params.get('type') === DotCMSBaseTypesContentTypes.CONTENT
                    );
                });

                expect(req.request.params.keys().length).toBeGreaterThan(1);
                req.flush(MOCK_API_RESPONSE);
            });
        });

        describe('Response Mapping', () => {
            it('should correctly map API response to return format', (done) => {
                const params: DotContentTypeQueryParams = {};

                spectator.service.getAllContentTypes(params).subscribe((response) => {
                    expect(response).toHaveProperty('contenttypes');
                    expect(response).toHaveProperty('pagination');
                    expect(response.contenttypes).toEqual(MOCK_API_RESPONSE.entity);
                    expect(response.pagination).toEqual(MOCK_API_RESPONSE.pagination);
                    done();
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_API_URL)
                );
                req.flush(MOCK_API_RESPONSE);
            });

            it('should return empty array when API returns empty entity', (done) => {
                const emptyResponse: DotCMSAPIResponse<DotCMSContentType[]> = {
                    entity: [],
                    pagination: {
                        currentPage: 1,
                        perPage: DEFAULT_PER_PAGE,
                        totalEntries: 0
                    },
                    errors: [],
                    messages: [],
                    permissions: [],
                    i18nMessagesMap: {}
                };

                const params: DotContentTypeQueryParams = {};

                spectator.service.getAllContentTypes(params).subscribe((response) => {
                    expect(response.contenttypes).toEqual([]);
                    expect(response.contenttypes).toHaveLength(0);
                    expect(response.pagination.totalEntries).toBe(0);
                    done();
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_API_URL)
                );
                req.flush(emptyResponse);
            });

            it('should preserve pagination metadata', (done) => {
                const customPagination = {
                    currentPage: 5,
                    perPage: 10,
                    totalEntries: 150
                };

                const customResponse: DotCMSAPIResponse<DotCMSContentType[]> = {
                    entity: [MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2],
                    pagination: customPagination,
                    errors: [],
                    messages: [],
                    permissions: [],
                    i18nMessagesMap: {}
                };

                const params: DotContentTypeQueryParams = {};

                spectator.service.getAllContentTypes(params).subscribe((response) => {
                    expect(response.pagination).toEqual(customPagination);
                    expect(response.pagination.currentPage).toBe(5);
                    expect(response.pagination.perPage).toBe(10);
                    expect(response.pagination.totalEntries).toBe(150);
                    done();
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_API_URL)
                );
                req.flush(customResponse);
            });
        });

        describe('Error Handling', () => {
            it('should handle HTTP error responses', (done) => {
                const params: DotContentTypeQueryParams = {};

                spectator.service.getAllContentTypes(params).subscribe({
                    next: () => fail('should have failed with 403 error'),
                    error: (error) => {
                        expect(error.status).toBe(403);
                        expect(error.statusText).toBe('Forbidden');
                        done();
                    }
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_API_URL)
                );
                req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
            });

            it('should handle server errors (500)', (done) => {
                const params: DotContentTypeQueryParams = {};

                spectator.service.getAllContentTypes(params).subscribe({
                    next: () => fail('should have failed with 500 error'),
                    error: (error) => {
                        expect(error.status).toBe(500);
                        done();
                    }
                });

                const req = httpMock.expectOne((request) =>
                    request.url.includes(CONTENTTYPE_API_URL)
                );
                req.flush('Internal Server Error', {
                    status: 500,
                    statusText: 'Internal Server Error'
                });
            });
        });
    });

    describe('Service Comparison', () => {
        it('should use different endpoints for get() and getAllContentTypes()', () => {
            const pageParams: DotPageContentTypeQueryParams = {
                pagePathOrId: '/test-page'
            };
            const allParams: DotContentTypeQueryParams = {};

            spectator.service.get(pageParams).subscribe();
            spectator.service.getAllContentTypes(allParams).subscribe();

            const pageReq = httpMock.expectOne((request) =>
                request.url.includes('/api/v1/contenttype/page')
            );
            const allReq = httpMock.expectOne((request) => request.url === '/api/v1/contenttype');

            expect(pageReq.request.url).not.toBe(allReq.request.url);
            expect(pageReq.request.url).toContain('/page');
            expect(allReq.request.url).not.toContain('/page');

            pageReq.flush(MOCK_API_RESPONSE);
            allReq.flush(MOCK_API_RESPONSE);
        });

        it('should handle same optional parameters in both methods', () => {
            const sharedParams = {
                language: 1,
                filter: 'test',
                page: 1,
                per_page: 20,
                orderby: 'name' as const,
                direction: 'ASC' as const,
                types: [DotCMSBaseTypesContentTypes.CONTENT]
            };

            const pageParams: DotPageContentTypeQueryParams = {
                pagePathOrId: '/test-page',
                ...sharedParams
            };

            spectator.service.get(pageParams).subscribe();

            const pageReq = httpMock.expectOne((request) =>
                request.url.includes('/api/v1/contenttype/page')
            );

            expect(pageReq.request.params.get('language')).toBe('1');
            expect(pageReq.request.params.get('filter')).toBe('test');
            expect(pageReq.request.params.get('orderby')).toBe('name');

            pageReq.flush(MOCK_API_RESPONSE);

            spectator.service.getAllContentTypes(sharedParams).subscribe();

            const allReq = httpMock.expectOne((request) => request.url === '/api/v1/contenttype');

            expect(allReq.request.params.get('language')).toBe('1');
            expect(allReq.request.params.get('filter')).toBe('test');
            expect(allReq.request.params.get('orderby')).toBe('name');

            allReq.flush(MOCK_API_RESPONSE);
        });
    });

    describe('Edge Cases', () => {
        it('should handle very long filter strings', (done) => {
            const longFilter = 'a'.repeat(500);
            const params: DotPageContentTypeQueryParams = {
                pagePathOrId: '/test-page',
                filter: longFilter
            };

            spectator.service.get(params).subscribe((response) => {
                expect(response.contenttypes).toBeDefined();
                done();
            });

            const req = httpMock.expectOne((request) =>
                request.url.includes('/api/v1/contenttype/page')
            );
            expect(req.request.params.get('filter')).toBe(longFilter);
            req.flush(MOCK_API_RESPONSE);
        });

        it('should handle special characters in pagePathOrId', (done) => {
            const specialPath = '/test-page?query=value&foo=bar';
            const params: DotPageContentTypeQueryParams = {
                pagePathOrId: specialPath
            };

            spectator.service.get(params).subscribe((response) => {
                expect(response.contenttypes).toBeDefined();
                done();
            });

            const req = httpMock.expectOne((request) =>
                request.url.includes('/api/v1/contenttype/page')
            );
            expect(req.request.params.get('pagePathOrId')).toBe(specialPath);
            req.flush(MOCK_API_RESPONSE);
        });

        it('should handle empty types array', () => {
            const params: DotPageContentTypeQueryParams = {
                pagePathOrId: '/test-page',
                types: []
            };

            spectator.service.get(params).subscribe();

            const req = httpMock.expectOne((request) =>
                request.url.includes('/api/v1/contenttype/page')
            );

            const types = req.request.params.getAll('type');
            expect(types).toBeNull();
            req.flush(MOCK_API_RESPONSE);
        });

        it('should handle page parameter value of 0', () => {
            const params: DotPageContentTypeQueryParams = {
                pagePathOrId: '/test-page',
                page: 0
            };

            spectator.service.get(params).subscribe();

            const req = httpMock.expectOne((request) =>
                request.url.includes('/api/v1/contenttype/page')
            );

            expect(req.request.params.get('page')).toBe('1');
            req.flush(MOCK_API_RESPONSE);
        });
    });
});

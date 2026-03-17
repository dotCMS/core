import { HttpRequest } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
    DotCMSContentType,
    DotCopyContentTypeDialogFormFields,
    StructureTypeView,
    DotPagination,
    DotCMSClazz,
    DotContentTypePaginationOptions
} from '@dotcms/dotcms-models';
import { dotcmsContentTypeBasicMock, mockDotContentlet } from '@dotcms/utils-testing';

import { DotContentTypeService } from './dot-content-type.service';

function isRecentContentType(type: StructureTypeView): boolean {
    return type.name.startsWith('RECENT');
}

const responseData: DotCMSContentType[] = [
    {
        icon: 'cloud',
        id: 'a1661fbc-9e84-4c00-bd62-76d633170da3',
        name: 'Product'
    },
    {
        icon: 'alt_route',
        id: '799f176a-d32e-4844-a07c-1b5fcd107578',
        name: 'Blog'
    },
    {
        icon: 'cloud',
        id: '897cf4a9-171a-4204-accb-c1b498c813fe',
        name: 'Contact'
    },
    {
        icon: 'person',
        id: '6044a806-f462-4977-a353-57539eac2a2c',
        name: 'Long name Blog Comment'
    }
] as DotCMSContentType[];

describe('DotContentletService', () => {
    let dotContentTypeService: DotContentTypeService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotContentTypeService]
        });
        dotContentTypeService = TestBed.inject(DotContentTypeService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should call the BE with correct endpoint url and method for getContentTypes()', (done) => {
        dotContentTypeService.getContentTypes({}).subscribe((contentTypes: DotCMSContentType[]) => {
            expect(contentTypes).toEqual(responseData);
            done();
        });
        const req = httpMock.expectOne(
            '/api/v1/contenttype?orderby=name&direction=ASC&per_page=40'
        );
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...responseData] });
    });

    it('should call the BE with correct endpoint url and method for getContentTypes() with multiple types', (done) => {
        dotContentTypeService
            .getContentTypes({
                type: 'contentType,contentTypeB'
            })
            .subscribe((contentTypes: DotCMSContentType[]) => {
                expect(contentTypes).toEqual(responseData);
                done();
            });
        const req = httpMock.expectOne(
            '/api/v1/contenttype?orderby=name&direction=ASC&per_page=40&type=contentType&type=contentTypeB'
        );
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...responseData] });
    });

    it('should get all content types excluding the RECENT ones for getAllContentTypes()', (done) => {
        const types = mockDotContentlet.filter(
            (structure: StructureTypeView) => !isRecentContentType(structure)
        );
        dotContentTypeService.getAllContentTypes().subscribe((structures: StructureTypeView[]) => {
            expect(structures).toEqual(types);
            done();
        });

        const req = httpMock.expectOne('/api/v1/contenttype/basetypes');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...mockDotContentlet] });
    });

    it('should call the BE with correct endpoint url and method for filterContentTypes()', (done) => {
        const body = {
            filter: {
                types: 'contant,blog',
                query: 'blog'
            },
            orderBy: 'name',
            direction: 'ASC',
            perPage: 40
        };

        const {
            filter: { query, types }
        } = body;

        dotContentTypeService
            .filterContentTypes(query, types)
            .subscribe((contentTypes: DotCMSContentType[]) => {
                expect(contentTypes).toEqual(responseData);
                done();
            });
        const req = httpMock.expectOne('/api/v1/contenttype/_filter');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(body);

        req.flush({ entity: [...responseData] });
    });

    describe('getContentTypesWithPagination', () => {
        const page = 20;

        const pagination: DotPagination = {
            currentPage: 1,
            perPage: page,
            totalEntries: 4
        };

        /**
         * Helper function to assert the request is made with the correct parameters
         * @param options The parameters to validate in the request
         * @returns void
         */
        function assertRequest(options: DotContentTypePaginationOptions): void {
            dotContentTypeService
                .getContentTypesWithPagination(options)
                .subscribe(({ contentTypes, pagination: resultPagination }) => {
                    expect(contentTypes).toEqual(responseData);
                    expect(resultPagination).toEqual(pagination);
                });

            const req = httpMock.expectOne((request) => {
                return (
                    request.url === '/api/v1/contenttype' &&
                    request.params.get('orderby') === 'name' &&
                    request.params.get('direction') === 'ASC' &&
                    request.params.get('per_page') === (options.per_page ?? 40).toString() &&
                    request.params.get('page') === options.page?.toString() &&
                    request.params.get('filter') === (options.filter ?? null) &&
                    validateTypeParam(request, options.type) &&
                    request.params.get('ensure') === (options.ensure ?? null)
                );
            });

            expect(req.request.method).toBe('GET');
            req.flush({ entity: [...responseData], pagination });
        }

        /**
         * Helper function to validate type parameters in HTTP requests
         * Handles single types, multiple types (comma-separated), and undefined/null cases
         */
        function validateTypeParam(request: HttpRequest<unknown>, expectedType?: string): boolean {
            const actualTypes = request.params.getAll('type') || [];

            if (expectedType === undefined || expectedType === null) {
                // No type expected - should have no type parameters
                return actualTypes.length === 0;
            }

            if (expectedType.includes(',')) {
                // Multiple types expected - split and compare arrays
                const expectedTypes = expectedType
                    .split(',')
                    .map((t) => t.trim())
                    .sort();
                return JSON.stringify(actualTypes.sort()) === JSON.stringify(expectedTypes);
            } else {
                // Single type expected (including empty string)
                return actualTypes.length === 1 && actualTypes[0] === expectedType;
            }
        }

        it('should call the BE with correct endpoint and map default parameters for getContentTypesWithPagination()', (done) => {
            assertRequest({ page });
            done();
        });

        it('should call the BE with correct endpoint and map pagination for getContentTypesWithPagination()', (done) => {
            const filter = 'blog';
            const type = 'contentType';

            assertRequest({ filter, page, type });
            done();
        });

        it('should call the BE with correct endpoint and map pagination for getContentTypesWithPagination() with multiple types', (done) => {
            const filter = 'blog';
            const type = 'contentType,contentTypeB';

            assertRequest({ filter, page, type });
            done();
        });

        it('should call the BE with correct endpoint and map pagination for getContentTypesWithPagination() with single ensure content type', (done) => {
            const filter = 'blog';
            const type = 'contentType';
            const ensure = 'blog';

            assertRequest({ filter, page, type, ensure });
            done();
        });

        it('should call the BE with correct endpoint and map pagination for getContentTypesWithPagination() with multiple ensure content types', (done) => {
            const filter = 'blog';
            const type = 'contentType';
            const ensure = 'blog,article';

            assertRequest({ filter, page, type, ensure });
            done();
        });
    });

    it('should get url by id for getUrlById()', (done) => {
        const idSearched = 'banner';

        dotContentTypeService.getUrlById(idSearched).subscribe((action: string) => {
            expect(action).toBe(mockDotContentlet[0].types[0].action);
            done();
        });

        const req = httpMock.expectOne('/api/v1/contenttype/basetypes');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...mockDotContentlet] });
    });

    it('should get one content type by id or varName', (done) => {
        const id = '1';
        const contentTypeExpected: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'clazz' as DotCMSClazz,
            defaultType: false,
            fixed: false,
            folder: 'folder',
            host: 'host',
            id: id,
            name: 'content type name',
            owner: 'user',
            system: false
        };

        dotContentTypeService.getContentType(id).subscribe((contentType: DotCMSContentType) => {
            expect(contentType).toBe(contentTypeExpected);
            done();
        });

        const req = httpMock.expectOne(`/api/v1/contenttype/id/${id}`);
        expect(req.request.method).toBe('GET');
        req.flush({ entity: contentTypeExpected });
    });

    describe('getContentTypeWithRender', () => {
        it('should get content type with render by id', (done) => {
            const id = 'a1661fbc-9e84-4c00-bd62-76d633170da3';
            const contentTypeExpected: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: false,
                fixed: false,
                folder: 'folder',
                host: 'host',
                id: id,
                name: 'Product Content Type',
                owner: 'user',
                system: false
            };

            dotContentTypeService
                .getContentTypeWithRender(id)
                .subscribe((contentType: DotCMSContentType) => {
                    expect(contentType).toEqual(contentTypeExpected);
                    expect(contentType.id).toBe(id);
                    done();
                });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${id}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: contentTypeExpected });
        });

        it('should get content type with render by variable name', (done) => {
            const variableName = 'blog-post';
            const contentTypeExpected: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: false,
                fixed: false,
                folder: 'folder',
                host: 'host',
                id: '799f176a-d32e-4844-a07c-1b5fcd107578',
                name: 'Blog Post',
                variable: variableName,
                owner: 'user',
                system: false
            };

            dotContentTypeService
                .getContentTypeWithRender(variableName)
                .subscribe((contentType: DotCMSContentType) => {
                    expect(contentType).toEqual(contentTypeExpected);
                    expect(contentType.variable).toBe(variableName);
                    done();
                });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${variableName}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: contentTypeExpected });
        });

        it('should get content type with render by numeric id', (done) => {
            const numericId = '12345';
            const contentTypeExpected: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: false,
                fixed: false,
                folder: 'folder',
                host: 'host',
                id: numericId,
                name: 'Numeric Content Type',
                owner: 'user',
                system: false
            };

            dotContentTypeService
                .getContentTypeWithRender(numericId)
                .subscribe((contentType: DotCMSContentType) => {
                    expect(contentType).toEqual(contentTypeExpected);
                    expect(contentType.id).toBe(numericId);
                    done();
                });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${numericId}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: contentTypeExpected });
        });

        it('should handle HTTP 404 error when content type not found', (done) => {
            const nonExistentId = 'non-existent-id';
            const errorMessage = 'Content type not found';

            dotContentTypeService.getContentTypeWithRender(nonExistentId).subscribe({
                next: () => {
                    fail('Expected error, but got success');
                    done();
                },
                error: (error) => {
                    expect(error.status).toBe(404);
                    expect(error.statusText).toBe('Not Found');
                    done();
                }
            });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${nonExistentId}`);
            expect(req.request.method).toBe('GET');
            req.flush(errorMessage, { status: 404, statusText: 'Not Found' });
        });

        it('should handle HTTP 500 error when server error occurs', (done) => {
            const id = 'server-error-id';
            const errorMessage = 'Internal Server Error';

            dotContentTypeService.getContentTypeWithRender(id).subscribe({
                next: () => {
                    fail('Expected error, but got success');
                    done();
                },
                error: (error) => {
                    expect(error.status).toBe(500);
                    expect(error.statusText).toBe('Internal Server Error');
                    done();
                }
            });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${id}`);
            expect(req.request.method).toBe('GET');
            req.flush(errorMessage, { status: 500, statusText: 'Internal Server Error' });
        });

        it('should handle network error', (done) => {
            const id = 'network-error-id';
            const networkError = new ErrorEvent('Network error', {
                message: 'Network request failed'
            });

            dotContentTypeService.getContentTypeWithRender(id).subscribe({
                next: () => {
                    fail('Expected error, but got success');
                    done();
                },
                error: (error) => {
                    expect(error.error).toBe(networkError);
                    done();
                }
            });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${id}`);
            expect(req.request.method).toBe('GET');
            req.error(networkError);
        });

        it('should handle empty string idOrVar', (done) => {
            const emptyId = '';
            const contentTypeExpected: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: false,
                fixed: false,
                folder: 'folder',
                host: 'host',
                id: '',
                name: 'Empty ID Content Type',
                owner: 'user',
                system: false
            };

            dotContentTypeService
                .getContentTypeWithRender(emptyId)
                .subscribe((contentType: DotCMSContentType) => {
                    expect(contentType).toEqual(contentTypeExpected);
                    done();
                });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${emptyId}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: contentTypeExpected });
        });

        it('should handle idOrVar with special characters', (done) => {
            const specialId = 'content-type_123.test';
            const contentTypeExpected: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: false,
                fixed: false,
                folder: 'folder',
                host: 'host',
                id: specialId,
                name: 'Special Characters Content Type',
                owner: 'user',
                system: false
            };

            dotContentTypeService
                .getContentTypeWithRender(specialId)
                .subscribe((contentType: DotCMSContentType) => {
                    expect(contentType).toEqual(contentTypeExpected);
                    expect(contentType.id).toBe(specialId);
                    done();
                });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${specialId}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: contentTypeExpected });
        });

        it('should handle idOrVar with URL-encoded characters', (done) => {
            const encodedId = 'content%20type%20with%20spaces';
            const decodedId = 'content type with spaces';
            const contentTypeExpected: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: false,
                fixed: false,
                folder: 'folder',
                host: 'host',
                id: decodedId,
                name: 'URL Encoded Content Type',
                owner: 'user',
                system: false
            };

            dotContentTypeService
                .getContentTypeWithRender(encodedId)
                .subscribe((contentType: DotCMSContentType) => {
                    expect(contentType).toEqual(contentTypeExpected);
                    done();
                });

            const req = httpMock.expectOne((request) => {
                return request.url.includes('/api/v1/contenttype/render/id/');
            });
            expect(req.request.method).toBe('GET');
            req.flush({ entity: contentTypeExpected });
        });

        it('should handle very long idOrVar', (done) => {
            const longId = 'a'.repeat(500);
            const contentTypeExpected: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: false,
                fixed: false,
                folder: 'folder',
                host: 'host',
                id: longId,
                name: 'Long ID Content Type',
                owner: 'user',
                system: false
            };

            dotContentTypeService
                .getContentTypeWithRender(longId)
                .subscribe((contentType: DotCMSContentType) => {
                    expect(contentType).toEqual(contentTypeExpected);
                    expect(contentType.id).toBe(longId);
                    done();
                });

            const req = httpMock.expectOne((request) => {
                return request.url.includes('/api/v1/contenttype/render/id/');
            });
            expect(req.request.method).toBe('GET');
            req.flush({ entity: contentTypeExpected });
        });

        it('should handle HTTP 403 error when access is forbidden', (done) => {
            const id = 'forbidden-id';
            const errorMessage = 'Access forbidden';

            dotContentTypeService.getContentTypeWithRender(id).subscribe({
                next: () => {
                    fail('Expected error, but got success');
                    done();
                },
                error: (error) => {
                    expect(error.status).toBe(403);
                    expect(error.statusText).toBe('Forbidden');
                    done();
                }
            });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${id}`);
            expect(req.request.method).toBe('GET');
            req.flush(errorMessage, { status: 403, statusText: 'Forbidden' });
        });

        it('should handle HTTP 400 error when bad request', (done) => {
            const invalidId = 'invalid@#$%id';
            const errorMessage = 'Bad request';

            dotContentTypeService.getContentTypeWithRender(invalidId).subscribe({
                next: () => {
                    fail('Expected error, but got success');
                    done();
                },
                error: (error) => {
                    expect(error.status).toBe(400);
                    expect(error.statusText).toBe('Bad Request');
                    done();
                }
            });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${invalidId}`);
            expect(req.request.method).toBe('GET');
            req.flush(errorMessage, { status: 400, statusText: 'Bad Request' });
        });

        it('should map response entity correctly', (done) => {
            const id = 'mapping-test-id';
            const responseEntity: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: true,
                fixed: true,
                folder: 'SYSTEM_FOLDER',
                host: 'SYSTEM_HOST',
                id: id,
                name: 'Mapping Test',
                owner: 'system',
                system: true
            };

            dotContentTypeService
                .getContentTypeWithRender(id)
                .subscribe((contentType: DotCMSContentType) => {
                    expect(contentType).toEqual(responseEntity);
                    expect(contentType.id).toBe(id);
                    expect(contentType.name).toBe('Mapping Test');
                    expect(contentType.system).toBe(true);
                    done();
                });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${id}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: responseEntity });
        });

        it('should only take the first emission due to take(1) operator', (done) => {
            const id = 'take-operator-test';
            const contentTypeExpected: DotCMSContentType = {
                ...dotcmsContentTypeBasicMock,
                clazz: 'clazz' as DotCMSClazz,
                defaultType: false,
                fixed: false,
                folder: 'folder',
                host: 'host',
                id: id,
                name: 'Take Operator Test',
                owner: 'user',
                system: false
            };

            let emissionCount = 0;

            dotContentTypeService
                .getContentTypeWithRender(id)
                .subscribe((contentType: DotCMSContentType) => {
                    emissionCount++;
                    expect(contentType).toEqual(contentTypeExpected);
                    expect(emissionCount).toBe(1);
                    done();
                });

            const req = httpMock.expectOne(`/api/v1/contenttype/render/id/${id}`);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: contentTypeExpected });
        });
    });

    it('should save a copy of content type selected', (done) => {
        const variableContentTypeToCopy = 'a1661fbc-9e84-4c00-bd62-76d633170da3';
        const id = '6dd314fe781cd9c3dc346c5d6fc92c90';

        const dialogFormFields: DotCopyContentTypeDialogFormFields = {
            name: 'Cloned Content type',
            variable: 'abcte',
            host: 'host',
            folder: 'folder',
            icon: 'icon'
        };

        const contentTypeExpected: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'clacczz' as DotCMSClazz,
            defaultType: false,
            fixed: false,
            id: id,
            owner: 'user',
            system: false,
            ...dialogFormFields
        };

        dotContentTypeService
            .saveCopyContentType(variableContentTypeToCopy, dialogFormFields)
            .subscribe((contentType: DotCMSContentType) => {
                expect(contentType).toBe(contentTypeExpected);
                done();
            });

        const req = httpMock.expectOne(`/api/v1/contenttype/${variableContentTypeToCopy}/_copy`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(dialogFormFields);

        req.flush({ entity: contentTypeExpected });
    });

    it('should get content by types', (done) => {
        const contenttypeA: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'hello-class-one' as DotCMSClazz,
            defaultType: false,
            fixed: false,
            id: '123',
            owner: 'user',
            system: false
        };

        const contentTypeB: DotCMSContentType = {
            ...contenttypeA,
            clazz: 'hello-class-two' as DotCMSClazz,
            id: '456',
            owner: 'user1'
        };

        dotContentTypeService
            .getByTypes('contentType', 200)
            .subscribe((contentType: DotCMSContentType[]) => {
                expect(contentType).toEqual([contenttypeA, contentTypeB]);
                done();
            });

        const req = httpMock.expectOne('/api/v1/contenttype?per_page=200&type=contentType');
        expect(req.request.method).toBe('GET');

        req.flush({ entity: [contenttypeA, contentTypeB] });
    });

    it('should get content by multiple types', (done) => {
        const contenttypeA: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'hello-class-one' as DotCMSClazz,
            defaultType: false,
            fixed: false,
            id: '123',
            owner: 'user',
            system: false
        };

        const contentTypeB: DotCMSContentType = {
            ...contenttypeA,
            clazz: 'hello-class-two' as DotCMSClazz,
            id: '456',
            owner: 'user1'
        };

        dotContentTypeService
            .getByTypes('contentType,contentTypeB', 200)
            .subscribe((contentType: DotCMSContentType[]) => {
                expect(contentType).toEqual([contenttypeA, contentTypeB]);
                done();
            });

        const req = httpMock.expectOne(
            '/api/v1/contenttype?per_page=200&type=contentType&type=contentTypeB'
        );
        expect(req.request.method).toBe('GET');

        req.flush({ entity: [contenttypeA, contentTypeB] });
    });

    it('should update the content type ', (done) => {
        const id = 'test-id-123';
        const payload = { title: 'Updated Content Type', description: 'Updated description' };
        const contentTypeExpected: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            id,
            description: payload.description
        };

        dotContentTypeService
            .updateContentType(id, payload)
            .subscribe((contentType: DotCMSContentType) => {
                expect(contentType).toEqual(contentTypeExpected);
                done();
            });

        const req = httpMock.expectOne(`/api/v1/contenttype/id/${id}`);
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(payload);
        req.flush({ entity: contentTypeExpected });
    });

    afterEach(() => {
        httpMock.verify();
    });
});

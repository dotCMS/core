import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import {
    DotCMSContentType,
    DotCopyContentTypeDialogFormFields,
    StructureTypeView,
    DotPagination,
    DotCMSClazz
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
    let injector: TestBed;
    let dotContentTypeService: DotContentTypeService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotContentTypeService]
        });
        injector = getTestBed();
        dotContentTypeService = injector.get(DotContentTypeService);
        httpMock = injector.get(HttpTestingController);
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

    it('should call the BE with correct endpoint and map pagination for getContentTypesWithPagination()', (done) => {
        const filter = 'blog';
        const page = 20;
        const type = 'contentType';

        const pagination: DotPagination = {
            currentPage: 1,
            perPage: page,
            totalEntries: 4
        };

        dotContentTypeService
            .getContentTypesWithPagination({ filter, page, type })
            .subscribe(({ contentTypes, pagination: resultPagination }) => {
                expect(contentTypes).toEqual(responseData);
                expect(resultPagination).toEqual(pagination);
                done();
            });

        const req = httpMock.expectOne((request) => {
            return (
                request.url === '/api/v1/contenttype' &&
                request.params.get('filter') === filter &&
                request.params.get('orderby') === 'name' &&
                request.params.get('direction') === 'ASC' &&
                request.params.get('per_page') === page.toString() &&
                request.params.get('type') === type
            );
        });
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...responseData], pagination });
    });

    it('should call the BE with correct endpoint and map pagination for getContentTypesWithPagination() with multiple types', (done) => {
        const filter = 'blog';
        const page = 20;
        const type = 'contentType,contentTypeB';

        const pagination: DotPagination = {
            currentPage: 1,
            perPage: page,
            totalEntries: 4
        };

        dotContentTypeService
            .getContentTypesWithPagination({ filter, page, type })
            .subscribe(({ contentTypes, pagination: resultPagination }) => {
                expect(contentTypes).toEqual(responseData);
                expect(resultPagination).toEqual(pagination);
                done();
            });

        const req = httpMock.expectOne((request) => {
            return (
                request.url === '/api/v1/contenttype' &&
                request.params.get('filter') === filter &&
                request.params.get('orderby') === 'name' &&
                request.params.get('direction') === 'ASC' &&
                request.params.get('per_page') === page.toString()
            );
        });
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...responseData], pagination });
    });

    it('should call the BE with correct endpoint and map pagination for getContentTypesWithPagination() with ensure', (done) => {
        const filter = 'blog';
        const page = 20;
        const type = 'contentType';
        const ensure = 'blog,article';

        const pagination: DotPagination = {
            currentPage: 1,
            perPage: page,
            totalEntries: 4
        };

        dotContentTypeService
            .getContentTypesWithPagination({ filter, page, type, ensure })
            .subscribe(({ contentTypes, pagination: resultPagination }) => {
                expect(contentTypes).toEqual(responseData);
                expect(resultPagination).toEqual(pagination);
                done();
            });

        const req = httpMock.expectOne((request) => {
            return (
                request.url === '/api/v1/contenttype' &&
                request.params.get('filter') === filter &&
                request.params.get('orderby') === 'name' &&
                request.params.get('direction') === 'ASC' &&
                request.params.get('per_page') === page.toString() &&
                request.params.get('type') === type &&
                request.params.get('ensure') === ensure
            );
        });
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...responseData], pagination });
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

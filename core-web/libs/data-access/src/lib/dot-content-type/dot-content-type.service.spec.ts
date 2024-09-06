import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import {
    DotCMSContentType,
    DotCopyContentTypeDialogFormFields,
    StructureTypeView
} from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    mockDotContentlet
} from '@dotcms/utils-testing';

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
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotContentTypeService
            ]
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
            '/api/v1/contenttype?filter=&orderby=name&direction=ASC&per_page=40'
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

        const req = httpMock.expectOne('v1/contenttype/basetypes');
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

    it('should get url by id for getUrlById()', (done) => {
        const idSearched = 'banner';

        dotContentTypeService.getUrlById(idSearched).subscribe((action: string) => {
            expect(action).toBe(mockDotContentlet[0].types[0].action);
            done();
        });

        const req = httpMock.expectOne('v1/contenttype/basetypes');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...mockDotContentlet] });
    });

    it('should get one content type by id or varName', (done) => {
        const id = '1';
        const contentTypeExpected: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'clazz',
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

        const req = httpMock.expectOne(`v1/contenttype/id/${id}`);
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
            clazz: 'clacczz',
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
            clazz: 'hello-class-one',
            defaultType: false,
            fixed: false,
            id: '123',
            owner: 'user',
            system: false
        };

        const contentTypeB: DotCMSContentType = {
            ...contenttypeA,
            clazz: 'hello-class-two',
            id: '456',
            owner: 'user1'
        };

        dotContentTypeService
            .getByTypes('contentType', 200)
            .subscribe((contentType: DotCMSContentType[]) => {
                expect(contentType).toEqual([contenttypeA, contentTypeB]);
                done();
            });

        const req = httpMock.expectOne('/api/v1/contenttype?type=contentType&per_page=200');
        expect(req.request.method).toBe('GET');

        req.flush({ entity: [contenttypeA, contentTypeB] });
    });

    afterEach(() => {
        httpMock.verify();
    });
});

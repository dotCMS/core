import { StructureTypeView } from '@models/contentlet/structure-type-view.model';
import { DotContentTypeService } from './dot-content-type.service';
import { getTestBed, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { mockDotContentlet } from '@tests/dot-contentlet.mock';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotCMSContentType, DotCopyContentTypeDialogFormFields } from '@dotcms/dotcms-models';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';

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

    it('should call the BE with correct endpoint url and method for getContentTypes()', () => {
        dotContentTypeService.getContentTypes({}).subscribe((contentTypes: DotCMSContentType[]) => {
            expect(contentTypes).toEqual(responseData);
        });
        const req = httpMock.expectOne(
            '/api/v1/contenttype?filter=&orderby=name&direction=ASC&per_page=40'
        );
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...responseData] });
    });

    it('should get all content types excluding the RECENT ones for getAllContentTypes()', () => {
        const types = mockDotContentlet.filter(
            (structure: StructureTypeView) => !isRecentContentType(structure)
        );
        dotContentTypeService.getAllContentTypes().subscribe((structures: StructureTypeView[]) => {
            expect(structures).toEqual(types);
        });

        const req = httpMock.expectOne('v1/contenttype/basetypes');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...mockDotContentlet] });
    });

    it('should call the BE with correct endpoint url and method for filterContentTypes()', () => {
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
            });
        const req = httpMock.expectOne('/api/v1/contenttype/_filter');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(body);

        req.flush({ entity: [...responseData] });
    });

    it('should get url by id for getUrlById()', () => {
        const idSearched = 'banner';

        dotContentTypeService.getUrlById(idSearched).subscribe((action: string) => {
            expect(action).toBe(mockDotContentlet[0].types[0].action);
        });

        const req = httpMock.expectOne('v1/contenttype/basetypes');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [...mockDotContentlet] });
    });

    it('should get one content type by id or varName', () => {
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
        });

        const req = httpMock.expectOne(`v1/contenttype/id/${id}`);
        expect(req.request.method).toBe('GET');
        req.flush({ entity: contentTypeExpected });
    });

    it('should save a copy of content type selected', () => {
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
            });

        const req = httpMock.expectOne(`/api/v1/contenttype/${variableContentTypeToCopy}/_copy`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(dialogFormFields);

        req.flush({ entity: contentTypeExpected });
    });

    afterEach(() => {
        httpMock.verify();
    });
});

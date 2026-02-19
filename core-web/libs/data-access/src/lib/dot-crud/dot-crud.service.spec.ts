import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotCrudService } from '.';

describe('CrudService', () => {
    let dotCrudService: DotCrudService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [{ provide: CoreWebService, useClass: CoreWebServiceMock }, DotCrudService]
        });
        dotCrudService = TestBed.inject(DotCrudService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should post data and return an entity', () => {
        const body = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            defaultType: false,
            description: 'This is the content type description',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: '12345-host',
            name: 'A content type',
            owner: 'user.id.1',
            system: false,
            variable: 'aContentType'
        };

        const mockResponse = {
            entity: [
                Object.assign({}, body, {
                    fields: [],
                    iDate: 1495670226000,
                    id: '1234-id-7890-entifier',
                    modDate: 1495670226000,
                    multilingualable: false,
                    system: false,
                    versionable: true
                })
            ]
        };

        dotCrudService.postData('v1/urldemo', body).subscribe((result) => {
            expect(result[0]).toEqual(mockResponse.entity[0]);
        });

        const req = httpMock.expectOne('v1/urldemo');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBe(body);
        req.flush({ entity: mockResponse.entity });
    });

    afterEach(() => {
        httpMock.verify();
    });
});

import { DotEditPageService } from './dot-edit-page.service';
import { DotPageContainer } from '../../../shared/models/dot-page-container/dot-page-container.model';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';

describe('DotEditPageService', () => {
    let injector: TestBed;
    let dotEditPageService: DotEditPageService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotEditPageService
            ]
        });
        injector = getTestBed();
        dotEditPageService = injector.get(DotEditPageService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should do a request for save content', () => {
        const pageId = '1';
        const model: DotPageContainer[] = [
            {
                identifier: '1',
                uuid: '2',
                contentletsId: ['3', '4']
            },
            {
                identifier: '5',
                uuid: '6',
                contentletsId: ['7', '8']
            }
        ];

        dotEditPageService.save(pageId, model).subscribe();

        const req = httpMock.expectOne(`v1/page/${pageId}/content`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBe(model);
        req.flush({});
    });

    it('should do a request for see whatChanged', () => {
        const pageId = '123-456';
        const languageId = '1';

        dotEditPageService.whatChange(pageId, languageId).subscribe();

        const req = httpMock.expectOne(`v1/page/${pageId}/render/versions?langId=${languageId}`);
        expect(req.request.method).toBe('GET');
        req.flush({});
    });

    afterEach(() => {
        httpMock.verify();
    });
});

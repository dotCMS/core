import { DotEditPageService } from './dot-edit-page.service';
import { DotPageContainer } from '../../../portlets/dot-edit-page/shared/models/dot-page-container.model';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

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

    afterEach(() => {
        httpMock.verify();
    });
});

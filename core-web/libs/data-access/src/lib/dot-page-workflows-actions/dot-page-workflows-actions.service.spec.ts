import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { DotCMSPageWorkflowState, DotPageWorkflowsActionsService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { mockWorkflowsActions, dotcmsContentletMock } from '@dotcms/utils-testing';

describe('DotPageWorkflowsActionsService', () => {
    let injector: TestBed;
    let dotPageWorkflowsActionsService: DotPageWorkflowsActionsService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPageWorkflowsActionsService
            ]
        });
        injector = getTestBed();
        dotPageWorkflowsActionsService = injector.inject(DotPageWorkflowsActionsService);
        httpMock = injector.inject(HttpTestingController);
    });

    it('should get actions by Url', () => {
        const host_id = '1';
        const url = '/about-us';
        const language_id = '1';
        const expectedResponse = { actions: [...mockWorkflowsActions], page: dotcmsContentletMock };

        dotPageWorkflowsActionsService
            .getByUrl({ host_id, language_id, url })
            .subscribe((data: DotCMSPageWorkflowState) => {
                expect(data).toEqual(expectedResponse);
            });

        const req = httpMock.expectOne('v1/page/actions');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({
            host_id,
            language_id,
            url
        });
        req.flush({ entity: expectedResponse });
    });
});

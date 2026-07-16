import { HttpClient } from '@angular/common/http';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { mockWorkflowsActions, dotcmsContentletMock } from '@dotcms/utils-testing';

import {
    DotCMSPageWorkflowState,
    DotPageWorkflowsActionsService
} from './dot-page-workflows-actions.service';

describe('DotPageWorkflowsActionsService', () => {
    let injector: TestBed;
    let dotPageWorkflowsActionsService: DotPageWorkflowsActionsService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [HttpClient, DotPageWorkflowsActionsService]
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

        const req = httpMock.expectOne('/api/v1/page/actions');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({
            host_id,
            language_id,
            url
        });
        req.flush({ entity: expectedResponse });
    });
});

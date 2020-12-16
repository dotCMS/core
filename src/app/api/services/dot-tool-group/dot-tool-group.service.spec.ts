import { getTestBed, TestBed } from '@angular/core/testing';

import { DotToolGroupService } from './dot-tool-group.service';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('DotToolGroupService', () => {
    let service: DotToolGroupService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotToolGroupService
            ]
        });
        service = TestBed.inject(DotToolGroupService);
        httpMock = getTestBed().get(HttpTestingController);
    });

    it('should call the API to hide the portlet', () => {
        const url = 'api/v1/toolgroups/gettingstarted/_removefromcurrentuser';
        service.hide('gettingstarted').subscribe();

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('PUT');
    });

    it('should call the API to show the portlet', () => {
        const url = 'api/v1/toolgroups/gettingstarted/_addtocurrentuser';
        service.show('gettingstarted').subscribe();

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('PUT');
    });

    afterEach(() => {
        httpMock.verify();
    });
});

import { getTestBed, TestBed } from '@angular/core/testing';
import { DotVersionableService } from './dot-versionable.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';

const mockResponse = { entity: { inode: '123' } };

describe('DotVersionableService', () => {
    let service: DotVersionableService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotVersionableService
            ]
        });
        service = TestBed.inject(DotVersionableService);
        httpMock = getTestBed().get(HttpTestingController);
    });

    it('should bring back version', () => {
        service.bringBack('123').subscribe((res) => {
            expect(res).toEqual(mockResponse.entity);
        });

        const req = httpMock.expectOne('/api/v1/versionables/123/_bringback');
        expect(req.request.method).toBe('PUT');
        req.flush(mockResponse);
    });
});

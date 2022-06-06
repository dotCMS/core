import { getTestBed, TestBed } from '@angular/core/testing';

import { DotContentletService } from './dot-contentlet.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

const mockResponse = {
    entity: {
        versions: {
            en: ([{ content: 'one' }, { content: 'two' }] as unknown) as DotCMSContentlet[]
        }
    }
};

describe('DotContentletService', () => {
    let service: DotContentletService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotContentletService
            ]
        });
        service = TestBed.inject(DotContentletService);
        httpMock = getTestBed().get(HttpTestingController);
    });

    it('should be created', () => {
        service.getContentletVersions('123', 'en').subscribe((res) => {
            expect(res).toEqual(mockResponse.entity.versions.en);
        });

        const req = httpMock.expectOne('/api/v1/content/versions?identifier=123&groupByLang=1');
        expect(req.request.method).toBe('GET');
        req.flush(mockResponse);
    });
});

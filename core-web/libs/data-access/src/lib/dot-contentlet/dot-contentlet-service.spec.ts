import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotContentletService } from './dot-contentlet.service';

const mockContentletVersionsResponse = {
    entity: {
        versions: {
            en: [{ content: 'one' }, { content: 'two' }] as unknown as DotCMSContentlet[]
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
            expect(res).toEqual(mockContentletVersionsResponse.entity.versions.en);
        });

        const req = httpMock.expectOne('/api/v1/content/versions?identifier=123&groupByLang=1');
        expect(req.request.method).toBe('GET');
        req.flush(mockContentletVersionsResponse);
    });
});

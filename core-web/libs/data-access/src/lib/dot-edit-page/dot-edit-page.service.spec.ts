import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotPageContainer } from '@dotcms/dotcms-models';

import { DotEditPageService } from './dot-edit-page.service';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

describe('DotEditPageService', () => {
    let dotEditPageService: DotEditPageService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotEditPageService,
                DotSessionStorageService
            ]
        });
        dotEditPageService = TestBed.inject(DotEditPageService);
        httpMock = TestBed.inject(HttpTestingController);
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

        const req = httpMock.expectOne(`/api/v1/page/${pageId}/content?variantName=DEFAULT`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBe(model);
        req.flush({});
    });

    it('should do a request for see whatChanged', () => {
        const pageId = '123-456';
        const languageId = '1';

        dotEditPageService.whatChange(pageId, languageId).subscribe();

        const req = httpMock.expectOne(
            `/api/v1/page/${pageId}/render/versions?langId=${languageId}`
        );
        expect(req.request.method).toBe('GET');
        req.flush({});
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('DotEditPageService with variantName', () => {
        it('should do a request for save content with variant', () => {
            window.sessionStorage.setItem('variantName', 'Testing');

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

            const req = httpMock.expectOne(`/api/v1/page/${pageId}/content?variantName=Testing`);
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toBe(model);
            req.flush({});
        });

        afterEach(() => {
            window.sessionStorage.removeItem('variantName');
        });
    });
});

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotTreeNode } from '@dotcms/dotcms-models';

import { DotCopyContentService, DEFAULT_PERSONALIZATION } from './dot-copy-content.service';

describe('DotCopyContentService', () => {
    let service: DotCopyContentService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule]
        });
        httpMock = TestBed.inject(HttpTestingController);
        service = TestBed.inject(DotCopyContentService);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('copyInPage', () => {
        it('should do the request correctly', () => {
            const contentToCopy: DotTreeNode = {
                containerId: '',
                contentId: '',
                pageId: '',
                relationType: '',
                treeOrder: '',
                variantId: '',
                personalization: DEFAULT_PERSONALIZATION
            };

            service.copyInPage(contentToCopy).subscribe();

            const req = httpMock.expectOne(`/api/v1/page/copyContent`);
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual(contentToCopy);
            req.flush({});
        });

        it('should set the DEFAULT_PERSONALIZATION and do the request', () => {
            const contentToCopy: DotTreeNode = {
                containerId: '',
                contentId: '',
                pageId: '',
                relationType: '',
                treeOrder: '',
                variantId: '',
                personalization: ''
            };

            service.copyInPage(contentToCopy).subscribe();

            const req = httpMock.expectOne(`/api/v1/page/copyContent`);
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual({
                ...contentToCopy,
                personalization: DEFAULT_PERSONALIZATION
            });
            req.flush({});
        });
    });
});

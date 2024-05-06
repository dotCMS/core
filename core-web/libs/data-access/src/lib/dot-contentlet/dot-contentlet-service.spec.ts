import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentletService } from './dot-contentlet.service';

const mockContentletVersionsResponse = {
    entity: {
        versions: {
            en: [{ content: 'one' }, { content: 'two' }] as unknown as DotCMSContentlet[]
        }
    }
};

const mockContentletByInodeResponse = {
    entity: {
        archived: false,
        baseType: 'CONTENT',
        caategory: [{ boys: 'Boys' }, { girls: 'Girls' }],
        contentType: 'ContentType1',
        date: 1639548000000,
        dateTime: 1639612800000,
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: '758cb37699eae8500d64acc16ebc468e',
        inode: '18f707db-ebf3-45f8-9b5a-d8bf6a6f383a',
        keyValue: { Colorado: 'snow', 'Costa Rica': 'summer' },
        languageId: 1,
        live: true,
        locked: false,
        modDate: 1639784363639,
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        publishDate: 1639784363639,
        sortOrder: 0,
        stInode: '0121c052881956cd95bfe5dde968ca07',
        text: 'final value',
        time: 104400000,
        title: '758cb37699eae8500d64acc16ebc468e',
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
        working: true
    } as unknown as DotCMSContentlet
};

describe('DotContentletService', () => {
    let service: DotContentletService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotContentletService]
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

    it('should retrieve by inode', () => {
        // Subscribe to the service method
        service
            .getContentletByInode(mockContentletByInodeResponse.entity.inode)
            .subscribe((res) => {
                expect(true).toEqual(mockContentletByInodeResponse.entity !== undefined);
                expect(res).toEqual(mockContentletByInodeResponse.entity);
            });

        // Expect the HTTP request and flush the mock entity as the response
        const req = httpMock.expectOne(
            '/api/v1/content/' + mockContentletByInodeResponse.entity.inode
        );
        expect(req.request.method).toBe('GET');
        req.flush(mockContentletByInodeResponse);
    });
});

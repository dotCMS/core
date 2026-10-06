import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
    DotReportIssueContentlet,
    DotReportIssuePayload,
    DotReportIssueService
} from './dot-report-issue.service';

const MOCK_REPORT_ISSUE_CONTENTLET: DotReportIssueContentlet = {
    archived: false,
    baseType: 'CONTENT',
    contentType: 'Bug',
    folder: 'SYSTEM_FOLDER',
    hasTitleImage: false,
    host: 'host-id',
    hostName: 'dotcms.dev',
    identifier: 'identifier-id',
    inode: 'inode-id',
    languageId: 1,
    live: false,
    locked: false,
    modDate: '1778806040235',
    modUser: 'user-id',
    modUserName: 'Bug Reporter',
    owner: 'user-id',
    sortOrder: 0,
    stInode: 'stInode-id',
    title: '/plugins [1.0.0-SNAPSHOT - Chrome]',
    titleImage: 'screenshot',
    url: '/content.inode-id',
    working: true,
    metadata: {
        browser: 'Chrome',
        url: 'http://localhost:8080/dotAdmin/#/plugins?mId=1a87'
    }
};

describe('DotReportIssueService', () => {
    let service: DotReportIssueService;
    let httpTesting: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotReportIssueService]
        });

        service = TestBed.inject(DotReportIssueService);
        httpTesting = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('should submit report issue multipart payload with description and metadata', () => {
        const payload: DotReportIssuePayload = {
            description: 'Login button is unresponsive',
            metadata: {
                browser: 'Chrome',
                url: 'http://localhost:8080/dotAdmin'
            }
        };

        service.reportIssue(payload).subscribe((response) => {
            expect(response).toEqual(MOCK_REPORT_ISSUE_CONTENTLET);
        });

        const req = httpTesting.expectOne('/api/v1/report-issue');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBeInstanceOf(FormData);
        expect(req.request.body.get('description')).toBe(payload.description);
        expect(req.request.body.get('metadata')).toBe(JSON.stringify(payload.metadata));
        expect(req.request.body.get('anonymous')).toBe('false');
        expect(req.request.body.get('screenshot')).toBeNull();

        req.flush({ entity: MOCK_REPORT_ISSUE_CONTENTLET });
    });

    it('should send anonymous=true when payload anonymous is true', () => {
        const payload: DotReportIssuePayload = {
            description: 'Anonymous report',
            metadata: { browser: 'Chrome' },
            anonymous: true
        };

        service.reportIssue(payload).subscribe();

        const req = httpTesting.expectOne('/api/v1/report-issue');
        expect(req.request.body.get('anonymous')).toBe('true');

        req.flush({ entity: MOCK_REPORT_ISSUE_CONTENTLET });
    });

    it('should include screenshot when provided', () => {
        const screenshot = new File(['image'], 'screenshot.png', { type: 'image/png' });
        const payload: DotReportIssuePayload = {
            description: 'Issue with screenshot',
            metadata: {
                browser: 'Safari'
            },
            screenshot
        };

        service.reportIssue(payload).subscribe();

        const req = httpTesting.expectOne('/api/v1/report-issue');
        expect(req.request.method).toBe('POST');
        expect(req.request.body.get('screenshot')).toBe(screenshot);

        req.flush({ entity: MOCK_REPORT_ISSUE_CONTENTLET });
    });
});

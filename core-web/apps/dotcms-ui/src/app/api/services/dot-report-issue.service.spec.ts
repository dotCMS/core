import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotReportIssuePayload, DotReportIssueService } from './dot-report-issue.service';

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
            expect(response).toEqual('');
        });

        const req = httpTesting.expectOne('/api/v1/report-issue');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBeInstanceOf(FormData);
        expect(req.request.body.get('description')).toBe(payload.description);
        expect(req.request.body.get('metadata')).toBe(JSON.stringify(payload.metadata));
        expect(req.request.body.get('screenshot')).toBeNull();

        req.flush({ entity: '' });
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

        req.flush({ entity: '' });
    });
});

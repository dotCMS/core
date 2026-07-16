import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { FileSelectEvent } from 'primeng/fileupload';

import {
    DotGlobalMessageService,
    DotMessageService,
    DotPropertiesService
} from '@dotcms/data-access';

import { DotReportIssueComponent } from './dot-report-issue.component';

import { DotReportIssueService } from '../../../api/services/dot-report-issue.service';
import { LOCATION_TOKEN } from '../../../providers';

describe('DotReportIssueComponent', () => {
    let spectator: Spectator<DotReportIssueComponent>;
    let component: DotReportIssueComponent;

    const reportIssueMock = jest.fn(() => of(''));
    const successMock = jest.fn();
    const getKeyMock = jest.fn(() => of(true as string | boolean));

    const createComponent = createComponentFactory({
        component: DotReportIssueComponent,
        providers: [
            mockProvider(DotReportIssueService, {
                reportIssue: reportIssueMock
            }),
            mockProvider(DotGlobalMessageService, {
                success: successMock
            }),
            mockProvider(DotMessageService, {
                get: (key: string) => key
            }),
            mockProvider(DotPropertiesService, {
                getKey: getKeyMock
            }),
            {
                provide: LOCATION_TOKEN,
                useValue: {
                    href: 'http://localhost:8080/dotAdmin'
                }
            }
        ]
    });

    beforeEach(() => {
        reportIssueMock.mockReset();
        reportIssueMock.mockReturnValue(of(''));
        successMock.mockReset();
        getKeyMock.mockReset();
        getKeyMock.mockReturnValue(of(true));

        spectator = createComponent();
        spectator.setInput('visible', true);
        component = spectator.component;
    });

    it('should require a non-empty description', () => {
        component.form.get('description')?.setValue('   ');
        component.save();
        spectator.detectChanges();

        expect(component.form.get('description')?.hasError('required')).toBe(true);
        expect(
            document.querySelector('[data-testid="dot-report-issue-description-error"]')
        ).toBeTruthy();
        expect(reportIssueMock).not.toHaveBeenCalled();
    });

    it('should keep submit enabled before a submit attempt when the form is invalid', () => {
        spectator.detectChanges();

        expect(component.form.invalid).toBe(true);
        expect(spectator.query('[data-testid="dot-report-issue-description-error"]')).toBeFalsy();
        expect(
            spectator.query('[data-testid="dot-report-issue-submit-button"]')
        ).not.toBeDisabled();
    });

    it('should reject an invalid screenshot mime type', () => {
        const file = new File(['bad'], 'screenshot.gif', { type: 'image/gif' });
        component.form.get('description')?.setValue('Broken publish button');

        component.onScreenshotSelected({
            files: [file]
        } as FileSelectEvent);
        component.save();
        spectator.detectChanges();

        expect(component.form.get('screenshot')?.hasError('invalidFileType')).toBe(true);
        expect(
            document.querySelector('[data-testid="dot-report-issue-screenshot-error"]')
        ).toBeTruthy();
    });

    it('should reject an oversized screenshot', () => {
        const largeFile = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.png', {
            type: 'image/png'
        });
        component.form.get('description')?.setValue('Broken publish button');

        component.onScreenshotSelected({
            files: [largeFile]
        } as FileSelectEvent);
        component.save();
        spectator.detectChanges();

        expect(component.form.get('screenshot')?.hasError('maxFileSize')).toBe(true);
    });

    it('should remove the selected screenshot', () => {
        const file = new File(['image'], 'screenshot.png', { type: 'image/png' });

        component.onScreenshotSelected({
            files: [file]
        } as FileSelectEvent);

        component.removeScreenshot();

        expect(component.screenshotFile()).toBeNull();
        expect(component.form.get('screenshot')?.value).toBeNull();
        expect(component.form.get('screenshot')?.valid).toBe(true);
    });

    it('should disable submit while request is in flight', () => {
        const response$ = new Subject<unknown>();
        reportIssueMock.mockReturnValue(response$);

        component.form.get('description')?.setValue('Report issue');
        spectator.detectChanges();

        component.save();
        spectator.detectChanges();

        expect(component.isSubmitting()).toBe(true);
        expect(reportIssueMock).toHaveBeenCalledTimes(1);

        response$.next('');
        response$.complete();
    });

    it('should submit a trimmed description, close, and show success message', () => {
        jest.spyOn(component.shutdown, 'emit');

        component.form.get('description')?.setValue('  Broken button on editor  ');
        component.save();

        expect(reportIssueMock).toHaveBeenCalledWith(
            expect.objectContaining({
                description: 'Broken button on editor',
                screenshot: null,
                anonymous: false,
                metadata: expect.objectContaining({
                    browser: window.navigator.userAgent,
                    platform: window.navigator.platform,
                    url: 'http://localhost:8080/dotAdmin',
                    viewport: `${window.innerWidth}x${window.innerHeight}`
                })
            })
        );
        expect(successMock).toHaveBeenCalledWith('report-an-issue.success');
        expect(component.visible()).toBe(false);
        expect(component.shutdown.emit).not.toHaveBeenCalled();
    });

    it('should emit shutdown once when the dialog hides', () => {
        jest.spyOn(component.shutdown, 'emit');

        component.form.get('description')?.setValue('Close me');
        component.requestClose();

        expect(component.visible()).toBe(false);
        expect(component.shutdown.emit).not.toHaveBeenCalled();

        component.onDialogHide();

        expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
    });

    it('should forward the anonymous flag when the checkbox is checked', () => {
        component.form.get('description')?.setValue('Anonymous report');
        component.form.get('anonymous')?.setValue(true);
        component.save();

        expect(reportIssueMock).toHaveBeenCalledWith(
            expect.objectContaining({
                description: 'Anonymous report',
                anonymous: true
            })
        );
    });

    it('should disable the anonymous checkbox and show enforcement hint when operator disallows PII', () => {
        getKeyMock.mockReturnValue(of(false));
        spectator = createComponent();
        spectator.setInput('visible', true);
        component = spectator.component;
        spectator.detectChanges();

        expect(component.form.get('anonymous')?.disabled).toBe(true);
        expect(component.form.get('anonymous')?.value).toBe(true);
        expect(spectator.query('[data-testid="dot-report-issue-anonymous-enforced"]')).toBeTruthy();
    });

    it('should keep the dialog open and preserve values on error', () => {
        const screenshot = new File(['image'], 'screenshot.png', { type: 'image/png' });
        jest.spyOn(component.shutdown, 'emit');
        reportIssueMock.mockReturnValue(
            throwError(() => new HttpErrorResponse({ status: 502, statusText: 'Bad Gateway' }))
        );

        component.form.get('description')?.setValue('Broken publish button');
        component.onScreenshotSelected({
            files: [screenshot]
        } as FileSelectEvent);

        component.save();

        expect(component.isSubmitting()).toBe(false);
        expect(component.form.get('description')?.value).toBe('Broken publish button');
        expect(component.screenshotFile()).toBe(screenshot);
        expect(component.shutdown.emit).not.toHaveBeenCalled();
        expect(component.errorMessage()).toBeTruthy();
    });

    it('should show the fallback message when the backend returns an HTML error page', () => {
        reportIssueMock.mockReturnValue(
            throwError(
                () =>
                    new HttpErrorResponse({
                        status: 404,
                        error: '<!doctype html><html><body>Not Found</body></html>'
                    })
            )
        );

        component.form.get('description')?.setValue('Broken publish button');
        component.save();

        expect(component.isSubmitting()).toBe(false);
        expect(component.errorMessage()).toBe('report-an-issue.error.unavailable');
    });

    it('should surface a backend media type error and stop loading', () => {
        const screenshot = new File(['image'], 'screenshot.png', { type: 'image/png' });
        reportIssueMock.mockReturnValue(
            throwError(
                () =>
                    new HttpErrorResponse({
                        status: 415,
                        error: JSON.stringify({
                            message: 'HTTP 415 Unsupported Media Type'
                        })
                    })
            )
        );

        component.form.get('description')?.setValue('Broken publish button');
        component.onScreenshotSelected({
            files: [screenshot]
        } as FileSelectEvent);

        component.save();
        spectator.detectChanges();

        expect(component.isSubmitting()).toBe(false);
        expect(component.errorMessage()).toBe('HTTP 415 Unsupported Media Type');
        expect(spectator.query('[data-testid="dot-report-issue-error-message"]'))?.toHaveText(
            'HTTP 415 Unsupported Media Type'
        );
    });
});

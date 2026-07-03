import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService, DotPublishingQueueService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueUploadDialogComponent } from './dot-publishing-queue-upload-dialog.component';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

describe('DotPublishingQueueUploadDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueUploadDialogComponent>;
    let dialogRef: jest.Mocked<DynamicDialogRef>;
    let service: jest.Mocked<DotPublishingQueueService>;
    let store: jest.Mocked<{ refresh: jest.Mock }>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueUploadDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueStore, { refresh: jest.fn() }),
            mockProvider(DotPublishingQueueService, {
                uploadBundle: jest
                    .fn()
                    .mockReturnValue(of({ bundleName: 'b.tar.gz', status: 'BUNDLE_REQUESTED' }))
            }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    function bundleFile(name = 'bundle.tar.gz'): File {
        return new File(['x'], name, { type: 'application/gzip' });
    }

    beforeEach(() => {
        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef) as jest.Mocked<DynamicDialogRef>;
        service = spectator.inject(
            DotPublishingQueueService
        ) as jest.Mocked<DotPublishingQueueService>;
        store = spectator.inject(DotPublishingQueueStore) as unknown as jest.Mocked<{
            refresh: jest.Mock;
        }>;
        jest.clearAllMocks();
    });

    describe('file selection', () => {
        it('accepts a .tar.gz file', () => {
            const file = bundleFile('my.tar.gz');
            spectator.component.onFileSelect({ files: [file] } as never);
            expect(spectator.component.selectedFile()).toBe(file);
        });

        it('accepts a .tgz file', () => {
            const file = bundleFile('legacy.tgz');
            spectator.component.onFileSelect({ files: [file] } as never);
            expect(spectator.component.selectedFile()).toBe(file);
        });

        it('rejects files with a non-bundle extension', () => {
            const file = new File(['x'], 'image.png', { type: 'image/png' });
            spectator.component.onFileSelect({ files: [file] } as never);
            expect(spectator.component.selectedFile()).toBeNull();
        });

        it('clears the file (and any previous error) on clear', () => {
            spectator.component.onFileSelect({ files: [bundleFile()] } as never);
            spectator.component['errorMessage'].set('previous error');
            spectator.component.onFileClear();
            expect(spectator.component.selectedFile()).toBeNull();
            expect(spectator.component.errorMessage()).toBeNull();
        });
    });

    describe('submit', () => {
        it('does not call the service when nothing is selected; surfaces a warning instead', () => {
            spectator.component.onSubmit();
            expect(service.uploadBundle).not.toHaveBeenCalled();
            // The button is no longer disabled — clicking without a file must
            // surface the file-required message inline so the user knows why.
            expect(spectator.component.errorMessage()).toBe(
                'publishing-queue.upload.warning.file-required'
            );
        });

        it('clears the file-required warning once a valid bundle is selected', () => {
            spectator.component.onSubmit();
            expect(spectator.component.errorMessage()).toBe(
                'publishing-queue.upload.warning.file-required'
            );
            spectator.component.onFileSelect({ files: [bundleFile()] } as never);
            expect(spectator.component.errorMessage()).toBeNull();
        });

        it('calls service.uploadBundle, refreshes the store, and closes with uploaded:true', () => {
            const file = bundleFile();
            spectator.component.onFileSelect({ files: [file] } as never);
            spectator.component.onSubmit();
            expect(service.uploadBundle).toHaveBeenCalledWith(file);
            expect(store.refresh).toHaveBeenCalled();
            expect(dialogRef.close).toHaveBeenCalledWith({ uploaded: true });
            expect(spectator.component.uploading()).toBe(false);
        });
    });

    describe('error handling (inline, not toast)', () => {
        function makeError(body: unknown, status = 400): HttpErrorResponse {
            return new HttpErrorResponse({ error: body, status, statusText: 'Bad Request' });
        }

        function submitWithError(error: HttpErrorResponse): void {
            (service.uploadBundle as jest.Mock).mockReturnValueOnce(throwError(() => error));
            spectator.component.onFileSelect({ files: [bundleFile()] } as never);
            spectator.component.onSubmit();
        }

        it('surfaces `body.message` inside the dialog (does NOT close)', () => {
            submitWithError(makeError({ message: 'License required to upload' }));
            expect(spectator.component.errorMessage()).toBe('License required to upload');
            expect(dialogRef.close).not.toHaveBeenCalled();
            expect(store.refresh).not.toHaveBeenCalled();
            expect(spectator.component.uploading()).toBe(false);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-upload-error'))?.textContent).toContain(
                'License required to upload'
            );
        });

        it('surfaces the first entry of `body.errors[]`', () => {
            submitWithError(makeError({ errors: [{ message: 'Invalid bundle archive' }] }));
            expect(spectator.component.errorMessage()).toBe('Invalid bundle archive');
        });

        it('surfaces the first entry when the body itself is an array', () => {
            submitWithError(makeError([{ error: 'Unauthorized' }], 401));
            expect(spectator.component.errorMessage()).toBe('Unauthorized');
        });

        it('falls back to a plain-string body', () => {
            submitWithError(makeError('Upload failed: disk full', 500));
            expect(spectator.component.errorMessage()).toBe('Upload failed: disk full');
        });

        it('preserves an empty-string body.message instead of falling back to HTTP status', () => {
            // Regression: `||` chaining would treat '' as falsy and surface the
            // generic HTTP message ("Http failure response for..."). `??` keeps
            // whatever the BE actually said — including intentional empties.
            submitWithError(makeError({ message: '' }));
            expect(spectator.component.errorMessage()).toBe('');
        });

        it('clears the previous error before retrying', () => {
            submitWithError(makeError({ message: 'first error' }));
            expect(spectator.component.errorMessage()).toBe('first error');
            (service.uploadBundle as jest.Mock).mockReturnValueOnce(
                of({ bundleName: 'b.tar.gz', status: 'BUNDLE_REQUESTED' })
            );
            spectator.component.onSubmit();
            expect(spectator.component.errorMessage()).toBeNull();
            expect(dialogRef.close).toHaveBeenCalledWith({ uploaded: true });
        });
    });

    it('cancel closes the dialog without a result', () => {
        spectator.component.onCancel();
        expect(dialogRef.close).toHaveBeenCalledWith();
    });
});

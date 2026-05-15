import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService, DotOsgiService } from '@dotcms/data-access';

import { DotPluginsUploadComponent } from './dot-plugins-upload.component';

const makeFile = (name: string): File =>
    new File(['content'], name, { type: 'application/java-archive' });

describe('DotPluginsUploadComponent', () => {
    let spectator: Spectator<DotPluginsUploadComponent>;
    let component: DotPluginsUploadComponent;
    let dialogRef: DynamicDialogRef;
    let osgiService: DotOsgiService;

    const createComponent = createComponentFactory({
        component: DotPluginsUploadComponent,
        providers: [
            mockProvider(DotMessageService, { get: (key: string, ..._args: string[]) => key }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            mockProvider(DotOsgiService, { uploadBundles: jest.fn().mockReturnValue(of(null)) })
        ],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createComponent();
        component = spectator.component;
        dialogRef = spectator.inject(DynamicDialogRef);
        osgiService = spectator.inject(DotOsgiService);
    });

    describe('onFileSelect', () => {
        it('should store only .jar files from the selection', () => {
            const jarFile = makeFile('plugin.jar');
            component.onFileSelect({ currentFiles: [jarFile, makeFile('readme.txt')] } as never);
            expect(component.selectedFiles()).toEqual([jarFile]);
        });

        it('should store an empty array when no jar files are in the selection', () => {
            component.onFileSelect({ currentFiles: [makeFile('readme.txt')] } as never);
            expect(component.selectedFiles()).toEqual([]);
        });

        it('should clear selection on file clear', () => {
            component.selectedFiles.set([makeFile('plugin.jar')]);
            component.onFileClear();
            expect(component.selectedFiles()).toEqual([]);
        });

        it('should clear the error message when a new file is selected', () => {
            component.errorMessage.set('previous error');
            component.onFileSelect({ currentFiles: [makeFile('plugin.jar')] } as never);
            expect(component.errorMessage()).toBeNull();
        });
    });

    describe('upload', () => {
        it('should not call service when no files are selected', () => {
            component.upload();
            expect(osgiService.uploadBundles).not.toHaveBeenCalled();
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should call uploadBundles and close dialog with true on success', () => {
            const file = makeFile('plugin.jar');
            component.selectedFiles.set([file]);
            component.upload();

            expect(osgiService.uploadBundles).toHaveBeenCalledWith([file]);
            expect(dialogRef.close).toHaveBeenCalledWith(true);
            expect(component.uploading()).toBe(false);
        });

        it('should show inline error and keep dialog open on HTTP error', () => {
            (osgiService.uploadBundles as jest.Mock).mockReturnValue(
                throwError(() => ({ error: { message: 'Upload failed' } }))
            );

            const file = makeFile('plugin.jar');
            component.selectedFiles.set([file]);
            component.upload();

            expect(component.errorMessage()).toBe('Upload failed');
            expect(component.uploading()).toBe(false);
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should set uploading to true while in progress', () => {
            const file = makeFile('plugin.jar');
            component.selectedFiles.set([file]);

            // uploading starts as false
            expect(component.uploading()).toBe(false);

            component.upload();

            // after the synchronous mock resolves, uploading resets
            expect(component.uploading()).toBe(false);
        });
    });

    describe('close', () => {
        it('should close the dialog with null without uploading', () => {
            component.close();
            expect(dialogRef.close).toHaveBeenCalledWith(null);
        });
    });

    describe('selectedFilesSummary', () => {
        it('should return empty string when no files are selected', () => {
            expect(component.selectedFilesSummary()).toBe('');
        });

        it('should return the file name when exactly one file is selected', () => {
            component.selectedFiles.set([makeFile('plugin.jar')]);
            expect(component.selectedFilesSummary()).toBe('plugin.jar');
        });

        it('should return first file name and an "N more" suffix for multiple files', () => {
            component.selectedFiles.set([makeFile('a.jar'), makeFile('b.jar'), makeFile('c.jar')]);
            const summary = component.selectedFilesSummary();
            expect(summary).toContain('a.jar');
            expect(summary).toContain('plugins.upload.and-n-more');
        });
    });
});

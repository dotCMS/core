import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService, DotOsgiService } from '@dotcms/data-access';

import { DotPluginsUploadComponent } from './dot-plugins-upload.component';

const makeFile = (name: string): File =>
    new File(['content'], name, { type: 'application/java-archive' });

describe('DotPluginsUploadComponent', () => {
    let spectator: Spectator<DotPluginsUploadComponent>;
    let component: DotPluginsUploadComponent;
    let osgiService: DotOsgiService;
    let dialogRef: DynamicDialogRef;
    let httpErrorManager: DotHttpErrorManagerService;

    const createComponent = createComponentFactory({
        component: DotPluginsUploadComponent,
        providers: [
            mockProvider(DotOsgiService, {
                uploadBundles: jest.fn().mockReturnValue(of({ entity: {} }))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            mockProvider(DotMessageService, { get: (key: string, ..._args: string[]) => key }),
            mockProvider(DynamicDialogRef, { close: jest.fn() })
        ],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createComponent();
        component = spectator.component;
        osgiService = spectator.inject(DotOsgiService);
        dialogRef = spectator.inject(DynamicDialogRef);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
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
    });

    describe('upload', () => {
        it('should not call the service when no files are selected', () => {
            component.upload();
            expect(osgiService.uploadBundles).not.toHaveBeenCalled();
        });

        it('should upload selected files and close the dialog with true on success', () => {
            const file = makeFile('plugin.jar');
            component.selectedFiles.set([file]);
            component.upload();
            expect(osgiService.uploadBundles).toHaveBeenCalledWith([file]);
            expect(dialogRef.close).toHaveBeenCalledWith(true);
        });

        it('should handle upload error and reset uploading state', () => {
            const error = new Error('Upload failed');
            jest.spyOn(osgiService, 'uploadBundles').mockReturnValue(throwError(error));
            component.selectedFiles.set([makeFile('plugin.jar')]);
            component.upload();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(component.uploading()).toBe(false);
        });
    });

    describe('close', () => {
        it('should close the dialog with false without uploading', () => {
            component.close();
            expect(osgiService.uploadBundles).not.toHaveBeenCalled();
            expect(dialogRef.close).toHaveBeenCalledWith(false);
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

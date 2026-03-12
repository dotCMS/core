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
        spectator = createComponent();
        component = spectator.component;
        osgiService = spectator.inject(DotOsgiService);
        dialogRef = spectator.inject(DynamicDialogRef);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('onFileSelect', () => {
        it('should filter and store only .jar files', () => {
            const jarFile = makeFile('plugin.jar');
            const txtFile = makeFile('readme.txt');
            component.onFileSelect({ files: [jarFile, txtFile] } as never);
            expect(component.selectedFiles()).toEqual([jarFile]);
        });

        it('should set empty array when no jar files match', () => {
            component.onFileSelect({ files: [makeFile('readme.txt')] } as never);
            expect(component.selectedFiles()).toEqual([]);
        });
    });

    describe('onFileClear', () => {
        it('should clear selectedFiles', () => {
            component.selectedFiles.set([makeFile('plugin.jar')]);
            component.onFileClear();
            expect(component.selectedFiles()).toEqual([]);
        });
    });

    describe('upload', () => {
        it('should do nothing when no files are selected', () => {
            component.upload();
            expect(osgiService.uploadBundles).not.toHaveBeenCalled();
        });

        it('should upload files and close dialog with true on success', () => {
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
        it('should close dialog with false', () => {
            component.close();
            expect(dialogRef.close).toHaveBeenCalledWith(false);
        });
    });

    describe('selectedFilesSummary', () => {
        it('should return empty string when no files', () => {
            expect(component.selectedFilesSummary()).toBe('');
        });

        it('should return file name when one file selected', () => {
            component.selectedFiles.set([makeFile('plugin.jar')]);
            expect(component.selectedFilesSummary()).toBe('plugin.jar');
        });

        it('should return summary with "and N more" when multiple files', () => {
            component.selectedFiles.set([makeFile('a.jar'), makeFile('b.jar'), makeFile('c.jar')]);
            expect(component.selectedFilesSummary()).toContain('a.jar');
            expect(component.selectedFilesSummary()).toContain('plugins.upload.and-n-more');
        });
    });
});

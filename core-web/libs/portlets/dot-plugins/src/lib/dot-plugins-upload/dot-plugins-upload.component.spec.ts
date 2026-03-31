import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotPluginsUploadComponent } from './dot-plugins-upload.component';

const makeFile = (name: string): File =>
    new File(['content'], name, { type: 'application/java-archive' });

describe('DotPluginsUploadComponent', () => {
    let spectator: Spectator<DotPluginsUploadComponent>;
    let component: DotPluginsUploadComponent;
    let dialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotPluginsUploadComponent,
        providers: [
            mockProvider(DotMessageService, { get: (key: string, ..._args: string[]) => key }),
            mockProvider(DynamicDialogRef, { close: jest.fn() })
        ],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createComponent();
        component = spectator.component;
        dialogRef = spectator.inject(DynamicDialogRef);
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
        it('should not close the dialog when no files are selected', () => {
            component.upload();
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should close the dialog with the selected files', () => {
            const file = makeFile('plugin.jar');
            component.selectedFiles.set([file]);
            component.upload();
            expect(dialogRef.close).toHaveBeenCalledWith([file]);
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

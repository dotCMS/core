import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent } from 'primeng/fileupload';

import { DotHttpErrorManagerService, DotTagsService } from '@dotcms/data-access';

import { DotTagsImportComponent } from './dot-tags-import.component';

describe('DotTagsImportComponent', () => {
    let spectator: Spectator<DotTagsImportComponent>;
    let component: DotTagsImportComponent;

    const mockFile = new File(['tag1,SYSTEM_HOST'], 'test.csv', { type: 'text/csv' });

    const IMPORT_RESPONSE = {
        entity: { totalRows: 10, successCount: 10, failureCount: 0 }
    };

    const createComponent = createComponentFactory({
        component: DotTagsImportComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
            mockProvider(DotTagsService, {
                importTags: jest.fn().mockReturnValue(of(IMPORT_RESPONSE))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    describe('Initial State', () => {
        it('should have null selectedFile initially', () => {
            expect(component.selectedFile()).toBeNull();
        });

        it('should have importing as false', () => {
            expect(component.importing()).toBe(false);
        });

        it('should have null result initially', () => {
            expect(component.result()).toBeNull();
        });
    });

    describe('onFileSelect', () => {
        it('should set the selected file', () => {
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            expect(component.selectedFile()).toBe(mockFile);
        });

        it('should clear result when new file selected', () => {
            // First, do a successful import to set a result
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();
            expect(component.result()).toEqual(IMPORT_RESPONSE.entity);

            // Select a new file and verify result is cleared
            const newFile = new File(['tag2,SYSTEM_HOST'], 'test2.csv', { type: 'text/csv' });
            component.onFileSelect({ files: [newFile] } as FileSelectEvent);
            expect(component.result()).toBeNull();
            expect(component.selectedFile()).toBe(newFile);
        });
    });

    describe('onFileClear', () => {
        it('should clear file and result', () => {
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            component.onFileClear();
            expect(component.selectedFile()).toBeNull();
            expect(component.result()).toBeNull();
        });
    });

    describe('importFile', () => {
        it('should not call service when no file selected', () => {
            const tagsService = spectator.inject(DotTagsService);
            (tagsService.importTags as jest.Mock).mockClear();
            component.importFile();
            expect(tagsService.importTags).not.toHaveBeenCalled();
        });

        it('should import file successfully', () => {
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(component.result()).toEqual({
                totalRows: 10,
                successCount: 10,
                failureCount: 0
            });
            expect(component.importing()).toBe(false);
        });

        it('should handle import error', () => {
            const tagsService = spectator.inject(DotTagsService);
            const httpErrorManager = spectator.inject(DotHttpErrorManagerService);

            (tagsService.importTags as jest.Mock).mockReturnValue(
                throwError(() => new Error('fail'))
            );

            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(component.importing()).toBe(false);
            expect(component.result()).toBeNull();
        });
    });

    describe('close', () => {
        it('should close with true when imported', () => {
            const ref = spectator.inject(DynamicDialogRef);
            component.close(true);
            expect(ref.close).toHaveBeenCalledWith(true);
        });

        it('should close with false when cancelled', () => {
            const ref = spectator.inject(DynamicDialogRef);
            component.close(false);
            expect(ref.close).toHaveBeenCalledWith(false);
        });
    });

    describe('Template', () => {
        it('should show Import button disabled when no file selected', () => {
            spectator.detectChanges();
            const importBtnHost = spectator.query(byTestId('tag-import-submit-btn'));
            expect(importBtnHost).toBeTruthy();

            const innerButton = importBtnHost!.querySelector('button');
            expect(innerButton).toBeTruthy();
            expect(innerButton!.disabled).toBe(true);

            component.selectedFile.set(mockFile);
            spectator.detectChanges();
            expect(innerButton!.disabled).toBe(false);
        });
    });
});

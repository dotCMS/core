import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent } from 'primeng/fileupload';

import { DotMessageService, DotTagsService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { getDownloadLink } from '@dotcms/utils';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTagsImportComponent } from './dot-tags-import.component';

const downloadClickMock = jest.fn();

jest.mock('@dotcms/utils', () => ({
    getDownloadLink: jest.fn(() => ({
        click: downloadClickMock
    }))
}));

function readBlobAsText(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();

        reader.onload = () => resolve(reader.result as string);
        reader.onerror = () => reject(reader.error);
        reader.readAsText(blob);
    });
}

describe('DotTagsImportComponent', () => {
    let spectator: Spectator<DotTagsImportComponent>;
    let component: DotTagsImportComponent;

    const mockFile = new File(['tag1,SYSTEM_HOST'], 'test.csv', { type: 'text/csv' });

    const IMPORT_RESPONSE = {
        entity: { totalRows: 10, successCount: 10, failureCount: 0, success: true }
    };

    const createComponent = createComponentFactory({
        component: DotTagsImportComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
            mockProvider(DotTagsService, {
                importTags: jest.fn().mockReturnValue(of(IMPORT_RESPONSE))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            {
                provide: GlobalStore,
                useValue: {
                    currentSiteId: jest.fn().mockReturnValue('demo-site-id')
                }
            }
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
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
    });

    describe('onFileSelect', () => {
        it('should set the selected file', () => {
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            expect(component.selectedFile()).toBe(mockFile);
        });

        it('should ignore non-csv files', () => {
            const nonCsv = new File(['x'], 'test.txt', { type: 'text/plain' });
            component.onFileSelect({ files: [nonCsv] } as FileSelectEvent);

            expect(component.selectedFile()).toBeNull();
        });
    });

    describe('onFileClear', () => {
        it('should clear selected file', () => {
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.onFileClear();
            expect(component.selectedFile()).toBeNull();
        });
    });

    describe('importFile', () => {
        it('should not call service when no file selected', () => {
            const tagsService = spectator.inject(DotTagsService);
            (tagsService.importTags as jest.Mock).mockClear();
            component.importFile();
            expect(tagsService.importTags).not.toHaveBeenCalled();
        });

        it('should close dialog with result entity after successful import', () => {
            const ref = spectator.inject(DynamicDialogRef);
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(ref.close).toHaveBeenCalledWith(IMPORT_RESPONSE.entity);
            expect(component.importing()).toBe(false);
        });

        it('should show inline error and keep dialog open on HTTP error', () => {
            const tagsService = spectator.inject(DotTagsService);
            const ref = spectator.inject(DynamicDialogRef);

            (tagsService.importTags as jest.Mock).mockReturnValue(
                throwError(() => ({ error: { message: 'Import failed' } }))
            );

            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(component.errorMessage()).toBe('Import failed');
            expect(component.importing()).toBe(false);
            expect(ref.close).not.toHaveBeenCalled();
        });

        it('should show inline error and keep dialog open when API returns success:false', () => {
            const tagsService = spectator.inject(DotTagsService);
            const ref = spectator.inject(DynamicDialogRef);

            (tagsService.importTags as jest.Mock).mockReturnValue(
                of({ entity: { totalRows: 5, successCount: 0, failureCount: 5, success: false } })
            );

            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(component.errorMessage()).toBeTruthy();
            expect(component.importing()).toBe(false);
            expect(ref.close).not.toHaveBeenCalled();
        });
    });

    describe('close', () => {
        it('should close with null when cancelled', () => {
            const ref = spectator.inject(DynamicDialogRef);
            component.close();
            expect(ref.close).toHaveBeenCalledWith(null);
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

        it('should render Cancel button', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('tag-import-cancel-btn'))).toBeTruthy();
        });

        it('should disable Cancel button while importing', () => {
            spectator.detectChanges();
            const cancelBtnHost = spectator.query(byTestId('tag-import-cancel-btn'));
            const cancelBtn = cancelBtnHost?.querySelector('button');
            expect(cancelBtn?.disabled).toBe(false);

            component.importing.set(true);
            spectator.detectChanges();
            expect(cancelBtn?.disabled).toBe(true);
        });
    });

    describe('downloadTemplate', () => {
        it('should generate a csv template with current site id', async () => {
            component.downloadTemplate();

            expect(getDownloadLink).toHaveBeenCalledWith(
                expect.any(Blob),
                'tags-import-template.csv'
            );
            expect(downloadClickMock).toHaveBeenCalled();

            const [blobArg] = (getDownloadLink as jest.Mock).mock.calls[0];
            const csvText = await readBlobAsText(blobArg as Blob);

            expect(csvText).toContain('"Tag Name","Host ID"');
            expect(csvText).toContain('"Marketing","demo-site-id"');
            expect(csvText).toContain('"News","demo-site-id"');
        });

        it('should fallback to SYSTEM_HOST when current site id is null', async () => {
            const globalStore = spectator.inject(GlobalStore);
            (globalStore.currentSiteId as jest.Mock).mockReturnValue(null);

            component.downloadTemplate();

            expect(getDownloadLink).toHaveBeenCalledWith(
                expect.any(Blob),
                'tags-import-template.csv'
            );
            const [blobArg] = (getDownloadLink as jest.Mock).mock.calls[0];
            const csvText = await readBlobAsText(blobArg as Blob);

            expect(csvText).toContain('"Marketing","SYSTEM_HOST"');
        });
    });
});

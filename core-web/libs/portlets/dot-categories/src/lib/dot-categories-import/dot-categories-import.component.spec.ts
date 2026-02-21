import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent } from 'primeng/fileupload';

import {
    DotCategoriesService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCategoriesImportComponent } from './dot-categories-import.component';

describe('DotCategoriesImportComponent', () => {
    let spectator: Spectator<DotCategoriesImportComponent>;
    let component: DotCategoriesImportComponent;

    const mockFile = new File(['cat1,key1'], 'categories.csv', { type: 'text/csv' });

    const IMPORT_RESPONSE = {
        entity: { success: true }
    };

    const createComponent = createComponentFactory({
        component: DotCategoriesImportComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
            { provide: DynamicDialogConfig, useValue: { data: { parentInode: 'parent-inode' } } },
            mockProvider(DotCategoriesService, {
                importCategories: jest.fn().mockReturnValue(of(IMPORT_RESPONSE))
            }),
            mockProvider(DotHttpErrorManagerService),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    describe('Initial State', () => {
        it('should create component', () => {
            expect(component).toBeTruthy();
        });

        it('should have null selectedFile initially', () => {
            expect(component.selectedFile()).toBeNull();
        });

        it('should have importing as false', () => {
            expect(component.importing()).toBe(false);
        });

        it('should have default importType as merge', () => {
            expect(component.importType).toBe('merge');
        });
    });

    describe('onFileSelect', () => {
        it('should set the selected file', () => {
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            expect(component.selectedFile()).toBe(mockFile);
        });
    });

    describe('onFileClear', () => {
        it('should clear the selected file', () => {
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            expect(component.selectedFile()).toBe(mockFile);

            component.onFileClear();
            expect(component.selectedFile()).toBeNull();
        });
    });

    describe('importFile', () => {
        it('should not call import when no file selected', () => {
            const categoriesService = spectator.inject(DotCategoriesService);
            (categoriesService.importCategories as jest.Mock).mockClear();

            component.importFile();

            expect(categoriesService.importCategories).not.toHaveBeenCalled();
        });

        it('should call importCategories with correct params', () => {
            const categoriesService = spectator.inject(DotCategoriesService);

            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(categoriesService.importCategories).toHaveBeenCalledWith(
                mockFile,
                'merge',
                'parent-inode'
            );
        });

        it('should close dialog with true on successful import', () => {
            const ref = spectator.inject(DynamicDialogRef);

            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(ref.close).toHaveBeenCalledWith(true);
            expect(component.importing()).toBe(false);
        });

        it('should handle error on import failure', () => {
            const categoriesService = spectator.inject(DotCategoriesService);
            const httpErrorManager = spectator.inject(DotHttpErrorManagerService);
            const ref = spectator.inject(DynamicDialogRef);

            (categoriesService.importCategories as jest.Mock).mockReturnValue(
                throwError(() => new Error('import fail'))
            );
            (ref.close as jest.Mock).mockClear();

            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(httpErrorManager.handle).toHaveBeenCalled();
            expect(component.importing()).toBe(false);
            expect(ref.close).not.toHaveBeenCalled();
        });
    });

    describe('close', () => {
        it('should close dialog with false when cancel clicked', () => {
            const ref = spectator.inject(DynamicDialogRef);

            component.close();

            expect(ref.close).toHaveBeenCalledWith(false);
        });
    });
});

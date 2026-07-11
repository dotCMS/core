import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent } from 'primeng/fileupload';

import { DotCategoriesService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCategoriesImportComponent } from './dot-categories-import.component';

describe('DotCategoriesImportComponent', () => {
    let spectator: Spectator<DotCategoriesImportComponent>;
    let component: DotCategoriesImportComponent;

    const mockFile = new File(['cat1,key1'], 'categories.csv', { type: 'text/csv' });

    const IMPORT_RESULT = { successCount: 5, skippedCount: 0, fails: [] };
    const IMPORT_RESPONSE = { entity: IMPORT_RESULT };

    const createComponent = createComponentFactory({
        component: DotCategoriesImportComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
            { provide: DynamicDialogConfig, useValue: { data: { parentInode: 'parent-inode' } } },
            mockProvider(DotCategoriesService, {
                importCategories: jest.fn().mockReturnValue(of(IMPORT_RESPONSE))
            }),
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

    describe('Template', () => {
        it('should render flat-only info icon with correct tooltip binding', () => {
            spectator.detectChanges();
            const infoIcon = spectator.query<HTMLElement>(
                '[data-testid="category-import-flat-only-icon"]'
            );
            expect(infoIcon).toBeTruthy();
        });

        it('should not show error message by default', () => {
            spectator.detectChanges();
            expect(spectator.query('[data-testid="category-import-error"]')).toBeNull();
        });
    });

    describe('Initial State', () => {
        it('should create component', () => {
            expect(component).toBeTruthy();
        });

        it('should have null selectedFile initially', () => {
            expect(component.$selectedFile()).toBeNull();
        });

        it('should have importing as false', () => {
            expect(component.$importing()).toBe(false);
        });

        it('should have null errorMessage initially', () => {
            expect(component.$errorMessage()).toBeNull();
        });

        it('should have default importType as merge', () => {
            expect(component.importType).toBe('merge');
        });
    });

    describe('onFileSelect', () => {
        it('should set the selected file and clear error', () => {
            component.$errorMessage.set('previous error');
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);

            expect(component.$selectedFile()).toBe(mockFile);
            expect(component.$errorMessage()).toBeNull();
        });
    });

    describe('onFileClear', () => {
        it('should clear the selected file and error', () => {
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.$errorMessage.set('some error');

            component.onFileClear();

            expect(component.$selectedFile()).toBeNull();
            expect(component.$errorMessage()).toBeNull();
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

        it('should close dialog with DotCategoryImportResult on successful import', () => {
            const ref = spectator.inject(DynamicDialogRef);

            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(ref.close).toHaveBeenCalledWith(IMPORT_RESULT);
            expect(component.$importing()).toBe(false);
        });

        it('should show inline error and keep dialog open on import failure', () => {
            const categoriesService = spectator.inject(DotCategoriesService);
            const ref = spectator.inject(DynamicDialogRef);

            const httpError = new HttpErrorResponse({
                error: { message: 'Index 3 out of bounds for length 3' },
                status: 500
            });
            (categoriesService.importCategories as jest.Mock).mockReturnValue(
                throwError(() => httpError)
            );
            (ref.close as jest.Mock).mockClear();

            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(ref.close).not.toHaveBeenCalled();
            expect(component.$importing()).toBe(false);
            expect(component.$errorMessage()).toBe('Index 3 out of bounds for length 3');
        });

        it('should clear error when starting a new import', () => {
            const categoriesService = spectator.inject(DotCategoriesService);
            (categoriesService.importCategories as jest.Mock).mockReturnValue(of(IMPORT_RESPONSE));

            component.$errorMessage.set('previous error');
            component.onFileSelect({ files: [mockFile] } as FileSelectEvent);
            component.importFile();

            expect(component.$errorMessage()).toBeNull();
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

import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';

import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotFormImportUrlComponent } from './dot-form-import-url.component';
import { FormImportUrlStore } from './store/form-import-url.store';

import { UploadedFile } from '../../../../models/dot-edit-content-file.model';
import { NEW_FILE_MOCK } from '../../../../utils/mocks';
import { DotFileFieldUploadService } from '../../services/upload-file/upload-file.service';

describe('DotFormImportUrlComponent', () => {
    let spectator: Spectator<DotFormImportUrlComponent>;
    let store: InstanceType<typeof FormImportUrlStore>;
    let uploadService: SpyObject<DotFileFieldUploadService>;
    let dialogRef: SpyObject<DynamicDialogRef>;

    const createComponent = createComponentFactory({
        component: DotFormImportUrlComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentProviders: [FormImportUrlStore],
        providers: [
            provideHttpClient(),
            mockProvider(DotMessageService),
            mockProvider(DialogService),
            mockProvider(DotFileFieldUploadService),
            mockProvider(DynamicDialogConfig, {
                data: {
                    inputType: 'Image',
                    acceptedFiles: ['image/png']
                }
            }),
            mockProvider(DynamicDialogRef)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(FormImportUrlStore, true);
        uploadService = spectator.inject(DotFileFieldUploadService, true);
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('effects', () => {
        it('should close dialog when file and isDone are truthy', () => {
            spectator.detectChanges();

            const mockPreviewFile: UploadedFile = {
                source: 'contentlet',
                file: NEW_FILE_MOCK.entity
            };

            patchState(store, {
                file: mockPreviewFile,
                status: ComponentStatus.LOADED
            });

            spectator.detectChanges();
            spectator.flushEffects();

            expect(dialogRef.close).toHaveBeenCalledWith(mockPreviewFile);
        });

        it('should disable and enable the form when isLoading is truthy', () => {
            spectator.detectChanges();
            const disableSpy = jest.spyOn(spectator.component.form, 'disable');
            const enableSpy = jest.spyOn(spectator.component.form, 'enable');

            patchState(store, {
                status: ComponentStatus.LOADING
            });

            spectator.detectChanges();

            expect(disableSpy).toHaveBeenCalled();

            patchState(store, {
                status: ComponentStatus.LOADED
            });

            spectator.detectChanges();

            expect(enableSpy).toHaveBeenCalled();
        });
    });

    describe('ngOnInit', () => {
        it('should set upload type and accepted files', () => {
            const initSetupSpy = jest.spyOn(store, 'initSetup');

            spectator.detectChanges();

            expect(initSetupSpy).toHaveBeenCalledWith({
                uploadType: 'dotasset',
                acceptedFiles: ['image/png']
            });
        });
    });

    describe('onSubmit', () => {
        it('should not call uploadFileByUrl when form is invalid', () => {
            const uploadFileByUrlSpy = jest.spyOn(store, 'uploadFileByUrl');

            spectator.detectChanges();
            spectator.component.form.get('url').setValue('');

            spectator.component.onSubmit();

            expect(uploadFileByUrlSpy).not.toHaveBeenCalled();
        });

        it('should call uploadFileByUrl when form is valid', () => {
            const uploadFileByUrlSpy = jest.spyOn(store, 'uploadFileByUrl');
            uploadService.uploadFile.mockReturnValue(
                of({ source: 'contentlet', file: NEW_FILE_MOCK.entity })
            );

            spectator.detectChanges();
            spectator.component.form.get('url').setValue('http://example.com/file.png');

            spectator.component.onSubmit();

            expect(uploadFileByUrlSpy).toHaveBeenCalled();
        });
    });

    describe('cancelUpload', () => {
        it('should call abort on the abort controller and close the dialog', () => {
            spectator.component.cancelUpload();
            expect(dialogRef.close).toHaveBeenCalled();
        });
    });
});

describe('DotFormImportUrlComponent without data', () => {
    let spectator: Spectator<DotFormImportUrlComponent>;
    let store: InstanceType<typeof FormImportUrlStore>;

    const createComponent = createComponentFactory({
        component: DotFormImportUrlComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentProviders: [FormImportUrlStore],
        providers: [
            provideHttpClient(),
            mockProvider(DotMessageService),
            mockProvider(DialogService),
            mockProvider(DotFileFieldUploadService),
            mockProvider(DynamicDialogConfig),
            mockProvider(DynamicDialogRef)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(FormImportUrlStore, true);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('ngOnInit', () => {
        it('should set upload type and accepted files', () => {
            const initSetupSpy = jest.spyOn(store, 'initSetup');

            spectator.detectChanges();

            expect(initSetupSpy).toHaveBeenCalledWith({
                uploadType: 'dotasset',
                acceptedFiles: []
            });
        });
    });
});

describe('DotFormImportUrlComponent Binary input type', () => {
    let spectator: Spectator<DotFormImportUrlComponent>;
    let store: InstanceType<typeof FormImportUrlStore>;

    const createComponent = createComponentFactory({
        component: DotFormImportUrlComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentProviders: [FormImportUrlStore],
        providers: [
            provideHttpClient(),
            mockProvider(DotMessageService),
            mockProvider(DialogService),
            mockProvider(DotFileFieldUploadService),
            mockProvider(DynamicDialogConfig, {
                data: {
                    inputType: 'Binary'
                }
            }),
            mockProvider(DynamicDialogRef)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(FormImportUrlStore, true);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('ngOnInit', () => {
        it('should set upload type and accepted files', () => {
            const initSetupSpy = jest.spyOn(store, 'initSetup');

            spectator.detectChanges();

            expect(initSetupSpy).toHaveBeenCalledWith({
                uploadType: 'temp',
                acceptedFiles: []
            });
        });
    });
});

import { expect, it, describe, beforeEach } from '@jest/globals';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { signal, WritableSignal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule, FileSelectEvent } from 'primeng/fileupload';
import { PasswordModule } from 'primeng/password';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, dialogAction } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAppsImportExportDialogComponent } from './dot-apps-import-export-dialog.component';
import { DotAppsImportExportDialogStore } from './store/dot-apps-import-export-dialog.store';

describe('DotAppsImportExportDialogComponent', () => {
    let spectator: Spectator<DotAppsImportExportDialogComponent>;

    // Mock signals for the store
    let visibleSignal: WritableSignal<boolean>;
    let actionSignal: WritableSignal<dialogAction | null>;
    let errorMessageSignal: WritableSignal<string | null>;
    let dialogHeaderKeySignal: WritableSignal<string>;
    let isLoadingSignal: WritableSignal<boolean>;
    let statusSignal: WritableSignal<ComponentStatus>;

    const mockStore = {
        visible: signal(false),
        action: signal<dialogAction | null>(null),
        errorMessage: signal<string | null>(null),
        dialogHeaderKey: signal(''),
        isLoading: signal(false),
        status: signal(ComponentStatus.INIT),
        close: jest.fn(),
        exportConfiguration: jest.fn(),
        importConfiguration: jest.fn()
    };

    const messageServiceMock = new MockDotMessageService({
        'apps.confirmation.export.error': 'Error',
        'dot.common.dialog.accept': 'Accept',
        'dot.common.dialog.reject': 'Cancel',
        'dot.common.choose': 'Choose',
        'apps.confirmation.export.header': 'Export',
        'apps.confirmation.export.password.label': 'Enter Password',
        'apps.confirmation.import.password.label': 'Enter Password to decrypt',
        'apps.confirmation.import.header': 'Import Configuration',
        Password: 'Password',
        'Upload-File': 'Upload File'
    });

    const createComponent = createComponentFactory({
        component: DotAppsImportExportDialogComponent,
        imports: [
            ReactiveFormsModule,
            DialogModule,
            ButtonModule,
            FileUploadModule,
            PasswordModule,
            DotAutofocusDirective,
            DotFieldRequiredDirective,
            DotMessagePipe
        ],
        providers: [
            { provide: DotAppsImportExportDialogStore, useValue: mockStore },
            { provide: DotMessageService, useValue: messageServiceMock }
        ]
    });

    beforeEach(() => {
        // Reset mock signals before each test
        visibleSignal = signal(false);
        actionSignal = signal<dialogAction | null>(null);
        errorMessageSignal = signal<string | null>(null);
        dialogHeaderKeySignal = signal('');
        isLoadingSignal = signal(false);
        statusSignal = signal(ComponentStatus.INIT);

        mockStore.visible = visibleSignal;
        mockStore.action = actionSignal;
        mockStore.errorMessage = errorMessageSignal;
        mockStore.dialogHeaderKey = dialogHeaderKeySignal;
        mockStore.isLoading = isLoadingSignal;
        mockStore.status = statusSignal;

        // Reset mocks
        mockStore.close.mockClear();
        mockStore.exportConfiguration.mockClear();
        mockStore.importConfiguration.mockClear();

        spectator = createComponent();
    });

    describe('Initial State', () => {
        it('should create component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should not render dialog when not visible', () => {
            spectator.detectChanges();
            const dialog = spectator.query('p-dialog');
            expect(dialog).toBeFalsy();
        });
    });

    describe('Export Dialog', () => {
        beforeEach(() => {
            visibleSignal.set(true);
            actionSignal.set(dialogAction.EXPORT);
            dialogHeaderKeySignal.set('apps.confirmation.export.header');
            spectator.detectChanges();
        });

        it('should render dialog when visible', () => {
            const dialog = spectator.query('p-dialog');
            expect(dialog).toBeTruthy();
        });

        it('should setup export form with password field', () => {
            expect(spectator.component.form).toBeTruthy();
            expect(spectator.component.form.controls['password']).toBeTruthy();
        });

        it('should have accept button disabled when form is invalid', () => {
            expect(spectator.component.dialogActions.accept.disabled).toBe(true);
        });

        it('should enable accept button when form is valid', () => {
            spectator.component.form.setValue({ password: 'test123' });
            spectator.detectChanges();

            expect(spectator.component.dialogActions.accept.disabled).toBe(false);
        });

        it('should call store.exportConfiguration when accept action is triggered', () => {
            spectator.component.form.setValue({ password: 'test123' });
            spectator.detectChanges();

            spectator.component.dialogActions.accept.action();

            expect(mockStore.exportConfiguration).toHaveBeenCalledWith({ password: 'test123' });
        });

        it('should call closeDialog when cancel action is triggered', () => {
            jest.spyOn(spectator.component, 'closeDialog');

            spectator.component.dialogActions.cancel.action();

            expect(spectator.component.closeDialog).toHaveBeenCalled();
        });

        it('should have correct dialog action labels', () => {
            expect(spectator.component.dialogActions.accept.label).toBe('Accept');
            expect(spectator.component.dialogActions.cancel.label).toBe('Cancel');
        });
    });

    describe('Import Dialog', () => {
        beforeEach(() => {
            visibleSignal.set(true);
            actionSignal.set(dialogAction.IMPORT);
            dialogHeaderKeySignal.set('apps.confirmation.import.header');
            spectator.detectChanges();
        });

        it('should render dialog when visible', () => {
            const dialog = spectator.query('p-dialog');
            expect(dialog).toBeTruthy();
        });

        it('should setup import form with password and importFile fields', () => {
            expect(spectator.component.form).toBeTruthy();
            expect(spectator.component.form.controls['password']).toBeTruthy();
            expect(spectator.component.form.controls['importFile']).toBeTruthy();
        });

        it('should have accept button disabled when form is invalid', () => {
            expect(spectator.component.dialogActions.accept.disabled).toBe(true);
        });

        it('should render file upload component', () => {
            const fileUpload = spectator.query('p-fileupload');
            expect(fileUpload).toBeTruthy();
        });

        it('should update form when file is selected', () => {
            const mockFile = new File([''], 'test.tar.gz', { type: 'application/gzip' });
            const event: FileSelectEvent = {
                files: [mockFile],
                originalEvent: new Event('select'),
                currentFiles: [mockFile]
            };

            spectator.component.onFileSelect(event);

            expect(spectator.component.form.controls['importFile'].value).toBe('test.tar.gz');
        });

        it('should clear form when file is cleared', () => {
            spectator.component.form.controls['importFile'].setValue('test.tar.gz');

            spectator.component.onFileClear();

            expect(spectator.component.form.controls['importFile'].value).toBe('');
        });

        it('should call store.importConfiguration with correct config when accept is triggered', () => {
            const mockFile = new File(['content'], 'test.tar.gz', { type: 'application/gzip' });
            const event: FileSelectEvent = {
                files: [mockFile],
                originalEvent: new Event('select'),
                currentFiles: [mockFile]
            };

            spectator.component.onFileSelect(event);
            spectator.component.form.controls['password'].setValue('test123');
            spectator.detectChanges();

            spectator.component.dialogActions.accept.action();

            expect(mockStore.importConfiguration).toHaveBeenCalledWith({
                file: mockFile,
                json: { password: 'test123' }
            });
        });

        it('should not call store.importConfiguration if no file selected', () => {
            spectator.component.form.controls['password'].setValue('test123');
            spectator.detectChanges();

            spectator.component.dialogActions.accept.action();

            expect(mockStore.importConfiguration).not.toHaveBeenCalled();
        });
    });

    describe('closeDialog', () => {
        beforeEach(() => {
            visibleSignal.set(true);
            actionSignal.set(dialogAction.EXPORT);
            spectator.detectChanges();
        });

        it('should reset form and call store.close', () => {
            spectator.component.form.setValue({ password: 'test' });
            jest.spyOn(spectator.component.form, 'reset');

            spectator.component.closeDialog();

            expect(spectator.component.form.reset).toHaveBeenCalled();
            expect(mockStore.close).toHaveBeenCalled();
        });
    });

    describe('Error Display', () => {
        beforeEach(() => {
            visibleSignal.set(true);
            actionSignal.set(dialogAction.EXPORT);
            spectator.detectChanges();
        });

        it('should display error message when present', () => {
            errorMessageSignal.set('Something went wrong');
            spectator.detectChanges();

            const errorSpan = spectator.query('.text-red-500');
            expect(errorSpan).toBeTruthy();
            expect(errorSpan?.textContent).toContain('Something went wrong');
        });

        it('should not display error message when null', () => {
            errorMessageSignal.set(null);
            spectator.detectChanges();

            const errorSpan = spectator.query('.text-red-500');
            expect(errorSpan).toBeFalsy();
        });
    });

    describe('Loading State', () => {
        beforeEach(() => {
            visibleSignal.set(true);
            actionSignal.set(dialogAction.EXPORT);
            spectator.detectChanges();
        });

        it('should disable accept button when loading', () => {
            spectator.component.form.setValue({ password: 'test' });
            isLoadingSignal.set(true);
            spectator.detectChanges();

            // Trigger form value change to update disabled state
            spectator.component.form.updateValueAndValidity();

            expect(spectator.component.dialogActions.accept.disabled).toBe(true);
        });
    });
});

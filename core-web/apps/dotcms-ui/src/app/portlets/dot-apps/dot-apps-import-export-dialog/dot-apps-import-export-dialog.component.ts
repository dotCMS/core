import { Component, effect, inject } from '@angular/core';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule, FileSelectEvent } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { DotMessageService } from '@dotcms/data-access';
import { dialogAction, DotDialogActions } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotAppsImportExportDialogStore } from './store/dot-apps-import-export-dialog.store';

@Component({
    selector: 'dot-apps-import-export-dialog',
    templateUrl: './dot-apps-import-export-dialog.component.html',
    styleUrls: ['./dot-apps-import-export-dialog.component.scss'],
    imports: [
        ReactiveFormsModule,
        DialogModule,
        ButtonModule,
        FileUploadModule,
        InputTextModule,
        PasswordModule,
        DotAutofocusDirective,
        DotFieldRequiredDirective,
        DotMessagePipe
    ]
})
export class DotAppsImportExportDialogComponent {
    readonly #store = inject(DotAppsImportExportDialogStore);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #fb = inject(UntypedFormBuilder);

    // Store selectors
    readonly visible = this.#store.visible;
    readonly action = this.#store.action;
    readonly errorMessage = this.#store.errorMessage;
    readonly dialogHeaderKey = this.#store.dialogHeaderKey;
    readonly isLoading = this.#store.isLoading;

    form: UntypedFormGroup;
    dialogActions: DotDialogActions;
    #selectedFile: File | null = null;

    // Effect to react to action changes to setup the form
    actionsEffect = effect(() => {
        const action = this.action();
        if (action) {
            this.setDialogForm(action);
        }
    });

    /**
     * Close the dialog
     */
    closeDialog(): void {
        this.form?.reset();
        this.#selectedFile = null;
        this.#store.close();
    }

    /**
     * Handles file selection from FileUpload component
     */
    onFileSelect(event: FileSelectEvent): void {
        if (event.files && event.files[0]) {
            this.#selectedFile = event.files[0];
            this.form.controls['importFile'].setValue(event.files[0].name);
        }
    }

    /**
     * Handles file removal/clear from FileUpload component
     */
    onFileClear(): void {
        this.#selectedFile = null;
        this.form.controls['importFile'].setValue('');
    }

    /**
     * Sets dialog form based on action Import/Export
     */
    private setDialogForm(action: dialogAction): void {
        if (action === dialogAction.EXPORT) {
            this.form = this.#fb.group({
                password: new UntypedFormControl('', Validators.required)
            });
            this.setExportDialogActions();
        } else if (action === dialogAction.IMPORT) {
            this.form = this.#fb.group({
                password: new UntypedFormControl('', Validators.required),
                importFile: new UntypedFormControl('', Validators.required)
            });
            this.setImportDialogActions();
        }

        this.form.valueChanges.subscribe(() => {
            this.dialogActions = {
                ...this.dialogActions,
                accept: {
                    ...this.dialogActions.accept,
                    disabled: !this.form.valid || this.isLoading()
                }
            };
        });
    }

    private setExportDialogActions(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.#store.exportConfiguration({ password: this.form.value.password });
                },
                label: this.#dotMessageService.get('dot.common.dialog.accept'),
                disabled: true
            },
            cancel: {
                label: this.#dotMessageService.get('dot.common.dialog.reject'),
                action: () => {
                    this.closeDialog();
                }
            }
        };
    }

    private setImportDialogActions(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    if (this.#selectedFile) {
                        this.#store.importConfiguration({
                            file: this.#selectedFile,
                            json: { password: this.form.value.password }
                        });
                    }
                },
                label: this.#dotMessageService.get('dot.common.dialog.accept'),
                disabled: true
            },
            cancel: {
                label: this.#dotMessageService.get('dot.common.dialog.reject'),
                action: () => {
                    this.closeDialog();
                }
            }
        };
    }
}

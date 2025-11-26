import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    OnInit,
    untracked
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe, DotFieldValidationMessageComponent, DotValidators } from '@dotcms/ui';

import { FormImportUrlStore } from './store/form-import-url.store';

import { INPUT_TYPE, INPUT_TYPES } from '../../../../models/dot-edit-content-file.model';

type DialogData = {
    inputType: INPUT_TYPE;
    acceptedFiles: string[];
};

@Component({
    selector: 'dot-form-import-url',
    imports: [
        DotMessagePipe,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        ButtonModule,
        InputTextModule
    ],
    templateUrl: './dot-form-import-url.component.html',
    styleUrls: ['./dot-form-import-url.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [FormImportUrlStore]
})
export class DotFormImportUrlComponent implements OnInit {
    readonly store = inject(FormImportUrlStore);
    readonly #formBuilder = inject(FormBuilder);
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig<DialogData>);
    #abortController: AbortController | null = null;

    readonly form = this.#formBuilder.nonNullable.group({
        url: ['', [Validators.required, DotValidators.url]]
    });

    /**
     * Listens to the `file` and `isDone` signals and closes the dialog once both are truthy.
     * The `file` value is passed as the dialog result.
     */
    constructor() {
        effect(() => {
            const file = this.store.file();
            const isDone = this.store.isDone();

            untracked(() => {
                if (isDone) {
                    this.#dialogRef.close(file);
                }
            });
        });

        effect(() => {
            const isLoading = this.store.isLoading();
            if (isLoading) {
                this.form.disable();
            } else {
                this.form.enable();
            }
        });
    }

    /**
     * Initializes the component by setting the upload type based on the input type
     * of the parent dialog.
     *
     * If the input type is 'Binary', the upload type is set to 'temp', otherwise it's set to 'dotasset'.
     */
    ngOnInit(): void {
        const data = this.#dialogConfig?.data as DialogData;

        this.store.initSetup({
            uploadType: data?.inputType === INPUT_TYPES.Binary ? 'temp' : 'dotasset',
            acceptedFiles: data?.acceptedFiles ?? []
        });
    }

    /**
     * Submits the form, if it's valid, by calling the `uploadFileByUrl` method of the store.
     *
     * @return {void}
     */
    onSubmit(): void {
        if (this.form.invalid) {
            return;
        }

        this.#abortController = new AbortController();

        const { url } = this.form.getRawValue();
        this.store.uploadFileByUrl({
            fileUrl: url,
            abortSignal: this.#abortController.signal
        });
    }

    /**
     * Cancels the upload and closes the dialog.
     *
     * @return {void}
     */
    cancelUpload(): void {
        if (this.#abortController) {
            this.#abortController.abort();
        }

        this.#dialogRef.close();
    }
}

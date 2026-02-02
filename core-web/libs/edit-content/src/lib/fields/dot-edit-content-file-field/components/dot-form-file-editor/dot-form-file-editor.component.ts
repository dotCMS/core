import { MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    OnInit,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';

import { DotMessagePipe, DotFieldValidationMessageComponent } from '@dotcms/ui';

import { FormFileEditorStore } from './store/form-file-editor.store';

import { UploadedFile } from '../../../../models/dot-edit-content-file.model';

type DialogProps = {
    allowFileNameEdit: boolean;
    userMonacoOptions: Partial<MonacoEditorConstructionOptions>;
    uploadedFile: UploadedFile | null;
};

@Component({
    selector: 'dot-form-file-editor',
    imports: [
        DotMessagePipe,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        ButtonModule,
        InputTextModule,
        MonacoEditorModule
    ],
    templateUrl: './dot-form-file-editor.component.html',
    styleUrl: './dot-form-file-editor.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [FormFileEditorStore]
})
export class DotFormFileEditorComponent implements OnInit {
    /**
     * Injects the FormFileEditorStore into the component.
     *
     * @readonly
     * @type {FormFileEditorStore}
     */
    readonly store = inject(FormFileEditorStore);
    /**
     * A private and readonly instance of FormBuilder injected into the component.
     * This instance is used to create and manage forms within the component.
     */
    readonly #formBuilder = inject(FormBuilder);
    /**
     * A reference to the dynamic dialog instance.
     * This is a read-only property that is injected using the `DynamicDialogRef` token.
     */
    readonly #dialogRef = inject(DynamicDialogRef);
    /**
     * A read-only private property that holds the configuration for the dynamic dialog.
     * This configuration is injected using the `DynamicDialogConfig` class with a generic type of `DialogProps`.
     */
    readonly #dialogConfig = inject(DynamicDialogConfig<DialogProps>);

    /**
     * Form group for the file editor component.
     *
     * This form contains the following controls:
     * - `name`: A required string field that must match the pattern of a valid file name (e.g., "filename.extension").
     * - `content`: An optional string field for the file content.
     *
     * @readonly
     */
    readonly form = this.#formBuilder.nonNullable.group({
        name: ['', [Validators.required, Validators.pattern(/^[^.]+\.[^.]+$/)]],
        content: ['']
    });

    /**
     * Reference to the MonacoEditorComponent instance within the view.
     * This is used to interact with the Monaco Editor component in the template.
     *
     * @type {MonacoEditorComponent}
     */
    #editorRef: monaco.editor.IStandaloneCodeEditor | null = null;

    constructor() {
        effect(() => {
            const isUploading = this.store.isUploading();

            if (isUploading) {
                this.#disableEditor();
            } else {
                this.#enableEditor();
            }
        });

        effect(() => {
            const isDone = this.store.isDone();
            const uploadedFile = this.store.uploadedFile();

            untracked(() => {
                if (isDone) {
                    this.#dialogRef.close(uploadedFile);
                }
            });
        });

        this.nameField.valueChanges
            .pipe(
                debounceTime(350),
                distinctUntilChanged(),
                filter((value) => value.length > 0),
                takeUntilDestroyed()
            )
            .subscribe((value) => {
                this.store.setFileName(value);
            });
    }

    /**
     * Initializes the component by extracting data from the dialog configuration
     * and setting up the form and store with the provided values.
     *
     * @returns {void}
     *
     * @memberof DotFormFileEditorComponent
     *
     * @method ngOnInit
     *
     * @description
     * This method is called once the component is initialized. It retrieves the
     * dialog configuration data and initializes the form with the uploaded file
     * if available. It also sets up the store with the provided Monaco editor
     * options, file name edit permission, uploaded file, accepted files, and
     * upload type.
     */
    ngOnInit(): void {
        const data = this.#dialogConfig?.data as DialogProps;
        if (!data) {
            return;
        }

        const { uploadedFile, userMonacoOptions, allowFileNameEdit } = data;

        if (uploadedFile) {
            this.#initValuesForm(uploadedFile);
        }

        this.store.initLoad({
            monacoOptions: userMonacoOptions || {},
            allowFileNameEdit: allowFileNameEdit || true,
            uploadedFile,
            acceptedFiles: [],
            uploadType: 'dotasset'
        });
    }

    /**
     * Handles the form submission event.
     *
     * This method performs the following actions:
     * 1. Checks if the form is invalid. If so, marks the form as dirty and updates its validity status.
     * 2. If the form is valid, retrieves the raw values from the form and triggers the file upload process via the store.
     *
     * @returns {void}
     */
    onSubmit(): void {
        if (this.form.invalid) {
            this.form.markAsDirty();
            this.form.updateValueAndValidity();

            return;
        }

        const values = this.form.getRawValue();
        this.store.uploadFile(values);
    }

    /**
     * Getter for the 'name' field control from the form.
     *
     * @returns The form control associated with the 'name' field.
     */
    get nameField() {
        return this.form.controls.name;
    }

    /**
     * Getter for the 'content' form control.
     *
     * @returns The 'content' form control from the form group.
     */
    get contentField() {
        return this.form.controls.content;
    }

    /**
     * Disables the form and sets the editor to read-only mode.
     *
     * This method disables the form associated with the component and updates the editor's options
     * to make it read-only. It is useful for preventing further user interaction with the form and editor.
     *
     * @private
     */
    #disableEditor() {
        if (!this.#editorRef) {
            return;
        }

        this.form.disable();
        this.#editorRef.updateOptions({ readOnly: true });
    }

    /**
     * Enables the form and sets the editor to be editable.
     *
     * This method performs the following actions:
     * 1. Enables the form associated with this component.
     * 2. Retrieves the editor instance from the `$editorRef` method.
     * 3. Updates the editor options to make it writable (readOnly: false).
     */
    #enableEditor() {
        if (!this.#editorRef) {
            return;
        }

        this.form.enable();
        this.#editorRef.updateOptions({ readOnly: false });
    }

    /**
     * Initializes the form values with the provided uploaded file data.
     *
     * @param {UploadedFile} param0 - The uploaded file object containing source and file information.
     * @param {string} param0.source - The source of the file, which can be 'temp' or another value.
     * @param {File} param0.file - The file object containing file details.
     * @param {string} param0.file.fileName - The name of the file if the source is 'temp'.
     * @param {string} param0.file.title - The title of the file if the source is not 'temp'.
     * @param {string} param0.file.content - The content of the file.
     */
    #initValuesForm({ source, file }: UploadedFile): void {
        this.form.patchValue({
            name: source === 'temp' ? file.fileName : file.title,
            content: file.content
        });
    }

    /**
     * Cancels the current file upload and closes the dialog.
     *
     * @remarks
     * This method is used to terminate the ongoing file upload process and
     * close the associated dialog reference.
     */
    cancelUpload(): void {
        this.#dialogRef.close();
    }

    onEditorInit(editor: monaco.editor.IStandaloneCodeEditor) {
        this.#editorRef = editor;
    }
}

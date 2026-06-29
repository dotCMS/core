import { MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    OnInit,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';

import { DotMessagePipe, DotFieldValidationMessageComponent } from '@dotcms/ui';

import { FormFileEditorStore } from './store/form-file-editor.store';

import { dotVelocityLanguageDefinition } from '../../../../custom-languages/velocity-monaco-language';
import { AvailableLanguageMonaco } from '../../../../models/dot-edit-content-field.constant';
import { UPLOAD_TYPE, UploadedFile } from '../../../../models/dot-edit-content-file.model';

type DialogProps = {
    header?: string;
    allowFileNameEdit: boolean;
    userMonacoOptions: Partial<MonacoEditorConstructionOptions>;
    uploadedFile: UploadedFile | null;
    uploadType?: UPLOAD_TYPE;
    acceptedFiles?: string[];
};

/**
 * Inline `.p-dialog` style props applied when the editor goes full-screen and
 * restored on exit. Overrides PrimeNG's `DynamicDialog` size (set inline via
 * `[ngStyle]`), so it must be applied as inline styles to win.
 */
const FULLSCREEN_DIALOG_STYLE: Record<string, string> = {
    width: '100vw',
    height: '100vh',
    maxWidth: '100vw',
    maxHeight: '100vh',
    borderRadius: '0'
};

/** Eased transition so the dialog grows/shrinks smoothly instead of snapping. */
const DIALOG_SIZE_TRANSITION = 'width 250ms ease, height 250ms ease, border-radius 250ms ease';

@Component({
    selector: 'dot-form-file-editor',
    imports: [
        DotMessagePipe,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        ButtonModule,
        InputTextModule,
        MonacoEditorModule,
        TooltipModule
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

    readonly #document = inject(DOCUMENT);

    // The PrimeNG dialog hosting this editor: injectable because the dialog content
    // is declared inside `<p-dialog>` in DynamicDialog's template, so we sit in the
    // Dialog's element injector. Its `container()` signal is the `.p-dialog` element.
    readonly #dialog = inject(Dialog, { optional: true });

    /** Windowed inline dialog styles saved on entering full-screen, restored on exit. */
    #windowedStyle: Record<string, string> | null = null;

    /** Dialog title, shown in the editor's own header (PrimeNG chrome header is hidden). */
    readonly $header = signal('');

    /** Whether the dialog is expanded to fill the viewport. */
    readonly $isFullscreen = signal(false);

    /** Material Symbol ligature for the full-screen toggle, by current state. */
    readonly $fullscreenIcon = computed(() =>
        this.$isFullscreen() ? 'close_fullscreen' : 'open_in_full'
    );

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
        // The editor owns its dialog, so it owns full-screen too: resize the host
        // `.p-dialog` to fill the viewport whenever `$isFullscreen` flips.
        effect(() => this.#applyFullscreen(this.$isFullscreen()));

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

        const {
            header,
            uploadedFile,
            userMonacoOptions,
            allowFileNameEdit,
            uploadType,
            acceptedFiles
        } = data;

        this.$header.set(header ?? '');

        if (uploadedFile) {
            this.#initValuesForm(uploadedFile);
        }

        this.store.initLoad({
            monacoOptions: userMonacoOptions || {},
            allowFileNameEdit: allowFileNameEdit ?? true,
            uploadedFile,
            acceptedFiles: acceptedFiles ?? [],
            uploadType: uploadType ?? 'dotasset'
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
            name: source === 'temp' ? file.fileName : (file.metaData?.name ?? file.title),
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

    /** Toggles the editor dialog between its windowed size and full-screen. */
    toggleFullscreen(): void {
        this.$isFullscreen.update((on) => !on);
    }

    /**
     * Expands the host dialog to the viewport (or restores it). `DialogService`
     * sets the dialog width/height as inline styles, so we override those inline
     * styles directly — a stylesheet rule can't win against inline without
     * `!important` — and restore the saved values on exit.
     */
    #applyFullscreen(on: boolean): void {
        const dialog = this.#dialog?.container() as HTMLElement | undefined;

        if (!dialog) {
            return;
        }

        // Set the size transition (idempotent) before any toggle, honouring
        // reduced-motion. It lands on the first (windowed) effect run, so the
        // first real toggle already animates.
        dialog.style.transition = this.#prefersReducedMotion() ? '' : DIALOG_SIZE_TRANSITION;

        if (on) {
            this.#windowedStyle ??= Object.fromEntries(
                Object.keys(FULLSCREEN_DIALOG_STYLE).map((prop) => [prop, dialog.style[prop]])
            );
            Object.assign(dialog.style, FULLSCREEN_DIALOG_STYLE);
        } else if (this.#windowedStyle) {
            Object.assign(dialog.style, this.#windowedStyle);
            this.#windowedStyle = null;
        }
    }

    /** Whether the user has requested reduced motion (skips the resize animation). */
    #prefersReducedMotion(): boolean {
        return (
            this.#document.defaultView?.matchMedia?.('(prefers-reduced-motion: reduce)').matches ??
            false
        );
    }

    onEditorInit(editor: monaco.editor.IStandaloneCodeEditor) {
        this.#editorRef = editor;

        // Monaco is now loaded. Register the custom Velocity language so .vtl files get
        // proper highlighting (its Monarch tokens — keyword.velocity, variable.velocity,
        // … — are coloured by the default `vs` theme).
        this.#registerVelocityLanguage();

        // initLoad ran in ngOnInit before Monaco's language registry existed, so the
        // detected language fell back to 'text'. Re-detect it (also fixes the upload
        // mime type)...
        this.store.refreshLanguage();

        // ...and apply it straight to the model so syntax highlighting kicks in.
        // Setting it directly is more reliable than waiting for the [options] binding
        // to flow the change through ngx-monaco-editor's ngOnChanges.
        const model = editor.getModel();

        if (model && typeof monaco !== 'undefined') {
            monaco.editor.setModelLanguage(model, this.store.file().language);
        }
    }

    /**
     * Registers the custom Velocity Monarch language with Monaco, once. Mirrors
     * `DotEditContentMonacoEditorControlComponent` so .vtl files highlight the same
     * way here. Idempotent: skips registration when another editor already added it.
     */
    #registerVelocityLanguage(): void {
        if (typeof monaco === 'undefined') {
            return;
        }

        const alreadyRegistered = monaco.languages
            .getLanguages()
            .some((lang) => lang.id === AvailableLanguageMonaco.Velocity);

        if (alreadyRegistered) {
            return;
        }

        monaco.languages.register({
            id: AvailableLanguageMonaco.Velocity,
            extensions: ['.vtl'],
            mimetypes: ['text/x-velocity']
        });
        monaco.languages.setMonarchTokensProvider(
            AvailableLanguageMonaco.Velocity,
            dotVelocityLanguageDefinition
        );
    }
}

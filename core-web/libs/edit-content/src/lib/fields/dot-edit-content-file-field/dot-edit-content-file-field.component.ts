import {
    ChangeDetectionStrategy,
    Component,
    effect,
    forwardRef,
    inject,
    input,
    OnInit,
    OnDestroy,
    DestroyRef,
    computed
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TooltipModule } from 'primeng/tooltip';

import { filter, map } from 'rxjs/operators';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotGeneratedAIImage } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotAIImagePromptComponent,
    DotSpinnerModule,
    DropZoneFileEvent,
    DropZoneFileValidity
} from '@dotcms/ui';

import { DotFileFieldPreviewComponent } from './components/dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from './components/dot-file-field-ui-message/dot-file-field-ui-message.component';
import { DotFormFileEditorComponent } from './components/dot-form-file-editor/dot-form-file-editor.component';
import { DotFormImportUrlComponent } from './components/dot-form-import-url/dot-form-import-url.component';
import { INPUT_TYPES, UploadedFile } from './models';
import { DotFileFieldUploadService } from './services/upload-file/upload-file.service';
import { FileFieldStore } from './store/file-field.store';
import { getUiMessage } from './utils/messages';

@Component({
    selector: 'dot-edit-content-file-field',
    standalone: true,
    imports: [
        ButtonModule,
        DotMessagePipe,
        DotDropZoneComponent,
        DotAIImagePromptComponent,
        DotSpinnerModule,
        DotFileFieldUiMessageComponent,
        DotFileFieldPreviewComponent,
        DotFormImportUrlComponent,
        TooltipModule
    ],
    providers: [
        DotFileFieldUploadService,
        FileFieldStore,
        DialogService,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentFileFieldComponent)
        }
    ],
    templateUrl: './dot-edit-content-file-field.component.html',
    styleUrls: ['./dot-edit-content-file-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentFileFieldComponent implements ControlValueAccessor, OnInit, OnDestroy {
    /**
     * A readonly instance of the FileFieldStore injected into the component.
     * This store is used to manage the state and actions related to the file field.
     */
    readonly store = inject(FileFieldStore);
    /**
     * A readonly private field that holds an instance of the DialogService.
     * This service is injected using Angular's dependency injection mechanism.
     * It is used to manage dialog interactions within the component.
     */
    readonly #dialogService = inject(DialogService);
    /**
     * A readonly private field that injects the DotMessageService.
     * This service is used for handling message-related functionalities within the component.
     */
    readonly #dotMessageService = inject(DotMessageService);
    /**
     * A readonly private field that holds a reference to the `DestroyRef` service.
     * This service is injected into the component to manage the destruction lifecycle.
     */
    readonly #destroyRef = inject(DestroyRef);
    /**
     * A readonly private field that injects the `DotAiService` service.
     * This service is used to provide AI-related functionalities within the component.
     */
    readonly #dotAiService = inject(DotAiService);
    /**
     * Reference to the dynamic dialog. It can be null if no dialog is currently open.
     *
     * @type {DynamicDialogRef | null}
     */
    #dialogRef: DynamicDialogRef | null = null;
    /**
     * DotCMS Content Type Field
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Signal indicating whether the AI plugin is installed.
     *
     * This signal is initialized with a boolean value indicating the installation status
     * of the AI plugin. It uses the `checkPluginInstallation` method from the `#dotAiService`
     * to determine the initial state.
     *
     * @type {boolean}
     * @default false
     */
    $isAIPluginInstalled = toSignal(this.#dotAiService.checkPluginInstallation(), {
        initialValue: false
    });
    /**
     * Computed property that returns the tooltip text for the AI button.
     * If the AI plugin is not installed, it retrieves the tooltip message from the dotMessageService.
     * Otherwise, it returns null.
     *
     * @returns {string | null} The tooltip text for the AI button or null if the AI plugin is installed.
     */
    $tooltipTextAIBtn = computed(() => {
        const isAIPluginInstalled = this.$isAIPluginInstalled();
        if (!isAIPluginInstalled) {
            return this.#dotMessageService.get('dot.file.field.action.generate.with.tooltip');
        }

        return '';
    });

    private onChange: (value: string) => void;
    private onTouched: () => void;

    constructor() {
        effect(() => {
            if (!this.onChange && !this.onTouched) {
                return;
            }

            const value = this.store.value();
            this.onChange(value);
            this.onTouched();
        });
    }

    /**
     * OnInit lifecycle hook.
     *
     * Initialize the store with the content type field data.
     *
     * @memberof DotEditContentFileFieldComponent
     */
    ngOnInit() {
        const field = this.$field();

        this.store.initLoad({
            fieldVariable: field.variable,
            inputType: field.fieldType as INPUT_TYPES
        });
    }

    /**
     * Set the value of the field.
     * If the value is empty, nothing happens.
     * If the value is not empty, the store is called to get the asset data.
     *
     * @param value the value to set
     */
    writeValue(value: string): void {
        if (!value) {
            return;
        }

        this.store.getAssetData(value);
    }
    /**
     * Registers a callback function that is called when the control's value changes in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that is called when the control is marked as touched in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    /**
     * Handle file drop event.
     *
     * If the file is invalid, show an error message.
     * If the file is valid, call the store to handle the upload file.
     *
     * @param {DropZoneFileEvent} { validity, file }
     *
     * @return {void}
     */
    handleFileDrop({ validity, file }: DropZoneFileEvent): void {
        if (!file) {
            return;
        }

        if (!validity.valid) {
            this.#handleFileDropError(validity);

            return;
        }

        this.store.handleUploadFile(file);
    }

    /**
     * Handles the file input change event.
     *
     * If the file is empty, nothing happens.
     * If the file is not empty, the store is called to handle the upload file.
     *
     * @param files The file list from the input change event.
     *
     * @return {void}
     */
    fileSelected(files: FileList | null) {
        if (!files || files.length === 0) {
            return;
        }

        const file = files[0];

        if (!file) {
            return;
        }

        this.store.handleUploadFile(file);
    }

    /**
     * Handles the file drop error event.
     *
     * Gets the first error type from the validity and gets the corresponding UI message.
     * Sets the UI message in the store.
     *
     * @param {DropZoneFileValidity} validity The validity object with the error type.
     *
     * @return {void}
     */
    #handleFileDropError({ errorsType }: DropZoneFileValidity): void {
        const errorType = errorsType[0];
        const uiMessage = getUiMessage(errorType);
        this.store.setUIMessage(uiMessage);
    }

    /**
     * Shows the import from URL dialog.
     *
     * Opens the dialog with the `DotFormImportUrlComponent` component
     * and passes the field type as data to the component.
     *
     * When the dialog is closed, gets the uploaded file from the component
     * and sets it as the preview file in the store.
     *
     * @return {void}
     */
    showImportUrlDialog() {
        const header = this.#dotMessageService.get('dot.file.field.dialog.import.from.url.header');

        this.#dialogRef = this.#dialogService.open(DotFormImportUrlComponent, {
            header,
            appendTo: 'body',
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            maskStyleClass: 'p-dialog-mask-transparent',
            modal: true,
            resizable: false,
            position: 'center',
            data: {
                inputType: this.$field().fieldType,
                acceptedFiles: this.store.acceptedFiles()
            }
        });

        this.#dialogRef.onClose
            .pipe(
                filter((file) => !!file),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((file: UploadedFile) => {
                this.store.setPreviewFile(file);
            });
    }

    /**
     * Opens a dialog for generating AI images using the `DotAIImagePromptComponent`.
     *
     * The dialog is configured with specific properties such as header, appendTo,
     * closeOnEscape, draggable, keepInViewport, maskStyleClass, resizable, modal,
     * width, and style.
     *
     * When the dialog is closed, it filters the selected image, maps it to an
     * `UploadedFile` object, and sets it as the preview file in the store.
     *
     * @private
     */
    showAIImagePromptDialog() {
        const header = this.#dotMessageService.get('dot.file.field.action.generate.dialog-title');

        this.#dialogRef = this.#dialogService.open(DotAIImagePromptComponent, {
            header,
            appendTo: 'body',
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            maskStyleClass: 'p-dialog-mask-transparent-ai',
            resizable: false,
            modal: true,
            width: '90%',
            style: { 'max-width': '1040px' }
        });

        this.#dialogRef.onClose
            .pipe(
                filter((selectedImage: DotGeneratedAIImage) => !!selectedImage),
                map((selectedImage) => {
                    const previewFile: UploadedFile = {
                        source: 'contentlet',
                        file: selectedImage.response.contentlet
                    };

                    return previewFile;
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((file) => {
                this.store.setPreviewFile(file);
            });
    }

    showFileEditorDialog() {
        const header = this.#dotMessageService.get('dot.file.field.dialog.create.new.file.header');

        this.#dialogRef = this.#dialogService.open(DotFormFileEditorComponent, {
            header,
            appendTo: 'body',
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            maskStyleClass: 'p-dialog-mask-transparent-ai',
            resizable: false,
            modal: true,
            width: '90%',
            style: { 'max-width': '1040px' },
            data: {
                uploadedFile: this.store.uploadedFile(),
                allowFileNameEdit: true
            }
        });

        this.#dialogRef.onClose.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe((file) => {
            console.log('file', file);
        });
    }

    /**
     * Cleanup method.
     *
     * Closes the dialog if it is still open when the component is destroyed.
     *
     * @return {void}
     */
    ngOnDestroy() {
        this.#dialogRef?.close();
    }
}

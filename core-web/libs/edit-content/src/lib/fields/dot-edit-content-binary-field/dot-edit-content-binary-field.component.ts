import { MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { AsyncPipe } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    DestroyRef,
    ElementRef,
    EventEmitter,
    forwardRef,
    inject,
    Input,
    OnDestroy,
    OnInit,
    Output,
    signal,
    Signal,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { delay, filter, skip, tap } from 'rxjs/operators';

import { DotAiService, DotLicenseService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    DotCMSTempFile,
    DotFileMetadata,
    DotGeneratedAIImage
} from '@dotcms/dotcms-models';
import {
    DotAIImagePromptComponent,
    DotDropZoneComponent,
    DotMessagePipe,
    DotSpinnerComponent,
    DropZoneErrorType,
    DropZoneFileEvent,
    DropZoneFileValidity
} from '@dotcms/ui';

import { DotBinaryFieldEditorComponent } from './components/dot-binary-field-editor/dot-binary-field-editor.component';
import { DotBinaryFieldPreviewComponent } from './components/dot-binary-field-preview/dot-binary-field-preview.component';
import { DotBinaryFieldUiMessageComponent } from './components/dot-binary-field-ui-message/dot-binary-field-ui-message.component';
import { DotBinaryFieldUrlModeComponent } from './components/dot-binary-field-url-mode/dot-binary-field-url-mode.component';
import { BinaryFieldMode, BinaryFieldStatus } from './interfaces';
import { DotBinaryFieldEditImageService } from './service/dot-binary-field-edit-image/dot-binary-field-edit-image.service';
import { DotBinaryFieldValidatorService } from './service/dot-binary-field-validator/dot-binary-field-validator.service';
import { DotBinaryFieldStore } from './store/binary-field.store';
import { getFileMetadata, getUiMessage } from './utils/binary-field-utils';

import { DEFAULT_MONACO_CONFIG } from '../../models/dot-edit-content-field.constant';
import { getFieldVariablesParsed, stringToJson } from '../../utils/functions.util';
import { IMAGE_EDITOR_LAUNCHER } from '../shared/image-editor-launcher';

export const DEFAULT_BINARY_FIELD_MONACO_CONFIG: MonacoEditorConstructionOptions = {
    ...DEFAULT_MONACO_CONFIG,
    language: 'text'
};

type SystemOptionsType = {
    allowURLImport: boolean;
    allowCodeWrite: boolean;
    allowGenerateImg: boolean;
};

@Component({
    selector: 'dot-edit-content-binary-field',
    imports: [
        ButtonModule,
        DialogModule,
        DotDropZoneComponent,
        MonacoEditorModule,
        DotMessagePipe,
        DotBinaryFieldUiMessageComponent,
        DotSpinnerComponent,
        DotBinaryFieldEditorComponent,
        InputTextModule,
        DotBinaryFieldUrlModeComponent,
        DotBinaryFieldPreviewComponent,
        TooltipModule,
        AsyncPipe
    ],
    providers: [
        DialogService,
        DotBinaryFieldEditImageService,
        DotBinaryFieldStore,
        DotLicenseService,
        DotBinaryFieldValidatorService,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentBinaryFieldComponent)
        }
    ],
    templateUrl: './dot-edit-content-binary-field.component.html',
    styleUrls: ['./dot-edit-content-binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentBinaryFieldComponent
    implements OnInit, AfterViewInit, OnDestroy, ControlValueAccessor
{
    readonly #dotBinaryFieldStore = inject(DotBinaryFieldStore);
    readonly #dotMessageService = inject(DotMessageService);
    // Optional: the field renders in several contexts; a toast is best-effort feedback
    // and must never make the field fail to construct where no global toast is provided.
    readonly #messageService = inject(MessageService, { optional: true });
    readonly #dotBinaryFieldEditImageService = inject(DotBinaryFieldEditImageService);
    readonly #dotBinaryFieldValidatorService = inject(DotBinaryFieldValidatorService);
    readonly #cd = inject(ChangeDetectorRef);
    readonly #dotAiService = inject(DotAiService);
    readonly #dialogService = inject(DialogService);
    readonly #destroyRef = inject(DestroyRef);
    // Optional: the launcher is provided by the Angular edit-content shell, so the new
    // image editor only activates there. When absent (e.g. a non-Angular host), or when
    // isAvailable() is false, `onEditImage()` safely no-ops.
    readonly #imageEditorLauncher = inject(IMAGE_EDITOR_LAUNCHER, { optional: true });

    $isAIPluginInstalled = toSignal(this.#dotAiService.checkPluginInstallation(), {
        initialValue: false
    });
    $tooltipTextAIBtn = computed(() => {
        const isAIPluginInstalled = this.$isAIPluginInstalled();
        if (!isAIPluginInstalled) {
            return this.#dotMessageService.get('dot.binary.field.action.generate.with.tooltip');
        }

        return null;
    });

    value: string | null = null;

    @Input({ required: true })
    set field(contentTypeField: DotCMSContentTypeField) {
        this.$field.set(contentTypeField);
    }
    @Input({ required: true }) contentlet: DotCMSContentlet;
    @Input() imageEditor = false;

    $field = signal<DotCMSContentTypeField>({} as DotCMSContentTypeField);
    $variable = computed(() => this.$field()?.variable);
    $disabled = signal<boolean>(false);

    @Output() valueUpdated = new EventEmitter<{ value: string; fileName: string }>();
    @ViewChild('inputFile') inputFile: ElementRef;
    readonly dialogFullScreenStyles = { height: '90%', width: '90%' };
    readonly dialogHeaderMap = {
        [BinaryFieldMode.URL]: 'dot.binary.field.dialog.import.from.url.header',
        [BinaryFieldMode.EDITOR]: 'dot.binary.field.dialog.create.new.file.header'
    };
    readonly BinaryFieldStatus = BinaryFieldStatus;
    readonly BinaryFieldMode = BinaryFieldMode;
    readonly vm$ = this.#dotBinaryFieldStore.vm$;
    #dialogRef: DynamicDialogRef | null = null;
    dialogOpen = false;
    customMonacoOptions: Signal<MonacoEditorConstructionOptions> = computed(() => {
        const field = this.$field();

        return {
            ...this.parseCustomMonacoOptions(field?.fieldVariables)
        };
    });
    private onChange: (value: string) => void;
    private onTouched: () => void;
    private tempId = '';

    systemOptions = signal<SystemOptionsType>({
        allowURLImport: false,
        allowCodeWrite: false,
        allowGenerateImg: false
    });

    constructor() {
        this.#dotMessageService.init();
    }

    get maxFileSize(): number {
        return this.#dotBinaryFieldValidatorService.maxFileSize;
    }

    get accept(): string[] {
        return this.#dotBinaryFieldValidatorService.accept;
    }

    get variable() {
        return this.$variable();
    }

    ngOnInit() {
        this.#dotBinaryFieldStore.value$
            .pipe(
                skip(1),
                filter(({ value }) => value !== this.getValue())
            )
            .subscribe(({ value, fileName }) => {
                this.tempId = value; // If the value changes, it means that a new file was uploaded
                this.valueUpdated.emit({ value, fileName });

                if (this.onChange) {
                    this.onChange(value);
                    this.onTouched();
                }
            });

        this.#dotBinaryFieldEditImageService
            .editedImage()
            .pipe(
                filter((tempFile) => !!tempFile),
                tap(() => this.#dotBinaryFieldStore.setStatus(BinaryFieldStatus.UPLOADING)),
                delay(500) // Loading animation
            )
            .subscribe((temp) => this.#dotBinaryFieldStore.setFileFromTemp(temp));

        this.#dotBinaryFieldStore.setMaxFileSize(this.maxFileSize);
    }

    ngAfterViewInit() {
        this.setFieldVariables();

        if (!this.contentlet || !this.getValue() || !this.checkMetadata()) {
            return;
        }

        this.#dotBinaryFieldStore.setFileFromContentlet({
            ...this.contentlet,
            value: this.getValue(),
            fieldVariable: this.variable
        });

        this.#cd.detectChanges();
    }

    writeValue(value: string): void {
        this.value = value;
        this.#dotBinaryFieldStore.setValue(value);
    }

    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.$disabled.set(isDisabled);
    }

    ngOnDestroy() {
        this.#dotBinaryFieldEditImageService.removeListener();
        this.#dialogRef?.close();
    }

    /**
     * Open dialog to create new file or import from url
     *
     * @param {BinaryFieldMode} mode
     * @memberof DotEditContentBinaryFieldComponent
     */
    openDialog(mode: BinaryFieldMode) {
        if (this.$disabled()) {
            return;
        }

        if (mode === BinaryFieldMode.AI) {
            this.openAIImagePrompt();
        } else {
            this.dialogOpen = true;
        }

        this.#dotBinaryFieldStore.setMode(mode);
    }

    /**
     * Opens a dialog for AI Image Prompt using the DotAIImagePromptComponent.
     * The dialog has various configurations such as header, appendTo, closeOnEscape, draggable,
     * keepInViewport, maskStyleClass, resizable, modal, width, and style.
     *
     * When the dialog is closed, it filters the selected image and if an image is selected,
     * it parses the image to a temporary file and sets it in the dotBinaryFieldStore.
     *
     * @private
     */
    openAIImagePrompt() {
        const header = this.#dotMessageService.get('dot.binary.field.action.generate.dialog-title');

        this.#dialogRef = this.#dialogService.open(DotAIImagePromptComponent, {
            header,
            appendTo: 'body',
            closable: true,
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
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((selectedImage: DotGeneratedAIImage) => {
                const tempFile = this.parseToTempFile(selectedImage);
                this.#dotBinaryFieldStore.setTempFile(tempFile);
            });
    }

    /**
     * Close dialog
     *
     * @memberof DotEditContentBinaryFieldComponent
     */
    closeDialog() {
        this.dialogOpen = false;
        this.#dialogRef?.close();
        this.#dotBinaryFieldStore.setMode(BinaryFieldMode.DROPZONE);
    }

    /**
     * Open file picker
     *
     * @memberof DotEditContentBinaryFieldComponent
     */
    openFilePicker() {
        if (this.$disabled()) {
            return;
        }

        this.inputFile.nativeElement.click();
    }

    /**
     * Handle file selection
     *
     * @param {Event} event
     * @memberof DotEditContentBinaryFieldComponent
     */
    handleFileSelection(event: Event) {
        if (this.$disabled()) {
            return;
        }

        const input = event.target as HTMLInputElement;
        const file = input.files[0];
        this.#dotBinaryFieldStore.handleUploadFile(file);
    }

    /**
     * Remove file
     *
     * @memberof DotEditContentBinaryFieldComponent
     */
    removeFile() {
        if (this.$disabled()) {
            return;
        }

        this.#dotBinaryFieldStore.removeFile();
    }

    /**
     * Set temp file
     *
     * @param {DotCMSTempFile} tempFile
     * @memberof DotEditContentBinaryFieldComponent
     */
    setTempFile(tempFile: DotCMSTempFile) {
        this.#dotBinaryFieldStore.setFileFromTemp(tempFile);
        this.dialogOpen = false;
    }

    /**
     * Open Dialog to edit file in editor
     *
     * @memberof DotEditContentBinaryFieldComponent
     */
    onEditFile() {
        this.openDialog(BinaryFieldMode.EDITOR);
    }

    /**
     * Open Image Editor
     *
     * Launches the editor through the {@link IMAGE_EDITOR_LAUNCHER} seam and applies
     * the edited image back to the field via the binary field store.
     *
     * @memberof DotEditContentBinaryFieldComponent
     */
    onEditImage() {
        const launcher = this.#imageEditorLauncher;

        // The new Angular editor is gated by FEATURE_FLAG_NEW_IMAGE_EDITOR (via the
        // launcher's `isAvailable()`). When it's off — or no launcher is provided in
        // this context — fall back to the legacy Dojo image editor.
        if (!launcher?.isAvailable()) {
            this.#dotBinaryFieldEditImageService.openImageEditor({
                inode: this.contentlet?.inode,
                tempId: this.tempId,
                variable: this.variable
            });

            return;
        }

        const inode = this.contentlet?.inode;
        const metadata = this.contentlet
            ? (getFileMetadata(this.contentlet) as Partial<DotFileMetadata>)
            : null;
        const fieldName = this.$field()?.name;
        const variable = this.variable;

        // The launcher contract requires a resolved field/variable; the image-editor lib
        // treats them as guaranteed strings. Bail (rather than leak `undefined`) if the
        // field input hasn't resolved yet.
        if (!fieldName || !variable) {
            console.error('Image editor: cannot open, the binary field is not resolved');

            return;
        }

        launcher
            .open({
                inode,
                tempId: this.tempId,
                variable,
                fieldName,
                byInode: !!inode,
                fileName: this.contentlet?.fileName ?? metadata?.name,
                mimeType: metadata?.contentType
            })
            .pipe(
                filter((tempFile): tempFile is DotCMSTempFile => !!tempFile),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe({
                next: (tempFile) => this.#dotBinaryFieldStore.setFileFromTemp(tempFile),
                // The dialog stream isn't expected to error, but guard it so an
                // unexpected failure both logs and tells the user, instead of being
                // swallowed with the edited image silently never applied.
                error: (error) => {
                    console.error('Image editor failed to apply the edited image', error);
                    this.#messageService?.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('dot.common.message.error'),
                        detail: this.#dotMessageService.get(
                            'dot.binary.field.image.editor.apply.error'
                        )
                    });
                }
            });
    }

    /**
     * Set drop zone active state
     *
     * @param {boolean} value
     * @memberof DotEditContentBinaryFieldComponent
     */
    setDropZoneActiveState(value: boolean) {
        this.#dotBinaryFieldStore.setDropZoneActive(value);
    }

    /**
     * Handle file drop
     *
     * @param {DropZoneFileEvent} { validity, file }
     * @return {*}
     * @memberof DotEditContentBinaryFieldComponent
     */
    handleFileDrop({ validity, file }: DropZoneFileEvent): void {
        if (this.$disabled()) {
            return;
        }

        if (!validity.valid) {
            this.handleFileDropError(validity);

            return;
        }

        this.#dotBinaryFieldStore.handleUploadFile(file);
    }

    /**
     * Set field variables
     *
     * @private
     * @memberof DotEditContentBinaryFieldComponent
     */
    private setFieldVariables() {
        const field = this.$field();
        const {
            accept,
            maxFileSize = 0,
            systemOptions = `{
                "allowURLImport": true,
                "allowCodeWrite": true,
                "allowGenerateImg": true
            }`
        } = getFieldVariablesParsed<{
            accept: string;
            maxFileSize: string;
            systemOptions: string;
        }>(field?.fieldVariables);

        this.#dotBinaryFieldValidatorService.setAccept(accept ? accept.split(',') : []);
        this.#dotBinaryFieldValidatorService.setMaxFileSize(Number(maxFileSize));
        this.systemOptions.set(JSON.parse(systemOptions));
        this.#cd.detectChanges();
    }

    /**
     * Handle file drop error
     *
     * @private
     * @param {DropZoneFileValidity} { errorsType }
     * @memberof DotEditContentBinaryFieldComponent
     */
    private handleFileDropError({ errorsType }: DropZoneFileValidity): void {
        const messageArgs = {
            [DropZoneErrorType.FILE_TYPE_MISMATCH]: this.accept.join(', '),
            [DropZoneErrorType.MAX_FILE_SIZE_EXCEEDED]: `${this.maxFileSize} bytes`
        };
        const errorType = errorsType[0];
        const uiMessage = getUiMessage(errorType, messageArgs[errorType]);

        this.#dotBinaryFieldStore.invalidFile(uiMessage);
    }

    /**
     * Parses the custom Monaco options for a given field of a DotCMSContentTypeField.
     *
     * @returns {Record<string, string>} Returns the parsed custom Monaco options as a key-value pair object.
     * @private
     * @param fieldVariables
     */
    private parseCustomMonacoOptions(
        fieldVariables: DotCMSContentTypeFieldVariable[]
    ): Record<string, string> {
        const { monacoOptions } = getFieldVariablesParsed<{ monacoOptions: string }>(
            fieldVariables
        );

        return stringToJson(monacoOptions);
    }

    /**
     * Check if the contentlet has metadata
     *
     * @private
     * @return {*}  {boolean}
     * @memberof DotEditContentBinaryFieldComponent
     */
    private checkMetadata(): boolean {
        const { baseType } = this.contentlet;
        const isFileAsset = baseType === DotCMSBaseTypesContentTypes.FILEASSET;
        const key = isFileAsset ? 'metaData' : this.variable + 'MetaData';

        return !!this.contentlet[key];
    }

    private parseToTempFile(selectedImage: DotGeneratedAIImage) {
        const { response } = selectedImage;
        const { contentlet } = response;
        const metaData = contentlet['assetMetaData'];

        const tempFile: DotCMSTempFile = {
            id: response.response,
            fileName: response.tempFileName,
            folder: contentlet.folder,
            image: true,
            length: metaData.length,
            mimeType: metaData.contentType,
            referenceUrl: contentlet.asset,
            thumbnailUrl: contentlet.asset,
            metadata: metaData
        };

        return tempFile;
    }

    private getValue() {
        if (this.value !== null) {
            return this.value;
        }

        return this.contentlet?.[this.variable] ?? this.$field().defaultValue;
    }
}

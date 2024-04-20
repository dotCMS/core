import { MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    ElementRef,
    EventEmitter,
    forwardRef,
    Input,
    OnDestroy,
    OnInit,
    Output,
    signal,
    Signal,
    ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { delay, filter, skip, tap } from 'rxjs/operators';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    DotCMSTempFile
} from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotSpinnerModule,
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
import { getUiMessage } from './utils/binary-field-utils';

import { DEFAULT_MONACO_CONFIG } from '../../models/dot-edit-content-field.constant';
import { getFieldVariablesParsed, stringToJson } from '../../utils/functions.util';

export const DEFAULT_BINARY_FIELD_MONACO_CONFIG: MonacoEditorConstructionOptions = {
    ...DEFAULT_MONACO_CONFIG,
    language: 'text'
};

@Component({
    selector: 'dot-edit-content-binary-field',
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        DialogModule,
        DotDropZoneComponent,
        MonacoEditorModule,
        DotMessagePipe,
        DotBinaryFieldUiMessageComponent,
        DotSpinnerModule,
        HttpClientModule,
        DotBinaryFieldEditorComponent,
        InputTextModule,
        DotBinaryFieldUrlModeComponent,
        DotBinaryFieldPreviewComponent
    ],
    providers: [
        DotBinaryFieldStore,
        DotLicenseService,
        DotBinaryFieldEditImageService,
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
    contentTypeField = signal<DotCMSContentTypeField>({} as DotCMSContentTypeField);
    @Input() contentlet: DotCMSContentlet;
    @Input() imageEditor = false;
    @Output() valueUpdated = new EventEmitter<{ value: string; fileName: string }>();
    @ViewChild('inputFile') inputFile: ElementRef;
    readonly dialogFullScreenStyles = { height: '90%', width: '90%' };
    readonly dialogHeaderMap = {
        [BinaryFieldMode.URL]: 'dot.binary.field.dialog.import.from.url.header',
        [BinaryFieldMode.EDITOR]: 'dot.binary.field.dialog.create.new.file.header'
    };
    readonly BinaryFieldStatus = BinaryFieldStatus;
    readonly BinaryFieldMode = BinaryFieldMode;
    readonly vm$ = this.dotBinaryFieldStore.vm$;
    dialogOpen = false;
    customMonacoOptions: Signal<MonacoEditorConstructionOptions> = computed(() => {
        return {
            ...this.parseCustomMonacoOptions(this.contentTypeField().fieldVariables)
        };
    });
    private onChange: (value: string) => void;
    private onTouched: () => void;
    private tempId = '';

    protected systemOptions: Record<string, boolean> = {};

    constructor(
        private readonly dotBinaryFieldStore: DotBinaryFieldStore,
        private readonly dotMessageService: DotMessageService,
        private readonly dotBinaryFieldEditImageService: DotBinaryFieldEditImageService,
        private readonly DotBinaryFieldValidatorService: DotBinaryFieldValidatorService,
        private readonly cd: ChangeDetectorRef
    ) {
        this.dotMessageService.init();
    }

    @Input({ required: true })
    set field(contentTypeField: DotCMSContentTypeField) {
        this.contentTypeField.set(contentTypeField);
    }

    get value(): string {
        return this.contentlet?.[this.variable] ?? this.contentTypeField().defaultValue;
    }

    get maxFileSize(): number {
        return this.DotBinaryFieldValidatorService.maxFileSize;
    }

    get accept(): string[] {
        return this.DotBinaryFieldValidatorService.accept;
    }

    get variable(): string {
        return this.contentTypeField().variable;
    }

    ngOnInit() {
        this.dotBinaryFieldStore.value$
            .pipe(
                skip(1),
                filter(({ value }) => value !== this.value)
            )
            .subscribe(({ value, fileName }) => {
                this.tempId = value; // If the value changes, it means that a new file was uploaded
                this.valueUpdated.emit({ value, fileName });

                if (this.onChange) {
                    this.onChange(value);
                    this.onTouched();
                }
            });

        this.dotBinaryFieldEditImageService
            .editedImage()
            .pipe(
                filter((tempFile) => !!tempFile),
                tap(() => this.dotBinaryFieldStore.setStatus(BinaryFieldStatus.UPLOADING)),
                delay(500) // Loading animation
            )
            .subscribe((temp) => this.dotBinaryFieldStore.setFileFromTemp(temp));

        this.dotBinaryFieldStore.setMaxFileSize(this.maxFileSize);
    }

    ngAfterViewInit() {
        this.setFieldVariables();

        if (!this.value || !this.checkMetadata()) {
            return;
        }

        this.dotBinaryFieldStore.setFileFromContentlet({
            ...this.contentlet,
            value: this.value,
            fieldVariable: this.variable
        });

        this.cd.detectChanges();
    }

    writeValue(value: string): void {
        this.dotBinaryFieldStore.setValue(value);
    }

    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    ngOnDestroy() {
        this.dotBinaryFieldEditImageService.removeListener();
    }

    /**
     * Open dialog to create new file or import from url
     *
     * @param {BinaryFieldMode} mode
     * @memberof DotEditContentBinaryFieldComponent
     */
    openDialog(mode: BinaryFieldMode) {
        this.dialogOpen = true;
        this.dotBinaryFieldStore.setMode(mode);
    }

    /**
     * Close dialog
     *
     * @memberof DotEditContentBinaryFieldComponent
     */
    closeDialog() {
        this.dialogOpen = false;
        this.dotBinaryFieldStore.setMode(BinaryFieldMode.DROPZONE);
    }

    /**
     * Open file picker
     *
     * @memberof DotEditContentBinaryFieldComponent
     */
    openFilePicker() {
        this.inputFile.nativeElement.click();
    }

    /**
     * Handle file selection
     *
     * @param {Event} event
     * @memberof DotEditContentBinaryFieldComponent
     */
    handleFileSelection(event: Event) {
        const input = event.target as HTMLInputElement;
        const file = input.files[0];
        this.dotBinaryFieldStore.handleUploadFile(file);
    }

    /**
     * Remove file
     *
     * @memberof DotEditContentBinaryFieldComponent
     */
    removeFile() {
        this.dotBinaryFieldStore.removeFile();
    }

    /**
     * Set temp file
     *
     * @param {DotCMSTempFile} tempFile
     * @memberof DotEditContentBinaryFieldComponent
     */
    setTempFile(tempFile: DotCMSTempFile) {
        this.dotBinaryFieldStore.setFileFromTemp(tempFile);
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
     * @memberof DotEditContentBinaryFieldComponent
     */
    onEditImage() {
        this.dotBinaryFieldEditImageService.openImageEditor({
            inode: this.contentlet?.inode,
            tempId: this.tempId,
            variable: this.contentTypeField().variable
        });
    }

    /**
     * Set drop zone active state
     *
     * @param {boolean} value
     * @memberof DotEditContentBinaryFieldComponent
     */
    setDropZoneActiveState(value: boolean) {
        this.dotBinaryFieldStore.setDropZoneActive(value);
    }

    /**
     * Handle file drop
     *
     * @param {DropZoneFileEvent} { validity, file }
     * @return {*}
     * @memberof DotEditContentBinaryFieldComponent
     */
    handleFileDrop({ validity, file }: DropZoneFileEvent): void {
        if (!validity.valid) {
            this.handleFileDropError(validity);

            return;
        }

        this.dotBinaryFieldStore.handleUploadFile(file);
    }

    /**
     * Set field variables
     *
     * @private
     * @memberof DotEditContentBinaryFieldComponent
     */
    private setFieldVariables() {
        const {
            accept,
            maxFileSize = 0,
            systemOptions = `{
                "allowURLImport": true,
                "allowCodeWrite": true
            }`
        } = getFieldVariablesParsed<{
            accept: string;
            maxFileSize: string;
            systemOptions: string;
        }>(this.contentTypeField().fieldVariables);

        this.DotBinaryFieldValidatorService.setAccept(accept ? accept.split(',') : []);
        this.DotBinaryFieldValidatorService.setMaxFileSize(Number(maxFileSize));
        this.systemOptions = JSON.parse(systemOptions);
        this.cd.detectChanges();
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

        this.dotBinaryFieldStore.invalidFile(uiMessage);
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
}

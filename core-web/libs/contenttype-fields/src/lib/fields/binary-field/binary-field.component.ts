import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { filter, skip } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotSpinnerModule,
    DropZoneFileEvent,
    DropZoneFileValidity
} from '@dotcms/ui';

import { DotBinaryFieldEditorComponent } from './components/dot-binary-field-editor/dot-binary-field-editor.component';
import { DotBinaryFieldPreviewComponent } from './components/dot-binary-field-preview/dot-binary-field-preview.component';
import { DotBinaryFieldUiMessageComponent } from './components/dot-binary-field-ui-message/dot-binary-field-ui-message.component';
import { DotBinaryFieldUrlModeComponent } from './components/dot-binary-field-url-mode/dot-binary-field-url-mode.component';
import { DotEditBinaryFieldImageService } from './service/dot-edit-binary-field-image.service';
import {
    BINARY_FIELD_MODE,
    BINARY_FIELD_STATUS,
    BinaryFieldState,
    DotBinaryFieldStore
} from './store/binary-field.store';

import {
    UI_MESSAGE_KEYS,
    UiMessageI,
    getFieldVariables,
    getUiMessage
} from '../../utils/binary-field-utils';

const initialState: BinaryFieldState = {
    file: null,
    tempFile: null,
    mode: BINARY_FIELD_MODE.DROPZONE,
    status: BINARY_FIELD_STATUS.INIT,
    dropZoneActive: false,
    UiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT)
};

@Component({
    selector: 'dot-binary-field',
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
    providers: [DotBinaryFieldStore, DotEditBinaryFieldImageService],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldComponent implements OnInit, AfterViewInit {
    @Input() field: DotCMSContentTypeField;
    @Input() contentlet: DotCMSContentlet;

    @ViewChild('inputFile') inputFile: ElementRef;
    @Output() tempFile = new EventEmitter<DotCMSTempFile>();

    readonly dialogHeaderMap = {
        [BINARY_FIELD_MODE.URL]: 'dot.binary.field.dialog.import.from.url.header',
        [BINARY_FIELD_MODE.EDITOR]: 'dot.binary.field.dialog.create.new.file.header'
    };
    readonly BINARY_FIELD_STATUS = BINARY_FIELD_STATUS;
    readonly BINARY_FIELD_MODE = BINARY_FIELD_MODE;
    readonly vm$ = this.dotBinaryFieldStore.vm$;

    private file;
    dialogOpen = false;
    accept: string[] = [];
    maxFileSize: number;
    helperText: string;

    constructor(
        private readonly dotBinaryFieldStore: DotBinaryFieldStore,
        private readonly dotMessageService: DotMessageService,
        private readonly dotEditBinaryFieldImageService: DotEditBinaryFieldImageService
    ) {
        // WIP - This will receive the contentlet from the parent component (PREVIEW MODE)
        this.dotBinaryFieldStore.setState(initialState);
        this.dotMessageService.init();
    }

    ngOnInit() {
        this.dotBinaryFieldStore.tempFile$
            .pipe(skip(1)) // Skip initial state
            .subscribe((tempFile) => {
                this.tempFile.emit(tempFile);
            });

        this.dotEditBinaryFieldImageService
            .editedImage()
            .pipe(filter((tempFile) => !!tempFile))
            .subscribe((tempFile) => {
                this.setTempFile(tempFile);
            });

        this.dotBinaryFieldStore.setMaxFileSize(this.maxFileSize);
    }

    ngAfterViewInit() {
        this.setFieldVariables();
        if (this.contentlet) {
            this.setFile();
        }
    }

    /**
     *  Set drop zone active state
     *
     * @param {boolean} value
     * @memberof DotBinaryFieldComponent
     */
    setDropZoneActiveState(value: boolean) {
        this.dotBinaryFieldStore.setDropZoneActive(value);
    }

    /**
     * Handle file dropped
     *
     * @param {DropZoneFileEvent} { validity }
     * @return {*}
     * @memberof BinaryFieldComponent
     */
    handleFileDrop({ validity, file }: DropZoneFileEvent) {
        if (!validity.valid) {
            const uiMessage = this.handleFileDropError(validity);
            this.dotBinaryFieldStore.invalidFile(uiMessage);

            return;
        }

        this.dotBinaryFieldStore.handleUploadFile(file);
    }

    /**
     * Open dialog
     *
     * @param {BINARY_FIELD_MODE} mode
     * @memberof DotBinaryFieldComponent
     */
    openDialog(mode: BINARY_FIELD_MODE) {
        this.dialogOpen = true;
        this.dotBinaryFieldStore.setMode(mode);
    }

    /**
     * Close Dialog
     *
     * @memberof DotBinaryFieldComponent
     */
    closeDialog() {
        this.dialogOpen = false;
    }

    /**
     * Listen to dialog close event
     *
     * @memberof DotBinaryFieldComponent
     */
    afterDialogClose() {
        this.dotBinaryFieldStore.setMode(null);
    }

    /**
     * Open file picker dialog
     *
     * @memberof DotBinaryFieldComponent
     */
    openFilePicker() {
        this.inputFile.nativeElement.click();
    }

    /**
     * Handle file selection
     *
     * @param {Event} event
     * @memberof DotBinaryFieldComponent
     */
    handleFileSelection(event: Event) {
        const input = event.target as HTMLInputElement;
        const file = input.files[0];
        this.dotBinaryFieldStore.handleUploadFile(file);
    }

    /**
     * Remove file
     *
     * @memberof DotBinaryFieldComponent
     */
    removeFile() {
        this.dotBinaryFieldStore.removeFile();
    }

    /**
     * Set TempFile
     *
     * @param {DotCMSTempFile} tempFile
     * @memberof DotBinaryFieldComponent
     */
    setTempFile(tempFile: DotCMSTempFile) {
        this.dotBinaryFieldStore.setTempFile(tempFile);
        this.dialogOpen = false;
        this.file = null;
    }

    onEditFile({ content }: { content?: string }) {
        if (!content) {
            this.dotEditBinaryFieldImageService.openImageEditor(
                this.contentlet.inode,
                this.field.variable
            );

            return;
        }

        this.openDialog(BINARY_FIELD_MODE.EDITOR);
    }

    private setFile() {
        const variable = this.field.variable;
        const { titleImage, inode } = this.contentlet;
        const { contentType, ...cotentlet } = this.contentlet[variable + 'MetaData'];
        this.file = {
            inode,
            titleImage,
            mimeType: contentType,
            ...cotentlet
        };

        this.dotBinaryFieldStore.setFile(this.file);
    }

    private setFieldVariables() {
        const {
            accept = '',
            maxFileSize = 0,
            helperText = ''
        } = this.field.fieldVariables.reduce(getFieldVariables, {});

        this.accept = accept.split(',').filter((type) => type.trim().length > 0);
        this.maxFileSize = Number(maxFileSize);
        this.helperText = helperText;
    }

    /**
     *  Handle file drop error
     *
     * @private
     * @param {DropZoneFileValidity} { fileTypeMismatch, maxFileSizeExceeded }
     * @memberof DotBinaryFieldStore
     */
    private handleFileDropError({
        fileTypeMismatch,
        maxFileSizeExceeded
    }: DropZoneFileValidity): UiMessageI {
        const acceptedTypes = this.accept.join(', ');
        const maxSize = `${this.maxFileSize} bytes`;
        let uiMessage: UiMessageI;

        if (fileTypeMismatch) {
            uiMessage = getUiMessage(UI_MESSAGE_KEYS.FILE_TYPE_MISMATCH, acceptedTypes);
        } else if (maxFileSizeExceeded) {
            uiMessage = getUiMessage(UI_MESSAGE_KEYS.MAX_FILE_SIZE_EXCEEDED, maxSize);
        }

        return uiMessage;
    }
}

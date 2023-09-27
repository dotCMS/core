import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import {
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

import { skip } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotSpinnerModule,
    DropZoneFileEvent,
    DropZoneFileValidity
} from '@dotcms/ui';

import { InputTextModule } from 'primeng/inputtext';

import { DotBinaryFieldUiMessageComponent } from './components/dot-binary-field-ui-message/dot-binary-field-ui-message.component';
import {
    BINARY_FIELD_MODE,
    BINARY_FIELD_STATUS,
    BinaryFieldState,
    DotBinaryFieldStore
} from './store/binary-field.store';

import { UI_MESSAGE_KEYS, UiMessageI, getUiMessage } from '../../utils/binary-field-utils';

const initialState: BinaryFieldState = {
    file: null,
    tempFile: null,
    mode: BINARY_FIELD_MODE.DROPZONE,
    status: BINARY_FIELD_STATUS.INIT,
    dialogOpen: false,
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
        InputTextModule
    ],
    providers: [DotBinaryFieldStore],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldComponent implements OnInit {
    //Inputs
    acceptedTypes: string[] = [];
    @Input() set accept(accept: string) {
        this.acceptedTypes = accept.split(',').map((type) => type.trim());
    }

    @Input() maxFileSize: number;
    @Input() helperText: string;

    @ViewChild('inputFile') inputFile: ElementRef;

    @Output() tempFile = new EventEmitter<DotCMSTempFile>();

    @Input() contentlet;

    readonly dialogHeaderMap = {
        [BINARY_FIELD_MODE.URL]: 'dot.binary.field.dialog.import.from.url.header',
        [BINARY_FIELD_MODE.EDITOR]: 'dot.binary.field.dialog.create.new.file.header'
    };
    readonly BINARY_FIELD_STATUS = BINARY_FIELD_STATUS;
    readonly BINARY_FIELD_MODE = BINARY_FIELD_MODE;
    readonly vm$ = this.dotBinaryFieldStore.vm$;

    constructor(
        private readonly dotBinaryFieldStore: DotBinaryFieldStore,
        private readonly dotMessageService: DotMessageService
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

        this.dotBinaryFieldStore.setMaxFileSize(this.maxFileSize);
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
        this.dotBinaryFieldStore.openDialog(mode);
    }

    /**
     * Listen to dialog visibility change
     * and set mode to dropzone when dialog is closed
     *
     * @param {boolean} visibily
     * @memberof DotBinaryFieldComponent
     */
    visibleChange(visibily: boolean) {
        if (!visibily) {
            this.dotBinaryFieldStore.closeDialog();
        }
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

    handleCreateFile(_event) {
        // TODO: Implement - Write Code
    }

    handleExternalSourceFile(event: string) {
        this.dotBinaryFieldStore.handleUploadFile(event);
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
        const acceptedTypes = this.acceptedTypes.join(', ');
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

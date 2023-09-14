import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { NgClass, NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, Input, ViewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import {
    DotDropZoneComponent,
    DotDropZoneMessageComponent,
    DotMessagePipe,
    DropZoneFileEvent,
    DropZoneFileValidity
} from '@dotcms/ui';

import { DropZoneMessage, getDropZoneMessage } from '../../utils/binary-field-utils';

enum UPLOAD_FILE_METHOD {
    UPLOAD = 'UPLOAD',
    IMPORT_FROM_URL = 'IMPORT_FROM_URL',
    WRITE_CODE = 'WRITE_CODE'
}

@Component({
    selector: 'dotcms-binary-field',
    standalone: true,
    imports: [
        NgIf,
        NgClass,
        NgSwitch,
        NgSwitchCase,
        ButtonModule,
        DialogModule,
        DotDropZoneComponent,
        MonacoEditorModule,
        DotMessagePipe,
        DotDropZoneMessageComponent
    ],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BinaryFieldComponent {
    //Inputs
    @Input() accept: string[];
    @Input() maxFileSize: number;
    @Input() helperLabel: string;

    @ViewChild('inputFile', { static: true }) inputFile: ElementRef;

    dropZoneActive = false;
    dropZoneMessage: DropZoneMessage = getDropZoneMessage('default');
    readonly UPLOAD_FILE_METHOD = UPLOAD_FILE_METHOD;
    readonly dialogOptions = {
        mode: UPLOAD_FILE_METHOD.UPLOAD,
        header: '',
        visible: false
    };

    setDropZoneActiveState(value: boolean) {
        this.dropZoneActive = value;
    }

    /**
     * Handle file dropped
     *
     * @param {DropZoneFileEvent} { validity }
     * @return {*}
     * @memberof BinaryFieldComponent
     */
    handleFileDrop({ validity }: DropZoneFileEvent) {
        this.setDropZoneActiveState(false);

        if (!validity.valid) {
            this.handleDropZoneError(validity);

            return;
        }

        this.dropZoneMessage = getDropZoneMessage('default');
    }

    /**
     * Handle drop zone error
     * TODO: Consider a component state or service to handle this
     *
     * @param {DropZoneFileValidity} validity
     * @memberof BinaryFieldComponent
     */
    handleDropZoneError({ fileTypeMismatch, maxFileSizeExceeded }: DropZoneFileValidity): void {
        if (fileTypeMismatch) {
            const acceptedTypes = this.accept.join(', ');
            this.dropZoneMessage = getDropZoneMessage('fileTypeMismatch', acceptedTypes);
        } else if (maxFileSizeExceeded) {
            const maxSize = `${this.maxFileSize} bytes`;
            this.dropZoneMessage = getDropZoneMessage('maxFileSizeExceeded', maxSize);
        } else {
            this.dropZoneMessage = getDropZoneMessage('couldNotLoad');
        }
    }

    openFilePicker() {
        this.inputFile.nativeElement.click();
    }

    handleFileSelection(_event) {
        // TODO: Implement
    }

    openDialog(method: UPLOAD_FILE_METHOD) {
        this.dialogOptions.visible = true;
        this.dialogOptions.mode = method;
        this.dialogOptions.header = this.getDialogLabel(method);
    }

    getDialogLabel(method: UPLOAD_FILE_METHOD): string {
        switch (method) {
            case UPLOAD_FILE_METHOD.IMPORT_FROM_URL:
                return 'URL';

            case UPLOAD_FILE_METHOD.WRITE_CODE:
                return 'File Details';
        }
    }
}

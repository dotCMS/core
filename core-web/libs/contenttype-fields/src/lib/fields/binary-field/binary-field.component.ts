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

import { BinaryFieldMessage, getBinaryFieldMessage } from '../../utils/binary-field-utils';

export enum BINARY_FIELD_MODE {
    DROPZONE = 'DROPZONE',
    URL = 'URL',
    EDITOR = 'EDITOR'
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
    @Input() helperText: string;

    @ViewChild('inputFile', { static: true }) inputFile: ElementRef;

    dropZoneActive = false;
    dropZoneMessage: BinaryFieldMessage = getBinaryFieldMessage('default');
    readonly BINARY_FIELD_MODE = BINARY_FIELD_MODE;
    readonly dialogOptions = {
        mode: BINARY_FIELD_MODE.DROPZONE,
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

        this.dropZoneMessage = getBinaryFieldMessage('default');
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
            this.dropZoneMessage = getBinaryFieldMessage('fileTypeMismatch', acceptedTypes);
        } else if (maxFileSizeExceeded) {
            const maxSize = `${this.maxFileSize} bytes`;
            this.dropZoneMessage = getBinaryFieldMessage('maxFileSizeExceeded', maxSize);
        } else {
            this.dropZoneMessage = getBinaryFieldMessage('couldNotLoad');
        }
    }

    openFilePicker() {
        this.inputFile.nativeElement.click();
    }

    handleFileSelection(_event) {
        // TODO: Implement
    }

    openDialog(method: BINARY_FIELD_MODE) {
        this.dialogOptions.visible = true;
        this.dialogOptions.mode = method;
        this.dialogOptions.header = this.getDialogLabel(method);
    }

    getDialogLabel(method: BINARY_FIELD_MODE): string {
        switch (method) {
            case BINARY_FIELD_MODE.URL:
                return 'URL';

            case BINARY_FIELD_MODE.EDITOR:
                return 'File Details';
        }
    }
}

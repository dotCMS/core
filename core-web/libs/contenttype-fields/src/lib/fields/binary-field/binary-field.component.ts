import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { NgClass, NgIf } from '@angular/common';
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

@Component({
    selector: 'dotcms-binary-field',
    standalone: true,
    imports: [
        NgIf,
        NgClass,
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

    active = false;
    dropZoneMessage: DropZoneMessage = getDropZoneMessage('default');

    setActiveState(value: boolean) {
        this.active = value;
    }

    /**
     * Handle file dropped
     *
     * @param {DropZoneFileEvent} { validity }
     * @return {*}
     * @memberof BinaryFieldComponent
     */
    onFileDropped({ validity }: DropZoneFileEvent) {
        this.setActiveState(false);

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

    openChooseFileDialog() {
        this.inputFile.nativeElement.click();
    }

    onSelectFile(_event) {
        // TODO: Implement
    }
}

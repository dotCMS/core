import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { NgClass, NgIf } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    Input,
    ViewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import {
    DotDropZoneComponent,
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
        DotMessagePipe
    ],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BinaryFieldComponent implements AfterViewInit {
    //Inputs
    @Input() accept: string[];
    @Input() maxFileSize: number;
    @Input() helperLabel: string;

    @ViewChild('inputFile', { static: true }) inputFile: ElementRef;
    @ViewChild('message', { static: true }) messageHTML: ElementRef;

    active = false;
    dropZoneMessage: DropZoneMessage = getDropZoneMessage('default');

    ngAfterViewInit(): void {
        // TODO: Consider a component state or service to handle this
        const observer = new MutationObserver(() => this.bindChooseFileBtn());
        // Start observing the target node for configured mutations
        observer.observe(this.messageHTML.nativeElement, {
            childList: true,
            characterData: true,
            subtree: true
        });
        this.bindChooseFileBtn();
    }

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

    onSelectFile(_event) {
        // TODO: Implement
    }

    private bindChooseFileBtn() {
        const chooseFileBtn: HTMLAnchorElement = this.messageHTML.nativeElement.querySelector('a');
        chooseFileBtn.addEventListener('click', () => this.inputFile.nativeElement.click());
    }
}

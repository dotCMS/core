import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostListener,
    Input,
    Output
} from '@angular/core';

export interface DropZoneFileEvent {
    file: File | null;
    validity: DropZoneFileValidity;
}

export interface DropZoneFileValidity {
    isFileTypeMismatch: boolean;
    isFileTooBig: boolean;
    multipleFiles: boolean;
    valid: boolean;
}

@Component({
    selector: 'dot-drop-zone',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-drop-zone.component.html',
    styleUrls: ['./dot-drop-zone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDropZoneComponent {
    @Output() fileDropped = new EventEmitter<DropZoneFileEvent>();
    @Output() fileDragEnter = new EventEmitter<boolean>();
    @Output() fileDragLeave = new EventEmitter<boolean>();

    @Input() maxLength: number;

    @Input() set accept(types: string[]) {
        this._accept = types.map((type) => {
            // Remove the wildcard character
            return type.toLowerCase().replace(/\*/g, '');
        });
    }

    private _accept: string[] = [];
    private validity: DropZoneFileValidity = {
        isFileTypeMismatch: false,
        isFileTooBig: false,
        multipleFiles: false,
        valid: true
    };

    @HostListener('drop', ['$event'])
    onDrop(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();

        const { dataTransfer } = event;
        const files = this.getFiles(dataTransfer);
        const file = files?.length === 1 ? files[0] : null;

        if (files.length === 0) return;

        this.setValidity(files);

        dataTransfer.items?.clear();
        dataTransfer.clearData();
        this.fileDropped.emit({
            file, // Only one file is allowed
            validity: this.validity
        });
    }

    @HostListener('dragenter', ['$event'])
    onDragEnter(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
        this.fileDragEnter.emit(true);
    }

    @HostListener('dragover', ['$event'])
    onDragOver(event: DragEvent) {
        // Prevent the default behavior to allow drop
        event.stopPropagation();
        event.preventDefault();
    }

    @HostListener('dragleave', ['$event'])
    onDragLeave(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
        this.fileDragLeave.emit(true);
    }

    /**
     * Check if the file is valid based on the allowed extensions and mime types
     *
     * @private
     * @param {File} file
     * @return {*}  {boolean}
     * @memberof DotDropzoneComponent
     */
    private typeMatch(file: File): boolean {
        if (!this.areValidationsEnabled()) {
            return true;
        }

        const extension = file.name.split('.').pop().toLowerCase();
        const mimeType = file.type.toLowerCase();

        const isValidType = this._accept.some(
            (type) => mimeType.includes(type) || type.includes(`.${extension}`)
        );

        return isValidType;
    }

    /**
     * Check if the validations are enabled
     *
     * @private
     * @return {*}  {boolean}
     * @memberof DotDropzoneComponent
     */
    private areValidationsEnabled(): boolean {
        return this._accept.length > 0;
    }

    /**
     * Get the files from the dataTransfer object
     *
     * @private
     * @param {DataTransfer} dataTransfer
     * @return {*}  {File[]}
     * @memberof DotDropZoneComponent
     */
    private getFiles(dataTransfer: DataTransfer): File[] {
        const { items, files } = dataTransfer;

        if (items) {
            return Array.from(items)
                .filter((item) => item.kind === 'file')
                .map((item) => item.getAsFile());
        }

        return Array.from(files) || [];
    }

    /**
     * Check if the file is too long
     *
     * @private
     * @param {File} file
     * @return {*}  {boolean}
     * @memberof DotDropZoneComponent
     */
    private isFileTooLong(file: File): boolean {
        if (!this.maxLength) {
            return false;
        }

        return file.size > this.maxLength;
    }

    /**
     * Set the validity object
     *
     * @private
     * @param {File} file
     * @memberof DotDropZoneComponent
     */
    private setValidity(files: File[]): void {
        const file = files[0]; // Only one file is allowed
        const multipleFiles = files.length > 1;
        const isFileTypeMismatch = !this.typeMatch(file);
        const isFileTooBig = this.isFileTooLong(file);
        const valid = !isFileTypeMismatch && !isFileTooBig && !multipleFiles;

        this.validity = {
            ...this.validity,
            multipleFiles,
            isFileTypeMismatch,
            isFileTooBig,
            valid
        };
    }
}

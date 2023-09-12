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
    fileTypeMismatch: boolean;
    maxFileSizeExceeded: boolean;
    multipleFilesDropped: boolean;
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
    @Output() fileDragOver = new EventEmitter<boolean>();
    @Output() fileDragLeave = new EventEmitter<boolean>();

    @Input() maxFileSize: number;

    @Input() set accept(types: string[]) {
        this._accept = types.map((type) => {
            // Remove the wildcard character
            return type.toLowerCase().replace(/\*/g, '');
        });
    }

    private _accept: string[] = [];
    private _validity: DropZoneFileValidity = {
        fileTypeMismatch: false,
        maxFileSizeExceeded: false,
        multipleFilesDropped: false,
        valid: true
    };

    get validity(): DropZoneFileValidity {
        return this._validity;
    }

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
            validity: this._validity
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
        this.fileDragOver.emit(true);
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
        if (!this._accept.length) {
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
        if (!this.maxFileSize) {
            return false;
        }

        return file.size > this.maxFileSize;
    }

    /**
     * Set the _validity object
     *
     * @private
     * @param {File} file
     * @memberof DotDropZoneComponent
     */
    private setValidity(files: File[]): void {
        const file = files[0]; // Only one file is allowed
        const multipleFilesDropped = files.length > 1;
        const fileTypeMismatch = !this.typeMatch(file);
        const maxFileSizeExceeded = this.isFileTooLong(file);
        const valid = !fileTypeMismatch && !maxFileSizeExceeded && !multipleFilesDropped;

        this._validity = {
            ...this._validity,
            multipleFilesDropped,
            fileTypeMismatch,
            maxFileSizeExceeded,
            valid
        };
    }
}

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

export enum DropZoneErrorType {
    FILE_TYPE_MISMATCH = 'FILE_TYPE_MISMATCH',
    MAX_FILE_SIZE_EXCEEDED = 'MAX_FILE_SIZE_EXCEEDED',
    MULTIPLE_FILES_DROPPED = 'MULTIPLE_FILES_DROPPED'
}

export interface DropZoneFileValidity {
    fileTypeMismatch: boolean;
    maxFileSizeExceeded: boolean;
    multipleFilesDropped: boolean;
    errorsType: DropZoneErrorType[];
    valid: boolean;
}

@Component({
    selector: 'dot-drop-zone',
    standalone: true,
    imports: [],
    templateUrl: './dot-drop-zone.component.html',
    styleUrls: ['./dot-drop-zone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDropZoneComponent {
    @Output() fileDropped = new EventEmitter<DropZoneFileEvent>();
    @Output() fileDragEnter = new EventEmitter<boolean>();
    @Output() fileDragOver = new EventEmitter<boolean>();
    @Output() fileDragLeave = new EventEmitter<boolean>();

    /*
     * Max file size in bytes.
     * See Docs: https://www.dotcms.com/docs/latest/binary-field#FieldVariables
     */
    @Input() maxFileSize: number;

    @Input() set accept(types: string[]) {
        this._accept = types
            ?.filter((value) => value !== '*/*')
            .map((type) => {
                // Remove the wildcard character
                return type.toLowerCase().replace(/\*/g, '');
            });
    }

    private _accept: string[] = [];
    private errorsType: DropZoneErrorType[] = [];
    private _validity: DropZoneFileValidity = {
        fileTypeMismatch: false,
        maxFileSizeExceeded: false,
        multipleFilesDropped: false,
        errorsType: [],
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

        if (!isValidType) {
            this.errorsType.push(DropZoneErrorType.FILE_TYPE_MISMATCH);
        }

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

        const isTooLong = file.size > this.maxFileSize;

        if (isTooLong) {
            this.errorsType.push(DropZoneErrorType.MAX_FILE_SIZE_EXCEEDED);
        }

        return isTooLong;
    }

    /**
     * Check if multiple files were dropped
     *
     * @private
     * @param {File[]} files
     * @return {*}  {boolean}
     * @memberof DotDropZoneComponent
     */
    private multipleFilesDropped(files: File[]): boolean {
        const multipleFilesDropped = files.length > 1;

        if (multipleFilesDropped) {
            this.errorsType.push(DropZoneErrorType.MULTIPLE_FILES_DROPPED);
        }

        return multipleFilesDropped;
    }

    /**
     * Set the _validity object
     *
     * @private
     * @param {File} file
     * @memberof DotDropZoneComponent
     */
    private setValidity(files: File[]): void {
        this.errorsType = []; // Reset the errors type
        const file = files[0]; // Only one file is allowed
        const multipleFilesDropped = this.multipleFilesDropped(files);
        const fileTypeMismatch = !this.typeMatch(file);
        const maxFileSizeExceeded = this.isFileTooLong(file);
        const valid = !fileTypeMismatch && !maxFileSizeExceeded && !multipleFilesDropped;

        this._validity = {
            ...this._validity,
            multipleFilesDropped,
            fileTypeMismatch,
            maxFileSizeExceeded,
            errorsType: this.errorsType,
            valid
        };
    }
}

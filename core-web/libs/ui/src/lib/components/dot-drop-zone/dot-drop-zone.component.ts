import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostBinding,
    HostListener,
    Input,
    Output
} from '@angular/core';

export enum DropZoneError {
    INVALID_FILE = 'INVALID_FILE',
    MULTIFILE_ERROR = 'MULTIFILE_ERROR'
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
    @Output() fileDrop = new EventEmitter<File>();
    @Output() fileDropError = new EventEmitter<DropZoneError>();
    @Output() fileDragEnter = new EventEmitter<boolean>();
    @Output() fileDragLeave = new EventEmitter<boolean>();

    @Input() set accept(types: string[]) {
        this._accept = types.map((type) => {
            // Remove the wildcard character
            return type.toLowerCase().replace(/\*/g, '');
        });
    }

    private _accept: string[] = [];

    @HostBinding('class.drop-zone-active')
    active = false;

    @HostBinding('class.drop-zone-error')
    error = false;

    @HostListener('drop', ['$event'])
    onDrop(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
        this.active = false;

        const { dataTransfer } = event;
        const files = this.getFiles(dataTransfer);
        const file = files[0];

        if (files.length === 0) return;

        if (files.length > 1) {
            this.emitError(DropZoneError.MULTIFILE_ERROR);

            return;
        }

        if (!this.isValidFile(file)) {
            this.emitError(DropZoneError.INVALID_FILE);

            return;
        }

        dataTransfer.items?.clear();
        dataTransfer.clearData();
        this.fileDrop.emit(file);
    }

    @HostListener('dragenter', ['$event'])
    onDragEnter(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
        this.setActive();
        this.fileDragEnter.emit(true);
    }

    @HostListener('dragover', ['$event'])
    onDragOver(event: DragEvent) {
        // Prevent the default behavior to allow drop
        event.stopPropagation();
        event.preventDefault();
        this.setActive();
    }

    @HostListener('dragleave', ['$event'])
    onDragLeave(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
        this.setDesactive();
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
    private isValidFile(file: File): boolean {
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
     * Emit the error event
     *
     * @private
     * @param {DropZoneError} error
     * @memberof DotDropZoneComponent
     */
    private emitError(error: DropZoneError) {
        this.error = true;
        this.fileDropError.emit(error);
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
     *  Set the active state to true and error to false
     *
     * @private
     * @memberof DotDropZoneComponent
     */
    private setActive(): void {
        this.active = true;
        this.error = false;
    }

    /**
     * Set the active state to false and error to false
     *
     * @private
     * @memberof DotDropZoneComponent
     */
    private setDesactive(): void {
        this.active = false;
        this.error = false;
    }
}

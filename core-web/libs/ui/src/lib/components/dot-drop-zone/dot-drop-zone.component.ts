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
    @Output() dropZoneError = new EventEmitter<DropZoneError>();
    @Output() dragStart = new EventEmitter<boolean>();
    @Output() dragStop = new EventEmitter<boolean>();

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

        if (this.error) return;

        const { dataTransfer } = event;
        const { items, files } = dataTransfer;
        const file = items ? Array.from(items)[0].getAsFile() : Array.from(files)[0];

        if (!this.isValidFile(file)) {
            this.error = true;
            this.dropZoneError.emit(DropZoneError.INVALID_FILE);

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
        this.dragStart.emit(true);

        const { items, files } = event.dataTransfer;
        const length = items ? items.length : files.length;
        const multipleFile = length > 1;

        if (multipleFile) {
            this.dropZoneError.emit(DropZoneError.MULTIFILE_ERROR);
        }

        this.error = multipleFile;
        this.active = !multipleFile;
    }

    @HostListener('dragover', ['$event'])
    onDragOver(event: DragEvent) {
        // Prevent the default behavior to allow drop
        event.stopPropagation();
        event.preventDefault();
    }

    @HostListener('dragleave', ['$event'])
    onDragLeave(_event: DragEvent) {
        this.dragStop.emit(true);
        this.active = false;
        this.error = false;
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
}

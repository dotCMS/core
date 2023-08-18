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

    @Input() allowedExtensions: string[] = [];
    @Input() set allowedMimeTypes(mineTypes: string[]) {
        this._allowedMimeTypes = mineTypes.map((type) => {
            // Remove the wildcard character
            return type.toLowerCase().replace('*', '');
        });
    }

    private _allowedMimeTypes: string[] = [];

    @HostBinding('class.drop-zone-active')
    active = false;

    @HostBinding('class.drop-zone-error')
    invalidFile = false;

    @HostBinding('class.drop-zone-error')
    multiFileError = false;

    @HostListener('drop', ['$event'])
    onDrop(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
        this.active = false;

        if (this.invalidFile) return;

        const { dataTransfer } = event;
        const { items, files } = dataTransfer;
        const file = items ? Array.from(items)[0].getAsFile() : Array.from(files)[0];

        if (!this.isValidFile(file)) {
            this.invalidFile = true;

            return;
        }

        dataTransfer.items?.clear();
        dataTransfer.clearData();
        this.fileDrop.emit(file);
    }

    @HostListener('dragover', ['$event'])
    onDragOver(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();

        const { items, files } = event.dataTransfer;
        const length = items ? items.length : files.length;
        const multiple = length > 1;

        this.multiFileError = multiple; // Only show the error when multiple files are dropped
        this.active = !multiple; // Only show the active state when a single file is dropped
    }

    @HostListener('dragleave', ['$event'])
    onDragLeave(_event: DragEvent) {
        this.active = false;
        this.invalidFile = false;
        this.multiFileError = false;
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

        return (
            this.allowedExtensions.includes(`.${extension}`) ||
            this._allowedMimeTypes.includes(mimeType)
        );
    }

    /**
     * Check if the validations are enabled
     *
     * @private
     * @return {*}  {boolean}
     * @memberof DotDropzoneComponent
     */
    private areValidationsEnabled(): boolean {
        return this.allowedExtensions.length > 0 || this._allowedMimeTypes.length > 0;
    }
}

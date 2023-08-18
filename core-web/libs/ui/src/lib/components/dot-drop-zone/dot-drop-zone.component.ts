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
    @Output() fileDrop = new EventEmitter<File[]>();

    @Input() multiple = false;
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

    @HostListener('drop', ['$event'])
    onDrop(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
        const { dataTransfer } = event;
        this.active = false;

        if (dataTransfer.items) {
            const files = Array.from(dataTransfer.items)
                .filter((item) => item.kind === 'file') // Only files
                .map((item) => item.getAsFile()) // Get file
                .filter((file) => this.isValidFile(file)); // Filter by extension and mime type

            dataTransfer.items.clear();
            this.fileDrop.emit(files);
        } else if (dataTransfer.files) {
            const files = Array.from(dataTransfer.files).filter((file) => this.isValidFile(file));

            dataTransfer.clearData();
            this.fileDrop.emit(files);
        }
    }

    @HostListener('dragover', ['$event'])
    onDragOver(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
        this.active = true;
    }

    @HostListener('dragleave', ['$event'])
    onDragLeave(_event: DragEvent) {
        this.active = false;
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
        if (this.areValidationsEnabled()) {
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

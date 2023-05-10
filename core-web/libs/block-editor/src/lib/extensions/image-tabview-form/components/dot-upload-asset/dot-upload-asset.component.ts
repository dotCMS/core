import { Component, EventEmitter, Output, Input } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotImageService } from '../../../image-uploader/services/dot-image/dot-image.service';

export enum STATUS {
    SELECT = 'SELECT',
    PREVIEW = 'PREVIEW',
    UPLOAD = 'UPLOAD'
}

@Component({
    selector: 'dot-upload-asset',
    templateUrl: './dot-upload-asset.component.html',
    styleUrls: ['./dot-upload-asset.component.scss']
})
export class DotUploadAssetComponent {
    @Output()
    uploadedFile = new EventEmitter<DotCMSContentlet>();

    @Input()
    acceptedTypes = 'image/*';
    public status = STATUS.SELECT;
    public file: File;

    constructor(
        private readonly sanitizer: DomSanitizer,
        private readonly imageService: DotImageService
    ) {}

    /**
     * Set Selected File
     *
     * @param {*} event
     * @memberof DotUploadAssetComponent
     */
    onSelectFile(event) {
        const file = event.files[0];
        file.objectURL = this.sanitizer.bypassSecurityTrustUrl(window.URL.createObjectURL(file));
        this.file = file;
        this.status = STATUS.PREVIEW;
    }

    /**
     * Remove Current Selected File.
     *
     * @memberof DotUploadAssetComponent
     */
    removeFile() {
        this.file = null;
        this.status = STATUS.SELECT;
    }

    /**
     * Upload the selected File to dotCMS
     *
     * @memberof DotUploadAssetComponent
     */
    uploadFile() {
        this.status = STATUS.UPLOAD;
        this.imageService.publishContent({ data: this.file }).subscribe((data) => {
            const contentlet = data[0];
            this.uploadedFile.emit(contentlet[Object.keys(contentlet)[0]]);
            this.status = STATUS.SELECT;
        });
    }
}

import { Component, EventEmitter, Output } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotImageService } from '../../../image-uploader/services/dot-image/dot-image.service';

enum STATUS {
    SELECT = 'SELECT',
    PREVIEW = 'PREVIEW',
    UPLOAD = 'UPLOAD'
}

@Component({
    selector: 'dot-upload-tab',
    templateUrl: './dot-upload-tab.component.html',
    styleUrls: ['./dot-upload-tab.component.scss']
})
export class DotUploadTabComponent {
    @Output()
    uploadedFile = new EventEmitter<DotCMSContentlet>();

    public status = STATUS.SELECT;
    public file: File;

    constructor(
        private readonly sanitizer: DomSanitizer,
        private readonly imageService: DotImageService
    ) {}

    onSelect(event) {
        const file = event.files[0];
        file.objectURL = this.sanitizer.bypassSecurityTrustUrl(window.URL.createObjectURL(file));
        this.file = file;
        this.status = STATUS.PREVIEW;
    }

    onBack() {
        this.file = null;
        this.status = STATUS.SELECT;
    }

    onInsert() {
        this.status = STATUS.UPLOAD;
        this.imageService.publishContent({ data: this.file }).subscribe((data) => {
            const contentlet = data[0];
            this.uploadedFile.emit(contentlet[Object.keys(contentlet)[0]]);
        });
    }
}

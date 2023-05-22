import {
    Component,
    EventEmitter,
    Output,
    Input,
    ChangeDetectorRef,
    ChangeDetectionStrategy
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { DotImageService } from '../../../image-uploader/services/dot-image/dot-image.service';

export enum STATUS {
    SELECT = 'SELECT',
    PREVIEW = 'PREVIEW',
    UPLOAD = 'UPLOAD'
}

@Component({
    selector: 'dot-upload-asset',
    templateUrl: './dot-upload-asset.component.html',
    styleUrls: ['./dot-upload-asset.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUploadAssetComponent {
    @Output()
    uploadedFile = new EventEmitter<DotCMSContentlet>();

    @Input()
    type: EditorAssetTypes;
    public status = STATUS.SELECT;
    public file: File;
    public src: string | ArrayBuffer;

    constructor(
        private readonly sanitizer: DomSanitizer,
        private readonly imageService: DotImageService,
        private readonly cd: ChangeDetectorRef
    ) {}

    /**
     * Set Selected File
     *
     * @param {File[]} files
     * @memberof DotUploadAssetComponent
     */
    onSelectFile(files: File[]) {
        const file = files[0];
        const reader = new FileReader();
        reader.onload = (e) => this.setFile(file, e.target.result);
        // Allows us to get a secure url without usin the Angular bypasssecuritytrusthtml
        reader.readAsDataURL(file);
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

    /**
     * Set vide File and asset src.
     *
     * @private
     * @param {File} file
     * @param {string | ArrayBuffer} src
     * @memberof DotUploadAssetComponent
     */
    private setFile(file: File, src: string | ArrayBuffer): void {
        this.file = file;
        this.src = src;
        this.status = STATUS.PREVIEW;
        this.cd.markForCheck();
    }
}

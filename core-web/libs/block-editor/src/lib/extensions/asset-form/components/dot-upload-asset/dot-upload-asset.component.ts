import { throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    Component,
    EventEmitter,
    Output,
    Input,
    ChangeDetectorRef,
    ChangeDetectionStrategy
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { catchError } from 'rxjs/operators';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { DotImageService } from '../../../image-uploader/services/dot-image/dot-image.service';

export enum STATUS {
    SELECT = 'SELECT',
    PREVIEW = 'PREVIEW',
    UPLOAD = 'UPLOAD',
    ERROR = 'ERROR'
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

    @Output()
    preventClose = new EventEmitter<boolean>();

    @Input()
    type: EditorAssetTypes;

    public status = STATUS.SELECT;
    public file: File;
    public src: string | ArrayBuffer;
    public error: string;

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
        this.preventClose.emit(true);
        this.imageService
            .publishContent({ data: this.file })
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    this.status = STATUS.ERROR;
                    this.preventClose.emit(false);
                    this.error = error?.error?.errors[0] || error.error;

                    console.error(error);

                    return throwError(error);
                })
            )
            .subscribe((data) => {
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

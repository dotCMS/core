import { Subscription, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    Component,
    EventEmitter,
    Output,
    Input,
    ChangeDetectorRef,
    ChangeDetectionStrategy,
    OnDestroy,
    HostListener,
    ElementRef
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { catchError, take } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { shakeAnimation } from './animations';

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
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [shakeAnimation]
})
export class DotUploadAssetComponent implements OnDestroy {
    @Output()
    uploadedFile = new EventEmitter<DotCMSContentlet>();

    @Output()
    preventClose = new EventEmitter<boolean>();

    @Output()
    hide = new EventEmitter<boolean>();

    @Input()
    type: EditorAssetTypes;

    public status = STATUS.SELECT;
    public file: File;
    public src: string | ArrayBuffer | SafeResourceUrl;
    public error: string;
    public animation = 'shakeend';
    public $uploadRequestSubs: Subscription;
    public controller: AbortController;

    get errorMessage() {
        return ` Don't close this window while the ${this.type} uploads`;
    }

    @HostListener('window:click', ['$event.target']) onClick(e) {
        const clickedOutside = !this.el.nativeElement.contains(e);

        // If it's uploading and the user click outside the component, shake the message
        if (this.status === STATUS.UPLOAD && clickedOutside) {
            this.shakeMe();
        }
    }

    constructor(
        private readonly sanitizer: DomSanitizer,
        private readonly dotUploadFileService: DotUploadFileService,
        private readonly cd: ChangeDetectorRef,
        private readonly el: ElementRef
    ) {}

    ngOnDestroy(): void {
        this.preventClose.emit(false);
    }

    /**
     * Set Selected File
     *
     * @param {File[]} files
     * @memberof DotUploadAssetComponent
     */
    onSelectFile(files: File[]) {
        const file = files[0];
        const reader = new FileReader();
        this.preventClose.emit(true);
        reader.onload = (e) => this.setFile(file, e.target.result);

        /*
         * We can not use `reader.readDataAsUrl()` method because of this:
         * https://stackoverflow.com/questions/40325410/filereader-is-unable-to-read-large-files
         *
         */
        reader.readAsArrayBuffer(file);
    }

    /**
     * Remove Current Selected File.
     *
     * @memberof DotUploadAssetComponent
     */
    cancelAction() {
        this.file = null;
        this.status = STATUS.SELECT;

        this.cancelUploading();
        this.hide.emit(true);
    }
    /**
     * End the uploading message animation
     *
     * @memberof DotUploadAssetComponent
     */
    shakeEnd() {
        this.animation = 'shakeend';
    }

    /**
     * Shake the uploading message
     *
     * @private
     * @return {*}
     * @memberof DotUploadAssetComponent
     */
    private shakeMe() {
        if (this.animation === 'shakestart') {
            return; // already shaking
        }

        this.animation = 'shakestart';
    }

    /**
     * Upload the selected File to dotCMS
     *
     * @memberof DotUploadAssetComponent
     */
    private uploadFile() {
        this.controller = new AbortController();
        this.status = STATUS.UPLOAD;
        this.$uploadRequestSubs = this.dotUploadFileService
            .publishContent({ data: this.file, signal: this.controller.signal })
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => this.handleError(error))
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
     * @param {string | ArrayBuffer} buffer
     * @memberof DotUploadAssetComponent
     */
    private setFile(file: File, buffer: string | ArrayBuffer): void {
        // Convert the buffer to a blob:
        const videoBlob = new Blob([new Uint8Array(buffer as ArrayBufferLike)], {
            type: 'video/mp4'
        });

        this.src = this.sanitizer.bypassSecurityTrustResourceUrl(URL.createObjectURL(videoBlob));
        this.file = file;
        this.status = STATUS.UPLOAD;
        this.uploadFile();
        this.cd.markForCheck();
    }

    /**
     * Handle error on upload file.
     *
     * @private
     * @param {HttpErrorResponse} error
     * @return {*}
     * @memberof DotUploadAssetComponent
     */
    private handleError(error: HttpErrorResponse) {
        this.status = STATUS.ERROR;
        this.preventClose.emit(false);
        this.error = error?.error?.errors[0] || error.error;

        console.error(error);

        return throwError(error);
    }

    private cancelUploading(): void {
        this.$uploadRequestSubs.unsubscribe();
        this.controller?.abort();
    }
}

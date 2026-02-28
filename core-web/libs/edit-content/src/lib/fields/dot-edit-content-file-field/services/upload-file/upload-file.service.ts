import { from, Observable } from 'rxjs';

import { inject, Injectable } from '@angular/core';

import { map, switchMap, tap } from 'rxjs/operators';

import { DotContentletService, DotUploadFileService, DotUploadService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import { checkMimeType } from '@dotcms/utils';

import { UploadedFile, UPLOAD_TYPE } from '../../../../models/dot-edit-content-file.model';

export type UploadFileProps = {
    file: File | string;
    uploadType: UPLOAD_TYPE;
    acceptedFiles: string[];
    maxSize: string | null;
    abortSignal?: AbortSignal;
};

@Injectable()
export class DotFileFieldUploadService {
    readonly #fileService = inject(DotUploadFileService);
    readonly #tempFileService = inject(DotUploadService);
    readonly #dotContentletService = inject(DotContentletService);

    /**
     * Uploads a file or a string as a dotAsset contentlet.
     *
     * If a File is passed, it will be uploaded and the asset will be created
     * with the file name as the contentlet name.
     *
     * If a string is passed, it will be used as the asset id.
     *
     * @param file The file to be uploaded or the asset id.
     * @param uploadType The type of upload, can be 'temp' or 'contentlet'.
     * @returns An observable that resolves to the created contentlet.
     */
    uploadFile(params: UploadFileProps): Observable<UploadedFile> {
        const { file, uploadType, acceptedFiles } = params;

        if (uploadType === 'temp') {
            return this.uploadTempFile(file, acceptedFiles, params?.abortSignal).pipe(
                map((file) => ({ source: 'temp', file }))
            );
        }

        const uploadProcess =
            file instanceof File
                ? this.uploadDotAssetByFile(file, acceptedFiles)
                : this.uploadDotAssetByUrl(file, acceptedFiles, params?.abortSignal);

        return uploadProcess.pipe(map((file) => ({ source: 'contentlet', file })));
    }

    /**
     * Uploads a file to the temp service.
     * @param file The file to be uploaded, can be a File or a string.
     * @param acceptedFiles The accepted mime types.
     * @returns An observable that resolves to the uploaded temp file.
     * If the file type is not in the accepted types, it will throw an error.
     */
    uploadTempFile(
        file: File | string,
        acceptedFiles: string[],
        signal?: AbortSignal
    ): Observable<DotCMSTempFile> {
        return from(this.#tempFileService.uploadFile({ file, signal })).pipe(
            tap((tempFile) => {
                if (!checkMimeType(tempFile, acceptedFiles)) {
                    throw new Error('Invalid file type');
                }
            })
        );
    }

    /**
     * Uploads a file as a dotAsset contentlet.
     *
     * @param file The file to be uploaded.
     * @param acceptedFiles The accepted mime types.
     * @returns An observable that resolves to the created contentlet.
     * If the file type is not in the accepted types, it will throw an error.
     */
    uploadDotAssetByFile(file: File, acceptedFiles: string[]): Observable<DotCMSContentlet> {
        return this.uploadDotAsset(file).pipe(
            tap((file) => {
                if (!checkMimeType(file, acceptedFiles)) {
                    throw new Error('Invalid file type');
                }
            })
        );
    }

    /**
     * Uploads a file to the temp service and then as a dotAsset contentlet.
     * @param file The url of the file to be uploaded.
     * @param acceptedFiles The accepted mime types.
     * @returns An observable that resolves to the created contentlet.
     * If the file type is not in the accepted types, it will throw an error.
     */
    uploadDotAssetByUrl(
        file: string,
        acceptedFiles: string[],
        signal?: AbortSignal
    ): Observable<DotCMSContentlet> {
        return this.uploadTempFile(file, acceptedFiles, signal).pipe(
            switchMap((tempFile) => this.uploadDotAsset(tempFile.id))
        );
    }

    /**
     * Uploads a file and returns a contentlet with the file metadata and id.
     * @param file the file to be uploaded
     * @returns a contentlet with the file metadata and id
     */
    uploadDotAsset(file: File | string) {
        return this.#fileService.uploadDotAssetWithContent(file);
    }

    /**
     * Returns a contentlet by its identifier and adds the content if it's a editable as text file.
     * @param identifier the identifier of the contentlet
     * @returns a contentlet with the content if it's a editable as text file
     */
    getContentById(identifier: string) {
        return this.#dotContentletService.getContentletByInodeWithContent(identifier);
    }
}

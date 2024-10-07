import { from, Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotUploadFileService, DotUploadService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../../../services/dot-edit-content.service';
import { UploadedFile, UPLOAD_TYPE } from '../../models';
import { getFileMetadata, getFileVersion } from '../../utils';

@Injectable()
export class DotFileFieldUploadService {
    readonly #fileService = inject(DotUploadFileService);
    readonly #tempFileService = inject(DotUploadService);
    readonly #contentService = inject(DotEditContentService);
    readonly #httpClient = inject(HttpClient);

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
    uploadFile({
        file,
        uploadType
    }: {
        file: File | string;
        uploadType: UPLOAD_TYPE;
    }): Observable<UploadedFile> {
        if (uploadType === 'temp') {
            return from(this.#tempFileService.uploadFile({ file })).pipe(
                map((tempFile) => ({ source: 'temp', file: tempFile }))
            );
        } else {
            if (file instanceof File) {
                return this.uploadDotAsset(file).pipe(
                    map((file) => ({ source: 'contentlet', file }))
                );
            }

            return from(this.#tempFileService.uploadFile({ file })).pipe(
                switchMap((tempFile) => this.uploadDotAsset(tempFile.id)),
                map((file) => ({ source: 'contentlet', file }))
            );
        }
    }
    /**
     * Uploads a file and returns a contentlet with the file metadata and id.
     * @param file the file to be uploaded
     * @returns a contentlet with the file metadata and id
     */
    uploadDotAsset(file: File | string) {
        return this.#fileService
            .uploadDotAsset(file)
            .pipe(switchMap((contentlet) => this.#addContent(contentlet)));
    }

    /**
     * Returns a contentlet by its identifier and adds the content if it's a editable as text file.
     * @param identifier the identifier of the contentlet
     * @returns a contentlet with the content if it's a editable as text file
     */
    getContentById(identifier: string) {
        return this.#contentService
            .getContentById(identifier)
            .pipe(switchMap((contentlet) => this.#addContent(contentlet)));
    }

    /**
     * Adds the content of a contentlet if it's a editable as text file.
     * @param contentlet the contentlet to be processed
     * @returns a contentlet with the content if it's a editable as text file, otherwise the original contentlet
     */
    #addContent(contentlet: DotCMSContentlet) {
        const { editableAsText } = getFileMetadata(contentlet);
        const contentURL = getFileVersion(contentlet);

        if (editableAsText && contentURL) {
            return this.#getContentFile(contentURL).pipe(
                map((content) => ({ ...contentlet, content }))
            );
        }

        return of(contentlet);
    }

    /**
     * Downloads the content of a file by its URL.
     * @param contentURL the URL of the file content
     * @returns an observable of the file content
     */
    #getContentFile(contentURL: string) {
        return this.#httpClient.get(contentURL, { responseType: 'text' });
    }
}

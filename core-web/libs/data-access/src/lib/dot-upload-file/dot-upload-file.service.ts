import { from, Observable, of, throwError } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, pluck, switchMap } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import { getFileMetadata, getFileVersion } from '@dotcms/utils';

import { DotUploadService } from '../dot-upload/dot-upload.service';
import {
    DotActionRequestOptions,
    DotWorkflowActionsFireService
} from '../dot-workflow-actions-fire/dot-workflow-actions-fire.service';

export enum FileStatus {
    DOWNLOAD = 'DOWNLOADING',
    IMPORT = 'IMPORTING',
    COMPLETED = 'COMPLETED',
    ERROR = 'ERROR'
}

interface PublishContentProps {
    data: string | File;
    maxSize?: string;
    statusCallback?: (status: FileStatus) => void;
    signal?: AbortSignal;
}

/**
 *
 * @export
 * @class DotImageService
 */
@Injectable({ providedIn: 'root' })
export class DotUploadFileService {
    readonly #BASE_URL = '/api/v1/workflow/actions/default';
    readonly #http = inject(HttpClient);
    readonly #uploadService = inject(DotUploadService);
    readonly #workflowActionsFireService = inject(DotWorkflowActionsFireService);

    publishContent({
        data,
        maxSize,
        statusCallback = (_status) => {
            /* */
        },
        signal
    }: PublishContentProps): Observable<DotCMSContentlet[]> {
        statusCallback(FileStatus.DOWNLOAD);

        return this.setTempResource({ data, maxSize, signal }).pipe(
            switchMap((response: DotCMSTempFile | DotCMSTempFile[]) => {
                const files = Array.isArray(response) ? response : [response];
                const contentlets: Record<string, string>[] = [];
                files.forEach((file: DotCMSTempFile) => {
                    contentlets.push({
                        baseType: 'dotAsset',
                        asset: file.id,
                        hostFolder: '',
                        indexPolicy: 'WAIT_FOR'
                    });
                });

                statusCallback(FileStatus.IMPORT);

                return this.#http
                    .post(`${this.#BASE_URL}/fire/PUBLISH`, JSON.stringify({ contentlets }), {
                        headers: {
                            Origin: window.location.hostname,
                            'Content-Type': 'application/json;charset=UTF-8'
                        }
                    })
                    .pipe(pluck('entity', 'results')) as Observable<DotCMSContentlet[]>;
            }),
            catchError((error) => throwError(error))
        );
    }

    private setTempResource({
        data: file,
        maxSize,
        signal
    }: PublishContentProps): Observable<DotCMSTempFile | DotCMSTempFile[]> {
        return from(
            this.#uploadService.uploadFile({
                file,
                maxSize,
                signal
            })
        );
    }

    /**
     * Uploads a file or a string as a dotAsset contentlet.
     *
     * If a File is passed, it will be uploaded and the asset will be created
     * with the file name as the contentlet name.
     *
     * If a string is passed, it will be used as the asset id.
     *
     * @param file The file to be uploaded or the asset id.
     * @param extraData Additional data to be included in the contentlet object. This will be merged with
     * the base contentlet data in the request body.
     * @returns An observable that resolves to the created contentlet.
     */
    uploadDotAsset(
        file: File | string,
        extraData?: DotActionRequestOptions['data']
    ): Observable<DotCMSContentlet> {
        if (file instanceof File) {
            const formData = new FormData();
            formData.append('file', file);

            return this.#workflowActionsFireService.newContentlet<DotCMSContentlet>(
                'dotAsset',
                { file: file.name, ...extraData },
                formData
            );
        }

        return this.#workflowActionsFireService.newContentlet<DotCMSContentlet>('dotAsset', {
            asset: file
        });
    }

    /**
     * Uploads a file and returns a contentlet with the content if it's a editable as text file.
     * @param file the file to be uploaded
     * @param extraData additional data to be included in the contentlet object
     * @returns a contentlet with the content if it's a editable as text file
     */
    uploadDotAssetWithContent(
        file: File | string,
        extraData?: DotActionRequestOptions['data']
    ): Observable<DotCMSContentlet> {
        return this.uploadDotAsset(file, extraData).pipe(
            switchMap((contentlet) => this.addContent(contentlet))
        );
    }

    /**
     * Adds the content of a contentlet if it's a editable as text file.
     * @param contentlet the contentlet to be processed
     * @returns a contentlet with the content if it's a editable as text file, otherwise the original contentlet
     */
    addContent(contentlet: DotCMSContentlet): Observable<DotCMSContentlet> {
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
        return this.#http.get(contentURL, { responseType: 'text' });
    }
}

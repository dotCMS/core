import { from, Observable, throwError } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, pluck, switchMap } from 'rxjs/operators';

import { DotUploadService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

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
@Injectable()
export class DotUploadFileService {
    readonly #BASE_URL = '/api/v1/workflow/actions/default';
    readonly #httpClient = inject(HttpClient);
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

                return this.#httpClient
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
     * Uploads a file to dotCMS and creates a new dotAsset contentlet
     * @param file the file to be uploaded
     * @returns an Observable that emits the created contentlet
     */
    uploadDotAsset(file: File) {
        const formData = new FormData();
        formData.append('file', file);

        return this.#workflowActionsFireService.newContentlet<DotCMSContentlet>(
            'dotAsset',
            { file: file.name },
            formData
        );
    }
}

import { from, Observable, of, throwError } from 'rxjs';

import { HttpClient, HttpEventType, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, filter, map, switchMap } from 'rxjs/operators';

import {
    DotBulkUploadEvent,
    DotBulkUploadForm,
    DotBulkUploadSubmitResponse,
    DotCMSContentlet,
    DotCMSTempFile
} from '@dotcms/dotcms-models';
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
    /** The batch endpoint. Job-backed, so it answers a handle rather than a contentlet. */
    readonly #BULK_UPLOAD_URL = '/api/v1/assets/_bulkupload';
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
                    .post<{ entity: { results: DotCMSContentlet[] } }>(
                        `${this.#BASE_URL}/fire/PUBLISH`,
                        JSON.stringify({ contentlets }),
                        {
                            headers: {
                                Origin: window.location.hostname,
                                'Content-Type': 'application/json;charset=UTF-8'
                            }
                        }
                    )
                    .pipe(map((x) => x?.entity?.results));
            }),
            catchError((error) => throwError(() => error))
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
     * Uploads a file by resolving the content type from a base type instead of an explicit content
     * type. The base type is sent to the backend, which resolves the matching content type for it
     * (e.g. `FILEASSET` → File Asset, `DOTASSET` → dotAsset).
     *
     * @param file The file to be uploaded or the asset id.
     * @param baseType The base type to create the contentlet as (e.g. `FILEASSET`, `DOTASSET`).
     * @param extraData Additional data to be included in the contentlet object. This will be merged
     * with the base contentlet data in the request body.
     * @returns An observable that resolves to the created contentlet.
     */
    uploadFileByBaseType(
        file: File | string,
        baseType: string,
        extraData?: DotActionRequestOptions['data']
    ): Observable<DotCMSContentlet> {
        if (file instanceof File) {
            const formData = new FormData();
            formData.append('file', file);

            return this.#workflowActionsFireService.newContentletByBaseType<DotCMSContentlet>(
                baseType,
                { file: file.name, ...extraData },
                formData
            );
        }

        return this.#workflowActionsFireService.newContentletByBaseType<DotCMSContentlet>(
            baseType,
            {
                asset: file
            }
        );
    }

    /**
     * Submits several files as **one batch**, to be created in the background.
     *
     * The plural sibling of {@link uploadFileByBaseType}, and deliberately a separate method rather
     * than the same one taking an array: the two answer differently. The singular creates the
     * contentlet and hands it back, so a caller can show the row it just made. This one is
     * job-backed and answers `202` with a handle before any file exists, so following the run is
     * the caller's next move (`DotJobService`) and the outcome arrives later.
     *
     * That is also why the singular is untouched. The Asset Picker calls it and cannot follow a
     * job: it runs inside the legacy editor host, which has no `Router`.
     *
     * @param files Every file the author chose, in the order they chose them. That order is
     *     preserved in the outcome, which is how a per-file result is matched back to a file.
     * @param form Target and base type for the whole batch, plus the declared total size.
     * @returns The accepted run's handle. Nothing has been created yet.
     */
    uploadFilesByBaseType(files: File[], form: DotBulkUploadForm): Observable<DotBulkUploadEvent> {
        if (!files.length) {
            // Nothing to create, so nothing worth a request — and an empty batch is one of the
            // submissions the server refuses anyway.
            return throwError(() => new Error('A batch needs at least one file'));
        }

        const body = new FormData();

        files.forEach((file) => body.append('files', file));
        // A Blob rather than a string, so the part carries `application/json` and the server reads
        // it with Jackson instead of receiving text/plain.
        body.append(
            'form',
            new Blob(
                [
                    JSON.stringify({
                        ...form,
                        // Summed here rather than left to the caller: the server's early refusal
                        // exists only where a total is declared, and this is the layer that holds
                        // the files. A caller-supplied total still wins, so a caller that knows
                        // better can say so.
                        totalSizeBytes:
                            form.totalSizeBytes ??
                            files.reduce((total, file) => total + file.size, 0)
                    })
                ],
                { type: 'application/json' }
            )
        );

        return this.#http
            .post<{
                entity: DotBulkUploadSubmitResponse;
            }>(this.#BULK_UPLOAD_URL, body, { reportProgress: true, observe: 'events' })
            .pipe(
                // Only the two events a caller can act on. `Sent` and the response headers say
                // nothing it can report, and passing them through would make every consumer
                // re-implement this filter.
                filter(
                    (event) =>
                        event.type === HttpEventType.UploadProgress ||
                        event.type === HttpEventType.Response
                ),
                map(
                    (event): DotBulkUploadEvent =>
                        event.type === HttpEventType.UploadProgress
                            ? {
                                  kind: 'progress',
                                  loaded: event.loaded,
                                  // Left undefined rather than defaulted to zero: absent means the
                                  // browser could not compute a length, and a caller needs to tell that
                                  // apart from a body of nothing.
                                  total: event.total
                              }
                            : {
                                  kind: 'accepted',
                                  // `body` is nullable on HttpResponse. A `202` with no body would mean
                                  // no handle, and there is nothing honest to invent — the caller would
                                  // have a run it cannot follow, so this fails rather than fabricating.
                                  handle: nonNullHandle(
                                      event as HttpResponse<{
                                          entity: DotBulkUploadSubmitResponse;
                                      }>
                                  )
                              }
                )
            );
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

/** Reads the accepted batch's handle, refusing a `202` that carried none. */
function nonNullHandle(
    response: HttpResponse<{ entity: DotBulkUploadSubmitResponse }>
): DotBulkUploadSubmitResponse {
    const handle = response.body?.entity;

    if (!handle) {
        throw new Error('The batch was accepted without a handle to follow it by');
    }

    return handle;
}

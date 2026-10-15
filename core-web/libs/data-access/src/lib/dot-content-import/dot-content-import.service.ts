import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotCMSAPIResponse,
    DotCMSContentType,
    DotContentImportForm,
    DotContentTypePaginationOptions,
    DotImportContentType,
    DotImportJob,
    DotImportJobCreated,
    DotImportJobParameters,
    DotPagination
} from '@dotcms/dotcms-models';

import { DotJobQueueService } from '../dot-job-queue/dot-job-queue.service';

/** A running import, as restored after a reload (#37896). */
export interface DotActiveImport {
    id: string;
    userId?: string;
    /** The CSV's name, or the content type when the server didn't record one. */
    fileName?: string;
}

/** Base path of the content import endpoints. */
export const DOT_CONTENT_IMPORT_URL = '/api/v1/content/_import';

/**
 * Talks to the content import endpoints (`/api/v1/content/_import`, #37896): the content types a
 * user can import into, the synchronous preview, the import job, and the sample CSV.
 */
@Injectable({ providedIn: 'root' })
export class DotContentImportService {
    readonly #http = inject(HttpClient);
    readonly #jobQueue = inject(DotJobQueueService);

    /**
     * One page of the content types the current user can import into: those they can write to,
     * except Host, sorted by name, optionally narrowed by a search on name or variable. The server
     * applies the permission rule and pages it.
     *
     * The answer has the shape the shared content type selector reads, so it can be handed to that
     * selector as its page loader. The endpoint sends only each type's id, variable, name, base type
     * and icon, which are all the selector uses.
     *
     * @param options the page, its size and the search
     */
    getWritableContentTypes(
        options: DotContentTypePaginationOptions
    ): Observable<{ contentTypes: DotCMSContentType[]; pagination: DotPagination }> {
        let params = new HttpParams();
        if (options.page !== undefined) {
            params = params.set('page', options.page);
        }

        if (options.per_page !== undefined) {
            params = params.set('per_page', options.per_page);
        }

        if (options.filter) {
            params = params.set('filter', options.filter);
        }

        return this.#http
            .get<
                DotCMSAPIResponse<DotImportContentType[]> & { pagination: DotPagination }
            >(`${DOT_CONTENT_IMPORT_URL}/contenttypes`, { params })
            .pipe(
                map((response) => ({
                    contentTypes: response.entity as unknown as DotCMSContentType[],
                    pagination: response.pagination
                }))
            );
    }

    /**
     * Runs the preview: a dry run of the import that writes nothing, answered in the same request
     * as Content Search's is, with the finished job and its result.
     *
     * @param file the CSV to import
     * @param form the import options; `language` and `site` are always sent
     */
    preview(file: File, form: DotContentImportForm): Observable<DotImportJob> {
        return this.#http
            .post<
                DotCMSAPIResponse<DotImportJob>
            >(`${DOT_CONTENT_IMPORT_URL}/_preview`, this.#multipart(file, form))
            .pipe(map((response) => response.entity));
    }

    /**
     * Starts the import itself, with the file and options the user previewed.
     *
     * @param file the CSV to import
     * @param form the import options; `language` and `site` are always sent
     */
    importContent(file: File, form: DotContentImportForm): Observable<DotImportJobCreated> {
        return this.#http
            .post<
                DotCMSAPIResponse<DotImportJobCreated>
            >(DOT_CONTENT_IMPORT_URL, this.#multipart(file, form))
            .pipe(map((response) => response.entity));
    }

    /**
     * The imports still running on the server, so the status toast can come back after a reload.
     * Previews are left out: they belong to an import dialog, which a reload closes.
     */
    readActiveImports(): Observable<DotActiveImport[]> {
        return this.#jobQueue
            .readActiveJobs<DotImportJobParameters, DotActiveImport | null>(
                'importContentlets',
                (job) =>
                    job.parameters?.cmd === 'publish'
                        ? {
                              id: job.id,
                              userId: job.parameters.userId,
                              fileName: job.parameters.fileName ?? job.parameters.contentType
                          }
                        : null
            )
            .pipe(map((runs) => runs.filter((run): run is DotActiveImport => run !== null)));
    }

    /**
     * The sample CSV Content Search offers for a content type: a header row of its importable
     * fields and a row of placeholders showing each field's format.
     *
     * @param contentType the content type's id or variable
     */
    downloadSample(contentType: string): Observable<Blob> {
        return this.#http.get(`${DOT_CONTENT_IMPORT_URL}/_sample`, {
            params: { contentType },
            responseType: 'blob'
        });
    }

    /** The multipart body the import endpoints take: the CSV, and the options as a JSON part. */
    #multipart(file: File, form: DotContentImportForm): FormData {
        const body = new FormData();
        body.append('file', file);
        body.append('form', new Blob([JSON.stringify(form)], { type: 'application/json' }));

        return body;
    }
}

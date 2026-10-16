import { merge, Observable, timer } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { exhaustMap, filter, map, switchMap, take } from 'rxjs/operators';

import {
    DotCMSAPIResponse,
    DotContentImportForm,
    DotImportCompletedEvent,
    DotImportJob,
    DotImportJobCreated,
    DotImportJobParameters,
    DotJobState
} from '@dotcms/dotcms-models';

import { DotJobQueueService } from '../dot-job-queue/dot-job-queue.service';
import { DotEventsSocket } from '../dot-websocket/dot-events-socket.service';
import { DotSystemEventType } from '../dot-websocket/dot-system-event-type.model';

/** How often a queued preview is read while its completion event hasn't arrived. */
const PREVIEW_POLL_MS = 1000;

/**
 * The states a preview job ends in. An import job is never retried by the server, so a FAILED
 * preview is final too.
 */
const FINISHED_JOB_STATES: ReadonlySet<DotJobState> = new Set<DotJobState>([
    'SUCCESS',
    'FAILED',
    'FAILED_PERMANENTLY',
    'ABANDONED_PERMANENTLY',
    'CANCELED'
]);

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
    readonly #events = inject(DotEventsSocket);

    /**
     * Runs the preview: a dry run of the import, queued as a job through `/_validate` (PR #38054
     * review), and answered with the finished job and its result.
     *
     * The job is read as soon as its completion event arrives over the websocket, and also every
     * second in case that event is missed (the socket reconnecting, or the job ending on another
     * node of a cluster). Whichever comes first, the finished job is returned once.
     *
     * @param file the CSV to import
     * @param form the import options; `language` and `site` are always sent
     */
    preview(file: File, form: DotContentImportForm): Observable<DotImportJob> {
        return this.#http
            .post<
                DotCMSAPIResponse<DotImportJobCreated>
            >(`${DOT_CONTENT_IMPORT_URL}/_validate`, this.#multipart(file, form))
            .pipe(
                map((response) => response.entity.jobId),
                switchMap((jobId) =>
                    merge(
                        this.#events
                            .on<DotImportCompletedEvent>(
                                DotSystemEventType.IMPORT_CONTENTLETS_COMPLETED
                            )
                            .pipe(filter((event) => event?.jobId === jobId)),
                        timer(PREVIEW_POLL_MS, PREVIEW_POLL_MS)
                    ).pipe(
                        // One read at a time: a tick that lands while a read is in flight is dropped.
                        exhaustMap(() => this.readJob(jobId)),
                        filter((job) => FINISHED_JOB_STATES.has(job.state)),
                        take(1)
                    )
                )
            );
    }

    /**
     * Reads an import job, with its state and, once it has one, its result.
     *
     * @param jobId the job's id
     */
    readJob(jobId: string): Observable<DotImportJob> {
        return this.#http
            .get<DotCMSAPIResponse<DotImportJob>>(`${DOT_CONTENT_IMPORT_URL}/${jobId}`)
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
     * Starts an import of a CSV the server already holds, by its temp file id: the one a preview
     * stored. The CSV isn't uploaded again, and the import reads exactly the file that was
     * previewed. The server refuses an id that doesn't exist or isn't this user's.
     *
     * @param tempFileId the preview job's `tempFileId`
     * @param form the import options; `language` and `site` are always sent
     */
    importPreviewed(
        tempFileId: string,
        form: DotContentImportForm
    ): Observable<DotImportJobCreated> {
        const body = new FormData();
        body.append('tempFileId', tempFileId);
        body.append('form', new Blob([JSON.stringify(form)], { type: 'application/json' }));

        return this.#http
            .post<DotCMSAPIResponse<DotImportJobCreated>>(DOT_CONTENT_IMPORT_URL, body)
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

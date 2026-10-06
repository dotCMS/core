import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotBulkRefreshSubmitResponse } from '@dotcms/dotcms-models';

const BULK_REFRESH_URL = '/api/v1/content/_bulkrefresh';

/**
 * Submits a selection of contentlets to be reindexed through `POST /api/v1/content/_bulkrefresh`.
 *
 * The endpoint is job-backed and answers `202` immediately: the reindex continues in the background and
 * this service does not wait for it. Completion arrives by push — a `BULK_REFRESH_COMPLETED` system event
 * over the websocket the admin UI already holds open — so there is deliberately nothing here that polls a
 * status endpoint. Asking every 1.5 seconds for up to five minutes is what this replaced.
 */
@Injectable({
    providedIn: 'root'
})
export class DotBulkRefreshService {
    readonly #http = inject(HttpClient);

    /**
     * Asks for every given contentlet to be reindexed.
     *
     * @param inodes Contentlet inodes. The server collapses them by identifier, so the `total` it later
     *     reports can be lower than what was sent.
     * @returns The accepted job's handle, or `null` for an empty selection — which is not worth a request.
     */
    refresh(inodes: string[]): Observable<DotBulkRefreshSubmitResponse | null> {
        if (!inodes.length) {
            return of(null);
        }

        return this.#http
            .post<{
                entity: DotBulkRefreshSubmitResponse;
            }>(BULK_REFRESH_URL, {
                contentletIds: inodes,
                // Counters are all the completion event carries, and nothing in this feature reads the
                // per-item records. They remain reachable through the generic job-status endpoint if a
                // drill-down is ever built.
                includeItemResults: false
            })
            .pipe(map((response) => response.entity));
    }
}

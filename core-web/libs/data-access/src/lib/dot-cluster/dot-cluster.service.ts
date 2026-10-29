import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse, DotClusterNodes } from '@dotcms/dotcms-models';

/**
 * Reads the status of every node in the cluster for the Network portlet.
 *
 * Backed by `GET /api/v1/cluster/nodes` (contract in the backend spec, #38032). The call waits on
 * the server while the nodes answer — at least 2 s, and longer when a node does not answer — and
 * each call makes every node write a test file to the shared assets volume, so callers must not
 * issue overlapping requests.
 */
@Injectable({ providedIn: 'root' })
export class DotClusterService {
    readonly #http = inject(HttpClient);

    /** Every node seen in the last heartbeat window, including the ones that did not answer. */
    getNodes(): Observable<DotClusterNodes> {
        return this.#http
            .get<DotCMSResponse<DotClusterNodes>>('/api/v1/cluster/nodes')
            .pipe(map((response) => response.entity));
    }
}

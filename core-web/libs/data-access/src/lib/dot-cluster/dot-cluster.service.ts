import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { delay } from 'rxjs/operators';

import { DotClusterNodes } from '@dotcms/dotcms-models';

import { DOT_CLUSTER_NODES_MOCK } from './dot-cluster.mock';

/**
 * Simulated server time. The real call blocks for up to `CLUSTER_SERVER_THREAD_SLEEP` (2 s or
 * more) while it waits for the nodes to answer, so the loading and refreshing states matter.
 */
const MOCK_LATENCY_MS = 600;

/**
 * Reads the status of every node in the cluster for the Network portlet.
 *
 * Coded against the typed `GET /api/v1/cluster/nodes` proposed by the Network spike (#37802,
 * §2.8). That endpoint does not exist yet, so the response is served from
 * `DOT_CLUSTER_NODES_MOCK`. When it ships, inject `HttpClient` and replace the body of
 * `getNodes` with:
 *
 * ```ts
 * return this.#http
 *     .get<DotCMSResponse<DotClusterNodes>>('/api/v1/cluster/nodes')
 *     .pipe(map((response) => response.entity));
 * ```
 */
@Injectable({ providedIn: 'root' })
export class DotClusterService {
    /** Every node seen in the last heartbeat window, including the ones that did not answer. */
    getNodes(): Observable<DotClusterNodes> {
        return of(DOT_CLUSTER_NODES_MOCK).pipe(delay(MOCK_LATENCY_MS));
    }
}

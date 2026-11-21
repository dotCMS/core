import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map, mergeMap, take } from 'rxjs/operators';

import { SiteService } from '@dotcms/dotcms-js';
import { DotCMSResponse } from '@dotcms/dotcms-models';

import { DotCDNStats, PurgeReturnData, PurgeUrlOptions } from './dot-cdn.models';

export interface StatsRequest {
    hostId: string;
    dateFrom: string;
    dateTo: string;
    hourly?: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class DotCDNService {
    private http = inject(HttpClient);
    private siteService = inject(SiteService);

    /**
     * Loads the CDN statistics of one site. It completes after the single response, so the
     * caller decides when to ask again (for example when the selected site changes).
     */
    requestStats(request: StatsRequest): Observable<DotCDNStats> {
        let url =
            `/api/v1/dotcdn/stats?hostId=${request.hostId}` +
            `&dateFrom=${request.dateFrom}&dateTo=${request.dateTo}`;

        if (request.hourly) {
            url += '&hourly=true';
        }

        return this.http
            .get<DotCMSResponse<DotCDNStats>>(url)
            .pipe(map((response) => response.entity));
    }

    /**
     * Purges the given URLs for the site selected when it is called. The current-site stream
     * keeps emitting on every later site switch, so only its first value is used.
     */

    purgeCache(urls?: string[]): Observable<PurgeReturnData> {
        return this.siteService.getCurrentSite().pipe(
            take(1),
            map((site) => site.identifier),
            mergeMap((hostId: string) => {
                return this.purgeUrlRequest({ hostId, invalidateAll: false, urls });
            }),
            map((response) => response.entity)
        );
    }

    /**
     * Purges the whole pull zone of the site selected when it is called, once.
     */
    purgeCacheAll(): Observable<PurgeReturnData> {
        return this.siteService.getCurrentSite().pipe(
            take(1),
            map((site) => site.identifier),
            mergeMap((hostId: string) => this.purgeUrlRequest({ hostId, invalidateAll: true })),
            map((response) => response.entity)
        );
    }

    private purgeUrlRequest({
        urls = [],
        invalidateAll,
        hostId
    }: PurgeUrlOptions): Observable<DotCMSResponse<PurgeReturnData>> {
        return this.http.request<DotCMSResponse<PurgeReturnData>>('DELETE', '/api/v1/dotcdn', {
            body: {
                urls,
                invalidateAll,
                hostId
            }
        });
    }
}

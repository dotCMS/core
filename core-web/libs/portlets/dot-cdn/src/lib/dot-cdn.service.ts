import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map, mergeMap } from 'rxjs/operators';

import { SiteService } from '@dotcms/dotcms-js';
import { DotCMSResponse } from '@dotcms/dotcms-models';

import { DotCDNStats, PurgeReturnData, PurgeUrlOptions } from './dot-cdn.models';

export interface StatsRequest {
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

    requestStats(request: StatsRequest): Observable<DotCDNStats> {
        return this.siteService.getCurrentSite().pipe(
            map((site) => site.identifier),
            mergeMap((hostId: string) => {
                let url = `/api/v1/dotcdn/stats?hostId=${hostId}`
                    + `&dateFrom=${request.dateFrom}&dateTo=${request.dateTo}`;

                if (request.hourly) {
                    url += '&hourly=true';
                }

                return this.http
                    .get<DotCMSResponse<DotCDNStats>>(url)
                    .pipe(map((response) => response.entity));
            })
        );
    }

    purgeCache(urls?: string[]): Observable<PurgeReturnData> {
        return this.siteService.getCurrentSite().pipe(
            map((site) => site.identifier),
            mergeMap((hostId: string) => {
                return this.purgeUrlRequest({ hostId, invalidateAll: false, urls });
            }),
            map((response) => response.entity)
        );
    }

    purgeCacheAll(): Observable<PurgeReturnData> {
        return this.siteService.getCurrentSite().pipe(
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

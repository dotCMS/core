import { format, subDays } from 'date-fns';
import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { mergeMap, map } from 'rxjs/operators';

import { DotCMSResponse, SiteService } from '@dotcms/dotcms-js';

import { DotCDNStats, PurgeReturnData, PurgeUrlOptions } from './app.models';

// Response type for endpoints that return bodyJsonObject
interface DotBodyJsonResponse<T> {
    bodyJsonObject: T;
}

@Injectable({
    providedIn: 'root'
})
export class DotCDNService {
    private http = inject(HttpClient);
    private siteService = inject(SiteService);

    /**
     * Request stats via Core Web Service
     *
     * @param {string} period
     * @return {*}  {Observable<DotCDNStats>}
     * @memberof DotCDNService
     */
    requestStats(period: string): Observable<DotCDNStats> {
        return this.siteService.getCurrentSite().pipe(
            map((site) => site.identifier),
            mergeMap((hostId: string) => {
                const dateTo = format(new Date(), 'yyyy-MM-dd');
                const dateFrom = format(subDays(new Date(), parseInt(period, 10)), 'yyyy-MM-dd');

                return this.http
                    .get<
                        DotCMSResponse<DotCDNStats>
                    >(`/api/v1/dotcdn/stats?hostId=${hostId}&dateFrom=${dateFrom}&dateTo=${dateTo}`)
                    .pipe(map((response) => response.entity));
            })
        );
    }

    /**
     *  Makes a request to purge the cache
     *
     * @param {string[]} [urls=[]]
     * @return {Observable<PurgeReturnData>}
     * @memberof DotCDNService
     */
    purgeCache(urls?: string[]): Observable<PurgeReturnData> {
        return this.siteService.getCurrentSite().pipe(
            map((site) => site.identifier),
            mergeMap((hostId: string) => {
                return this.purgeUrlRequest({ hostId, invalidateAll: false, urls });
            }),
            map((response) => response.bodyJsonObject)
        );
    }

    /**
     *  Makes a request to purge the cache
     *
     * @return {Observable<PurgeReturnData>}
     * @memberof DotCDNService
     */
    purgeCacheAll(): Observable<PurgeReturnData> {
        return this.siteService.getCurrentSite().pipe(
            map((site) => site.identifier),
            mergeMap((hostId: string) => this.purgeUrlRequest({ hostId, invalidateAll: true })),
            map((response) => response.bodyJsonObject)
        );
    }

    private purgeUrlRequest({
        urls = [],
        invalidateAll,
        hostId
    }: PurgeUrlOptions): Observable<DotBodyJsonResponse<PurgeReturnData>> {
        return this.http.request<DotBodyJsonResponse<PurgeReturnData>>('DELETE', '/api/v1/dotcdn', {
            body: JSON.stringify({
                urls,
                invalidateAll,
                hostId
            })
        });
    }
}

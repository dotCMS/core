import { format, subDays } from 'date-fns';
import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { mergeMap, pluck } from 'rxjs/operators';

import { DotSiteService } from '@dotcms/data-access';
import { ResponseView } from '@dotcms/dotcms-js';

import { DotCDNStats, PurgeReturnData, PurgeUrlOptions } from './app.models';

@Injectable({
    providedIn: 'root'
})
export class DotCDNService {
    constructor(
        private readonly http: HttpClient,
        private readonly siteService: DotSiteService
    ) {}

    /**
     * Request stats via Core Web Service
     *
     * @param {string} period
     * @return {*}  {Observable<DotCDNStats>}
     * @memberof DotCDNService
     */
    requestStats(period: string): Observable<DotCDNStats> {
        return this.siteService.getCurrentSite().pipe(
            pluck('identifier'),
            mergeMap((hostId: string) => {
                const dateTo = format(new Date(), 'yyyy-MM-dd');
                const dateFrom = format(subDays(new Date(), parseInt(period, 10)), 'yyyy-MM-dd');
                const params = new HttpParams()
                    .set('hostId', hostId)
                    .set('dateFrom', dateFrom)
                    .set('dateTo', dateTo);

                return this.http.get<ResponseView<DotCDNStats>>('/api/v1/dotcdn/stats', {
                    params: params
                });
            }),
            pluck('entity')
        );
    }

    /**
     *  Makes a request to purge the cache
     *
     * @param {string[]} [urls=[]]
     * @return {Observable<ResponseView<PurgeReturnData>>}
     * @memberof DotCDNService
     */
    purgeCache(urls?: string[]): Observable<PurgeReturnData> {
        return this.siteService.getCurrentSite().pipe(
            pluck('identifier'),
            mergeMap((hostId: string) => {
                return this.purgeUrlRequest({ urls: urls, invalidateAll: false, hostId: hostId });
            }),
            pluck('bodyJsonObject')
        );
    }

    /**
     *  Makes a request to purge the cache
     *
     * @return {Observable<ResponseView<PurgeReturnData>>}
     * @memberof DotCDNService
     */
    purgeCacheAll(): Observable<PurgeReturnData> {
        return this.siteService.getCurrentSite().pipe(
            pluck('identifier'),
            mergeMap((hostId: string) => {
                return this.purgeUrlRequest({ invalidateAll: true, hostId: hostId });
            }),
            pluck('bodyJsonObject')
        );
    }

    private purgeUrlRequest({
        urls = [],
        invalidateAll,
        hostId
    }: PurgeUrlOptions): Observable<ResponseView<PurgeReturnData>> {
        return this.http.delete<ResponseView<PurgeReturnData>>(`/api/v1/dotcdn`, {
            body: {
                urls: urls,
                invalidateAll: invalidateAll,
                hostId: hostId
            },
            headers: new HttpHeaders({
                'Content-Type': 'application/json'
            })
        });
    }
}

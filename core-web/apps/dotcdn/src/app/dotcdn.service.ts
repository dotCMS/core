import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CoreWebService, ResponseView, SiteService } from '@dotcms/dotcms-js';
import { pluck, mergeMap } from 'rxjs/operators';
import { format, subDays } from 'date-fns';
import { DotCDNStats, PurgeReturnData, PurgeUrlOptions } from './app.models';

@Injectable({
    providedIn: 'root'
})
export class DotCDNService {
    constructor(private coreWebService: CoreWebService, private siteService: SiteService) {}

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

                return this.coreWebService.requestView<DotCDNStats>({
                    url: `/api/v1/dotcdn/stats?hostId=${hostId}&dateFrom=${dateFrom}&dateTo=${dateTo}`
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
                return this.purgeUrlRequest({ hostId, invalidateAll: false, urls });
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
            mergeMap((hostId: string) => this.purgeUrlRequest({ hostId, invalidateAll: true })),
            pluck('bodyJsonObject')
        );
    }

    private purgeUrlRequest({
        urls = [],
        invalidateAll,
        hostId
    }: PurgeUrlOptions): Observable<ResponseView<PurgeReturnData>> {
        return this.coreWebService.requestView<PurgeReturnData>({
            url: `/api/v1/dotcdn`,
            method: 'DELETE',
            body: JSON.stringify({
                urls,
                invalidateAll,
                hostId
            })
        });
    }
}

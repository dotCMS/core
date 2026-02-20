import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, mergeMap } from 'rxjs/operators';

import {
    DotAjaxActionResponseView,
    DotBundle,
    DotCurrentUser,
    DotCMSResponseJsonObject
} from '@dotcms/dotcms-models';

import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';

@Injectable()
export class AddToBundleService {
    private http = inject(HttpClient);
    private currentUser = inject(DotCurrentUserService);

    private bundleUrl = '/api/bundle/getunsendbundles/userid';
    private addToBundleUrl =
        '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle';

    /**
     * Get bundle items
     * @return {*}  {Observable<DotBundle[]>}
     * @memberof AddToBundleService
     */
    getBundles(): Observable<DotBundle[]> {
        return this.currentUser.getCurrentUser().pipe(
            mergeMap((user: DotCurrentUser) => {
                return this.http
                    .get<
                        DotCMSResponseJsonObject<{ items: DotBundle[] }>
                    >(`${this.bundleUrl}/${user.userId}`)
                    .pipe(map((response) => response.bodyJsonObject.items));
            })
        );
    }

    /**
     * Add to bundle asset with specified name and id
     * @param string ruleId
     * @param DotBundle bundleData
     * @returns Observable<DotAjaxActionResponseView>
     * @memberof AddToBundleService
     */
    addToBundle(
        assetIdentifier: string,
        bundleData: DotBundle
    ): Observable<DotAjaxActionResponseView> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/x-www-form-urlencoded'
        });

        const body = `assetIdentifier=${assetIdentifier}&bundleName=${bundleData.name}&bundleSelect=${bundleData.id}`;

        return this.http
            .post<DotAjaxActionResponseView>(this.addToBundleUrl, body, { headers })
            .pipe(map((res) => res));
    }
}

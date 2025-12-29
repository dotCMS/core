import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { filter, map, mergeMap, toArray } from 'rxjs/operators';

import { ApiRoot } from '@dotcms/dotcms-js';
import {
    DotAjaxActionResponseView,
    DotCurrentUser,
    DotEnvironment,
    DotPushPublishData,
    DotCMSResponseJsonObject
} from '@dotcms/dotcms-models';

import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';
import { DotFormatDateService } from '../dot-format-date/dot-format-date.service';

/**
 * Provide method to push publish to content types
 * @export
 * @class PushPublishService
 */
@Injectable()
export class PushPublishService {
    _apiRoot = inject(ApiRoot);
    private http = inject(HttpClient);
    private currentUser = inject(DotCurrentUserService);
    private dotFormatDateService = inject(DotFormatDateService);

    private pushEnvironementsUrl = '/api/environment/loadenvironments/roleId';
    private publishUrl =
        '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish';
    private publishBundleURL =
        '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle';

    private _lastEnvironmentPushed!: string[];

    get lastEnvironmentPushed(): string[] {
        return this._lastEnvironmentPushed;
    }

    /**
     * Get push publish environments.
     * @returns Observable<DotEnvironment[]>
     * @memberof PushPublishService
     */
    getEnvironments(): Observable<DotEnvironment[]> {
        return this.currentUser.getCurrentUser().pipe(
            mergeMap((user: DotCurrentUser) => {
                return this.http
                    .get<
                        DotCMSResponseJsonObject<DotEnvironment[]>
                    >(`${this.pushEnvironementsUrl}/${user.roleId}`)
                    .pipe(map((response) => response.bodyJsonObject));
            }),
            mergeMap((environments: DotEnvironment[]) => environments),
            filter((environment: DotEnvironment) => environment.name !== ''),
            toArray()
        );
    }

    /**
     * Push publish asset to specified environment.
     * @param string contentTypeId
     * @param * formValue
     * @returns Observable<DotAjaxActionResponseView>
     * @memberof PushPublishService
     */
    pushPublishContent(
        assetIdentifier: string,
        pushPublishData: DotPushPublishData,
        isBundle: boolean
    ): Observable<DotAjaxActionResponseView> {
        this._lastEnvironmentPushed = pushPublishData.environment;

        const headers = new HttpHeaders({
            'Content-Type': 'application/x-www-form-urlencoded'
        });

        const body = this.getPublishEnvironmentData(assetIdentifier, pushPublishData);
        const url = isBundle ? this.publishBundleURL : this.publishUrl;

        return this.http
            .post<DotAjaxActionResponseView>(url, body, { headers })
            .pipe(map((res) => res));
    }

    private getPublishEnvironmentData(
        assetIdentifier: string,
        {
            publishDate,
            expireDate,
            pushActionSelected,
            environment,
            filterKey,
            timezoneId
        }: DotPushPublishData
    ): string {
        let result = '';
        result += `assetIdentifier=${encodeURIComponent(assetIdentifier)}`;
        result += `&remotePublishDate=${this.dotFormatDateService.format(
            publishDate ? new Date(publishDate) : new Date(),
            'yyyy-MM-dd'
        )}`;
        result += `&remotePublishTime=${this.dotFormatDateService.format(
            publishDate ? new Date(publishDate) : new Date(),
            'HH-mm'
        )}`;
        result += `&remotePublishExpireDate=${this.dotFormatDateService.format(
            expireDate ? new Date(expireDate) : new Date(),
            'yyyy-MM-dd'
        )}`;
        result += `&remotePublishExpireTime=${this.dotFormatDateService.format(
            expireDate ? new Date(expireDate) : new Date(),
            'HH-mm'
        )}`;
        result += `&timezoneId=${timezoneId}`;
        result += `&iWantTo=${pushActionSelected}`;
        result += `&whoToSend=${environment}`;
        result += '&bundleName=';
        result += '&bundleSelect=';

        if (filterKey) {
            result += `&filterKey=${filterKey}`;
        }

        return result;
    }
}

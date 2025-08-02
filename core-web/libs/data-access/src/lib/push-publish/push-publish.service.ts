import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { filter, map, mergeMap, pluck, toArray } from 'rxjs/operators';

import { ApiRoot, CoreWebService, ResponseView } from '@dotcms/dotcms-js';
import {
    DotAjaxActionResponseView,
    DotCurrentUser,
    DotEnvironment,
    DotPushPublishData
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
    private coreWebService = inject(CoreWebService);
    private currentUser = inject(DotCurrentUserService);
    private dotFormatDateService = inject(DotFormatDateService);

    private pushEnvironementsUrl = '/api/environment/loadenvironments/roleId';
    /*
        TODO: I had to do this because this line concat'api/' into the URL
        https://github.com/dotCMS/dotcms-js/blob/master/src/core/core-web.service.ts#L169
    */
    private publishUrl = `/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish`;
    private publishBundleURL = `/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle`;

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
                return this.coreWebService.requestView<DotEnvironment[]>({
                    url: `${this.pushEnvironementsUrl}/${user.roleId}`
                });
            }),
            pluck<ResponseView<DotEnvironment[]>, DotEnvironment[]>('bodyJsonObject'),
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

        return this.coreWebService
            .request<DotAjaxActionResponseView>({
                body: this.getPublishEnvironmentData(assetIdentifier, pushPublishData),
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                method: 'POST',
                url: isBundle ? this.publishBundleURL : this.publishUrl
            })
            .pipe(map((res) => res as DotAjaxActionResponseView));
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

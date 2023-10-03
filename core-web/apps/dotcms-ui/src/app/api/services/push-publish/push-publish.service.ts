import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { filter, map, mergeMap, pluck, toArray } from 'rxjs/operators';

import { DotCurrentUserService } from '@dotcms/data-access';
import { ApiRoot, CoreWebService } from '@dotcms/dotcms-js';
import { DotAjaxActionResponseView, DotCurrentUser } from '@dotcms/dotcms-models';
import { DotFormatDateService } from '@dotcms/ui';
import { DotEnvironment } from '@models/dot-environment/dot-environment';
import { DotPushPublishData } from '@models/dot-push-publish-data/dot-push-publish-data';

/**
 * Provide method to push publish to content types
 * @export
 * @class PushPublishService
 */
@Injectable()
export class PushPublishService {
    private pushEnvironementsUrl = '/api/environment/loadenvironments/roleId';
    /*
        TODO: I had to do this because this line concat'api/' into the URL
        https://github.com/dotCMS/dotcms-js/blob/master/src/core/core-web.service.ts#L169
    */
    private publishUrl = `/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish`;
    private publishBundleURL = `/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle`;

    constructor(
        public _apiRoot: ApiRoot,
        private coreWebService: CoreWebService,
        private currentUser: DotCurrentUserService,
        private dotFormatDateService: DotFormatDateService
    ) {}

    private _lastEnvironmentPushed: string[];

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
                return this.coreWebService.requestView({
                    url: `${this.pushEnvironementsUrl}/${user.roleId}`
                });
            }),
            pluck('bodyJsonObject'),
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
            .pipe(map((res: DotAjaxActionResponseView) => res));
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

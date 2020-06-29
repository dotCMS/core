import { toArray, filter, pluck, mergeMap } from 'rxjs/operators';
import { CoreWebService, ApiRoot } from 'dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { DotEnvironment } from '@models/dot-environment/dot-environment';
import { AjaxActionResponseView } from '@models/ajax-action-response/ajax-action-response';
import * as moment from 'moment';
import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';
import { DotCurrentUser } from '@models/dot-current-user/dot-current-user';
import { DotPushPublishData } from '@models/dot-push-publish-data/dot-push-publish-data';

/**
 * Provide method to push publish to content types
 * @export
 * @class PushPublishService
 */
@Injectable()
export class PushPublishService {
    private pushEnvironementsUrl = 'environment/loadenvironments/roleId';
    private _lastEnvironmentPushed: string[];
    /*
        TODO: I had to do this because this line concat'api/' into the URL
        https://github.com/dotCMS/dotcms-js/blob/master/src/core/core-web.service.ts#L169
    */
    private publishUrl = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish`;
    private publishBundleURL = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle`;

    constructor(
        public _apiRoot: ApiRoot,
        private coreWebService: CoreWebService,
        private currentUser: DotCurrentUserService
    ) {}

    /**
     * Get push publish environments.
     * @returns Observable<DotEnvironment[]>
     * @memberof PushPublishService
     */
    getEnvironments(): Observable<DotEnvironment[]> {
        return this.currentUser.getCurrentUser().pipe(
            mergeMap((user: DotCurrentUser) => {
                return this.coreWebService.requestView({
                    method: RequestMethod.Get,
                    url: `${this.pushEnvironementsUrl}/${user.roleId}/name=0`
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
     * @returns Observable<AjaxActionResponseView>
     * @memberof PushPublishService
     */
    pushPublishContent(
        assetIdentifier: string,
        pushPublishData: DotPushPublishData,
        isBundle: boolean
    ): Observable<AjaxActionResponseView> {
        this._lastEnvironmentPushed = pushPublishData.environment;
        return this.coreWebService.request({
            body: this.getPublishEnvironmentData(assetIdentifier, pushPublishData),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            method: RequestMethod.Post,
            url: isBundle ? this.publishBundleURL : this.publishUrl
        });
    }

    private getPublishEnvironmentData(
        assetIdentifier: string,
        {
            publishdate,
            expiredate,
            pushActionSelected,
            environment,
            forcePush,
            filterKey
        }: DotPushPublishData
    ): string {
        let result = '';
        result += `assetIdentifier=${assetIdentifier}`;
        result += `&remotePublishDate=${moment(publishdate).format('YYYY-MM-DD')}`;
        result += `&remotePublishTime=${moment(publishdate).format('h-mm')}`;
        result += `&remotePublishExpireDate=${moment(expiredate).format('YYYY-MM-DD')}`;
        result += `&remotePublishExpireTime=${moment(expiredate).format('h-mm')}`;
        result += `&iWantTo=${pushActionSelected}`;
        result += `&whoToSend=${environment}`;
        result += '&bundleName=';
        result += '&bundleSelect=';
        result += `&forcePush=${forcePush}`;

        if (filterKey) {
            result += `&filterKey=${filterKey}`;
        }

        return result;
    }

    get lastEnvironmentPushed(): string[] {
        return this._lastEnvironmentPushed;
    }
}

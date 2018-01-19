import { CoreWebService, ApiRoot, ResponseView } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { DotEnvironment } from '../../../shared/models/dot-environment/dot-environment';
import { AjaxActionResponseView } from '../../../shared/models/ajax-action-response/ajax-action-response';
import { DotCurrentUser } from '../../../shared/models/dot-current-user/dot-current-user';
import { PushPublishData } from '../../../shared/models/push-publish-data/push-publish-data';
import * as moment from 'moment';

/**
 * Provide method to push publish to content types
 * @export
 * @class PushPublishService
 */
@Injectable()
export class PushPublishService {
    private pushEnvironementsUrl= 'environment/loadenvironments/roleId';
    private currentUsersUrl = 'v1/users/current/';
    /*
        TODO: I had to do this because this line concat'api/' into the URL
        https://github.com/dotCMS/dotcms-js/blob/master/src/core/core-web.service.ts#L169
    */
    private publishUrl = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish`;

    constructor(public _apiRoot: ApiRoot, private coreWebService: CoreWebService) {}

    /**
     * Get push publish environments.
     * @returns {Observable<DotEnvironment[]>}
     * @memberof PushPublishService
     */
    getEnvironments(): Observable<DotEnvironment[]> {
        return this.getCurrentUser().mergeMap(user => {
            return this.coreWebService.requestView({
                method: RequestMethod.Get,
                url: `${this.pushEnvironementsUrl}/${user.roleId}/name=0`
            }).map((res: any) => JSON.parse(res.response._body));
        })
        .flatMap((environments: DotEnvironment[]) => environments)
        .filter(environment => environment.name !== '')
        .toArray();
    }

    /**
     * Push publish asset to specified environment.
     * @param {string} contentTypeId
     * @param {*} formValue
     * @returns {Observable<AjaxActionResponseView>}
     * @memberof PushPublishService
     */
    pushPublishContent(assetIdentifier: string, pushPublishData: PushPublishData): Observable<AjaxActionResponseView> {
        return this.coreWebService.request({
            body: this.getPublishEnvironmentData(assetIdentifier, pushPublishData),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            method: RequestMethod.Post,
            url: this.publishUrl
        });
    }

    // TODO: We need to update the LoginService to get the roleid in the User object
    /**
     * Get logged user and role id.
     * @returns {Observable<DotCurrentUser>}
     * @memberof PushPublishService
     */
    getCurrentUser(): Observable<DotCurrentUser> {
        return this.coreWebService.request({
            method: RequestMethod.Get,
            url: this.currentUsersUrl
        });
    }

    private getPublishEnvironmentData(assetIdentifier: string, pushPublishData: PushPublishData): string {
        let result = '';
        result += `assetIdentifier=${assetIdentifier}`;
        result += `&remotePublishDate=${moment(pushPublishData.publishdate).format('YYYY-MM-DD')}`;
        result += `&remotePublishTime=${moment(pushPublishData.publishdatetime).format('h-mm')}`;
        result += `&remotePublishExpireDate=${moment(pushPublishData.expiredate).format('YYYY-MM-DD')}`;
        result += `&remotePublishExpireTime=${moment(pushPublishData.expiredatetime).format('h-mm')}`;
        result += `&iWantTo=${pushPublishData.pushActionSelected}`;
        result += `&whoToSend=${pushPublishData.environment}`;
        result += '&bundleName=';
        result += '&bundleSelect=';
        result += `&forcePush=${pushPublishData.forcePush}`;
        return result;
    }
}

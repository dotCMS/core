import { CoreWebService, ApiRoot, ResponseView } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { DotCurrentUser } from '../../../shared/models/dot-current-user/dot-current-user';
import { DotBundle } from '../../../shared/models/dot-bundle/dot-bundle';
import { AjaxActionResponseView } from '../../../shared/models/ajax-action-response/ajax-action-response';
import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';

@Injectable()
export class AddToBundleService {
    private bundleUrl = `bundle/getunsendbundles/userid`;
    /*
        TODO: I had to do this because this line concat 'api/' into the URL
        https://github.com/dotCMS/dotcms-js/blob/master/src/core/core-web.service.ts#L169
    */
    private addToBundleUrl = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle`;

    constructor(public _apiRoot: ApiRoot, private coreWebService: CoreWebService, private currentUser: DotCurrentUserService) { }

    /**
     * Get bundle items
     * @returns {Observable<any[]>}
     * @memberof AddToBundleService
     */
    getBundles(): Observable<any[]> {
        return this.currentUser.getCurrentUser().mergeMap((user: DotCurrentUser) => {
            return this.coreWebService.requestView({
                method: RequestMethod.Get,
                url: `${this.bundleUrl}/${user.userId}`
            })
            .pluck('bodyJsonObject', 'items');
        });
    }

    /**
     * Add to bundle asset with specified name and id
     * @param {string} ruleId
     * @param {DotBundle} bundleData
     * @returns {Observable<AjaxActionResponseView>}
     * @memberof AddToBundleService
     */
    addToBundle(assetIdentifier: string, bundleData: DotBundle): Observable<AjaxActionResponseView> {
        return this.coreWebService.request({
            body: `assetIdentifier=${assetIdentifier}&bundleName=${bundleData.name}&bundleSelect=${bundleData.id}`,
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            method: RequestMethod.Post,
            url: this.addToBundleUrl
        });
    }
}

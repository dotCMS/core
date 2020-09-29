import { pluck, mergeMap } from 'rxjs/operators';
import { CoreWebService, ApiRoot } from 'dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Headers, RequestMethod } from '@angular/http';
import { DotCurrentUser } from '@models/dot-current-user/dot-current-user';
import { DotBundle } from '@models/dot-bundle/dot-bundle';
import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';
import { DotAjaxActionResponseView } from '@shared/models/ajax-action-response/dot-ajax-action-response';

@Injectable()
export class AddToBundleService {
    private bundleUrl = `bundle/getunsendbundles/userid`;
    /*
        TODO: I had to do this because this line concat 'api/' into the URL
        https://github.com/dotCMS/dotcms-js/blob/master/src/core/core-web.service.ts#L169
    */
    private addToBundleUrl = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle`;

    constructor(
        public _apiRoot: ApiRoot,
        private coreWebService: CoreWebService,
        private currentUser: DotCurrentUserService
    ) {}

    /**
     * Get bundle items
     * @returns Observable<any[]>
     * @memberof AddToBundleService
     */
    getBundles(): Observable<any[]> {
        return this.currentUser.getCurrentUser().pipe(
            mergeMap((user: DotCurrentUser) => {
                return this.coreWebService
                    .requestView({
                        method: RequestMethod.Get,
                        url: `${this.bundleUrl}/${user.userId}`
                    })
                    .pipe(pluck('bodyJsonObject', 'items'));
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
        const headers = new Headers();
        headers.set('Content-Type', 'application/x-www-form-urlencoded');
        return this.coreWebService.request({
            body: `assetIdentifier=${assetIdentifier}&bundleName=${bundleData.name}&bundleSelect=${bundleData.id}`,
            headers,
            method: RequestMethod.Post,
            url: this.addToBundleUrl
        });
    }
}

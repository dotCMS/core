import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, mergeMap, pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotAjaxActionResponseView, DotBundle, DotCurrentUser } from '@dotcms/dotcms-models';

import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';

@Injectable()
export class AddToBundleService {
    private coreWebService = inject(CoreWebService);
    private currentUser = inject(DotCurrentUserService);

    private bundleUrl = `api/bundle/getunsendbundles/userid`;

    /*
  TODO: I had to do this because this line concat 'api/' into the URL
  https://github.com/dotCMS/dotcms-js/blob/master/src/core/core-web.service.ts#L169
*/
    private addToBundleUrl = `/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle`;

    /**
     * Get bundle items
     * @return {*}  {Observable<DotBundle[]>}
     * @memberof AddToBundleService
     */
    getBundles(): Observable<DotBundle[]> {
        return this.currentUser.getCurrentUser().pipe(
            mergeMap((user: DotCurrentUser) => {
                return this.coreWebService
                    .requestView({
                        url: `${this.bundleUrl}/${user.userId}`
                    })
                    .pipe(pluck('bodyJsonObject', 'items'));
            })
        ) as Observable<DotBundle[]>;
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
        return this.coreWebService
            .request({
                body: `assetIdentifier=${assetIdentifier}&bundleName=${bundleData.name}&bundleSelect=${bundleData.id}`,
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                method: 'POST',
                url: this.addToBundleUrl
            })
            .pipe(map((res) => res as DotAjaxActionResponseView));
    }
}

import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { filter, mergeMap, toArray } from 'rxjs/operators';

import { ApiRoot } from '@dotcms/dotcms-js';
import {
    DotAjaxActionResponseView,
    DotCurrentUser,
    DotEnvironment,
    DotPushPublishData,
    DotWorkflowPushPublishValue
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
                return this.http.get<DotEnvironment[]>(
                    `${this.pushEnvironementsUrl}/${user.roleId}`
                );
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

        return this.http.post<DotAjaxActionResponseView>(url, body, { headers });
    }

    /**
     * Push publishes one or more assets, from a payload already in the servlet's own shape.
     *
     * Backing endpoint: the same `RemotePublishAjaxAction/cmd/publish` servlet
     * {@link pushPublishContent} uses. `assetIdentifier` accepts several **identifiers**
     * comma-joined — the action splits on "," (`RemotePublishAjaxAction`, "Support for multiple ids
     * in the assetIdentifier parameter"), so bulk needs no new endpoint.
     *
     * **Why the servlet and not the workflow fire path.** There is nothing to route through. A push
     * publish over an arbitrary selection has no workflow action to name: `bulkFire` needs a real
     * action UUID belonging to the contentlets' own scheme and only pushes if that action wires
     * `PushPublishActionlet`, and there is no `PUSH_PUBLISH` in `WorkflowAPI.SystemAction`, so no
     * `default/fire/{systemAction}` route either — the way Lock and Unlock get one. A mixed
     * content-type selection with no action chosen cannot supply one.
     *
     * Routing through it would buy nothing regardless: `PushPublishActionlet.doPushPublish` and
     * `RemotePublishAjaxAction.publish` are duplicate implementations of the same sequence, and the
     * actionlet imports the servlet's own `DIALOG_ACTION_*` constants.
     *
     * Separate from {@link pushPublishContent} for a second, smaller reason: date handling.
     * `DotPushPublishData` carries whole dates that this service splits into the servlet's
     * `remotePublishDate` / `remotePublishTime` pair, while {@link DotWorkflowPushPublishValue}
     * arrives already split — that is what the workflow push publish form emits — so reusing that
     * method would mean recombining two strings into a `Date` only to take it apart again, losing
     * the timezone the user picked. Here the values pass straight through.
     *
     * @param assetIdentifier One identifier, or several comma-joined
     * @param value The payload emitted by `DotWorkflowPushPublishComponent`
     * @returns Observable<DotAjaxActionResponseView>
     * @memberof PushPublishService
     */
    pushPublishAssets(
        assetIdentifier: string,
        value: DotWorkflowPushPublishValue
    ): Observable<DotAjaxActionResponseView> {
        this._lastEnvironmentPushed = value.whereToSend.split(',');

        const headers = new HttpHeaders({
            'Content-Type': 'application/x-www-form-urlencoded'
        });

        // Every value is encoded, not just the identifier. Most are URL-safe in practice —
        // environment ids are UUIDs, `iWantTo` is a fixed enum, dates arrive pre-formatted and
        // `timezoneId` is a Java zone id — but `filterKey` is a descriptor key with no such
        // guarantee, and one unescaped `&` or `=` in a form-encoded body silently swallows every
        // parameter after it.
        const params = [
            `assetIdentifier=${encodeURIComponent(assetIdentifier)}`,
            `remotePublishDate=${encodeURIComponent(value.publishDate)}`,
            `remotePublishTime=${encodeURIComponent(value.publishTime)}`,
            `remotePublishExpireDate=${encodeURIComponent(value.expireDate)}`,
            `remotePublishExpireTime=${encodeURIComponent(value.expireTime)}`,
            `timezoneId=${encodeURIComponent(value.timezoneId)}`,
            `iWantTo=${encodeURIComponent(value.iWantTo)}`,
            `whoToSend=${encodeURIComponent(value.whereToSend)}`,
            // Sent empty, as the legacy form does: a bundle name or id here would divert the push
            // into a bundle instead of sending it.
            'bundleName=',
            'bundleSelect='
        ];

        // Only when set. The servlet reads it straight into `getFilterDescriptorByKey`, and expiring
        // takes no filter, so an empty value is a real case rather than a missing one.
        if (value.filterKey) {
            params.push(`filterKey=${encodeURIComponent(value.filterKey)}`);
        }

        return this.http.post<DotAjaxActionResponseView>(this.publishUrl, params.join('&'), {
            headers
        });
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
        // Encoded for the same reason as `pushPublishAssets`: an unescaped `&` or `=` in any value
        // silently swallows every parameter after it in a form-encoded body.
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
        result += `&timezoneId=${encodeURIComponent(timezoneId)}`;
        result += `&iWantTo=${encodeURIComponent(pushActionSelected)}`;
        result += `&whoToSend=${encodeURIComponent(environment.join(','))}`;
        result += '&bundleName=';
        result += '&bundleSelect=';

        if (filterKey) {
            result += `&filterKey=${encodeURIComponent(filterKey)}`;
        }

        return result;
    }
}

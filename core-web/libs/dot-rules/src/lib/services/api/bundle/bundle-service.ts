import { of as observableOf, Observable, Subject } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { mergeMap, map } from 'rxjs/operators';

import { ApiRoot, CoreWebService } from '@dotcms/dotcms-js';

export interface IUser {
    givenName?: string;
    surname?: string;
    roleId?: string;
    userId?: string;
}

export interface IBundle {
    name?: string;
    id?: string;
}

export interface IPublishEnvironment {
    name?: string;
    id?: string;
}

@Injectable()
export class BundleService {
    _apiRoot = inject(ApiRoot);
    private coreWebService = inject(CoreWebService);

    bundles$: Subject<IBundle[]> = new Subject();

    private _bundleStoreUrl: string;
    private _loggedUserUrl: string;
    private _addToBundleUrl: string;
    private _pushEnvironementsUrl: string;
    private _pushRuleUrl: string;
    private _environmentsAry: IPublishEnvironment[] = [];

    static fromServerBundleTransformFn(data: { items?: IBundle[] }): IBundle[] {
        return data.items || [];
    }

    static fromServerEnvironmentTransformFn(data: IPublishEnvironment[]): IPublishEnvironment[] {
        // Endpoint return extra empty environment
        data.shift();

        return data;
    }

    constructor() {
        this._bundleStoreUrl = `/api/bundle/getunsendbundles/userid`;
        this._loggedUserUrl = `/api/v1/users/current/`;
        this._addToBundleUrl = `/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle`;
        this._pushEnvironementsUrl = `/api/environment/loadenvironments/roleId`;
        this._pushRuleUrl = `/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish`;
    }

    /**
     * Get current logged in user
     *
     * @return {*}  {Observable<IUser>}
     * @memberof BundleService
     * @deprecated use getCurrentUser in LoginService
     */
    getLoggedUser(): Observable<IUser> {
        return this.coreWebService
            .request<IUser>({
                url: this._loggedUserUrl
            })
            .pipe(map((res: IUser) => res));
    }

    loadBundleStores(): void {
        const obs = this._doLoadBundleStores().pipe(
            map((bundles: IBundle[]) => {
                return bundles;
            })
        );
        obs.subscribe((bundles) => this.bundles$.next(bundles));
    }

    _doLoadBundleStores(): Observable<IBundle[]> {
        return this.getLoggedUser().pipe(
            mergeMap((user: IUser) => {
                return this.coreWebService
                    .request({
                        url: `${this._bundleStoreUrl}/${user.userId}`
                    })
                    .pipe(map(BundleService.fromServerBundleTransformFn));
            })
        );
    }

    loadPublishEnvironments(): Observable<IPublishEnvironment[]> {
        let obs: Observable<IPublishEnvironment[]>;
        if (this._environmentsAry.length) {
            obs = observableOf(this._environmentsAry);
        } else {
            obs = this._doLoadPublishEnvironments().pipe(
                map((environments: IPublishEnvironment[]) => {
                    this._environmentsAry = environments;

                    return environments;
                })
            );
        }

        return obs;
    }

    _doLoadPublishEnvironments(): Observable<IPublishEnvironment[]> {
        return this.getLoggedUser().pipe(
            mergeMap((user: IUser) => {
                return this.coreWebService
                    .request({
                        url: `${this._pushEnvironementsUrl}/${user.roleId}/`
                    })
                    .pipe(map(BundleService.fromServerEnvironmentTransformFn));
            })
        );
    }

    addRuleToBundle(
        ruleId: string,
        bundle: IBundle
    ): Observable<{ errorMessages: string[]; total: number; errors: number }> {
        return this.coreWebService.request<{
            errorMessages: string[];
            total: number;
            errors: number;
        }>({
            body: `assetIdentifier=${ruleId}&bundleName=${bundle.name}&bundleSelect=${bundle.id}`,
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            method: 'POST',
            url: this._addToBundleUrl
        }) as Observable<{ errorMessages: string[]; total: number; errors: number }>;
    }

    pushPublishRule(
        ruleId: string,
        environmentId: string
    ): Observable<{ errorMessages: string[]; total: number; bundleId: string; errors: number }> {
        return this.coreWebService.request<{
            errorMessages: string[];
            total: number;
            bundleId: string;
            errors: number;
        }>({
            body: this.getPublishRuleData(ruleId, environmentId),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            method: 'POST',
            url: this._pushRuleUrl
        }) as Observable<{
            errorMessages: string[];
            total: number;
            bundleId: string;
            errors: number;
        }>;
    }

    private getFormattedDate(date: Date): string {
        const yyyy = date.getFullYear().toString();
        const mm = (date.getMonth() + 1).toString();
        const dd = date.getDate().toString();

        return yyyy + '-' + (mm[1] ? mm : '0' + mm[0]) + '-' + (dd[1] ? dd : '0' + dd[0]);
    }

    private getPublishRuleData(ruleId: string, environmentId: string): string {
        let resul = '';
        resul += `assetIdentifier=${ruleId}`;
        resul += `&remotePublishDate=${this.getFormattedDate(new Date())}`;
        resul += '&remotePublishTime=00-00';
        resul += `&remotePublishExpireDate=${this.getFormattedDate(new Date())}`;
        resul += '&remotePublishExpireTime=00-00';
        resul += '&iWantTo=publish';
        resul += `&whoToSend=${environmentId}`;
        resul += '&bundleName=';
        resul += '&bundleSelect=';
        resul += '&forcePush=false';

        return resul;
    }
}

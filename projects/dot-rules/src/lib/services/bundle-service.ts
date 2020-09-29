import { of as observableOf, Observable, Subject } from 'rxjs';

import { map, mergeMap } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Headers, RequestMethod } from '@angular/http';

import { ApiRoot } from 'dotcms-js';
import { CoreWebService } from 'dotcms-js';

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
    bundles$: Subject<IBundle[]> = new Subject();

    private _bundleStoreUrl: string;
    private _loggedUserUrl: string;
    private _addToBundleUrl: string;
    private _pushEnvironementsUrl: string;
    private _pushRuleUrl: string;
    private _bundlesAry: IBundle[] = [];
    private _environmentsAry: IPublishEnvironment[] = [];

    static fromServerBundleTransformFn(data): IBundle[] {
        return data.items || [];
    }

    static fromServerEnvironmentTransformFn(data): IPublishEnvironment[] {
        // Endpoint return extra empty environment
        data.shift();
        return data;
    }

    constructor(public _apiRoot: ApiRoot, private coreWebService: CoreWebService) {
        this._bundleStoreUrl = `${this._apiRoot.baseUrl}api/bundle/getunsendbundles/userid`;
        this._loggedUserUrl = `${this._apiRoot.baseUrl}api/v1/users/current/`;
        this._addToBundleUrl = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle`;
        this._pushEnvironementsUrl = `${this._apiRoot.baseUrl}api/environment/loadenvironments/roleId`;
        this._pushRuleUrl = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish`;
    }

    getLoggedUser(): Observable<IUser> {
        return this.coreWebService.request({
            method: RequestMethod.Get,
            url: this._loggedUserUrl
        });
    }

    loadBundleStores(): void {
        const obs = this._doLoadBundleStores().pipe(
            map((bundles: IBundle[]) => {
                this._bundlesAry = bundles;
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
                        method: RequestMethod.Get,
                        url: `${this._bundleStoreUrl}/${user.userId}`
                    })
                    .pipe(map(BundleService.fromServerBundleTransformFn));
            })
        );
    }

    loadPublishEnvironments(): Observable<any> {
        let obs: Observable<any>;
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
                        method: RequestMethod.Get,
                        url: `${this._pushEnvironementsUrl}/${user.roleId}/?name=0`
                    })
                    .pipe(map(BundleService.fromServerEnvironmentTransformFn));
            })
        );
    }

    addRuleToBundle(
        ruleId: string,
        bundle: IBundle
    ): Observable<{ errorMessages: string[]; total: number; errors: number }> {
        const headers = new Headers();
        headers.set('Content-Type', 'application/x-www-form-urlencoded');
        return this.coreWebService.request({
            body: `assetIdentifier=${ruleId}&bundleName=${bundle.name}&bundleSelect=${bundle.id}`,
            headers,
            method: RequestMethod.Post,
            url: this._addToBundleUrl
        });
    }

    pushPublishRule(
        ruleId: string,
        environmentId: string
    ): Observable<{ errorMessages: string[]; total: number; bundleId: string; errors: number }> {
        const headers = new Headers();
        headers.set('Content-Type', 'application/x-www-form-urlencoded');
        return this.coreWebService.request({
            body: this.getPublishRuleData(ruleId, environmentId),
            headers,
            method: RequestMethod.Post,
            url: this._pushRuleUrl
        });
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

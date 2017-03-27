import {Injectable} from '@angular/core';
import {Http, RequestMethod} from '@angular/http';
import {Observable, BehaviorSubject} from 'rxjs/Rx';

import {ApiRoot} from '../persistence/ApiRoot';
import {I18nService} from '../system/locale/I18n';
import {CoreWebService} from '../services/core-web-service';

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
  private _bundleStoreUrl: string;
  private _loggedUserUrl: string;
  private _addToBundleUrl: string;
  private _pushEnvironementsUrl: string;
  private _pushRuleUrl: string;

  private bundles$: BehaviorSubject<IBundle[]> = new BehaviorSubject([]);
  private environments$: BehaviorSubject<IDBEnvironment[]> = new BehaviorSubject([]);
  private _bundlesAry: IBundle[] = [];
  private _environmentsAry: IDBEnvironment[] = [];

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
      url: this._loggedUserUrl,
    });
  }

  loadBundleStores(): void {
    let obs;
    if (this._bundlesAry.length) {
      obs = Observable.from(this._bundlesAry);
    } else {
      obs = this._doLoadBundleStores().map((bundles: IBundle[]) => {
        this._bundlesAry = bundles;
        return bundles;
      });
    }
    obs.subscribe((bundles) => this.bundles$.next(bundles));
  }

  _doLoadBundleStores(): Observable<IBundle[]> {
    return this.getLoggedUser().flatMap((user: IUser) => {
      return this.coreWebService.request({
        method: RequestMethod.Get,
        url: `${this._bundleStoreUrl}/${user.userId}`,
      }).map(BundleService.fromServerBundleTransformFn);
    });
  }

  loadPublishEnvironments(): Observable<any> {
    let obs: Observable<any>;
    if (this._environmentsAry.length) {
      obs = Observable.from(this._environmentsAry);
    } else {
      obs = this._doLoadPublishEnvironments().map((environments: IDBEnvironment[]) => {
        this._environmentsAry = environments;
        return environments;
      });
    }
    return obs;
  }

  _doLoadPublishEnvironments(): Observable<IPublishEnvironment[]> {
    return this.getLoggedUser().flatMap((user: IUser) => {
      return this.coreWebService.request({
        method: RequestMethod.Get,
        url: `${this._pushEnvironementsUrl}/${user.roleId}/?name=0`,
      }).map(BundleService.fromServerEnvironmentTransformFn);
    });
  }

  addRuleToBundle(ruleId: string, bundle: IBundle): Observable<{errorMessages: string[], total: number, errors: number}> {
    return this.coreWebService.request({
      body: `assetIdentifier=${ruleId}&bundleName=${bundle.name}&bundleSelect=${bundle.id}`,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      method: RequestMethod.Post,
      url: this._addToBundleUrl
    });
  }

  pushPublishRule(ruleId: string, environmentId: string): Observable<{errorMessages: string[], total: number, bundleId: string, errors: number}> {
    return this.coreWebService.request({
      body: this.getPublishRuleData(ruleId, environmentId),
          headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      method: RequestMethod.Post,
      url: this._pushRuleUrl
    });
  }

  private getFormattedDate(date: Date): string {
    let yyyy = date.getFullYear().toString();
    let mm = (date.getMonth() + 1).toString();
    let dd  = date.getDate().toString();
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
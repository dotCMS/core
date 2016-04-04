import {Injectable} from 'angular2/core'
import {Http, RequestMethod} from 'angular2/http'
import {Observable, BehaviorSubject} from 'rxjs/Rx'

import {ApiRoot} from "../persistence/ApiRoot";
import {I18nService} from "../system/locale/I18n";
import {CoreWebService} from "../services/core-web-service";

export interface IUser {
  givenName?: string,
  surname?: string,
  roleId?: string,
  userId?: string
}

export interface IBundle {
  name?: string,
  id?: string
}

export interface IPublishEnvironment {
  name?: string,
  id?: string
}

@Injectable()
export class BundleService extends CoreWebService {
  private _bundleStoreUrl:string
  private _loggedUserUrl:string
  private _addToBundleUrl:string
  private _pushEnvironementsUrl:string
  private _pushRuleUrl:string

  bundles$:BehaviorSubject<IBundle[]> = new BehaviorSubject([]);
  environments$:BehaviorSubject<IDBEnvironment[]> = new BehaviorSubject([]);
  private _bundlesAry:IBundle[] = []
  private _environmentsAry:IDBEnvironment[] = []

  constructor(public _apiRoot:ApiRoot, _http:Http, private _resources:I18nService) {
    super(_apiRoot, _http)
    this._bundleStoreUrl = `${this._apiRoot.baseUrl}api/bundle/getunsendbundles/userid`
    this._loggedUserUrl = `${this._apiRoot.baseUrl}api/v1/users/current/`
    this._addToBundleUrl = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle`
    this._pushEnvironementsUrl = `${this._apiRoot.baseUrl}api/environment/loadenvironments/roleId`
    this._pushRuleUrl = `${this._apiRoot.baseUrl}DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish`


  }

  getLoggedUser():Observable<IUser> {
    return this.request({
      method: RequestMethod.Get,
      url: this._loggedUserUrl,
    })
  }



  loadBundleStores(){
    let obs
    if (this._bundlesAry.length) {
      obs = Observable.fromArray(this._bundlesAry)
    } else {
      obs = this._doLoadBundleStores().map((bundles:IBundle[])=> {
        this._bundlesAry = bundles
        return bundles
      })
    }
    obs.subscribe((bundles) => this.bundles$.next(bundles))
  }

  _doLoadBundleStores():Observable<IBundle[]> {
    return this.getLoggedUser().flatMap((user:IUser) => {
      return this.request({
        method: RequestMethod.Get,
        url: `${this._bundleStoreUrl}/${user.userId}`,
      }).map(BundleService.fromServerBundleTransformFn)
    })
  }

  loadPublishEnvironments() {
    let obs
    if (this._environmentsAry.length) {
      obs = Observable.fromArray(this._environmentsAry)
    } else {
      obs = this._doLoadPublishEnvironments().map((environments:IDBEnvironment[])=> {
        this._environmentsAry = environments
        return environments
      })
    }
    return obs
  }

  _doLoadPublishEnvironments():Observable<IPublishEnvironment[]> {
    return this.getLoggedUser().flatMap((user:IUser) => {
      return this.request({
        method: RequestMethod.Get,
        url: `${this._pushEnvironementsUrl}/${user.roleId}/?name=0`,
      }).map(BundleService.fromServerEnvironmentTransformFn)
    })
  }


  addRuleToBundle(ruleId:string, bundle:IBundle):Observable<{errorMessages:string[],total:number,errors:number}> {
    return this.request({
      body: `assetIdentifier=${ruleId}&bundleName=${bundle.name}&bundleSelect=${bundle.id}`,
      method: RequestMethod.Post,
      headers: {
        "Content-Type": "application/x-www-form-urlencoded"
      },
      url: this._addToBundleUrl
    })
  }


  private getFormattedDate(date:Date) {
    let month = (date.getMonth() + 1).toString()
    month += month.length < 2 ? "0" + month : month
    return `${month}-${date.getDate()}-${date.getFullYear()}`
  }

  private getPublishRuleData(ruleId:string, environmentId:string) {
    let resul:string = "";
    resul += `assetIdentifier=${ruleId}`
    resul += `&remotePublishDate=${this.getFormattedDate(new Date())}`
    resul += "&remotePublishTime=00-00"
    resul += `&remotePublishExpireDate=${this.getFormattedDate(new Date())}`
    resul += "&remotePublishExpireTime=00-00"
    resul += "&iWantTo=publish"
    resul += `&whoToSend=${environmentId}`
    resul += "&bundleName="
    resul += "&bundleSelect="
    resul += "&forcePush=false"
    return resul
  }

  pushPublishRule(ruleId:string, environmentId:string):Observable<{errorMessages:string[],total:number,bundleId:string,errors:number}> {
    return this.request({
      body: this.getPublishRuleData(ruleId, environmentId),
      method: RequestMethod.Post,
      headers: {
        "Content-Type": "application/x-www-form-urlencoded"
      },
      url: this._pushRuleUrl
    })
  }

  static fromServerBundleTransformFn(data):IBundle[] {
    return data.items || [];
  }

  static fromServerEnvironmentTransformFn(data):IPublishEnvironment[] {
    // Endpoint return extra empty environment
    data.shift()
    return data
  }

  
}


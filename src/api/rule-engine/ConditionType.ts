import {Inject, EventEmitter, Injectable} from 'angular2/angular2';
import {Observable, ConnectableObservable} from 'rxjs/Rx.KitchenSink'

import {ApiRoot} from "../persistence/ApiRoot";
import {CwModel, CwI18nModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {CwChangeEvent} from "../util/CwEvent";
import {I18nService, I18nResourceModel} from "../system/locale/I18n";
import {ParameterDefinition} from "../util/CwInputModel";
import {ServerSideTypeModel} from "./ServerSideFieldModel";

let noop = (...arg:any[])=> {
}



// @todo ggranum: Remove this and code that defers to it once we either add an 'enabled' field to conditionlet types,
// or we have implemented all the conditionlet types we intend to release with.
var DISABLED_CONDITION_TYPE_IDS = {
  //UsersCountryConditionlet: false,
  //UsersBrowserHeaderConditionlet: false,
  //UsersContinentConditionlet: true,
  //UsersPlatformConditionlet: true,
  UsersIpAddressConditionlet: true,
  UsersVisitedUrlConditionlet: true,
  UsersCityConditionlet: true,
  UsersTimeConditionlet: true,
  UsersLandingPageUrlConditionlet: true,
  UsersLanguageConditionlet: true,
  UsersPageVisitsConditionlet: true,
  MockTrueConditionlet: true,
  UsersUrlParameterConditionlet: true,
  UsersReferringUrlConditionlet: true,
  UsersCurrentUrlConditionlet: true,
  UsersHostConditionlet: true,
  UsersStateConditionlet: true,
  UsersSiteVisitsConditionlet: true,
  UsersDateTimeConditionlet: true,
  UsersOperatingSystemConditionlet: true,
  UsersLogInConditionlet: true,
  UsersBrowserConditionlet: true
}
@Injectable()
export class ConditionTypeService {
  private _apiRoot;
  private _ref;
  private _cacheMap:{[key:string]: ServerSideTypeModel}
  private _rsrcService:I18nService;
  private comparisonRsrc = {};

  constructor(apiRoot:ApiRoot, rsrcService:I18nService) {
    this._apiRoot = apiRoot
    this._rsrcService = rsrcService
    this._ref = apiRoot.root.child('system/ruleengine/conditionlets')
    rsrcService.get(apiRoot.authUser.locale, 'api.sites.ruleengine.rules.inputs.comparison', (model:I18nResourceModel)=> {
      this.comparisonRsrc = model.messages
    })
    this._cacheMap = {}
  }


  list(cb:Function = noop):Observable<ServerSideTypeModel[]> {
    return Observable.defer(() => this._list(cb))
  }

  private _list(cb:Function = noop):Observable<ServerSideTypeModel[]> {
    let ee;
    var cachedKeys = Object.keys(this._cacheMap);
    if (cachedKeys.length > 0) {
      ee = this._listFromCache(cachedKeys);
    } else {
      ee = new EventEmitter()
      this._ref.once('value', (snap:EntitySnapshot) => {
        let types = snap.val()
        let conditionTypes = []
        let keys = Object.keys(types).filter(key => DISABLED_CONDITION_TYPE_IDS[key] !== true);
        let count = 0
        keys.forEach((key) => {
          let json:any = snap.child(key).val()
          json.key = key
          this._rsrcService.get(this._apiRoot.authUser.locale, json.i18nKey, (rsrcResult:I18nResourceModel)=> {
            count++
            let model = ServerSideTypeModel.fromJson(json)
            if (rsrcResult) {
              /* @todo ggranum: Remove the rsrc params from the Model object. */
              model.i18n = rsrcResult
            }
            this._cacheMap[model.key] = model
            conditionTypes.push(model)
            if (count === keys.length) {
              ee.emit(conditionTypes)
            }
          })
        })
        cb(conditionTypes)
      }, (e)=> {
        throw e
      })
    }
    return ee
  }

  private _listFromCache(cachedKeys) {
    let conditionTypes = []
    cachedKeys.forEach(key => conditionTypes.push(this._cacheMap[key]))
    return Observable.of(conditionTypes);
  };

  get(key:string, cb:Function = noop) {
    console.log("ConditionTypeService", "get")
    let cachedValue = this._cacheMap[key]
    if (cachedValue) {
      cb(cachedValue)
    } else {
      /* There is no direct endpoint to get conditions by key. So we'll hydrate all of them  */
      var sub = this.list().subscribe(()=> {
        sub.unsubscribe()
        cb(this._cacheMap[key])
      })
    }
  }

}
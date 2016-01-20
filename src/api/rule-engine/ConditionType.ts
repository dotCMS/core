import {Inject, EventEmitter, Injectable} from 'angular2/core';
import {Observable, ConnectableObservable} from 'rxjs/Rx'

import {ApiRoot} from "../persistence/ApiRoot";
import {CwModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {CwChangeEvent} from "../util/CwEvent";
import {I18nService} from "../system/locale/I18n";
import {ParameterDefinition} from "../util/CwInputModel";
import {ServerSideTypeModel} from "./ServerSideFieldModel";

let noop = (...arg:any[])=> {
}

@Injectable()
export class ConditionTypeService {
  private _apiRoot;
  private _ref;
  private _cacheMap:{[key:string]: ServerSideTypeModel}
  private _rsrcService:I18nService;

  constructor(apiRoot:ApiRoot, rsrcService:I18nService) {
    this._apiRoot = apiRoot
    this._rsrcService = rsrcService
    this._ref = apiRoot.root.child('system/ruleengine/conditionlets')
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
        let keys = Object.keys(types);
        let count = 0
        keys.forEach((key) => {
          let json:any = snap.child(key).val()
          json.key = key
          let model = ServerSideTypeModel.fromJson(json)
          this._cacheMap[model.key] = model
          conditionTypes.push(model)
        })
        ee.emit(conditionTypes)
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
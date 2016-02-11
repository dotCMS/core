import {EventEmitter, Injectable} from 'angular2/core';
import {Observable} from 'rxjs/Rx'

import {ApiRoot} from "../persistence/ApiRoot";
import {CwModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {CwChangeEvent} from "../util/CwEvent";
import {I18nService} from "../system/locale/I18n";
import {ServerSideTypeModel} from "./ServerSideFieldModel";
import {TreeNode} from "../system/locale/I18n";

let noop = (...arg:any[])=> {
}

// @todo : Adding this meanwhile we move CountRulesActionlet to OSGI. And improve Jenkins to install osgi.
var DISABLED_ACTION_TYPE_IDS = {
  CountRulesActionlet: true
}

interface ActionTypeParameter {
  key:string
  dataType:string,
  i18nKey:string
  priority:number
}

@Injectable()
export class ActionTypeService {
  private _apiRoot:ApiRoot;
  private _ref;
  private _cacheMap:{[key:string]: ServerSideTypeModel}
  private _rsrcService:I18nService;

  constructor(apiRoot:ApiRoot, rsrcService:I18nService) {
    this._ref = apiRoot.root.child('system/ruleengine/actionlets')
    this._apiRoot = apiRoot
    this._rsrcService = rsrcService;
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
        let hydratedTypes = []
        let keys = Object.keys(types).filter(key => DISABLED_ACTION_TYPE_IDS[key] !== true);
        let count = 0
        keys.forEach((key) => {
          let json:any = snap.child(key).val()
          json.key = key
          let model = ServerSideTypeModel.fromJson(json)
          this._cacheMap[model.key] = model
          hydratedTypes.push(model)
        })
        ee.emit(hydratedTypes)
        cb(hydratedTypes)
      }, (e)=> {
        throw e
      })
    }
    return ee
  }

  private _listFromCache(cachedKeys) {
    let types = []
    cachedKeys.forEach(key => types.push(this._cacheMap[key]))
    return Observable.of(types);
  };

  get(key:string, cb:Function = noop) {
    let cachedValue = this._cacheMap[key]
    if (cachedValue) {
      cb(cachedValue)
    } else {
      /* There is no direct endpoint to get actions by key. So we'll hydrate all of them  */
      var sub = this.list().subscribe(()=> {
        cb(this._cacheMap[key])
        sub.unsubscribe()
      })
    }
  }

}
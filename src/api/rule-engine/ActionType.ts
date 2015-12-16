import {EventEmitter, Injectable} from 'angular2/angular2';
import {Observable} from 'rxjs/Rx.KitchenSink'

import {ApiRoot} from "../persistence/ApiRoot";
import {CwModel, CwI18nModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {CwChangeEvent} from "../util/CwEvent";
import {I18nService, I18nResourceModel, Internationalized} from "../system/locale/I18n";
import {ServerSideTypeModel} from "./ServerSideFieldModel";

let noop = (...arg:any[])=> {
}

interface ActionTypeParameter {
  key:string
  dataType:string,
  i18nKey:string
  priority:number
}


var DISABLED_ACTION_TYPE_IDS = {
  TestActionlet: true, // comment out to prove we don't need to know its name.
  CountRequestsActionlet: true
}

@Injectable()
export class ActionTypeService {
  private _apiRoot:ApiRoot;
  private _ref;
  private _cacheMap:{[key:string]: ServerSideTypeModel}
  private _rsrcService:I18nService;

  constructor( apiRoot:ApiRoot, rsrcService:I18nService) {
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
        let keys = Object.keys(types)
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
            hydratedTypes.push(model)
            if (count === keys.length) {
              ee.emit(hydratedTypes)
            }
          })
        })
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
    if (cachedValue ) {
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
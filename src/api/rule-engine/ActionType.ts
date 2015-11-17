import {Inject, EventEmitter} from 'angular2/angular2';
//import * as Rx from '../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'

import {ApiRoot} from "../persistence/ApiRoot";
import {CwModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {CwChangeEvent} from "../util/CwEvent";
import {I18nService} from "../system/locale/I18n";
import {I18nResourceModel, Internationalized} from "../system/locale/I18n";


let noop = (...arg:any[])=> {
}

interface ActionTypeParameter {
  key:string
  dataType:string,
  i18nKey:string
  priority:number
}

export class ActionTypeModel extends CwModel implements Internationalized {

  i18nKey:string
  parameters:{[key:string]:ActionTypeParameter}

  private _i18n:I18nResourceModel

  constructor(key:string = 'NoSelection', i18nKey:string = null, parameters:{[key:string]:ActionTypeParameter} = {}) {
    super(key ? key : 'NoSelection')
    this.i18nKey = i18nKey ? i18nKey : key;
    this.parameters = parameters ? parameters : {}
  }

  getMessage(key:string):string{
    return this._i18n ? this._i18n.getMessage(key) : this[key]
  }

  get i18n():I18nResourceModel {
    return this._i18n;
  }

  set i18n(value:I18nResourceModel) {
    this._i18n = value;
    this._changed('i18n')
  }

  isValid() {
    return this.isPersisted() && !!this.i18nKey
  }
}

var DISABLED_ACTION_TYPE_IDS = {
  TestActionlet: true, // comment out to prove we don't need to know its name.
  CountRequestsActionlet: true
}

export class ActionTypeService {
  private _added:EventEmitter
  private _refreshed:EventEmitter
  onAdd:Rx.Observable<ActionTypeModel>
  onRefresh:Rx.Observable<ActionTypeModel>
  private _apiRoot:ApiRoot;
  private _ref;
  private _map:{[key:string]: ActionTypeModel}
  private _rsrcService:I18nService;

  constructor(@Inject(ApiRoot) apiRoot, @Inject(I18nService) rsrcService:I18nService) {
    this._ref = apiRoot.root.child('system/ruleengine/actionlets')
    this._apiRoot = apiRoot
    this._rsrcService = rsrcService;
    this._added = new EventEmitter()
    this._refreshed = new EventEmitter()
    this.onAdd = Rx.Observable.from(this._added.toRx()).publishReplay()
    this.onRefresh = Rx.Observable.from(this._refreshed.toRx()).share()
    this._map = {}
    this.onAdd.connect()
  }

  fromSnapshot(snapshot:EntitySnapshot):ActionTypeModel {
    let val:any = snapshot.val()
    let model = new ActionTypeModel(snapshot.key(), val.i18nKey, val.parameters)
    this._rsrcService.get(this._apiRoot.authUser.locale, model.i18nKey, (result)=>{
      model.i18n = result;
    })
    return model
  }

  private _entryReceived(entry:ActionTypeModel) {
    let isRefresh = this._map[entry.key] != null
    this._map[entry.key] = entry
    if (isRefresh) {
      this._refreshed.next(entry)
    }
    else {
      this._added.next(entry)
    }
  }


  list(cb:Function = noop):Rx.Observable<ActionTypeModel> {
    this._ref.once('value', (snap:EntitySnapshot) => {
      let types = snap.val()
      let result = []
      Object.keys(types).forEach((key) => {
        if (DISABLED_ACTION_TYPE_IDS[key] !== true) {
          let actionType = snap.child(key)
          let typeModel = this.fromSnapshot(actionType)
          this._entryReceived(typeModel)
          result.push(typeModel)
        }
      })
      cb(result)
    }, (e)=> {
      throw e;
    })
    return this.onAdd
  }

  get(key:string, cb:Function = noop) {
    let cachedValue = this._map[key]
    if (cachedValue ) {
      cb(cachedValue)
    } else {
      /* There is no direct endpoint to get conditions by key. So we'll fake it a bit, and just wait for a call to
       'list' to trigger the observer. */
      var sub = this.onAdd.subscribe((type)=>{
        if(type.key == key){
          cb(type)
          sub.unsubscribe()
        }
      })
    }
  }

}
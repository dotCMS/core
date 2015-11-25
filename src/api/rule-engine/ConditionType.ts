import {Inject, EventEmitter} from 'angular2/angular2';
//import * as Rx from '../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'

import {ApiRoot} from "../persistence/ApiRoot";
import {CwModel, CwI18nModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {CwChangeEvent} from "../util/CwEvent";
import {I18nService, I18nResourceModel} from "../system/locale/I18n";

let noop = (...arg:any[])=> {
}

export class ConditionTypeModel extends CwI18nModel {

  i18nKey:string
  comparisons:Array<string>

  constructor(key:string = 'NoSelection', i18nKey:string = null, comparisons:Array<any> = []) {
    super(key ? key : 'NoSelection', i18nKey, {name: i18nKey})
    this.comparisons = comparisons || []
  }

  isValid() {
    return this.isPersisted() && !!this.i18nKey && this.comparisons && this.comparisons.length > 0
  }

}


// @todo ggranum: Remove this and code that defers to it once we either add an 'enabled' field to conditionlet types,
// or we have implemented all the conditionlet types we intend to release with.
var DISABLED_CONDITION_TYPE_IDS = {
  //UsersCountryConditionlet: false,
  //UsersBrowserHeaderConditionlet: false,
  //UsersContinentConditionlet: true, // comment out to prove we don't need to know its name.
  UsersIpAddressConditionlet: true,
  UsersVisitedUrlConditionlet: true,
  UsersCityConditionlet: true,
  UsersTimeConditionlet: true,
  UsersLandingPageUrlConditionlet: true,
  UsersPlatformConditionlet: true,
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

export class ConditionTypeService {
  private _added:EventEmitter
  private _refreshed:EventEmitter
  onAdd:Rx.Observable<ConditionTypeModel>
  onRefresh:Rx.Observable<ConditionTypeModel>
  private _apiRoot;
  private _ref;
  private _map:{[key:string]: ConditionTypeModel}
  private _rsrcService:I18nService;

  constructor(@Inject(ApiRoot) apiRoot, @Inject(I18nService) rsrcService:I18nService) {
    this._apiRoot = apiRoot
    this._rsrcService = rsrcService
    this._ref = apiRoot.root.child('system/ruleengine/conditionlets')

    this._added = new EventEmitter()
    this._refreshed = new EventEmitter()
    this.onAdd = Rx.Observable.from(this._added.toRx()).publishReplay()
    this.onRefresh = Rx.Observable.from(this._refreshed.toRx()).share()
    this._map = {}
    this.onAdd.connect()
  }

  static fromSnapshot(snapshot:EntitySnapshot):ConditionTypeModel {
    let val:any = snapshot.val()
    return new ConditionTypeModel(snapshot.key(), val.i18nKey, val.comparisons)
  }

  private _entryReceived(entry:ConditionTypeModel) {
    let isRefresh = this._map[entry.key] != null
    this._map[entry.key] = entry
    if (isRefresh) {
      this._refreshed.next(entry)
    }
    else {
      this._added.next(entry)
    }
  }


  list(cb:Function = noop):Rx.Observable<ConditionTypeModel> {
    this._ref.once('value', (snap:EntitySnapshot) => {
      let types = snap.val()
      let conditionTypes = []
      Object.keys(types).forEach((key) => {
        if (DISABLED_CONDITION_TYPE_IDS[key] !== true) {
          let conditionType = snap.child(key)
          let model = ConditionTypeService.fromSnapshot(conditionType)
          this._rsrcService.get(this._apiRoot.authUser.locale, model.i18nKey, (rsrcResult:I18nResourceModel)=>{
            if (rsrcResult) {
              model.i18n = rsrcResult;
            }
            this._entryReceived(model)
            conditionTypes.push(model)
          })
        }
      })
      cb(conditionTypes)
    }, (e)=> {
      throw e
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
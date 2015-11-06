import {Inject} from 'angular2/angular2';
import {EntityMeta} from "../persistence/EntityBase";
import {ApiRoot} from "../persistence/ApiRoot";


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
export class ConditionTypeModel {

  private _id:string
  i18nKey:string
  comparisons:Array<string>

  constructor(id:string = 'NoSelection', i18nKey:string = null, type:any = null) {
    this._id = id;
    this.i18nKey = i18nKey;

    this.comparisons = []
    if (type && type.comparisons) {
      Object.keys(type.comparisons).forEach((key)=> {
        this.comparisons.push(type.comparisons[key]._id)
      })
    }
  }

  get id():string {
    return this._id;
  }

  set id(value:string) {
    this._id = (value && value.length) ? value : 'NoSelection'
  }

  rhsValues(parameters:any):any {
    let map = {}
    if (parameters) {
      Object.keys(parameters).forEach((key)=> {
        map[parameters[key].key] = parameters[key].value
      })
    }
    return map
  }
}


export class ConditionTypesProvider {
  typeModels:Map<string, Function>
  ref:EntityMeta
  ary:Array<any>
  map:Map<string,ConditionTypeModel>
  promise:Promise<any>

  constructor(@Inject(ApiRoot) apiRoot) {
    this.typeModels = new Map<string, Function>()
    this.map = new Map<string, ConditionTypeModel>()
    this.ary = []
    this.ref = apiRoot.root.child('system/ruleengine/conditionlets')
    this.init();
  }

  init() {
    this.promise = new Promise((resolve, reject) => {
      this.ref.once('value', (snap) => {
        let types = snap['val']()
        Object.keys(types).forEach((key) => {
          if (DISABLED_CONDITION_TYPE_IDS[key] !== true) {
            let conditionType = types[key]
            let typeModel = new ConditionTypeModel(key, types[key])
            Object.assign(typeModel, conditionType)
            this.map.set(key, typeModel)
            this.ary.push(types[key])
          }
        })
        resolve(this);
      })
    });
  }

  getType(id:string):ConditionTypeModel {
    return this.map.get(id);
  }

}


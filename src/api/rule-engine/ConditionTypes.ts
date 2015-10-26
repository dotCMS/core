/// <reference path="../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />
import {Inject} from 'angular2/angular2';

import {ApiRoot} from 'api/persistence/ApiRoot';
import {EntityMeta} from "api/persistence/EntityBase";

// @todo ggranum: Remove this and code that defers to it once we either add an 'enabled' field to conditionlet types,
// or we have implemented all the conditionlet types we intend to release with.
var ENABLED_CONDITION_TYPE_IDS = {
  UsersBrowserHeaderConditionlet: true,
  UsersCountryConditionlet: true
}


export class ConditionTypeModel {
  id:string
  i18nKey:string
  comparisons:Array<string>

  constructor(id:string, type) {
    this.id = id
    this.comparisons = []
    if (type && type.comparisons) {
      Object.keys(type.comparisons).forEach((key)=> {
        this.comparisons.push(type.comparisons[key].id)
      })
    }
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
  ary:Array
  map:Map<string,ConditionTypeModel>
  promise:Promise

  constructor(@Inject(ApiRoot) apiRoot) {
    this.typeModels = new Map<string, Function>()
    this.map = new Map<string,any>()
    this.ary = []
    this.ref = apiRoot.root.child('system/ruleengine/conditionlets')
    this.init();
  }

  init() {
    this.promise = new Promise((resolve, reject) => {
      this.ref.once('value', (snap) => {
        let types = snap['val']()
        Object.keys(types).forEach((key) => {
          if (ENABLED_CONDITION_TYPE_IDS[key]) {
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


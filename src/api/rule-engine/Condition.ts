import {EventEmitter, Injectable} from 'angular2/core';
import {Observable, ConnectableObservable} from 'rxjs/Rx'

import {RuleModel} from "./Rule";
import {CwModel} from "../util/CwModel";
import {ApiRoot} from "../persistence/ApiRoot";
import {EntitySnapshot} from "../persistence/EntityBase";
import {ConditionGroupModel} from "./ConditionGroup";
import {CwChangeEvent} from "../util/CwEvent";
import {ConditionTypeService} from "./ConditionType";
import {ParameterDefinition} from "../util/CwInputModel";
import {ServerSideTypeModel} from "./ServerSideFieldModel";
import {ServerSideFieldModel} from "./ServerSideFieldModel";


let noop = (...arg:any[])=> {
}

export interface ParameterModel {
  key:string
  value:string
  priority:number
}

export class ConditionModel extends ServerSideFieldModel {
  operator:string
  owningGroup:ConditionGroupModel

  constructor(key:string, type:ServerSideTypeModel) {
    super(key, type)
  }

  isValid() {
    return !!this.owningGroup && !!this.getParameterValue('comparison') && super.isValid()
  }

  toJson():any {
    let json = super.toJson()
    json.owningGroup = this.owningGroup;
    return json
  }

  static fromJson():ConditionModel {
    return null
  }
}

@Injectable()
export class ConditionService {
  private _apiRoot;
  private _ref;
  private _conditionTypeService:ConditionTypeService
  private _cacheMap:{[key:string]: ServerSideTypeModel}

  constructor(apiRoot:ApiRoot, conditionTypeService:ConditionTypeService) {
    this._apiRoot = apiRoot
    this._conditionTypeService = conditionTypeService
    this._ref = apiRoot.defaultSite.child('ruleengine/conditions')
    this._cacheMap = {};
  }

  fromSnapshot(group:ConditionGroupModel, snapshot:EntitySnapshot, cb:Function = noop) {
    let val:any = snapshot.val()
    let count = 0
    this._conditionTypeService.get(val.conditionlet, (type:ServerSideTypeModel)=> {
      console.log("ConditionService", "count:", count++, snapshot.key())
      try {
        let ra = new ConditionModel(snapshot.key(), type)
        ra.owningGroup = group
        ra.priority = val.priority
        ra.operator = val.operator

        Object.keys(val.values).forEach((key)=> {
          let x = val.values[key]
          ra.setParameter(key, x.value, x.priority)
        })
        cb(ra)
      } catch (e) {
        console.log("Error reading Condition.", e)
        throw e;
      }
    })
  }

  static toJson(condition:ConditionModel):any {
    let json:any = {}
    json.id = condition.key
    json.owningGroup = condition.owningGroup.key
    json.conditionlet = condition.type.key
    json.priority = condition.priority
    json.operator = condition.operator
    json.values = condition.parameters
    return json
  }

  listForGroup(group:ConditionGroupModel):Observable<ConditionModel[]> {
    let ee = new EventEmitter()
    let deferred = Observable.defer(() => ee)
    var keys = Object.keys(group.conditions);
    let count = 0
    let conditions = []
    keys.forEach((conditionId)=> {
      if (this._cacheMap[conditionId]) {
        count++
        conditions.push(this._cacheMap[conditionId])
        if (count == keys.length) {
          ee.emit(conditions)
        }
      } else {
        let cRef = this._ref.child(conditionId)
        cRef.once('value', (conditionSnap)=> {
          this.fromSnapshot(group, conditionSnap, (model)=> {
            count++
            conditions.push(model)
            this._cacheMap[model.key] = model
            if (count == keys.length) {
              ee.emit(conditions)
            }
          })
        }, (e)=> {
          throw e
        })
      }
    })
    return deferred
  }

  get(group:ConditionGroupModel, key:string, cb:Function = null) {
    this._ref.child(key).once('value', (conditionSnap)=> {
      let model = this.fromSnapshot(group, conditionSnap, (model) => {
        cb(model)
      });

    }, (e)=> {
      throw e
    })
  }

  add(model:ConditionModel, cb:Function = noop) {
    console.log("api.rule-engine.ConditionService", "add", model)
    if (!model.isValid()) {
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    let json = ConditionService.toJson(model)
    this._ref.push(json, (e, result)=> {
      if (e) {
        throw e;
      }
      model.key = result.key()
      cb(model)
    })
  }

  save(model:ConditionModel, cb:Function = noop) {
    console.log("api.rule-engine.ConditionService", "save", model)
    if (!model.isValid()) {
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    if (!model.isPersisted()) {
      this.add(model, cb)
    } else {
      let json = ConditionService.toJson(model)
      this._ref.child(model.key).set(json, (result)=> {
        cb(model)
      })
    }
  }

  remove(model:ConditionModel, cb:Function = noop) {
    console.log("api.rule-engine.ConditionService", "remove", model)
    this._ref.child(model.key).remove(()=> {
      cb(model)
    })
  }
}
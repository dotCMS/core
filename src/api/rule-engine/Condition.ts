import {Inject, EventEmitter} from 'angular2/angular2';
//import * as Rx from '../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'

import {RuleModel} from "./Rule";
import {ConditionTypeModel} from "./ConditionType";
import {CwModel} from "../util/CwModel";
import {ApiRoot} from "../persistence/ApiRoot";
import {EntitySnapshot} from "../persistence/EntityBase";
import {ConditionGroupModel} from "./ConditionGroup";
import {CwChangeEvent} from "../util/CwEvent";
import {ConditionTypeService} from "./ConditionType";
import {ParameterDefinition} from "../util/CwInputModel";


let noop = (...arg:any[])=> {
}

export interface ParameterModel {
  key:string
  value:string
  priority:number
}

export class ConditionModel extends CwModel {

  private _name:string // @todo ggranum: vestigial field, kill on server side.

  private _comparison:string
  private _operator:string
  private _owningGroup:ConditionGroupModel
  private _conditionType:ConditionTypeModel
  private _parameters:{[key:string]: ParameterModel}
  private _parameterDefs:{[key:string]: ParameterDefinition}

  constructor(key:string = null, conditionType:ConditionTypeModel) {
    super(key)
    this._conditionType = conditionType
    this.operator = 'AND'
    this.priority = 1
    this._parameters = {}
    this._parameterDefs = {}
  }
  setParameterDef(key:string, paramDef:ParameterDefinition) {
    this._parameterDefs[key] = paramDef
    let defVal = paramDef.defaultValue
    if(defVal === ''){
      defVal = null // components won't show placeholders as they treat empty string as a real value.
    }
    this._parameters[key] = {key: key, value: defVal, priority: paramDef.priority}
    console.log("Foo:", key, paramDef.defaultValue==null)

  }
  setParameter(key:string, value:any, priority:number = 1) {
    if(this._parameterDefs[key] === undefined){
      console.log("Unsupported parameter: ", key)
      return;
    }
    let existing = this._parameters[key]
    this._parameters[key] = {key: key, value: value, priority: priority}
    this._changed('parameters')
  }

  getParameter(key:string):ParameterModel {
    let v:any = ''
    if (this.parameters[key] !== undefined) {
      v = this.parameters[key]
    }
    return v
  }
  getParameterValue(key:string):string {
    let v:any = ''
    if (this.parameters[key] !== undefined) {
      v = this.parameters[key].value
    }
    return v
  }

  getParameterDef(key:string):ParameterDefinition {
    let v:any = ''
    if (this._parameterDefs[key] !== undefined) {
      v = this._parameterDefs[key]
    }
    return v
  }

  clearParameters() {
    this._parameters = {}
    this._changed('parameters')
  }

  get parameterDefs():{[key:string]: ParameterDefinition} {
    return this._parameterDefs
  }

  get parameters():{[key:string]: ParameterModel} {
    return this._parameters
  }


  get conditionType():ConditionTypeModel {
    return this._conditionType;
  }

  set conditionType(value:ConditionTypeModel) {
    this._conditionType = value;
    this._changed('conditionType')
  }

  get owningGroup():ConditionGroupModel {
    return this._owningGroup;
  }

  set owningGroup(value:ConditionGroupModel) {
    this._owningGroup = value;
    this._changed('owningGroup')
  }

  get name():string {
    return this._name;
  }

  set name(value:string) {
    this._name = value;
    this._changed('name')
  }

  get comparison():string {
    return this._comparison;
  }

  get operator():string {
    return this._operator;
  }

  set operator(value:string) {
    this._operator = value;
    this._changed('operator')
  }

  _changed(key:string) {
    console.log("api.rule-engine.ConditionModel", "_changed", key)
    super._changed(key)
  }

  isValid() {
    let valid = !!this._owningGroup
    valid = valid && this._owningGroup.isValid() && this._owningGroup.isPersisted()
    valid = valid && this._conditionType && this._conditionType.key && this._conditionType.key != 'NoSelection'
    return valid
  }
}

export class ConditionService {
  private _removed:EventEmitter
  private _added:EventEmitter
  onRemove:Rx.Observable<ConditionModel>
  onAdd:Rx.Observable<ConditionModel>
  private _apiRoot;
  private _ref;
  private _conditionTypeService:ConditionTypeService

  constructor(@Inject(ApiRoot) apiRoot, @Inject(ConditionTypeService) conditionTypeService:ConditionTypeService) {
    this._apiRoot = apiRoot
    this._conditionTypeService = conditionTypeService
    this._ref = apiRoot.defaultSite.child('ruleengine/conditions')
    this._added = new EventEmitter()
    this._removed = new EventEmitter()
    let onAdd = Rx.Observable.from(this._added.toRx())
    let onRemove = Rx.Observable.from(this._removed.toRx())
    this.onAdd = onAdd.share()
    this.onRemove = onRemove.share()
  }

  fromSnapshot(group:ConditionGroupModel, snapshot:EntitySnapshot, cb:Function=noop) {
    let val:any = snapshot.val()

    this._conditionTypeService.get(val.conditionlet, (type:ConditionTypeModel)=> {
      try {
        let ra = new ConditionModel(snapshot.key(), type)
        ra.name = val.name;
        ra.owningGroup = group
        ra.priority = val.priority
        ra.operator = val.operator
        Object.keys(type.parameters).forEach((key)=> {
          let x = type.parameters[key]
          ra.setParameterDef(key, ParameterDefinition.fromJson(x))
        })
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
    json.name = condition.name || "fake_name_" + new Date().getTime() + '_' + Math.random()
    json.owningGroup = condition.owningGroup.key
    json.conditionlet = condition.conditionType.key
    json.priority = condition.priority
    json.operator = condition.operator
    json.values = condition.parameters
    return json
  }


  addConditionsFromRule(rule:RuleModel) {
    Object.keys(rule.groups).forEach((groupId)=> {
      let group:ConditionGroupModel = rule.groups[groupId];
      this.addConditionsFromConditionGroup(group)
    })
  }

  addConditionsFromConditionGroup(group:ConditionGroupModel) {
    Object.keys(group.conditions).forEach((conditionId)=> {
      let cRef = this._ref.child(conditionId)
      cRef.once('value', (conditionSnap)=> {
        this.fromSnapshot(group, conditionSnap, (model)=>{
          this._added.next(model)
        })
      }, (e)=> {
        throw e
      })
    })
  }

  listForRule(rule:RuleModel):Rx.Observable<ConditionModel> {
    if (rule.isPersisted()) {
      this.addConditionsFromRule(rule)
    } else {
      rule.onChange.subscribe((event) => {
        if (event.type == 'key') {
          this.addConditionsFromRule(event.target)
        }
      })
    }
    return this.onAdd
  }

  listForGroup(group:ConditionGroupModel) {
    this.addConditionsFromConditionGroup(group)
  }

  get(group:ConditionGroupModel, key:string, cb:Function = null) {
    this._ref.child(key).once('value', (conditionSnap)=> {
      let model = this.fromSnapshot(group, conditionSnap, (model) => {
        this._added.next(model)
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
      if(e){
        throw e;
      }
      model.key = result.key()
      this._added.next(model)
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
      this._removed.next(model)
      cb(model)
    })
  }
}
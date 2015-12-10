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

  name:string // @todo ggranum: vestigial field, kill on server side.
  comparison:string
  operator:string
  owningGroup:ConditionGroupModel
  conditionType:ConditionTypeModel
  parameters:{[key:string]: ParameterModel}
  parameterDefs:{[key:string]: ParameterDefinition}

  constructor(key:string, type:ConditionTypeModel) {
    super(key)
    this.parameters = {}
    this.parameterDefs = {}
    this.operator = 'AND'
    this.priority = type.priority
    Object.keys(type.parameters).forEach((key)=> {
      let x = type.parameters[key]
      let paramDef = ParameterDefinition.fromJson(x)
      this.parameterDefs[key] = paramDef
      this.parameters[key] = {key: key, value: paramDef.defaultValue, priority: paramDef.priority}
    })
  }

  setParameter(key:string, value:any, priority:number = 1) {
    if(this.parameterDefs[key] === undefined){
      console.log("Unsupported parameter: ", key)
      return;
    }
    let existing = this.parameters[key]
    this.parameters[key] = {key: key, value: value, priority: priority}
  }

  getParameter(key:string):ParameterModel {
    let v:any = ''
    if (this.parameters[key] !== undefined) {
      v = this.parameters[key]
    }
    return v
  }
  getParameterValue(key:string):string {
    let v:any = null
    if (this.parameters[key] !== undefined) {
      v = this.parameters[key].value
    }
    return v
  }

  getParameterDef(key:string):ParameterDefinition {
    let v:any = ''
    if (this.parameterDefs[key] !== undefined) {
      v = this.parameterDefs[key]
    }
    return v
  }

  isValid() {
    let valid = !!this.owningGroup
    valid = valid && this.owningGroup.isValid() && this.owningGroup.isPersisted()
    if(this.parameterDefs) {
      Object.keys(this.parameterDefs).forEach(key=> {
        let paramDef = this.getParameterDef(key)
        var value = this.parameters[key].value;
        valid = valid && paramDef.inputType.verify(value).valid
        console.log("validate => key: ", key, "  value: ", value, "  valid: ", valid)
      })
    }
    valid = valid && this.conditionType && this.conditionType.key && this.conditionType.key != 'NoSelection'
    console.log("validate => Result: ", valid)

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
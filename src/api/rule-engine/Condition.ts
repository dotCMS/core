/// <reference path="../../../jspm_packages/npm/@reactivex/rxjs@5.0.0-alpha.4/dist/cjs/Rx.KitchenSink.d.ts" />

import {Inject, EventEmitter} from 'angular2/angular2';
import * as Rx from '@reactivex/rxjs@5.0.0-alpha.4/dist/cjs/Rx.KitchenSink'


import {ApiRoot} from 'api/persistence/ApiRoot';
import {CwChangeEvent} from "api/util/CwEvent";
import {CwModel} from "api/util/CwModel";
import {EntityMeta, EntitySnapshot} from "api/persistence/EntityBase";
import {RuleModel, ConditionGroupModel} from "./Rule";
import {ConditionTypeModel} from "./ConditionTypes";


let noop = (...arg:any[])=> {
}

interface ConditionModelParameter {
  key:string
  value:any
  priority:number
}

export class ConditionModel extends CwModel {

  private _name:string // @todo ggranum: vestigial field, kill on server side.

  private _comparison:string
  private _operator:string
  private _owningGroup:ConditionGroupModel
  private _conditionType:ConditionTypeModel
  private _parameters:{[key:string]: ConditionModelParameter}

  constructor(key:string = null) {
    super(key)
    this._conditionType = new ConditionTypeModel('', '')
    this.comparison = 'is'
    this.operator = 'AND'
    this.priority = 1
    this._parameters = {}
  }

  setParameter(key:string, value:any, priority:number = 1) {
    let existing = this._parameters[key]
    this._parameters[key] = {key: key, value: value, priority: priority}
    this._changed('parameters')
  }

  getParameter(key:string):any {
    let v:any = ''
    if (this.parameters[key] !== undefined) {
      v = this.parameters[key].value
    }
    return v
  }

  clearParameters() {
    this._parameters = {}
    this._changed('parameters')
  }

  get parameters():{[key:string]: ConditionModelParameter} {
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

  set comparison(value:string) {
    this._comparison = value;
    this._changed('comparison')
  }

  get operator():string {
    return this._operator;
  }

  set operator(value:string) {
    this._operator = value;
  }

  _changed(key:string) {
    console.log("api.rule-engine.ConditionModel", "_changed", key)
    super._changed(key)
  }

  isValid() {
    let valid = !!this._owningGroup
    valid = valid && this._owningGroup.isValid()
    valid = valid && this._conditionType && this._conditionType.id && this._conditionType.id != 'NoSelection'
    return valid
  }
}

export class ConditionService {
  private _removed:EventEmitter
  private _added:EventEmitter
  onRemove:Rx.Observable
  onAdd:Rx.Observable
  private apiRoot;
  private ref;

  constructor(@Inject(ApiRoot) apiRoot) {
    this.ref = apiRoot.defaultSite.child('ruleengine/conditions')
    this.apiRoot = apiRoot
    this._added = new EventEmitter()
    this._removed = new EventEmitter()
    let onAdd = Rx.Observable.from(this._added.toRx())
    let onRemove = Rx.Observable.from(this._removed.toRx())
    this.onAdd = onAdd.share()
    this.onRemove = onRemove.share()
  }

  static fromSnapshot(group:ConditionGroupModel, snapshot:EntitySnapshot):ConditionModel {
    let val:any = snapshot.val()
    let ra = new ConditionModel(val.id)
    ra.name = val.name;
    ra.owningGroup = group
    ra.conditionType = new ConditionTypeModel(val.conditionlet, null)
    ra.comparison = val.comparison
    ra.priority = val.priority
    ra.operator = val.operator
    Object.keys(val.values).forEach((key)=> {
      let x = val.values[key]
      ra.setParameter(key, x.value, x.priority)
    })
    return ra
  }

  static toJson(condition:ConditionModel):any {
    let json:any = {}
    json.id = condition.key
    json.name = condition.name || "fake_name_" + new Date().getTime() + '_' + Math.random()
    json.owningGroup = condition.owningGroup.key
    json.conditionlet = condition.conditionType.id
    json.comparison = condition.comparison
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
      let cRef = this.ref.child(conditionId)
      cRef.once('value', (conditionSnap)=> {
        this._added.next(ConditionService.fromSnapshot(group, conditionSnap))
      })
    })
  }

  listForRule(rule:RuleModel):Rx.Observable {
    if (rule.isPersisted()) {
      this.addConditionsFromRule(rule)
    } else {
      rule.onChange.subscribe((event) => {
        if (event.key == 'key') {
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
        this.ref.child(key).once('value', (conditionSnap)=> {
      let model = ConditionService.fromSnapshot(group, conditionSnap);
      this._added.next(model)
      cb(model)
    })
  }

  add(model:ConditionModel, cb:Function = noop) {
    console.log("api.rule-engine.ConditionService", "add", model)
    if (!model.isValid()) {
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    let json = ConditionService.toJson(model)
    this.ref.push(json, (result)=> {
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
      this.ref.child(model.key).set(json, (result)=> {
        cb(model)
      })
    }
  }

  remove(model:ConditionModel, cb:Function = noop) {
    console.log("api.rule-engine.ConditionService", "remove", model)
    this.ref.child(model.key).remove(()=> {
      this._removed.next(model)
      cb(model)
    })
  }


}


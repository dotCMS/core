import {Inject, EventEmitter} from 'angular2/angular2';
import * as Rx from 'rxjs/Rx.KitchenSink'

import {RuleModel} from "./Rule";
import {ConditionService, ConditionModel} from "./Condition";
import {CwModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {ApiRoot} from "../persistence/ApiRoot";


export class ConditionGroupModel extends CwModel {

  owningRule:RuleModel
  operator:string
  conditions:{ [key: string]: boolean }

  constructor(key:string = null) {
    super(key)
    this.conditions = {}
  }

  isValid() {
    let valid = !!this.owningRule
    valid = valid && this.owningRule.isValid() && this.owningRule.isPersisted()
    valid = valid && this.operator && (this.operator === 'AND' || this.operator === 'OR')
    return valid
  }
}

export class ConditionGroupService {
  private _removed:EventEmitter<ConditionGroupModel>
  private _added:EventEmitter<ConditionGroupModel>
  onRemove:Rx.Observable<ConditionGroupModel>
  onAdd:Rx.Observable<ConditionGroupModel>
  private apiRoot;
  private ref;

  constructor(@Inject(ApiRoot) apiRoot, @Inject(ConditionService) conditionService:ConditionService) {
    this.ref = apiRoot.defaultSite.child('ruleengine/rules')
    this.apiRoot = apiRoot
    this._added = new EventEmitter()
    this._removed = new EventEmitter()
    let onAdd = Rx.Observable.from(this._added)
    let onRemove = Rx.Observable.from(this._removed)
    this.onAdd = onAdd.share()
    this.onRemove = onRemove.share()

    conditionService.onAdd.subscribe((conditionModel:ConditionModel) => {
      console.log("api.rule-engine.ConditionGroup", "conditionService.onAdd.subscribe", conditionModel)
      if (!conditionModel.owningGroup.conditions[conditionModel.key]) {
        conditionModel.owningGroup.conditions[conditionModel.key] = true
        this.save(conditionModel.owningGroup)
      }
    })

    conditionService.onRemove.subscribe((conditionModel:ConditionModel) => {
      console.log("api.rule-engine.ConditionGroup", "conditionService.onRemove.subscriber", conditionModel)
      if (conditionModel.owningGroup.conditions[conditionModel.key]) {
        delete conditionModel.owningGroup.conditions[conditionModel.key]
        this.save(conditionModel.owningGroup)
      }
    })

  }

  static _fromSnapshot(rule:RuleModel, snapshot:EntitySnapshot):ConditionGroupModel {
    let val:any = snapshot.val()
    let ra = new ConditionGroupModel(snapshot.key())
    ra.owningRule = rule
    ra.operator = val.operator
    ra.priority = val.priority
    ra.conditions = val.conditions
    return ra
  }

  static _toJson(conditionGroup:ConditionGroupModel):any {
    let json:any = {}

    json.id = conditionGroup.key
    json.operator = conditionGroup.operator
    json.priority = conditionGroup.priority
    json.conditions = conditionGroup.conditions
    return json
  }

  static toJsonList(models:{[key:string]: ConditionGroupModel}):any {
    let list = {}
    Object.keys(models).forEach((key)=> {
      list[key] = ConditionGroupService._toJson(models[key])
    })
    return list
  }

  list(rule:RuleModel):Rx.Observable<ConditionGroupModel> {
    if (rule.isPersisted()) {
      this.addConditionGroupsFromRule(rule)
    }
    return this.onAdd
  }

  addConditionGroupsFromRule(rule:RuleModel) {
    let conditionGroupsSnap = rule.snapshot.child('conditionGroups')
    if (conditionGroupsSnap.exists()) {
      conditionGroupsSnap.forEach((conditionGroupSnap:EntitySnapshot) => {
        this._added.next(ConditionGroupService._fromSnapshot(rule, conditionGroupSnap))
      })
    }
  }

  get(rule:RuleModel, key:string) {
  }

  add(model:ConditionGroupModel, cb:Function = null) {
    console.log("api.rule-engine.ConditionGroupService", "add", model)

    if(!model.isValid()){
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    let json = ConditionGroupService._toJson(model)
    this.ref.child(model.owningRule.key).child('conditionGroups').push(json, (e, result)=> {
      if(e){
        throw e;
      }
      model.key = result.key()
      this._added.next(model)
      if (cb) {
        cb(model)
      }
    })
  }

  save(model:ConditionGroupModel, cb:Function = null) {
    console.log("api.rule-engine.ConditionGroupService", "save", model)
    if(!model.isValid()){
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    debugger
    if (!model.isPersisted()) {
      this.add(model, cb)
    } else {
      let json = ConditionGroupService._toJson(model)
      this.ref.child(model.owningRule.key).child('conditionGroups').child(model.key).set(json, (result)=> {
        if (cb) {
          cb(model)
        }
      })
    }
  }


  remove(model:ConditionGroupModel, cb:Function = null) {
    console.log("api.rule-engine.ConditionGroupService", "remove", model)
    // ConditionGroup is a special case. Have to delete the group from
    if (model.isPersisted()) {
      this.ref.child(model.owningRule.key).child('conditionGroups').child(model.key).remove(()=> {
        this._removed.next(model)
        if (cb) {
          cb(model)
        }
      })
    }
  }


}


import {Inject, EventEmitter} from 'angular2/angular2';
//noinspection TypeScriptCheckImport
import * as Rx from 'rxjs/dist/cjs/Rx'


import {ApiRoot} from 'api/persistence/ApiRoot';
import {CwChangeEvent} from "api/util/CwEvent";
import {CwModel} from "api/util/CwModel";
import {EntityMeta, EntitySnapshot} from "api/persistence/EntityBase";
import {RuleModel} from "./Rule";
import {ConditionTypeModel, ConditionTypesProvider} from "./ConditionTypes";
import {ConditionService, ConditionModel} from "./Condition";


export class ConditionGroupModel extends CwModel {

  private _owningRule:RuleModel
  private _operator:string
  conditions:{ [key: string]: boolean }

  constructor(key:string = null) {
    super(key)
    this.conditions = {}
  }

  get owningRule():RuleModel {
    return this._owningRule;
  }

  set owningRule(value:RuleModel) {
    this._owningRule = value;
    this._changed('owningRule')
  }

  get operator():string {
    return this._operator;
  }

  set operator(value:string) {
    this._operator = value;
    this._changed('operator')
  }


  isValid() {
    let valid = !!this._owningRule
    valid = valid && this._owningRule.isValid()
    valid = valid && this.operator && (this.operator === 'AND' || this.operator === 'OR')
    return valid
  }
}

export class ConditionGroupService {
  private _removed:EventEmitter
  private _added:EventEmitter
  onRemove:Rx.Observable
  onAdd:Rx.Observable
  private apiRoot;
  private ref;

  constructor(@Inject(ApiRoot) apiRoot, @Inject(ConditionService) conditionService:ConditionService) {
    this.ref = apiRoot.defaultSite.child('ruleengine/rules')
    this.apiRoot = apiRoot
    this._removed = new EventEmitter()
    this.onRemove = this._removed.toRx()
    this._added = new EventEmitter()
    this.onAdd = this._added.toRx()

    conditionService.onAdd.subscribe((conditionModel:ConditionModel) => {
      if (!conditionModel.owningGroup.conditions[conditionModel.key]) {
        conditionModel.owningGroup.conditions[conditionModel.key] = true
        this.save(conditionModel.owningGroup)
      }
    })

    conditionService.onRemove.subscribe((conditionModel:ConditionModel) => {
      if (conditionModel.owningGroup.conditions[conditionModel.key]) {
        delete conditionModel.owningGroup.conditions[conditionModel.key]
        this.save(conditionModel.owningGroup)
      }
    })

  }

  static _fromSnapshot(rule:RuleModel, snapshot:EntitySnapshot):ConditionGroupModel {
    let val:any = snapshot.val()
    let ra = new ConditionGroupModel(val.id)
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

  list(rule:RuleModel):Rx.Observable {
    if (rule.isPersisted()) {
      this.addConditionGroupsFromRule(rule)
    } else {
      rule.onChange.subscribe((event) => {
        if (event.key == 'key') {
          this.addConditionGroupsFromRule(event.target)
        }
      })
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

  add(conditionGroup:ConditionGroupModel, cb:Function = null) {
    let json = ConditionGroupService._toJson(conditionGroup)
    this.ref.child(conditionGroup.owningRule.key).child('conditionGroups').push(json, (result)=> {
      conditionGroup.key = result.key()
      this._added.next(conditionGroup)
      if (cb) {
        cb(conditionGroup)
      }
    })
  }

  save(conditionGroup:ConditionGroupModel, cb:Function = null) {
    if (!conditionGroup.isPersisted()) {
     this.add(conditionGroup, cb)
    } else {
      let json = ConditionGroupService._toJson(conditionGroup)
      this.ref.child(conditionGroup.owningRule.key).child('conditionGroups').child(conditionGroup.key).set(json, (result)=> {
        if (cb) {
          cb(conditionGroup)
        }
      })
    }
  }


}


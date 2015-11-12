import {Inject, EventEmitter} from 'angular2/angular2';
//import * as Rx from '../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'

import {ApiRoot} from "../persistence/ApiRoot";
import {CwModel} from "../util/CwModel";
import {RuleModel} from "./Rule";
import {ActionTypeModel} from "./ActionType";
import {EntitySnapshot} from "../persistence/EntityBase";
import {ActionTypeService} from "./ActionType";

interface ActionModelParameter {
  key:string
  value:any
}

let noop = (...arg:any[])=> {}

export class ActionModel extends CwModel {
  private _name:string
  private _owningRule:RuleModel
  private _actionType:ActionTypeModel
  parameters:{ [key: string]: ActionModelParameter }

  constructor(key:string = null) {
    super(key)
    this._actionType = new ActionTypeModel('NoSelection', '')
    this.parameters = {}
  }

  setParameter(key:string, value:any) {
    let existing = this.parameters[key]
    this.parameters[key] = {key: key, value: value}
    this._changed('parameters')
  }

  getParameter(key:string):string {
    let v:any = ''
    if (this.parameters[key]) {
      v = this.parameters[key].value
    }
    return v
  }

  clearParameters():void {
    this.parameters = {}
    this._changed('parameters')
  }

  get actionType():ActionTypeModel {
    return this._actionType;
  }

  set actionType(value:ActionTypeModel) {
    this._actionType = value;
    this._changed('actionType')
  }

  get owningRule():RuleModel {
    return this._owningRule;
  }

  set owningRule(value:RuleModel) {
    this._owningRule = value;
    this._changed('owningRule')
  }

  get name():string {
    return this._name;
  }

  set name(value:string) {
    this._name = value;
    this._changed('name')
  }

  isValid() {
    let valid = !!this._owningRule
    valid = valid && this._owningRule.isValid()
    valid = valid && this._actionType && this._actionType.key && this._actionType.key != 'NoSelection'
    return valid
  }
}

export class ActionService {
  private _removed:EventEmitter
  private _added:EventEmitter
  onRemove:Rx.Observable<ActionModel>
  onAdd:Rx.Observable<ActionModel>
  private apiRoot;
  private ref;
  private _typeService:ActionTypeService

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot, @Inject(ActionTypeService) typeService:ActionTypeService) {
    this._typeService = typeService
    this.ref = apiRoot.defaultSite.child('ruleengine/actions')
    this.apiRoot = apiRoot
    this._added = new EventEmitter()
    this._removed = new EventEmitter()
    let onAdd = Rx.Observable.from(this._added.toRx())
    let onRemove = Rx.Observable.from(this._removed.toRx())
    this.onAdd = onAdd.share()
    this.onRemove = onRemove.share()
  }

  _fromSnapshot(rule:RuleModel, snapshot:EntitySnapshot):ActionModel {
    let val:any = snapshot.val()
    let ra = new ActionModel(snapshot.key())
    ra.name = val.name;
    ra.owningRule = rule
    ra.actionType = new ActionTypeModel(val.actionlet)

    Object.keys(val.parameters).forEach((key)=> {
      let param = val.parameters[key]
      ra.setParameter(key, param.value)
    })
    this._typeService.get(val.actionlet, (type)=> {
      ra.actionType = type
    })
    return ra
  }

  _toJson(action:ActionModel):any {
    let json:any = {}
    json.name = action.name || "fake_name_" + new Date().getTime() + '_' + Math.random()
    json.owningRule = action.owningRule.key
    json.actionlet = action.actionType.key
    json.priority = action.priority
    json.parameters = action.parameters
    return json
  }

  list(rule:RuleModel):Rx.Observable<ActionModel> {
    if (rule.isPersisted()) {
      this.addActionsFromRule(rule)
    }
    return this.onAdd
  }

  addActionsFromRule(rule:RuleModel) {
    Object.keys(rule.actions).forEach((actionId)=> {
      this.get(rule, actionId)
    })
  }

  get(rule:RuleModel, key:string, cb:Function=noop) {
    this.ref.child(key).once('value', (actionSnap:EntitySnapshot)=> {
      let model = this._fromSnapshot(rule, actionSnap)
      this._added.next(model)
      cb(model)
    })
  }

  add(model:ActionModel, cb:Function = noop) {
    console.log("api.rule-engine.ActionService", "add", model)

    let json = this._toJson(model)
    this.ref.push(json, (result)=> {
      model.key = result.key()
      this._added.next(model)
      cb(model)
    })
  }

  save(model:ActionModel,cb:Function = noop ) {
    console.log("api.rule-engine.ActionService", "save", model)
    if (!model.isValid()) {
      throw new Error("This should be thrown from a checkValid function on model, and should provide the info needed to make the user aware of the fix.")
    }
    if (!model.isPersisted()) {
      this.add(model, cb)
    }
    else {
      let json = this._toJson(model)
      this.ref.child(model.key).set(json, (result)=>{
        cb(model)
      })
    }
  }

  remove(model:ActionModel, cb:Function = noop) {
    console.log("api.rule-engine.ActionService", "remove", model)
    this.ref.child(model.key).remove(()=> {
      this._removed.next(model)
      cb(model)
    })
  }
}


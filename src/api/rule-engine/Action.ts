import {Component, Input, Output, EventEmitter, Injectable} from 'angular2/angular2';
import * as Rx from 'rxjs/Rx.KitchenSink'

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

let noop = (...arg:any[])=> {
}

export class ActionModel extends CwModel {
  name:string
  owningRule:RuleModel
  actionType:ActionTypeModel
  parameters:{ [key: string]: ActionModelParameter }

  constructor(key:string, actionType:ActionTypeModel) {
    super(key)
    this.parameters = {}
    this.actionType = actionType;
    Object.keys(this.actionType.parameters).forEach((key)=> {
      if (this.getParameter(key) === undefined) {
        this.setParameter(key, "")
      }
    })
  }

  setParameter(key:string, value:any) {
    this.parameters[key] = {key: key, value: value}
  }

  getParameter(key:string):string {
    let v:any = null
    if (this.parameters[key]) {
      v = this.parameters[key].value
    }
    return v
  }

  clearParameters():void {
    this.parameters = {}
  }

  isValid() {
    let valid = !!this.owningRule
    valid = valid && this.owningRule.isValid()
    valid = valid && this.actionType && this.actionType.key && this.actionType.key != 'NoSelection'
    return valid
  }
}
@Injectable()
export class ActionService {

  private _removed:EventEmitter<ActionModel>
  private _added:EventEmitter<ActionModel>
  onRemove:Rx.Observable<ActionModel>
  onAdd:Rx.Observable<ActionModel>
  private apiRoot;
  private ref;
  private _typeService:ActionTypeService

  constructor(apiRoot:ApiRoot, typeService:ActionTypeService) {
    this._typeService = typeService
    this.ref = apiRoot.defaultSite.child('ruleengine/actions')
    this.apiRoot = apiRoot
    this._added = new EventEmitter()
    this._removed = new EventEmitter()
    let onAdd = Rx.Observable.from(this._added)
    let onRemove = Rx.Observable.from(this._removed)
    this.onAdd = onAdd.share()
    this.onRemove = onRemove.share()
  }

  _fromSnapshot(rule:RuleModel, snapshot:EntitySnapshot, cb:Function=noop) {
    let val:any = snapshot.val()
    this._typeService.get(val.actionlet, (type)=> {
      let ra = new ActionModel(snapshot.key(), type)
      ra.name = val.name;
      ra.owningRule = rule
      ra.actionType = new ActionTypeModel(val.actionlet)
      Object.keys(val.parameters).forEach((key)=> {
        let param = val.parameters[key]
        ra.setParameter(key, param.value)
      })
      ra.actionType = type
    })
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

  get(rule:RuleModel, key:string, cb:Function = noop) {
    this.ref.child(key).once('value', (actionSnap:EntitySnapshot)=> {
      this._fromSnapshot(rule, actionSnap, (model)=>{
        this._added.next(model)
        cb(model)
      })
    }, (e)=> {
      throw e
    })
  }

  add(model:ActionModel, cb:Function = noop) {
    console.log("api.rule-engine.ActionService", "add", model)

    let json = this._toJson(model)
    this.ref.push(json, (e, result) => {
      if (e) {
        cb(model, e)
      } else {
        model.key = result.key()
        this._added.next(model)
        cb(model)
      }
    })
  }

  save(model:ActionModel, cb:Function = noop) {
    console.log("api.rule-engine.ActionService", "save", model)
    if (!model.isValid()) {
      throw new Error("This should be thrown from a checkValid function on model, and should provide the info needed to make the user aware of the fix.")
    }
    if (!model.isPersisted()) {
      this.add(model, cb)
    }
    else {
      let json = this._toJson(model)
      this.ref.child(model.key).set(json, (e, result)=> {
        if (e) {
          throw e
        }
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


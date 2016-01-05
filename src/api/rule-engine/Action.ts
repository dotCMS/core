import {Component, Input, Output, EventEmitter, Injectable} from 'angular2/core';
import {Observable, ConnectableObservable} from 'rxjs/Rx'

import {ApiRoot} from "../persistence/ApiRoot";
import {CwModel} from "../util/CwModel";
import {RuleModel} from "./Rule";
import {EntitySnapshot} from "../persistence/EntityBase";
import {ActionTypeService} from "./ActionType";
import {ParameterDefinition} from "../util/CwInputModel";
import {ServerSideFieldModel} from "./ServerSideFieldModel";
import {ServerSideTypeModel} from "./ServerSideFieldModel";

let noop = (...arg:any[])=> {
}

export class ActionModel extends ServerSideFieldModel {
  comparison:string
  operator:string
  owningRule:RuleModel

  constructor(key:string, type:ServerSideTypeModel, rule:RuleModel, priority:number=1) {
    super(key, type, priority)
    this.owningRule = rule;
  }

  isValid():boolean {
    return !!this.owningRule && super.isValid()
  }

  toJson():any {
    let json = super.toJson()
    json.owningRule = this.owningRule;
    return json
  }

  static fromJson():ActionModel {
    return null
  }
}

@Injectable()
export class ActionService {

  private apiRoot;
  private ref;
  private _typeService:ActionTypeService

  constructor(apiRoot:ApiRoot, typeService:ActionTypeService) {
    this._typeService = typeService
    this.ref = apiRoot.defaultSite.child('ruleengine/actions')
    this.apiRoot = apiRoot
  }

  _fromSnapshot(rule:RuleModel, snapshot:EntitySnapshot, cb:Function=noop) {
    let val:any = snapshot.val()
    this._typeService.get(val.actionlet, (type)=> {
      let ra = new ActionModel(snapshot.key(), type, rule)
      ra.name = val.name;
      ra.owningRule = rule
      Object.keys(val.parameters).forEach((key)=> {
        let param = val.parameters[key]
        ra.setParameter(key, param.value)
      })
      cb(ra)
    })
  }

  _toJson(action:ActionModel):any {
    let json:any = {}
    json.name = action.name || "fake_name_" + new Date().getTime() + '_' + Math.random()
    json.owningRule = action.owningRule.key
    json.actionlet = action.type.key
    json.priority = action.priority
    json.parameters = action.parameters
    return json
  }

  list(rule:RuleModel):Observable<ActionModel[]> {
    let ee = new EventEmitter()

    if (rule.isPersisted()) {
      var keys = Object.keys(rule.actions);
      var count = 0
      var actions = []
      keys.forEach((actionId)=> {
        this.get(rule, actionId, action => {
          count++
          actions.push(action)
          if(count = keys.length){
            ee.emit(actions)
          }
        })
      })
      if(keys.length === 0){
        /* @todo ggranum remove stupid hack (ee returning after an emit means no fire on subscribe) */
        window.setTimeout(() => ee.emit([]), 500)
      }
    }
    return ee

  }

  get(rule:RuleModel, key:string, cb:Function = noop) {
    this.ref.child(key).once('value', (actionSnap:EntitySnapshot)=> {
      this._fromSnapshot(rule, actionSnap, (model)=>{
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
      cb(model)
    })
  }
}


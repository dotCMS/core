import {Inject, EventEmitter} from 'angular2/angular2';
//noinspection TypeScriptCheckImport
import * as Rx from 'rxjs/dist/cjs/Rx'


import {ApiRoot} from 'api/persistence/ApiRoot';
import {CwChangeEvent} from "api/util/CwEvent";
import {CwModel} from "api/util/CwModel";
import {EntityMeta, EntitySnapshot} from "api/persistence/EntityBase";
import {RuleModel} from "./Rule";
import {ActionTypeModel} from "./ActionType";
import {ActionTypesProvider} from "./ActionType";


interface ActionModelParameter {
  id: string
  key:string
  value:any
}

export class ActionModel extends CwModel {

  private _name:string
  private _owningRule:RuleModel
  private _actionType:ActionTypeModel
  parameters: { [key: string]: ActionModelParameter }

  constructor(key:string = null) {
    super(key)
    this._actionType = new ActionTypeModel('NoSelection','')
    this.parameters = {}
  }

  setParameter(key:string, value:any, id:string=null){
    let existing = this.parameters[key]
    if(id === null) {
      id = existing ? existing.id : ('random_' + Math.random())
    }
    this.parameters[key] = { id: id, key: key, value:value }
    this._changed('parameters')
  }

  getParameter(key:string):any {
    let v:any = ''
    if(this.parameters[key]){
      v = this.parameters[key].value
    }
    return v
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
    valid = valid && this._actionType && this._actionType.id && this._actionType.id != 'NoSelection'
    return valid
  }
}

export class ActionService {
  private _removed:EventEmitter
  private _added:EventEmitter
  onRemove:Rx.Observable
  onAdd:Rx.Observable
  private apiRoot;
  private ref;

  constructor(@Inject(ApiRoot) apiRoot) {
    this.ref = apiRoot.defaultSite.child('ruleengine/actions')
    this.apiRoot = apiRoot
    this._removed = new EventEmitter()
    this.onRemove = this._removed.toRx()
    this._added = new EventEmitter()
    this.onAdd = this._added.toRx()
  }

  _fromSnapshot(rule:RuleModel, snapshot:EntitySnapshot):ActionModel {
    let val:any = snapshot.val()
    let ra = new ActionModel(val.id)
    ra.name = val.name;
    ra.owningRule = rule
    ra.actionType = new ActionTypeModel(val.actionlet)
    if(val.parameters){
      Object.keys(val.parameters).forEach((paramId)=>{
        let param = val.parameters[paramId]
        ra.setParameter(param.key, param.value, paramId)
      })
    }
    return ra
  }

  _toJson(action:ActionModel):any {
    let json:any = {}
    json.name = action.name || "fake_name_" + new Date().getTime() + '_'+ Math.random()
    json.owningRule = action.owningRule.key
    json.actionlet = action.actionType.id
    json.priority = action.priority
    json.parameters = action.parameters
    return json
  }

  list(rule:RuleModel):Rx.Observable {
    if (rule.isPersisted()) {
      this.addActionsFromRule(rule)
    } else {
      rule.onChange.subscribe((event) => {
        if (event.key == 'key') {
          this.addActionsFromRule(event.target)
        }
      })
    }
    return this.onAdd
  }

  addActionsFromRule(rule:RuleModel) {
    Object.keys(rule.actions).forEach((actionId)=>{
      this.ref.child(actionId).once('value', (actionSnap:EntitySnapshot)=>{
        this._added.next(this._fromSnapshot(rule, actionSnap))
      })
    })
  }

  get(rule:RuleModel, key:string) {


  }

  add(action:ActionModel) {
    let json = this._toJson(action)
    this.ref.push(json, (result)=> {
      action.key = result.key()
      this._added.next(action)
    })
  }

  save(action:ActionModel) {
    if(!action.isValid()){
      throw new Error("This should be thrown from a checkValid function on action, and should provide the info needed to make the user aware of the fix.")
    }
    if(!action.isPersisted()){
      this.add(action)
    }
    else{
      let json = this._toJson(action)
      this.ref.child(action.key).set(json)
    }
  }

  remove(action:ActionModel, cb:Function = null) {
    this.ref.child(action.key).remove(()=> {
      this._removed.next(action)
      if(cb){
        cb(action)
      }
    })
  }
}


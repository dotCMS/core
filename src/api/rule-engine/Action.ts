import {Inject, EventEmitter} from 'angular2/angular2';


import {ApiRoot} from 'api/persistence/ApiRoot';
import {CwEvent} from "api/util/CwEvent";
import {CwModel} from "api/util/CwModel";
import {EntityMeta} from "api/persistence/EntityBase";
import {RuleModel, RuleService} from "./Rule";
import {ActionTypeModel} from "./ActionType";
import EventEmitter = ng.EventEmitter;


export class ActionModel extends CwModel {

  private _name:string
  private _owningRule:RuleModel
  private _actionType:ActionTypeModel


  constructor(key:string = null) {
    super(key)
  }

  get actionType():ActionTypeModel {
    return this._actionType;
  }

  set actionType(value:ActionTypeModel) {
    this._actionType = value;
  }
  get owningRule():RuleModel {
    return this._owningRule;
  }

  set owningRule(value:RuleModel) {
    this._owningRule = value;
  }
  get name():string {
    return this._name;
  }

  set name(value:string) {
    this._name = value;
  }

  isValid() {
    let valid = !!this._owningRule
    valid = valid && this._owningRule.isValid()
    return valid
  }


}

export class ActionService {
  ref:EntityMeta
  onAdd:EventEmitter
  private _ruleService:RuleService;

  constructor(@Inject(ApiRoot) apiRoot, @Inject(RuleService) ruleService:RuleService) {
    this._ruleService = ruleService;
    this.onAdd = new EventEmitter()
    this.ref = apiRoot.defaultSite.child('ruleengine/rules')
  }

  _fromRefVal(rule:RuleModel, val) {
    let ra = new ActionModel(val.id)
    ra.name = val.name;
    ra.owningRule = rule
    ra.actionType = val.actionlet
  }

  getAll(rule:RuleModel):EventEmitter{
    return new EventEmitter()
  }

  get(rule:RuleModel, key:string){

  }

  add() {

  }

  save(ruleAction:ActionModel) {

  }


}


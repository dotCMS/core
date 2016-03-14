import {Injectable} from 'angular2/core';
import {BehaviorSubject} from "rxjs/Rx";
import {Observable} from "rxjs/Observable";
import {RuleModel, ActionModel} from "../rule-engine/Rule";


export interface BusEvent {
  type:string,
  payload:any
}


export const RULE_ADD_RULE_ACTION = "RULE_ADD_RULE_ACTION"
export const RULE_PATCH_RULE_ACTION = "RULE_PATCH_RULE_ACTION"
export const RULE_PATCHED_RULE_ACTION = "RULE_PATCH_RULE_ACTION"


export const RULE_BECAME_INVALID = "RULE_BECAME_INVALID"
export const RULE_BECAME_VALID = "RULE_BECAME_VALID"


@Injectable()
export class GalacticBus {

  private _filters:{[key:string]:Observable<BusEvent>} = {}
  
  
  private _events$ = new BehaviorSubject<BusEvent>({type: null, payload: null});


  constructor(){
  }

  private _getFilter(action:string){
    let f = this._filters[action]
    if(!f){
      f = this._events$.filter(event => event.type === action)
      this._filters[action] = f
    }
    return f
  }

  ruleAddRuleAction(rule, action){
    this._events$.next({type: RULE_ADD_RULE_ACTION, payload: {rule:rule, action:action}})
  }

  ruleAddRuleAction$(){
    return this._getFilter(RULE_ADD_RULE_ACTION)
  }

  rulePatchRuleAction(rule:RuleModel, action:ActionModel) {
    this._events$.next({type: RULE_PATCH_RULE_ACTION, payload: {rule:rule, action:action}})
  }

  rulePatchRuleAction$() {
    return this._getFilter(RULE_PATCH_RULE_ACTION)
  }

  rulePatchedRuleAction(rule:RuleModel, action:ActionModel) {
    this._events$.next({type: RULE_PATCHED_RULE_ACTION, payload: {rule:rule, action:action}})
  }

  rulePatchedRuleAction$() {
    return this._getFilter(RULE_PATCHED_RULE_ACTION)
  }

  ruleBecameInvalid(rule:RuleModel, cause:any) {
    this._events$.next({type: RULE_BECAME_INVALID, payload: {rule:rule, cause:cause}})
  }

  ruleBecameInvalid$() {
    return this._getFilter(RULE_BECAME_INVALID)
  }

  ruleBecameValid(rule:RuleModel, cause:any) {
    this._events$.next({type: RULE_BECAME_VALID, payload: {rule:rule, cause:cause}})
  }

  ruleBecameValid$() {
    return this._getFilter(RULE_BECAME_VALID)
  }
  
  
}




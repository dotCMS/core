import {EventEmitter, Injectable} from 'angular2/core'
import {Http, Response, RequestMethod, Request} from 'angular2/http'
import {Observable} from 'rxjs/Rx'

import {ApiRoot} from "../persistence/ApiRoot";
import {
    hasContent, ResponseError, CwError, NETWORK_CONNECTION_ERROR, UNKNOWN_RESPONSE_ERROR,
    CLIENTS_ONLY_MESSAGES, SERVER_RESPONSE_ERROR
} from "../system/http-response-util";
import {ServerSideFieldModel, ServerSideTypeModel} from "./ServerSideFieldModel";


export const RULE_CREATE = 'RULE_CREATE'
export const RULE_DELETE = 'RULE_DELETE'
export const RULE_UPDATE_NAME = 'RULE_UPDATE_NAME'
export const RULE_UPDATE_ENABLED_STATE = 'RULE_UPDATE_ENABLED_STATE'

export const V_RULE_UPDATE_EXPANDED_STATE = 'V_RULE_UPDATE_EXPANDED_STATE'

export const RULE_UPDATE_FIRE_ON = 'RULE_UPDATE_FIRE_ON'

export const RULE_RULE_ACTION_CREATE = 'RULE_RULE_ACTION_CREATE'
export const RULE_RULE_ACTION_DELETE = 'RULE_RULE_ACTION_DELETE'
export const RULE_RULE_ACTION_UPDATE_TYPE = 'RULE_RULE_ACTION_UPDATE_TYPE'
export const RULE_RULE_ACTION_UPDATE_PARAMETER = 'RULE_RULE_ACTION_UPDATE_PARAMETER'

export const RULE_CONDITION_GROUP_UPDATE_OPERATOR = 'RULE_CONDITION_GROUP_UPDATE_OPERATOR'
export const RULE_CONDITION_GROUP_DELETE = 'RULE_CONDITION_GROUP_DELETE'
export const RULE_CONDITION_GROUP_CREATE = 'RULE_CONDITION_GROUP_CREATE'

export const RULE_CONDITION_CREATE = 'RULE_CONDITION_CREATE'
export const RULE_CONDITION_DELETE = 'RULE_CONDITION_DELETE'
export const RULE_CONDITION_UPDATE_TYPE = 'RULE_CONDITION_UPDATE_TYPE'
export const RULE_CONDITION_UPDATE_PARAMETER = 'RULE_CONDITION_UPDATE_PARAMETER'
export const RULE_CONDITION_UPDATE_OPERATOR = 'RULE_CONDITION_UPDATE_OPERATOR'

var idCounter = 1000
export function getNextId():string{
  return 'tempId' + ++idCounter
}

export interface IRecord {
  _id?: string,
  saving?: boolean
  saved?: boolean
  deleting?: boolean
  errors?:any
  set?(string, any):any
}

export interface IRuleAction extends IRecord {
  id?: string
  priority: number,
  type?:string
  parameters?: {[key:string]:any}
}

export interface ICondition extends IRecord {
  id?: string
  conditionlet?: string
  type?:string
  priority?: number,
  operator?:string
  parameters?: {[key:string]:any}
  _type?:ServerSideTypeModel
}

export interface IConditionGroup extends IRecord {
  id?: string
  priority: number,
  operator:string
  conditions?: any
}

export interface IRule extends IRecord {
  priority?:number
  name?: string
  fireOn?:string
  enabled?: boolean
  conditionGroups?: any
  ruleActions?: any
  set?(string, any):IRule
  id?: string
  saving?: boolean
  saved?: boolean
  deleting?: boolean
  _id?: string
  _expanded?:boolean
  _ruleActions?:ActionModel[]
  _conditionGroups?:ConditionGroupModel[]
  _ruleActionsLoaded?:boolean
  _errors?: CwError[]
}


export interface ParameterModel {
  key:string
  value:string
  priority:number
}


export class ActionModel extends ServerSideFieldModel {

  constructor(key:string, type:ServerSideTypeModel, rule:RuleModel, priority:number=1) {
    super(key, type, priority)
    this.priority = priority || 1
    this.type = type
  }

  isValid():boolean {
    try {
      return super.isValid()
    } catch (e) {
      console.error(e)
    }
  }
}

export class ConditionModel extends ServerSideFieldModel {
  operator:string = 'AND'
  conditionlet:string

  constructor(iCondition: ICondition ) {
    super(iCondition.id, iCondition._type)
    this.conditionlet = iCondition.conditionlet
    this.key = iCondition.id
    this.priority = iCondition.priority || 1
    this.type = iCondition._type
  }

  isValid() {
    try {
      return !!this.getParameterValue('comparison') && super.isValid()
    } catch (e) {
      console.error(e)
    }
  }
}

export class ConditionGroupModel {

  key:string
  priority:number
  
  operator:string
  conditions:{ [key: string]: boolean }
  _id:string
  _conditions:ConditionModel[] = []

  constructor(iGroup:IConditionGroup){ 
    Object.assign(this, iGroup)
    this.key = iGroup.id
    this._id = this.key != null ? this.key : getNextId()
    this.conditions = iGroup.conditions || {}
  }

  isPersisted() {
    return this.key != null
  }
  isValid() {
    let valid = this.operator && (this.operator === 'AND' || this.operator === 'OR')
    return valid
  }
}

export class RuleModel {
  key:string
  name:string
  enabled:boolean = false
  priority:number
  fireOn:string
  conditionGroups:{ [key: string]: ConditionGroupModel } = {}
  ruleActions:{ [key: string]: boolean } = {}

  _id:string
  _expanded:boolean = false
  _conditionGroups: ConditionGroupModel[] = []
  _ruleActions: ActionModel[] = []

  constructor(iRule:IRule) {
    Object.assign(this, iRule)
    this.key = iRule.id
    this._id = this.key != null ? this.key : getNextId()
    let conGroups = Object.keys(iRule.conditionGroups)
    conGroups.forEach(( groupId ) => {
      let g = this.conditionGroups[groupId]
      let mg = new ConditionGroupModel(Object.assign({id: groupId}, g))
      this.conditionGroups[groupId] = mg
      this._conditionGroups.push(mg)
    })
  }

  isPersisted() {
    return this.key != null
  }

  isValid() {
    let valid = !!this.name
    valid = valid && this.name.trim().length > 0
    return valid
  }
}

export const DEFAULT_RULE:IRule = {
  priority: 1,
  name: null,
  fireOn: "EVERY_PAGE",
  enabled: false,
  conditionGroups: {},
  ruleActions: {},
  _id: -1 + '',
  _expanded:false,
  _ruleActions: [],
  _conditionGroups: []

}

@Injectable()
export class RuleService {
  private _rulesEndpointUrl:string
  private _actionsEndpointUrl:string

  constructor(private _apiRoot:ApiRoot, private _http:Http) {
    this._rulesEndpointUrl = `${this._apiRoot.defaultSiteUrl}/ruleengine/rules`
    this._actionsEndpointUrl = `${this._apiRoot.defaultSiteUrl}/ruleengine/actions`

  }

  createRule(body:RuleModel):Observable<RuleModel|CwError> {
    return this.request({
      body:RuleService.fromClientRuleTransformFn(body),
      method: RequestMethod.Post,
      url: this._rulesEndpointUrl
    }).map((result:RuleModel)=> {
      body.key = result.key 
      return Object.assign({}, DEFAULT_RULE, body, result)
    });
  }

  deleteRule(ruleId:string):Observable<{success:boolean}|CwError> {
    return this.request({
      method: RequestMethod.Delete,
      url: `${this._rulesEndpointUrl}/${ruleId}`
    }).map((result)=> {
      return {success: true}
    });
  }

  loadRules():Observable<RuleModel[]|CwError> {
    return this.request({
      method: RequestMethod.Get,
      url: this._rulesEndpointUrl
    }).map(RuleService.fromServerRulesTransformFn);
  }


  loadRule(id:string):Observable<RuleModel|CwError> {
    return this.request({
      method: RequestMethod.Get,
      url: `${this._rulesEndpointUrl}/${id}`
    }).map((result)=> {
      let r = Object.assign({}, DEFAULT_RULE, result)
      r.id = id
      return r
    })
  }

  updateRule(id:string, rule:RuleModel):Observable<RuleModel|CwError> {
    let result
    if(!id){
      result = this.createRule(rule)
    }
    else {
      result = this.request({
        body: RuleService.fromClientRuleTransformFn(rule),
        method: RequestMethod.Put,
        url: `${this._rulesEndpointUrl}/${id}`
      }).map((result)=> {
        let r = Object.assign({}, DEFAULT_RULE, result)
        r.id = id
        return r
      });
    }
    return result
  }

  request(options:any):Observable<RuleModel[]|CwError|RuleModel|IConditionGroup> {
    options.headers = this._apiRoot.getDefaultRequestHeaders();
    var source = options.body
    if (options.body) {
      if (typeof options.body !== 'string') {
        options.body = JSON.stringify(options.body);
      }
      options.headers.append('Content-Type', 'application/json')
    }
    var request = new Request(options)
    return this._http.request(request)
        .map((resp:Response) => {
          return hasContent(resp) ? resp.json() : resp
        })
        .catch((response:Response, original:Observable<any>):Observable<any> => {
          if (response) {
            if (response.status === 500) {
              if(response.text() && response.text().indexOf("ECONNREFUSED") >= 0){
                throw new CwError(NETWORK_CONNECTION_ERROR, CLIENTS_ONLY_MESSAGES[NETWORK_CONNECTION_ERROR], request,  response, source)
              } else{
                throw new CwError(SERVER_RESPONSE_ERROR, response.headers.get('error-message'), request, response, source)
              }
            }
            else if (response.status === 404) {
              console.error("Could not execute request: 404 path not valid.", options.url)
              throw new CwError(UNKNOWN_RESPONSE_ERROR, response.headers.get('error-message'),request,  response, source)
            } else {
              console.log("Could not execute request: Response status code: ", response.status, 'error:', response, options.url)
              throw new CwError(UNKNOWN_RESPONSE_ERROR, response.headers.get('error-message'), request, response, source)
            }
          }
          return null
        })
  }

  static fromServerRulesTransformFn(ruleMap):RuleModel[]{
    return Object.keys(ruleMap).map((id:string) => {
      let r:IRule = ruleMap[id]
      r.id = id
      return new RuleModel(r)
    })
  }

  static fromClientRuleTransformFn(rule:RuleModel):any{
    let r = Object.assign({}, DEFAULT_RULE, rule)
    r.key = rule.key
    delete r.id
    return r;
  }

}


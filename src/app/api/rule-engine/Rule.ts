import {Injectable} from '@angular/core'
import {Http, RequestMethod} from '@angular/http'
import {Observable, BehaviorSubject} from 'rxjs/Rx'

import {ApiRoot} from "../persistence/ApiRoot";
import {ServerSideFieldModel, ServerSideTypeModel} from "./ServerSideFieldModel";
import {I18nService} from "../system/locale/I18n";
import {CoreWebService} from "../services/core-web-service";
import {SiteService} from "../services/site-service";
import {Subject} from 'rxjs/Subject';
import {Site} from "../services/site-service";
import {CwError} from '../system/http-response-util';

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
export function getNextId():string {
  return 'tempId' + ++idCounter
}

export class RuleEngineState {
  showRules:boolean = true;
  globalError:string = null
  loading:boolean = true
  saving:boolean = false
  hasError:boolean = false
  filter:string = ''
  deleting:boolean = false
}


export interface IRecord {
  _id?:string,
  _saving?:boolean
  _saved?:boolean
  deleting?:boolean
  errors?:any
  set?(string, any):any
}

export interface IRuleAction extends IRecord {
  id?:string
  priority:number,
  type?:string
  parameters?:{[key:string]:any},
  owningRule?:string,
  _owningRule?:RuleModel
}

export interface ICondition extends IRecord {
  id?:string
  conditionlet?:string
  type?:string
  priority?:number,
  operator?:string
  parameters?:{[key:string]:any}
  _type?:ServerSideTypeModel
}

export interface IConditionGroup extends IRecord {
  id?:string
  priority:number,
  operator:string
  conditions?:any
}

export interface IRule extends IRecord {
  priority?:number
  name?:string
  fireOn?:string
  enabled?:boolean
  conditionGroups?:any
  ruleActions?:any
  set?(string, any):IRule
  id?:string
  _saving?:boolean
  _saved?:boolean
  deleting?:boolean
  _id?:string
  _expanded?:boolean
  _ruleActions?:ActionModel[]
  _conditionGroups?:ConditionGroupModel[]
  _ruleActionsLoaded?:boolean
  _errors?:CwError[]
}


export interface ParameterModel {
  key:string
  value:string
  priority:number
}


export class ActionModel extends ServerSideFieldModel {
  owningRule:string
  _owningRule:RuleModel

  constructor(key:string, type:ServerSideTypeModel, priority:number = 1) {
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

  constructor(iCondition:ICondition) {
    super(iCondition.id, iCondition._type)
    this.conditionlet = iCondition.conditionlet
    this.key = iCondition.id
    this.priority = iCondition.priority || 1
    this.type = iCondition._type
    this.operator = iCondition.operator || 'AND'
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
  conditions:{ [key:string]:boolean }
  _id:string
  _conditions:ConditionModel[] = []

  constructor(iGroup:IConditionGroup) {
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
  conditionGroups:{ [key:string]:ConditionGroupModel } = {}
  ruleActions:{ [key:string]:boolean } = {}

  _id:string
  _expanded:boolean = false
  _conditionGroups:ConditionGroupModel[] = []
  _ruleActions:ActionModel[] = []
  _saved:boolean = true
  _saving:boolean = false
  _deleting:boolean = true
  _errors:{[key:string]:any}


  constructor(iRule:IRule) {
    Object.assign(this, iRule)
    this.key = iRule.id
    this._id = this.key != null ? this.key : getNextId()
    let conGroups = Object.keys(iRule.conditionGroups || {})
    conGroups.forEach((groupId) => {
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
  _expanded: false,
  _ruleActions: [],
  _conditionGroups: []

}

@Injectable()
export class RuleService {
  private _rulesEndpointUrl:string
  private _actionsEndpointUrl:string
  private _conditionTypesEndpointUrl:string
  private _ruleActionTypesEndpointUrl:string

  private _rules$: Subject<RuleModel[]> = new Subject();

  ruleActionTypes$:BehaviorSubject<ServerSideTypeModel[]> = new BehaviorSubject([]);
  conditionTypes$:BehaviorSubject<ServerSideTypeModel[]> = new BehaviorSubject([]);

  private _ruleActions:{[key:string]:ActionModel} = {}
  private _conditions:{[key:string]:ConditionModel} = {}

  _ruleActionTypes:{[key:string]:ServerSideTypeModel} = {}
  private _ruleActionTypesAry:ServerSideTypeModel[] = []

  _conditionTypes:{[key:string]:ServerSideTypeModel} = {}
  private _conditionTypesAry:ServerSideTypeModel[] = []

  private _rules: RuleModel[];

  constructor(public _apiRoot:ApiRoot, private _resources:I18nService, private siteService:SiteService,
              private coreWebService: CoreWebService) {

    this._rulesEndpointUrl = `${this._apiRoot.defaultSiteUrl}/ruleengine/rules`
    this._actionsEndpointUrl = `${this._apiRoot.defaultSiteUrl}/ruleengine/actions`
    this._conditionTypesEndpointUrl = `${this._apiRoot.baseUrl}api/v1/system/ruleengine/conditionlets`
    this._ruleActionTypesEndpointUrl = `${this._apiRoot.baseUrl}api/v1/system/ruleengine/actionlets`


    this._preCacheCommonResources(_resources)
    this.loadActionTypes().subscribe((types:ServerSideTypeModel[])=> this.ruleActionTypes$.next(types))
    this.loadConditionTypes().subscribe((types:ServerSideTypeModel[])=> this.conditionTypes$.next(types))
  }

  private _preCacheCommonResources(resources:I18nService) {
    resources.get('api.sites.ruleengine').subscribe((rsrc)=> {})
    resources.get('api.ruleengine.system').subscribe((rsrc)=> {})
    resources.get('api.system.ruleengine').subscribe((rsrc)=> {})
  }

  get rules(): RuleModel[]{
    return this._rules;
  }

  createRule(body:RuleModel):Observable<RuleModel|CwError> {
    return this.coreWebService.request({
      body: RuleService.fromClientRuleTransformFn(body),
      method: RequestMethod.Post,
      url: this._rulesEndpointUrl
    }).map((result:RuleModel)=> {
      body.key = result['id'] // @todo:ggranum type the POST result correctly.
      return Object.assign({}, DEFAULT_RULE, body, result)
    });
  }

  deleteRule(ruleId:string):Observable<{success:boolean}|CwError> {
    return this.coreWebService.request({
      method: RequestMethod.Delete,
      url: `${this._rulesEndpointUrl}/${ruleId}`
    }).map((result)=> {
      return {success: true}
    });
  }

  loadRules( ): Observable<RuleModel[]> {
    return this._rules$.asObservable();
  }

  public requestRules(siteId: string): Observable<any> {
    if(siteId) {
      return this.sendLoadRulesRequest(siteId);
    } else if (this.siteService.currentSite) {
      return this.sendLoadRulesRequest(this.siteService.currentSite.identifier);
    }
  }

  private sendLoadRulesRequest(siteId: string): Observable<any> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: `${this._apiRoot.baseUrl}api/v1/sites/${siteId}/ruleengine/rules`
    }).map(ruleMap => {
      this._rules = RuleService.fromServerRulesTransformFn(ruleMap);
      this._rules$.next(this.rules);

      this.siteService.switchSite$.subscribe(site => {
        this.sendLoadRulesRequest(site.identifier);
      });

      return RuleService.fromServerRulesTransformFn(ruleMap);
    });
  }

  loadRule(id:string):Observable<RuleModel|CwError> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: `${this._rulesEndpointUrl}/${id}`
    }).map((result)=> {
      return Object.assign({key: id}, DEFAULT_RULE, result)
    })
  }

  updateRule(id:string, rule:RuleModel):Observable<RuleModel|CwError> {
    let result
    if (!id) {
      result = this.createRule(rule)
    }
    else {
      result = this.coreWebService.request({
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

  getConditionTypes():Observable<ServerSideTypeModel[]> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: this._conditionTypesEndpointUrl,
    }).map(RuleService.fromServerServersideTypesTransformFn)
  }

  getRuleActionTypes():Observable<ServerSideTypeModel[]> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: this._ruleActionTypesEndpointUrl,
    }).map(RuleService.fromServerServersideTypesTransformFn)
  }


  private loadActionTypes():Observable<ServerSideTypeModel[]> {
    let obs
    if (this._ruleActionTypesAry.length) {
      obs = Observable.from(this._ruleActionTypesAry)
    } else {
      return this.actionAndConditionTypeLoader(this._doLoadRuleActionTypes(), this._ruleActionTypes)
    }
    return obs
  }

  _doLoadRuleActionTypes():Observable<ServerSideTypeModel[]> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: this._ruleActionTypesEndpointUrl,
    }).map(RuleService.fromServerServersideTypesTransformFn)
  }

  private actionAndConditionTypeLoader(
      requestObserver:Observable<ServerSideTypeModel[]>,
      typeMap:any):Observable<ServerSideTypeModel[]> {
      return requestObserver.flatMap((types:ServerSideTypeModel[])=> {
        return Observable.from(types).flatMap((type)=> {
          return this._resources.get(type.i18nKey + '.name', type.i18nKey).map(( label:string ) => {
            type._opt = {value: type.key, label: label}
            return type
          })
        }).reduce( (types:any[], type:any)=> {
          types.push(type)
          return types
        }, []).do((types:any[])=>{
          types = types.sort(( typeA, typeB ) => {
            return typeA._opt.label.localeCompare(typeB._opt.label)
          })
          types.forEach((type)=>{
            typeMap[type.key] = type
          })
          return types
        })
      })
  }

  private loadConditionTypes():Observable<ServerSideTypeModel[]> {
    let obs
    if (this._conditionTypesAry.length) {
      obs = Observable.from(this._conditionTypesAry)
    } else {
      return this.actionAndConditionTypeLoader(this._doLoadConditionTypes(), this._conditionTypes)
    }
    return obs
  }

  _doLoadConditionTypes():Observable<ServerSideTypeModel[]> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: this._conditionTypesEndpointUrl,
    }).map(RuleService.fromServerServersideTypesTransformFn)
  }
  
  static fromServerRulesTransformFn(ruleMap):RuleModel[] {
    return Object.keys(ruleMap).map((id:string) => {
      let r:IRule = ruleMap[id]
      r.id = id
      return new RuleModel(r)
    })
  }

  static fromClientRuleTransformFn(rule:RuleModel):any {
    let sendRule = Object.assign({}, DEFAULT_RULE, rule)
    sendRule.key = rule.key
    delete sendRule.id
    sendRule.conditionGroups = {}
    sendRule._conditionGroups.forEach((conditionGroup:ConditionGroupModel)=> {
      if (conditionGroup.key) {
        let sendGroup = {
          operator: conditionGroup.operator,
          priority: conditionGroup.priority,
          conditions: {}
        }
        conditionGroup._conditions.forEach((condition:ConditionModel)=> {
          sendGroup.conditions[condition.key] = true
        })
        sendRule.conditionGroups[conditionGroup.key] = sendGroup
      }
    })
    RuleService.removeMeta(sendRule)
    return sendRule;
  }

  static removeMeta(entity:any) {
    Object.keys(entity).forEach((key)=> {
      if (key[0] == '_') {
        delete entity[key]
      }
    })
  }

  static alphaSort(key) {
    return (a, b) => {
      let x
      if (a[key] > b[key]) {
        x = 1
      } else if (a[key] < b[key]) {
        x = -1
      }
      else {
        x = 0
      }
      return x
    }
  }


  static fromServerServersideTypesTransformFn(typesMap):ServerSideTypeModel[] {
    let types = Object.keys(typesMap).map((key:string) => {
      let json:any = typesMap[key]
      json.key = key
      return ServerSideTypeModel.fromJson(json)
    })
    //console.log("RuleService", "fromServerServersideTypesTransformFn - loaded", types)
    return types.filter((type)=> type.key != 'CountRulesActionlet')
  }
}


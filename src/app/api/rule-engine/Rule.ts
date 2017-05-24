// tslint:disable-next-line:max-file-line-count
import {Injectable} from '@angular/core';
import {RequestMethod} from '@angular/http';
import {Observable, BehaviorSubject} from 'rxjs/Rx';

import {ApiRoot} from '../persistence/ApiRoot';
import {ServerSideFieldModel, ServerSideTypeModel} from './ServerSideFieldModel';
import {I18nService} from '../system/locale/I18n';
import {CoreWebService} from '../services/core-web-service';
import {SiteService} from '../services/site-service';
import {Subject} from 'rxjs/Subject';
import {CwError} from '../system/http-response-util';

export const RULE_CREATE = 'RULE_CREATE';
export const RULE_DELETE = 'RULE_DELETE';
export const RULE_UPDATE_NAME = 'RULE_UPDATE_NAME';
export const RULE_UPDATE_ENABLED_STATE = 'RULE_UPDATE_ENABLED_STATE';

export const V_RULE_UPDATE_EXPANDED_STATE = 'V_RULE_UPDATE_EXPANDED_STATE';

export const RULE_UPDATE_FIRE_ON = 'RULE_UPDATE_FIRE_ON';

export const RULE_RULE_ACTION_CREATE = 'RULE_RULE_ACTION_CREATE';
export const RULE_RULE_ACTION_DELETE = 'RULE_RULE_ACTION_DELETE';
export const RULE_RULE_ACTION_UPDATE_TYPE = 'RULE_RULE_ACTION_UPDATE_TYPE';
export const RULE_RULE_ACTION_UPDATE_PARAMETER = 'RULE_RULE_ACTION_UPDATE_PARAMETER';

export const RULE_CONDITION_GROUP_UPDATE_OPERATOR = 'RULE_CONDITION_GROUP_UPDATE_OPERATOR';
export const RULE_CONDITION_GROUP_DELETE = 'RULE_CONDITION_GROUP_DELETE';
export const RULE_CONDITION_GROUP_CREATE = 'RULE_CONDITION_GROUP_CREATE';

export const RULE_CONDITION_CREATE = 'RULE_CONDITION_CREATE';
export const RULE_CONDITION_DELETE = 'RULE_CONDITION_DELETE';
export const RULE_CONDITION_UPDATE_TYPE = 'RULE_CONDITION_UPDATE_TYPE';
export const RULE_CONDITION_UPDATE_PARAMETER = 'RULE_CONDITION_UPDATE_PARAMETER';
export const RULE_CONDITION_UPDATE_OPERATOR = 'RULE_CONDITION_UPDATE_OPERATOR';

let idCounter = 1000;
// tslint:disable-next-line:only-arrow-functions
export function getNextId(): string {
  return 'tempId' + ++idCounter;
}

export class RuleEngineState {
  showRules = true;
  globalError: string = null;
  loading = true;
  saving = false;
  hasError = false;
  filter = '';
  deleting = false;
}

export interface IRecord {
  _id?: string;
  _saving?: boolean;
  _saved?: boolean;
  deleting?: boolean;
  errors?: any;
  set?(string, any): any;
}

export interface IRuleAction extends IRecord {
  id?: string;
  priority: number;
  type?: string;
  parameters?: {[key: string]: any};
  owningRule?: string;
  _owningRule?: RuleModel;
}

export interface ICondition extends IRecord {
  id?: string;
  conditionlet?: string;
  type?: string;
  priority?: number;
  operator?: string;
  parameters?: {[key: string]: any};
  _type?: ServerSideTypeModel;
}

export interface IConditionGroup extends IRecord {
  id?: string;
  priority: number;
  operator: string;
  conditions?: any;
}

export interface IRule extends IRecord {
    _id?: string;
  _expanded?: boolean;
  _ruleActions?: ActionModel[];
  _conditionGroups?: ConditionGroupModel[];
  _ruleActionsLoaded?: boolean;
  _errors?: CwError[];
  _saving?: boolean;
  _saved?: boolean;
  deleting?: boolean;
  id?: string;
  priority?: number;
  name?: string;
  fireOn?: string;
  enabled?: boolean;
  conditionGroups?: any;
  ruleActions?: any;
  set?(string, any): IRule;
}

export interface ParameterModel {
  key: string;
  value: string;
  priority: number;
}

export class ActionModel extends ServerSideFieldModel {
  owningRule: string;
  _owningRule: RuleModel;

  constructor(key: string, type: ServerSideTypeModel, priority = 1) {
    super(key, type, priority);
    this.priority = priority || 1;
    this.type = type;
  }

  isValid(): boolean {
    try {
      return super.isValid();
    } catch (e) {
      console.error(e);
    }
  }
}

export class ConditionModel extends ServerSideFieldModel {
  operator = 'AND';
  conditionlet: string;

  constructor(iCondition: ICondition) {
    super(iCondition.id, iCondition._type);
    this.conditionlet = iCondition.conditionlet;
    this.key = iCondition.id;
    this.priority = iCondition.priority || 1;
    this.type = iCondition._type;
    this.operator = iCondition.operator || 'AND';
  }

  isValid(): boolean {
    try {
      return !!this.getParameterValue('comparison') && super.isValid();
    } catch (e) {
      console.error(e);
    }
  }
}

export class ConditionGroupModel {

  key: string;
  priority: number;

  operator: string;
  conditions: { [key: string]: boolean };
  _id: string;
  _conditions: ConditionModel[] = [];

  constructor(iGroup: IConditionGroup) {
    Object.assign(this, iGroup);
    this.key = iGroup.id;
    this._id = this.key != null ? this.key : getNextId();
    this.conditions = iGroup.conditions || {};
  }

  isPersisted(): boolean {
    return this.key != null;
  }

  isValid(): boolean {
    let valid = this.operator && (this.operator === 'AND' || this.operator === 'OR');
    return valid;
  }
}

export class RuleModel {
  key: string;
  name: string;
  enabled = false;
  priority: number;
  fireOn: string;
  conditionGroups: { [key: string]: ConditionGroupModel } = {};
  ruleActions: { [key: string]: boolean } = {};

  _id: string;
  _expanded = false;
  _conditionGroups: ConditionGroupModel[] = [];
  _ruleActions: ActionModel[] = [];
  _saved = true;
  _saving = false;
  _deleting = true;
  _errors: {[key: string]: any};

  constructor(iRule: IRule) {
    Object.assign(this, iRule);
    this.key = iRule.id;
    this._id = this.key != null ? this.key : getNextId();
    let conGroups = Object.keys(iRule.conditionGroups || {});
    conGroups.forEach((groupId) => {
      let g = this.conditionGroups[groupId];
      let mg = new ConditionGroupModel(Object.assign({id: groupId}, g));
      this.conditionGroups[groupId] = mg;
      this._conditionGroups.push(mg);
    });
  }

  isPersisted(): boolean {
    return this.key != null;
  }

  isValid(): boolean {
    let valid = !!this.name;
    valid = valid && this.name.trim().length > 0;
    return valid;
  }
}

export const DEFAULT_RULE: IRule = {
  _conditionGroups: [],
  _expanded: false,
  _id: -1 + '',
  _ruleActions: [],
  conditionGroups: {},
  enabled: false,
  fireOn: 'EVERY_PAGE',
  name: null,
  priority: 1,
  ruleActions: {},
};

@Injectable()
export class RuleService {
  ruleActionTypes$: BehaviorSubject<ServerSideTypeModel[]> = new BehaviorSubject([]);
  conditionTypes$: BehaviorSubject<ServerSideTypeModel[]> = new BehaviorSubject([]);

  _ruleActionTypes: {[key: string]: ServerSideTypeModel} = {};
  _conditionTypes: {[key: string]: ServerSideTypeModel} = {};

  private _rulesEndpointUrl: string;
  private _actionsEndpointUrl: string;
  private _conditionTypesEndpointUrl: string;
  private _ruleActionTypesEndpointUrl: string;

  private _rules$: Subject<RuleModel[]> = new Subject();

  // tslint:disable-next-line:no-unused-variable
  private _ruleActions: {[key: string]: ActionModel} = {};
  // tslint:disable-next-line:no-unused-variable
  private _conditions: {[key: string]: ConditionModel} = {};

  private _ruleActionTypesAry: ServerSideTypeModel[] = [];
  private _conditionTypesAry: ServerSideTypeModel[] = [];

  private _rules: RuleModel[];

 static fromServerRulesTransformFn(ruleMap): RuleModel[] {
    return Object.keys(ruleMap).map((id: string) => {
      let r: IRule = ruleMap[id];
      r.id = id;
      return new RuleModel(r);
    });
  }

  static fromClientRuleTransformFn(rule: RuleModel): any {
    let sendRule = Object.assign({}, DEFAULT_RULE, rule);
    sendRule.key = rule.key;
    delete sendRule.id;
    sendRule.conditionGroups = {};
    sendRule._conditionGroups.forEach((conditionGroup: ConditionGroupModel) => {
      if (conditionGroup.key) {
        let sendGroup = {
          conditions: {},
          operator: conditionGroup.operator,
          priority: conditionGroup.priority,
        };
        conditionGroup._conditions.forEach((condition: ConditionModel) => {
          sendGroup.conditions[condition.key] = true;
        });
        sendRule.conditionGroups[conditionGroup.key] = sendGroup;
      }
    });
    RuleService.removeMeta(sendRule);
    return sendRule;
  }

  static removeMeta(entity: any): void {
    Object.keys(entity).forEach((key) => {
      if (key[0] === '_') {
        delete entity[key];
      }
    });
  }

  static alphaSort(key): (a, b) => number {
    return (a, b) => {
      let x;
      if (a[key] > b[key]) {
        x = 1;
      } else if (a[key] < b[key]) {
        x = -1;
      }else {
        x = 0;
      }
      return x;
    };
  }

  static fromServerServersideTypesTransformFn(typesMap): ServerSideTypeModel[] {
    let types = Object.keys(typesMap).map((key: string) => {
      let json: any = typesMap[key];
      json.key = key;
      return ServerSideTypeModel.fromJson(json);
    });
    return types.filter((type) => type.key !== 'CountRulesActionlet');
  }

  constructor(public _apiRoot: ApiRoot, private _resources: I18nService, private siteService: SiteService,
              private coreWebService: CoreWebService) {

    this._rulesEndpointUrl = `/ruleengine/rules`;
    this._actionsEndpointUrl = `/ruleengine/actions`;
    this._conditionTypesEndpointUrl = `${this._apiRoot.baseUrl}api/v1/system/ruleengine/conditionlets`;
    this._ruleActionTypesEndpointUrl = `${this._apiRoot.baseUrl}api/v1/system/ruleengine/actionlets`;

    this._preCacheCommonResources(_resources);
    this.loadActionTypes().subscribe((types: ServerSideTypeModel[]) => this.ruleActionTypes$.next(types));
    this.loadConditionTypes().subscribe((types: ServerSideTypeModel[]) => this.conditionTypes$.next(types));

    this.siteService.switchSite$.subscribe(site => {
      let siteId = this.loadRulesSiteId();
      if (siteId === site.identifier) {
        this.sendLoadRulesRequest(site.identifier);
      }
    });
  }

 createRule(body: RuleModel): Observable<RuleModel|CwError> {
    let siteId = this.loadRulesSiteId();
    return this.coreWebService.request({
      body: RuleService.fromClientRuleTransformFn(body),
      method: RequestMethod.Post,
      url: `${this._apiRoot.baseUrl}api/v1/sites/${siteId}${this._rulesEndpointUrl}`
    }).map((result: RuleModel) => {
      body.key = result['id']; // @todo:ggranum type the POST result correctly.
      return Object.assign({}, DEFAULT_RULE, body, result);
    });
  }

  deleteRule(ruleId: string): Observable<{success: boolean}|CwError> {
    let siteId = this.loadRulesSiteId();
    return this.coreWebService.request({
      method: RequestMethod.Delete,
      url: `${this._apiRoot.baseUrl}api/v1/sites/${siteId}${this._rulesEndpointUrl}/${ruleId}`
    }).map((result) => {
      return {success: true};
    });
  }

  loadRules( ): Observable<RuleModel[]> {
    return this._rules$.asObservable();
  }

  public requestRules(siteId: string): void {
    if (siteId) {
      this.sendLoadRulesRequest(siteId);
    } else if (this.siteService.currentSite) {
      this.sendLoadRulesRequest(this.siteService.currentSite.identifier);
    }
  }

  get rules(): RuleModel[]{
    return this._rules;
  }

  loadRule(id: string): Observable<RuleModel|CwError> {
    let siteId = this.loadRulesSiteId();
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: `${this._apiRoot.baseUrl}api/v1/sites/${siteId}${this._rulesEndpointUrl}/${id}`
    }).map((result) => {
      return Object.assign({key: id}, DEFAULT_RULE, result);
    });
  }

  updateRule(id: string, rule: RuleModel): Observable<RuleModel|CwError> {
    let result;
    let siteId = this.loadRulesSiteId();
    if (!id) {
      result = this.createRule(rule);
    }else {
      result = this.coreWebService.request({
        body: RuleService.fromClientRuleTransformFn(rule),
        method: RequestMethod.Put,
        url: `${this._apiRoot.baseUrl}api/v1/sites/${siteId}${this._rulesEndpointUrl}/${id}`
      }).map((result) => {
        let r = Object.assign({}, DEFAULT_RULE, result);
        r.id = id;
        return r;
      });
    }
    return result;
  }

  getConditionTypes(): Observable<ServerSideTypeModel[]> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: this._conditionTypesEndpointUrl,
    }).map(RuleService.fromServerServersideTypesTransformFn);
  }

  getRuleActionTypes(): Observable<ServerSideTypeModel[]> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: this._ruleActionTypesEndpointUrl,
    }).map(RuleService.fromServerServersideTypesTransformFn);
  }

  _doLoadRuleActionTypes(): Observable<ServerSideTypeModel[]> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: this._ruleActionTypesEndpointUrl,
    }).map(RuleService.fromServerServersideTypesTransformFn);
  }

  _doLoadConditionTypes(): Observable<ServerSideTypeModel[]> {
    return this.coreWebService.request({
      method: RequestMethod.Get,
      url: this._conditionTypesEndpointUrl,
    }).map(RuleService.fromServerServersideTypesTransformFn);
  }

  private _preCacheCommonResources(resources: I18nService): void {
    resources.get('api.sites.ruleengine').subscribe((rsrc) => {});
    resources.get('api.ruleengine.system').subscribe((rsrc) => {});
    resources.get('api.system.ruleengine').subscribe((rsrc) => {});
  }

  private sendLoadRulesRequest(siteId: string): void {
    this.coreWebService.request({
      method: RequestMethod.Get,
      url: `${this._apiRoot.baseUrl}api/v1/sites/${siteId}/ruleengine/rules`
    }).subscribe(ruleMap => {
      this._rules = RuleService.fromServerRulesTransformFn(ruleMap);
      this._rules$.next(this.rules);

      return RuleService.fromServerRulesTransformFn(ruleMap);
    });
  }

  private loadActionTypes(): Observable<ServerSideTypeModel[]> {
    let obs;
    if (this._ruleActionTypesAry.length) {
      obs = Observable.from(this._ruleActionTypesAry);
    } else {
      return this.actionAndConditionTypeLoader(this._doLoadRuleActionTypes(), this._ruleActionTypes);
    }
    return obs;
  }

  private actionAndConditionTypeLoader(
      requestObserver: Observable<ServerSideTypeModel[]>,
      typeMap: any): Observable<ServerSideTypeModel[]> {
      return requestObserver.flatMap((types: ServerSideTypeModel[]) => {
        return Observable.from(types).flatMap((type) => {
          return this._resources.get(type.i18nKey + '.name', type.i18nKey).map(( label: string ) => {
            type._opt = {value: type.key, label: label};
            return type;
          });
        }).reduce( (types: any[], type: any) => {
          types.push(type);
          return types;
        }, []).do((types: any[]) => {
          types = types.sort(( typeA, typeB ) => {
            return typeA._opt.label.localeCompare(typeB._opt.label);
          });
          types.forEach((type) => {
            typeMap[type.key] = type;
          });
          return types;
        });
      });
  }

  private loadConditionTypes(): Observable<ServerSideTypeModel[]> {
    let obs;
    if (this._conditionTypesAry.length) {
      obs = Observable.from(this._conditionTypesAry);
    } else {
      return this.actionAndConditionTypeLoader(this._doLoadConditionTypes(), this._conditionTypes);
    }
    return obs;
  }

  /**
   * Return the Site Id or Page Id for the rules operations.
   * First will check if the realmId parameter is included in the url.
   * If not then search for the current site Id.
   * @returns string
   */
  private loadRulesSiteId(): string {
    let siteId;
    let query = document.location.search.substring(1);
    if (query === '') {
      if (document.location.hash.indexOf('?') >= 0) {
        query = document.location.hash.substr(document.location.hash.indexOf('?') + 1);
      }
    }

    /**
     * Search if the realId parameter is set
     */
    siteId = ApiRoot.parseQueryParam(query, 'realmId');

    if (!siteId) {
      /**
       * If the realmId parameter is not set get the current Site Id
       */
      siteId = `${this.siteService.currentSite.identifier}`;
    }
    return siteId;
  }
}

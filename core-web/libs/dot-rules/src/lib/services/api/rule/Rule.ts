import { from as observableFrom, Subject, Observable, BehaviorSubject } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { mergeMap, reduce, map, tap } from 'rxjs/operators';
// tslint:disable-next-line:max-file-line-count

import { CoreWebService, SiteService, CwError, ApiRoot } from '@dotcms/dotcms-js';

import { I18nService } from '../../i18n/i18n.service';
import {
    ServerSideFieldModel,
    ServerSideTypeModel
} from '../serverside-field/ServerSideFieldModel';

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
    errors?: Record<string, unknown>;
    set?(key: string, value: unknown): unknown;
}

export interface IRuleAction extends IRecord {
    id?: string;
    priority: number;
    type?: string;
    parameters?: Record<string, { value: string }>;
    owningRule?: string;
    _owningRule?: RuleModel;
}

export interface ICondition extends IRecord {
    id?: string;
    conditionlet?: string;
    type?: string;
    priority?: number;
    operator?: string;
    parameters?: Record<string, { value: string; priority?: number }>;
    _type?: ServerSideTypeModel;
}

export interface IConditionGroup extends IRecord {
    id?: string;
    priority: number;
    operator: string;
    conditions?: Record<string, boolean>;
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
    conditionGroups?: Record<string, unknown>;
    ruleActions?: Record<string, boolean>;
    set?(key: string, value: unknown): IRule;
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
            this.loggerService.error(e);
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
            this.loggerService.error(e);
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
        const valid = this.operator && (this.operator === 'AND' || this.operator === 'OR');

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
    _errors: { [key: string]: string | Error };

    constructor(iRule: IRule) {
        Object.assign(this, iRule);
        this.key = iRule.id;
        this._id = this.key != null ? this.key : getNextId();
        const conGroups = Object.keys(iRule.conditionGroups || {});
        conGroups.forEach((groupId) => {
            const g = this.conditionGroups[groupId];
            const mg = new ConditionGroupModel(Object.assign({ id: groupId }, g));
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
    ruleActions: {}
};

// @dynamic
@Injectable()
export class RuleService {
    _apiRoot = inject(ApiRoot);
    private _resources = inject(I18nService);
    private siteService = inject(SiteService);
    private coreWebService = inject(CoreWebService);

    get rules(): RuleModel[] {
        return this._rules;
    }
    ruleActionTypes$: BehaviorSubject<ServerSideTypeModel[]> = new BehaviorSubject([]);
    conditionTypes$: BehaviorSubject<ServerSideTypeModel[]> = new BehaviorSubject([]);

    _ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};
    _conditionTypes: { [key: string]: ServerSideTypeModel } = {};

    public _errors$: Subject<{ message: string; response: Response }> = new Subject();

    protected _actionsEndpointUrl: string;
    // tslint:disable-next-line:no-unused-variable
    protected _ruleActions: { [key: string]: ActionModel } = {};
    // tslint:disable-next-line:no-unused-variable
    protected _conditions: { [key: string]: ConditionModel } = {};

    private _rulesEndpointUrl: string;
    private _conditionTypesEndpointUrl: string;
    private _ruleActionTypesEndpointUrl: string;

    private _rules$: Subject<RuleModel[]> = new Subject();

    private _ruleActionTypesAry: ServerSideTypeModel[] = [];
    private _conditionTypesAry: ServerSideTypeModel[] = [];

    private _rules: RuleModel[];

    constructor() {
        const _resources = this._resources;

        this._rulesEndpointUrl = `/ruleengine/rules`;
        this._actionsEndpointUrl = `/ruleengine/actions`;
        this._conditionTypesEndpointUrl = `/api/v1/system/ruleengine/conditionlets`;
        this._ruleActionTypesEndpointUrl = `/api/v1/system/ruleengine/actionlets`;

        this._preCacheCommonResources(_resources);
        this.loadActionTypes().subscribe(
            (types: ServerSideTypeModel[]) => {
                this.ruleActionTypes$.next(types);
            },
            (err) => {
                this._errors$.next(err);
            }
        );
        this.loadConditionTypes().subscribe(
            (types: ServerSideTypeModel[]) => {
                this.conditionTypes$.next(types);
            },
            (err) => {
                this._errors$.next(err);
            }
        );

        this.siteService.currentSite$.subscribe((site) => {
            const siteId = this.loadRulesSiteId();
            if (siteId === site.identifier) {
                this.sendLoadRulesRequest(site.identifier);
            }
        });
    }

    static fromServerRulesTransformFn(ruleMap: Record<string, IRule>): RuleModel[] {
        return Object.keys(ruleMap).map((id: string) => {
            const r: IRule = ruleMap[id];
            r.id = id;

            return new RuleModel(r);
        });
    }

    static fromClientRuleTransformFn(rule: RuleModel): IRule {
        const sendRule = Object.assign({}, DEFAULT_RULE, rule) as IRule & {
            conditionGroups: Record<string, IConditionGroup>;
            key?: string;
        };
        sendRule.key = rule.key;
        delete sendRule.id;
        sendRule.conditionGroups = {};
        sendRule._conditionGroups.forEach((conditionGroup: ConditionGroupModel) => {
            if (conditionGroup.key) {
                const sendGroup: IConditionGroup = {
                    conditions: {} as Record<string, boolean>,
                    operator: conditionGroup.operator,
                    priority: conditionGroup.priority
                };
                conditionGroup._conditions.forEach((condition: ConditionModel) => {
                    sendGroup.conditions[condition.key] = true;
                });
                sendRule.conditionGroups[conditionGroup.key] = sendGroup;
            }
        });
        this.removeMeta(sendRule as unknown as Record<string, unknown>);

        return sendRule;
    }

    static removeMeta(entity: Record<string, unknown>): void {
        Object.keys(entity).forEach((key) => {
            if (key[0] === '_') {
                delete entity[key];
            }
        });
    }

    static alphaSort(
        key: string
    ): (a: Record<string, string>, b: Record<string, string>) => number {
        return (a, b) => {
            let x;
            if (a[key] > b[key]) {
                x = 1;
            } else if (a[key] < b[key]) {
                x = -1;
            } else {
                x = 0;
            }

            return x;
        };
    }

    createRule(body: RuleModel): Observable<RuleModel | CwError> {
        const siteId = this.loadRulesSiteId();

        return this.coreWebService
            .request({
                body: RuleService.fromClientRuleTransformFn(body),
                method: 'POST',
                url: `/api/v1/sites/${siteId}${this._rulesEndpointUrl}`
            })
            .pipe(
                map((result: { id: string }) => {
                    body.key = result.id;

                    return <RuleModel | CwError>(
                        (<unknown>Object.assign({}, DEFAULT_RULE, body, result))
                    );
                })
            );
    }

    deleteRule(ruleId: string): Observable<{ success: boolean } | CwError> {
        const siteId = this.loadRulesSiteId();

        return this.coreWebService
            .request({
                method: 'DELETE',
                url: `/api/v1/sites/${siteId}${this._rulesEndpointUrl}/${ruleId}`
            })
            .pipe(
                map((_result) => {
                    return { success: true };
                })
            );
    }

    loadRules(): Observable<RuleModel[]> {
        return this._rules$.asObservable();
    }

    public requestRules(siteId: string): void {
        if (siteId) {
            this.sendLoadRulesRequest(siteId);
        } else if (this.siteService.currentSite) {
            this.sendLoadRulesRequest(this.siteService.currentSite.identifier);
        } else {
            this.siteService.getCurrentSite().subscribe((site) => {
                this.sendLoadRulesRequest(site.identifier);
            });
        }
    }

    loadRule(id: string): Observable<RuleModel | CwError> {
        const siteId = this.loadRulesSiteId();

        return this.coreWebService
            .request({
                url: `/api/v1/sites/${siteId}${this._rulesEndpointUrl}/${id}`
            })
            .pipe(
                map((result) => {
                    return <RuleModel | CwError>(
                        (<unknown>Object.assign({ key: id }, DEFAULT_RULE, result))
                    );
                })
            );
    }

    updateRule(id: string, rule: RuleModel): Observable<RuleModel | CwError> {
        let result;
        const siteId = this.loadRulesSiteId();
        if (!id) {
            result = this.createRule(rule);
        } else {
            result = this.coreWebService
                .request({
                    body: RuleService.fromClientRuleTransformFn(rule),
                    method: 'PUT',
                    url: `/api/v1/sites/${siteId}${this._rulesEndpointUrl}/${id}`
                })
                .pipe(
                    map((res) => {
                        const r = Object.assign({}, DEFAULT_RULE, res);
                        r.id = id;

                        return r;
                    })
                );
        }

        return result;
    }

    getConditionTypes(): Observable<ServerSideTypeModel[]> {
        return this.coreWebService
            .request({
                url: this._conditionTypesEndpointUrl
            })
            .pipe(map(this.fromServerServersideTypesTransformFn));
    }

    getRuleActionTypes(): Observable<ServerSideTypeModel[]> {
        return this.coreWebService
            .request({
                url: this._ruleActionTypesEndpointUrl
            })
            .pipe(map(this.fromServerServersideTypesTransformFn));
    }

    _doLoadRuleActionTypes(): Observable<ServerSideTypeModel[]> {
        return this.coreWebService
            .request({
                url: this._ruleActionTypesEndpointUrl
            })
            .pipe(map(this.fromServerServersideTypesTransformFn));
    }

    _doLoadConditionTypes(): Observable<ServerSideTypeModel[]> {
        return this.coreWebService
            .request({
                url: this._conditionTypesEndpointUrl
            })
            .pipe(map(this.fromServerServersideTypesTransformFn));
    }

    private fromServerServersideTypesTransformFn(
        typesMap: Record<string, { i18nKey: string; parameterDefinitions: Record<string, unknown> }>
    ): ServerSideTypeModel[] {
        const types = Object.keys(typesMap).map((key: string) => {
            const json = { ...typesMap[key], key };

            return ServerSideTypeModel.fromJson(json);
        });

        return types.filter((type) => type.key !== 'CountRulesActionlet');
    }

    private _preCacheCommonResources(resources: I18nService): void {
        resources.get('api.sites.ruleengine').subscribe((_rsrc) => {
            /**/
        });
        resources.get('api.ruleengine.system').subscribe((_rsrc) => {
            /**/
        });
        resources.get('api.system.ruleengine').subscribe((_rsrc) => {
            /**/
        });
    }

    private sendLoadRulesRequest(siteId: string): void {
        this.coreWebService
            .request<Record<string, IRule>>({
                url: `/api/v1/sites/${siteId}/ruleengine/rules`
            })
            .subscribe(
                (ruleMap) => {
                    this._rules = RuleService.fromServerRulesTransformFn(
                        ruleMap as Record<string, IRule>
                    );
                    this._rules$.next(this.rules);

                    return RuleService.fromServerRulesTransformFn(ruleMap as Record<string, IRule>);
                },
                (err) => {
                    this._errors$.next(err);
                }
            );
    }

    private loadActionTypes(): Observable<ServerSideTypeModel[]> {
        let obs;
        if (this._ruleActionTypesAry.length) {
            obs = observableFrom(this._ruleActionTypesAry);
        } else {
            return this.actionAndConditionTypeLoader(
                this._doLoadRuleActionTypes(),
                this._ruleActionTypes
            );
        }

        return obs;
    }

    private actionAndConditionTypeLoader(
        requestObserver: Observable<ServerSideTypeModel[]>,
        typeMap: Record<string, ServerSideTypeModel>
    ): Observable<ServerSideTypeModel[]> {
        return requestObserver.pipe(
            mergeMap((types: ServerSideTypeModel[]) => {
                return observableFrom(types).pipe(
                    mergeMap((type) => {
                        return this._resources.get(type.i18nKey + '.name', type.i18nKey).pipe(
                            map((label: string) => {
                                type._opt = { value: type.key, label: label };

                                return type;
                            })
                        );
                    }),
                    reduce((accTypes: ServerSideTypeModel[], type: ServerSideTypeModel) => {
                        accTypes.push(type);

                        return accTypes;
                    }, []),
                    tap((typ: ServerSideTypeModel[]) => {
                        typ = typ.sort((typeA, typeB) => {
                            return typeA._opt.label.localeCompare(typeB._opt.label);
                        });
                        typ.forEach((type) => {
                            typeMap[type.key] = type;
                        });

                        return typ;
                    })
                );
            })
        );
    }

    private loadConditionTypes(): Observable<ServerSideTypeModel[]> {
        let obs;
        if (this._conditionTypesAry.length) {
            obs = observableFrom(this._conditionTypesAry);
        } else {
            return this.actionAndConditionTypeLoader(
                this._doLoadConditionTypes(),
                this._conditionTypes
            );
        }

        return obs;
    }

    private getPageIdFromUrl(): string {
        let query;

        const hash = document.location.hash;

        if (hash.includes('fromCore')) {
            query = hash.substr(hash.indexOf('?') + 1);

            return ApiRoot.parseQueryParam(query, 'realmId');
        } else if (hash.includes('edit-page') || hash.includes('edit-ema')) {
            return hash.split('/').pop().split('?')[0];
        }

        return null;
    }

    /**
     * Return the Site Id or Page Id for the rules operations.
     * First will check if the realmId parameter is included in the url.
     * If not then search for the current site Id.
     * @returns string
     */
    private loadRulesSiteId(): string {
        let siteId = this.getPageIdFromUrl();

        if (!siteId) {
            /**
             * If the realmId parameter is not set get the current Site Id
             */
            siteId = `${this.siteService.currentSite.identifier}`;
        }

        return siteId;
    }
}

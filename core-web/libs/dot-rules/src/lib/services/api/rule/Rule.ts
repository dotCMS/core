import { BehaviorSubject, Observable, from as observableFrom, Subject } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map, mergeMap, reduce, tap } from 'rxjs/operators';
// tslint:disable-next-line:max-file-line-count

import { ApiRoot, CwError, SiteService } from '@dotcms/dotcms-js';

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
    globalError: string | null = null;
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
    /** `DEFAULT_RULE` sets this to `null` explicitly: a new rule is unnamed, not absent. */
    name?: string | null;
    fireOn?: string;
    enabled?: boolean;
    conditionGroups?: Record<string, unknown>;
    ruleActions?: Record<string, boolean>;
    set?(key: string, value: unknown): IRule;
}

/** What `/conditionlets` and `/actionlets` return: a map of type key to its definition. */
type ServerSideTypesResponse = Record<
    string,
    { i18nKey: string; parameterDefinitions: Record<string, unknown> }
>;

export interface ParameterModel {
    key: string;
    value: string;
    priority: number;
}

export class ActionModel extends ServerSideFieldModel {
    /** Both are attached after construction — by `Action.ts` on the way out to the server, and
     * by the container when it builds a brand new action for an unsaved rule. */
    owningRule?: string;
    _owningRule?: RuleModel;

    constructor(key: string | null, type: ServerSideTypeModel, priority = 1) {
        super(key, type, priority);
        this.priority = priority || 1;
        this.type = type;
    }

    override isValid(): boolean {
        try {
            return super.isValid();
        } catch (e) {
            this.loggerService?.error(e);

            // Was an implicit `undefined`, which every caller read as falsy anyway. Saying
            // `false` outright means the signature stops claiming a boolean it did not return.
            return false;
        }
    }
}

export class ConditionModel extends ServerSideFieldModel {
    operator = 'AND';
    /** Absent on a condition the user has not given a type yet. */
    conditionlet?: string;

    constructor(iCondition: ICondition) {
        // `?? new ServerSideTypeModel()` mirrors what the callers already do: every site that
        // builds a condition without a type passes `new ServerSideTypeModel()`, whose key is
        // 'NoSelection'. `ICondition._type` being optional made that implicit.
        super(iCondition.id ?? null, iCondition._type ?? new ServerSideTypeModel());
        this.conditionlet = iCondition.conditionlet;
        this.key = iCondition.id ?? null;
        this.priority = iCondition.priority || 1;
        this.type = iCondition._type ?? new ServerSideTypeModel();
        this.operator = iCondition.operator || 'AND';
    }

    override isValid(): boolean {
        try {
            return !!this.getParameterValue('comparison') && super.isValid();
        } catch (e) {
            this.loggerService?.error(e);

            return false;
        }
    }
}

export class ConditionGroupModel {
    /** `null` until saved — see `isPersisted()`. */
    key: string | null;
    priority: number;
    operator: string;
    conditions: { [key: string]: boolean };
    _id: string;
    _conditions: ConditionModel[] = [];

    constructor(iGroup: IConditionGroup) {
        // `Object.assign` still runs, for the `_`-prefixed view state a caller may pass in, but
        // `priority` and `operator` are assigned outright: they are required on `IConditionGroup`
        // and the compiler cannot see through `Object.assign` to know they arrive.
        Object.assign(this, iGroup);
        this.priority = iGroup.priority;
        this.operator = iGroup.operator;
        this.key = iGroup.id ?? null;
        this._id = this.key != null ? this.key : getNextId();
        this.conditions = iGroup.conditions || {};
    }

    /**
     * A `this`-typed predicate rather than a plain boolean: it lets `if (model.isPersisted())`
     * narrow `model.key` from `string | null` to `string`, which is what every caller inside such
     * a branch goes on to do when it builds a URL from it.
     */
    isPersisted(): this is this & { key: string } {
        return this.key != null;
    }

    isValid(): boolean {
        return this.operator === 'AND' || this.operator === 'OR';
    }
}

export class RuleModel {
    /** `null` until saved — see `isPersisted()`. */
    key: string | null;
    /** `null` for a rule the user has not named yet; `DEFAULT_RULE` seeds it that way. */
    name: string | null;
    enabled = false;
    priority = 1;
    fireOn = 'EVERY_PAGE';
    conditionGroups: { [key: string]: ConditionGroupModel } = {};
    ruleActions: { [key: string]: boolean } = {};

    _id: string;
    _expanded = false;
    _conditionGroups: ConditionGroupModel[] = [];
    _ruleActions: ActionModel[] = [];
    _saved = true;
    _saving = false;
    _deleting = true;
    /** `null` clears the errors; the container assigns it directly in `ruleUpdating`. */
    _errors: { [key: string]: string | Error } | null = null;

    constructor(iRule: IRule) {
        // As in `ConditionGroupModel`: `Object.assign` carries everything, and the fields the
        // compiler must see assigned are repeated below. The defaults above match `DEFAULT_RULE`.
        Object.assign(this, iRule);
        this.name = iRule.name ?? null;
        this.key = iRule.id ?? null;
        this._id = this.key != null ? this.key : getNextId();
        const conGroups = Object.keys(iRule.conditionGroups || {});
        conGroups.forEach((groupId) => {
            const g = this.conditionGroups[groupId];
            const mg = new ConditionGroupModel(Object.assign({ id: groupId }, g));
            this.conditionGroups[groupId] = mg;
            this._conditionGroups.push(mg);
        });
    }

    /**
     * A `this`-typed predicate rather than a plain boolean: it lets `if (model.isPersisted())`
     * narrow `model.key` from `string | null` to `string`, which is what every caller inside such
     * a branch goes on to do when it builds a URL from it.
     */
    isPersisted(): this is this & { key: string } {
        return this.key != null;
    }

    isValid(): boolean {
        return !!this.name && this.name.trim().length > 0;
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
    private http = inject(HttpClient);

    get rules(): RuleModel[] {
        return this._rules;
    }
    ruleActionTypes$ = new BehaviorSubject<ServerSideTypeModel[]>([]);
    conditionTypes$ = new BehaviorSubject<ServerSideTypeModel[]>([]);

    _ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};
    _conditionTypes: { [key: string]: ServerSideTypeModel } = {};

    public _errors$: Subject<HttpErrorResponse> = new Subject();

    protected _actionsEndpointUrl: string;
    // tslint:disable-next-line:no-unused-variable
    protected _ruleActions: { [key: string]: ActionModel } = {};
    // tslint:disable-next-line:no-unused-variable
    protected _conditions: { [key: string]: ConditionModel } = {};

    private _rulesEndpointUrl: string;
    private _conditionTypesEndpointUrl: string;
    private _ruleActionTypesEndpointUrl: string;

    private _rules$: Subject<RuleModel[]> = new Subject();

    private _rules: RuleModel[] = [];

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
        sendRule.key = rule.key ?? undefined;
        delete sendRule.id;
        sendRule.conditionGroups = {};
        // `?? []` because `_conditionGroups` is optional on `IRule` and the spread above comes
        // from `Object.assign({}, DEFAULT_RULE, rule)` — a `RuleModel` always has the array.
        (sendRule._conditionGroups ?? []).forEach((conditionGroup: ConditionGroupModel) => {
            if (conditionGroup.key) {
                const conditions: Record<string, boolean> = {};
                const sendGroup: IConditionGroup = {
                    conditions,
                    operator: conditionGroup.operator,
                    priority: conditionGroup.priority
                };
                conditionGroup._conditions.forEach((condition: ConditionModel) => {
                    // An unsaved condition has no key and nothing to reference server-side.
                    if (condition.key) {
                        conditions[condition.key] = true;
                    }
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

        return this.http
            .post<{
                id: string;
            }>(
                `/api/v1/sites/${siteId}${this._rulesEndpointUrl}`,
                RuleService.fromClientRuleTransformFn(body)
            )
            .pipe(
                map((result) => {
                    body.key = result.id;

                    return <RuleModel | CwError>(
                        (<unknown>Object.assign({}, DEFAULT_RULE, body, result))
                    );
                })
            );
    }

    deleteRule(ruleId: string): Observable<{ success: boolean } | CwError> {
        const siteId = this.loadRulesSiteId();

        return this.http.delete(`/api/v1/sites/${siteId}${this._rulesEndpointUrl}/${ruleId}`).pipe(
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

        return this.http.get(`/api/v1/sites/${siteId}${this._rulesEndpointUrl}/${id}`).pipe(
            map((result) => {
                return <RuleModel | CwError>(
                    (<unknown>Object.assign({ key: id }, DEFAULT_RULE, result))
                );
            })
        );
    }

    updateRule(id: string, rule: RuleModel): Observable<RuleModel | CwError> {
        let result: Observable<RuleModel | CwError>;
        const siteId = this.loadRulesSiteId();
        if (!id) {
            result = this.createRule(rule);
        } else {
            result = this.http
                .put(
                    `/api/v1/sites/${siteId}${this._rulesEndpointUrl}/${id}`,
                    RuleService.fromClientRuleTransformFn(rule)
                )
                .pipe(
                    map((res) => {
                        const r = Object.assign({}, DEFAULT_RULE, res);
                        r.id = id;

                        // Same shape-lie as `loadRule` above: the server sends an `IRule`, not a
                        // `RuleModel`, and every caller reads it as plain data.
                        return r as unknown as RuleModel;
                    })
                );
        }

        return result;
    }

    getConditionTypes(): Observable<ServerSideTypeModel[]> {
        return this.http
            .get<ServerSideTypesResponse>(this._conditionTypesEndpointUrl)
            .pipe(map(this.fromServerServersideTypesTransformFn));
    }

    getRuleActionTypes(): Observable<ServerSideTypeModel[]> {
        return this.http
            .get<ServerSideTypesResponse>(this._ruleActionTypesEndpointUrl)
            .pipe(map(this.fromServerServersideTypesTransformFn));
    }

    _doLoadRuleActionTypes(): Observable<ServerSideTypeModel[]> {
        return this.http
            .get<ServerSideTypesResponse>(this._ruleActionTypesEndpointUrl)
            .pipe(map(this.fromServerServersideTypesTransformFn));
    }

    _doLoadConditionTypes(): Observable<ServerSideTypeModel[]> {
        return this.http
            .get<ServerSideTypesResponse>(this._conditionTypesEndpointUrl)
            .pipe(map(this.fromServerServersideTypesTransformFn));
    }

    private fromServerServersideTypesTransformFn(
        typesMap: ServerSideTypesResponse
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
        this.http.get<Record<string, IRule>>(`/api/v1/sites/${siteId}/ruleengine/rules`).subscribe(
            (ruleMap) => {
                this._rules = RuleService.fromServerRulesTransformFn(ruleMap);
                this._rules$.next(this.rules);

                return RuleService.fromServerRulesTransformFn(ruleMap);
            },
            (err) => {
                this._errors$.next(err);
            }
        );
    }

    private loadActionTypes(): Observable<ServerSideTypeModel[]> {
        return this.actionAndConditionTypeLoader(
            this._doLoadRuleActionTypes(),
            this._ruleActionTypes
        );
    }

    private actionAndConditionTypeLoader(
        requestObserver: Observable<ServerSideTypeModel[]>,
        typeMap: Record<string, ServerSideTypeModel>
    ): Observable<ServerSideTypeModel[]> {
        return requestObserver.pipe(
            mergeMap((types: ServerSideTypeModel[]) => {
                return observableFrom(types).pipe(
                    mergeMap((type) => {
                        return this._resources
                            .get(`${type.i18nKey}.name`, type.i18nKey ?? undefined)
                            .pipe(
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
        return this.actionAndConditionTypeLoader(
            this._doLoadConditionTypes(),
            this._conditionTypes
        );
    }

    /** `null` when the current URL carries neither a realm id nor an edit-page path. */
    private getPageIdFromUrl(): string | null {
        const hash = document.location.hash;

        if (hash.includes('fromCore')) {
            const query = hash.substr(hash.indexOf('?') + 1);

            return ApiRoot.parseQueryParam(query, 'realmId');
        } else if (hash.includes('edit-page') || hash.includes('edit-ema')) {
            // `split` on a non-empty string always yields at least one element, so `pop()` is
            // only formally undefined here.
            return hash.split('/').pop()?.split('?')[0] ?? null;
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

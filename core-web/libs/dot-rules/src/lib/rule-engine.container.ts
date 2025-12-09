import { from as observableFrom, Observable, merge, Subject } from 'rxjs';

// tslint:disable-next-line:max-file-line-count
import { Component, EventEmitter, ViewEncapsulation, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { reduce, mergeMap, take, map, filter, takeUntil } from 'rxjs/operators';

import { CwError } from '@dotcms/dotcms-js';
import { HttpCode } from '@dotcms/dotcms-js';
import { LoggerService } from '@dotcms/dotcms-js';

import { ActionService } from './services/Action';
import { BundleService, IPublishEnvironment } from './services/bundle-service';
import { ConditionService } from './services/Condition';
import { ConditionGroupService } from './services/ConditionGroup';
import { RuleViewService } from './services/dot-view-rule-service';
import {
    RuleModel,
    RuleService,
    ConditionGroupModel,
    ConditionModel,
    ActionModel,
    RuleEngineState
} from './services/Rule';
import { ServerSideFieldModel, ServerSideTypeModel } from './services/ServerSideFieldModel';
import { CwChangeEvent } from './services/util/CwEvent';

export interface ParameterChangeEvent extends CwChangeEvent {
    rule?: RuleModel;
    source?: ServerSideFieldModel;
    name: string;
    value: string;
}

export interface TypeChangeEvent extends CwChangeEvent {
    rule?: RuleModel;
    source: ServerSideFieldModel;
    value: any;
    index: number;
}

export interface RuleActionEvent {
    type: string;
    payload: {
        rule?: RuleModel;
        value?: string | boolean;
    };
}

export interface RuleActionActionEvent extends RuleActionEvent {
    payload: {
        rule?: RuleModel;
        value?: string | boolean;
        ruleAction?: ActionModel;
        index?: number;
        name?: string;
    };
}

export interface ConditionGroupActionEvent extends RuleActionEvent {
    payload: {
        rule?: RuleModel;
        value?: string | boolean;
        conditionGroup?: ConditionGroupModel;
        index?: number;
        priority?: number;
    };
}

export interface ConditionActionEvent extends RuleActionEvent {
    payload: {
        rule?: RuleModel;
        value?: string | boolean;
        condition?: ConditionModel;
        conditionGroup?: ConditionGroupModel;
        index?: number;
        name?: string;
        type?: string;
    };
}

/**
 *
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'cw-rule-engine-container',
    styleUrls: ['./styles/rule-engine.scss', './styles/angular-material.layouts.scss'],
    template: `
        <cw-rule-engine (createRule)="onCreateRule($event)"
            (deleteRule)="onDeleteRule($event)"
            (updateName)="onUpdateRuleName($event)"
            (updateFireOn)="onUpdateFireOn($event)"
            (updateEnabledState)="onUpdateEnabledState($event)"
            (updateExpandedState)="onUpdateExpandedState($event)"
            (createRuleAction)="onCreateRuleAction($event)"
            (updateRuleActionType)="onUpdateRuleActionType($event)"
            (updateRuleActionParameter)="onUpdateRuleActionParameter($event)"
            (deleteRuleAction)="onDeleteRuleAction($event)"
            (createConditionGroup)="onCreateConditionGroup($event)"
            (updateConditionGroupOperator)="onUpdateConditionGroupOperator($event)"
            (createCondition)="onCreateCondition($event)"
            (updateConditionType)="onUpdateConditionType($event)"
            (updateConditionParameter)="onUpdateConditionParameter($event)"
            (updateConditionOperator)="onUpdateConditionOperator($event)"
            (deleteCondition)="onDeleteCondition($event)"
            [environmentStores]="environments"
            [rules]="rules"
            [ruleActionTypes]="_ruleService._ruleActionTypes"
            [conditionTypes]="_ruleService._conditionTypes"
            [loading]="state.loading"
            [showRules]="state.showRules"
            [pageId]="pageId"
            [isContentletHost]="isContentletHost" />
    `,
    standalone: false
})
export class RuleEngineContainer implements OnDestroy {
    _ruleService = inject(RuleService);
    private _ruleActionService = inject(ActionService);
    private _conditionGroupService = inject(ConditionGroupService);
    private _conditionService = inject(ConditionService);
    bundleService = inject(BundleService);
    private route = inject(ActivatedRoute);
    private loggerService = inject(LoggerService);
    private ruleViewService = inject(RuleViewService);

    rules: RuleModel[];
    state: RuleEngineState = new RuleEngineState();

    environments: IPublishEnvironment[] = [];

    rules$: EventEmitter<RuleModel[]> = new EventEmitter();
    ruleActions$: EventEmitter<ActionModel[]> = new EventEmitter();
    conditionGroups$: EventEmitter<ConditionGroupModel[]> = new EventEmitter();
    globalError: string;
    pageId: string;
    isContentletHost: boolean;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor() {
        this.rules$.subscribe((rules) => {
            this.rules = rules;
        });

        this.bundleService
            .loadPublishEnvironments()
            .pipe(take(1))
            .subscribe((environments) => (this.environments = environments));
        this.initRules();

        this._ruleService._errors$.subscribe((res) => {
            this.ruleViewService.showErrorMessage(
                res.message,
                false,
                res.response.headers.get('error-key')
            );
            this.state.loading = false;
            this.state.showRules = false;
        });

        merge(
            this._ruleActionService.error,
            this._conditionGroupService.error,
            this._conditionService.error
        )
            .pipe(takeUntil(this.destroy$))
            .subscribe((message: string) => {
                this.ruleViewService.showErrorMessage(message);

                this.initRules();
            });
    }

    alphaSort(key): (a, b) => number {
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

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     *
     * @param event
     */
    onCreateRule(event): void {
        this.loggerService.info('RuleEngineContainer', 'onCreateRule', event);
        const priority = this.rules.length ? this.rules[0].priority + 1 : 1;
        const rule = new RuleModel({ priority });
        const group = new ConditionGroupModel({ operator: 'AND', priority: 1 });
        group._conditions.push(new ConditionModel({ _type: new ServerSideTypeModel() }));
        rule._conditionGroups.push(group);
        const action = new ActionModel(null, new ServerSideTypeModel());
        action._owningRule = rule;
        rule._ruleActions.push(action);
        rule._saved = false;
        rule._expanded = false;
        this.rules$.emit([rule].concat(this.rules));
    }

    onDeleteRule(event: RuleActionEvent): void {
        const rule = event.payload.rule;
        rule._deleting = true;
        this.state.deleting = true;
        if (rule.isPersisted()) {
            this._ruleService.deleteRule(rule.key).subscribe(
                (_result) => {
                    this.state.deleting = false;
                    const rules = this.rules.filter((arrayRule) => arrayRule.key !== rule.key);
                    this.rules$.emit(rules);
                },
                (e: CwError) => {
                    this._handle403Error(e) ? null : { invalid: e.message };
                }
            );
        }
    }

    onUpdateEnabledState(event: RuleActionEvent): void {
        event.payload.rule.enabled = <boolean>event.payload.value;
        this.patchRule(event.payload.rule, false);
    }

    onUpdateRuleName(event: RuleActionEvent): void {
        event.payload.rule.name = <string>event.payload.value;
        this.patchRule(event.payload.rule, false);
    }

    onUpdateFireOn(event: RuleActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onUpdateFireOn', event);
        event.payload.rule.fireOn = <string>event.payload.value;
        this.patchRule(event.payload.rule, false);
    }

    onUpdateExpandedState(event: RuleActionEvent): void {
        const rule = event.payload.rule;
        rule._expanded = <boolean>event.payload.value;
        if (rule._expanded) {
            let obs2: Observable<ConditionGroupModel>;
            if (rule._conditionGroups.length === 0) {
                const obs: Observable<ConditionGroupModel[]> =
                    this._conditionGroupService.allAsArray(
                        rule.key,
                        Object.keys(rule.conditionGroups)
                    );
                obs2 = obs.pipe(
                    mergeMap((groups: ConditionGroupModel[]) => observableFrom(groups))
                );
            } else {
                obs2 = observableFrom(rule._conditionGroups);
            }

            const obs3: Observable<ConditionGroupModel> = obs2.pipe(
                mergeMap(
                    (group: ConditionGroupModel) =>
                        this._conditionService.listForGroup(
                            group,
                            this._ruleService._conditionTypes
                        ),
                    (group: ConditionGroupModel, conditions: ConditionModel[]) => {
                        if (conditions) {
                            conditions.forEach((condition: ConditionModel) => {
                                condition.type =
                                    this._ruleService._conditionTypes[condition.conditionlet];
                            });
                        }

                        group._conditions = conditions;

                        return group;
                    }
                )
            );

            const obs4: Observable<ConditionGroupModel[]> = obs3.pipe(
                reduce((acc: ConditionGroupModel[], group: ConditionGroupModel) => {
                    acc.push(group);

                    return acc;
                }, [])
            );

            obs4.subscribe(
                (groups: ConditionGroupModel[]) => {
                    rule._conditionGroups = groups;
                    if (rule._conditionGroups.length === 0) {
                        this.loggerService.info(
                            'RuleEngineContainer',
                            'conditionGroups',
                            'Add stub group'
                        );
                        const group = new ConditionGroupModel({ operator: 'AND', priority: 1 });
                        group._conditions.push(
                            new ConditionModel({
                                _type: new ServerSideTypeModel(),
                                operator: 'AND',
                                priority: 1
                            })
                        );
                        rule._conditionGroups.push(group);
                    } else {
                        rule._conditionGroups.sort(this.prioritySortFn);
                        rule._conditionGroups.forEach((group: ConditionGroupModel) => {
                            group._conditions.sort(this.prioritySortFn);
                            if (group._conditions.length === 0) {
                                group._conditions.push(
                                    new ConditionModel({
                                        _type: new ServerSideTypeModel(),
                                        operator: 'AND',
                                        priority: 1
                                    })
                                );
                            }
                        });
                    }
                },
                (e) => {
                    this.loggerService.error('RuleEngineContainer', e);
                }
            );

            if (rule._ruleActions.length === 0) {
                this._ruleActionService
                    .allAsArray(
                        rule.key,
                        Object.keys(rule.ruleActions),
                        this._ruleService._ruleActionTypes
                    )
                    .subscribe((actions) => {
                        rule._ruleActions = actions;
                        if (rule._ruleActions.length === 0) {
                            const action = new ActionModel(null, new ServerSideTypeModel(), 1);
                            rule._ruleActions.push(action);
                            rule._ruleActions.sort(this.prioritySortFn);
                        } else {
                            rule._ruleActions.sort(this.prioritySortFn);
                        }
                    });
            }
        }
    }

    onCreateRuleAction(event: RuleActionActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onCreateRuleAction', event);
        const rule = event.payload.rule;
        const priority = rule._ruleActions.length
            ? rule._ruleActions[rule._ruleActions.length - 1].priority + 1
            : 1;
        const entity = new ActionModel(null, new ServerSideTypeModel(), priority);

        this.patchRule(rule, true);
        rule._ruleActions.push(entity);
        rule._saved = false;
    }

    onDeleteRuleAction(event: RuleActionActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onDeleteRuleAction', event);
        const rule = event.payload.rule;
        const ruleAction = event.payload.ruleAction;
        if (ruleAction.isPersisted()) {
            this._ruleActionService.remove(rule.key, ruleAction).subscribe((_result) => {
                rule._ruleActions = rule._ruleActions.filter((aryAction) => {
                    return aryAction.key !== ruleAction.key;
                });
                if (rule._ruleActions.length === 0) {
                    rule._ruleActions.push(new ActionModel(null, new ServerSideTypeModel(), 1));
                }
            });
        }
    }

    onUpdateRuleActionType(event: RuleActionActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onUpdateRuleActionType');
        try {
            const ruleAction = event.payload.ruleAction;
            const rule = event.payload.rule;
            const idx = event.payload.index;
            const type: ServerSideTypeModel =
                this._ruleService._ruleActionTypes[<string>event.payload.value];
            rule._ruleActions[idx] = new ActionModel(ruleAction.key, type, ruleAction.priority);
            this.patchAction(rule, ruleAction);
        } catch (e) {
            this.loggerService.error('RuleComponent', 'onActionTypeChange', e);
        }
    }

    onUpdateRuleActionParameter(event: RuleActionActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onUpdateRuleActionParameter');
        const ruleAction = event.payload.ruleAction;
        ruleAction.setParameter(event.payload.name, event.payload.value);
        this.patchAction(event.payload.rule, ruleAction);
    }

    onCreateConditionGroup(event: ConditionGroupActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onCreateConditionGroup');
        const rule = event.payload.rule;
        const priority = rule._conditionGroups.length
            ? rule._conditionGroups[rule._conditionGroups.length - 1].priority + 1
            : 1;
        const group = new ConditionGroupModel({ operator: 'AND', priority: priority });
        group._conditions.push(
            new ConditionModel({ _type: new ServerSideTypeModel(), operator: 'AND', priority: 1 })
        );
        rule._conditionGroups.push(group);
        rule._conditionGroups.sort(this.prioritySortFn);
    }

    onUpdateConditionGroupOperator(event: ConditionGroupActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onUpdateConditionGroupOperator');
        const group = event.payload.conditionGroup;
        group.operator = <string>event.payload.value;
        if (group.key != null) {
            this.patchConditionGroup(event.payload.rule, group);
            this.patchRule(event.payload.rule);
        }
    }

    onDeleteConditionGroup(event: ConditionGroupActionEvent): void {
        const rule = event.payload.rule;
        const group = event.payload.conditionGroup;
        this._conditionGroupService.remove(rule.key, group).subscribe();
        rule._conditionGroups = rule._conditionGroups.filter(
            (aryGroup) => aryGroup.key !== group.key
        );
    }

    onCreateCondition(event: ConditionActionEvent): void {
        const rule = event.payload.rule;
        this.ruleUpdating(rule, true);
        try {
            const group = event.payload.conditionGroup;
            const priority = group._conditions.length
                ? group._conditions[group._conditions.length - 1].priority + 1
                : 1;
            const entity = new ConditionModel({
                _type: new ServerSideTypeModel(),
                operator: 'AND',
                priority: priority
            });
            group._conditions.push(entity);
            this.ruleUpdated(rule);
        } catch (e) {
            this.loggerService.error('RuleEngineContainer', 'onCreateCondition', e);
            this.ruleUpdated(rule, [{ unhandledError: e }]);
        }
    }

    onUpdateConditionType(event: ConditionActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onUpdateConditionType');
        try {
            let condition = event.payload.condition;
            const group = event.payload.conditionGroup;
            const rule = event.payload.rule;
            const idx = event.payload.index;
            const type: ServerSideTypeModel =
                this._ruleService._conditionTypes[<string>event.payload.value];
            // replace the condition rather than mutate it to force event for 'onPush' NG2 components.
            condition = new ConditionModel({
                _type: type,
                id: condition.key,
                operator: condition.operator,
                priority: condition.priority
            });
            group._conditions[idx] = condition;
            this.patchCondition(rule, group, condition);
        } catch (e) {
            this.loggerService.error('RuleComponent', 'onActionTypeChange', e);
        }
    }

    onUpdateConditionParameter(event: ConditionActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onUpdateConditionParameter');
        const condition = event.payload.condition;
        condition.setParameter(event.payload.name, event.payload.value);
        this.patchCondition(event.payload.rule, event.payload.conditionGroup, condition);
    }

    onUpdateConditionOperator(event: ConditionActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onUpdateConditionOperator');
        const condition = event.payload.condition;
        condition.operator = <string>event.payload.value;
        this.patchCondition(event.payload.rule, event.payload.conditionGroup, condition);
    }

    onDeleteCondition(event: ConditionActionEvent): void {
        this.loggerService.info('RuleEngineContainer', 'onDeleteCondition', event);
        const rule = event.payload.rule;
        const group = event.payload.conditionGroup;
        const condition = event.payload.condition;
        if (condition.isPersisted()) {
            this._conditionService.remove(condition).subscribe((_result) => {
                group._conditions = group._conditions.filter((aryCondition) => {
                    return aryCondition.key !== condition.key;
                });
                if (group._conditions.length === 0) {
                    this.loggerService.info(
                        'RuleEngineContainer',
                        'condition',
                        'Remove Condition and remove Groups is empty'
                    );
                    this._conditionGroupService.remove(rule.key, group).subscribe();
                    rule._conditionGroups = rule._conditionGroups.filter(
                        (aryGroup) => aryGroup.key !== group.key
                    );
                }

                if (rule._conditionGroups.length === 0) {
                    this.loggerService.info(
                        'RuleEngineContainer',
                        'conditionGroups',
                        'Add stub group if Groups are empty'
                    );
                    const conditionGroup = new ConditionGroupModel({
                        operator: 'AND',
                        priority: 1
                    });
                    conditionGroup._conditions.push(
                        new ConditionModel({
                            _type: new ServerSideTypeModel(),
                            operator: 'AND',
                            priority: 1
                        })
                    );
                    rule._conditionGroups.push(conditionGroup);
                }
            });
        }
    }

    ruleUpdating(rule, disable = true): void {
        if (disable && rule.enabled && rule.key) {
            this.loggerService.info(
                'RuleEngineContainer',
                'ruleUpdating',
                'disabling rule due for edit.'
            );
            this.patchRule(rule, disable);
        }

        rule._saved = false;
        rule._saving = true;
        rule._errors = null;
    }

    ruleUpdated(rule: RuleModel, errors?: { [key: string]: any }): void {
        rule._saving = false;
        if (!errors) {
            rule._saved = true;
        } else {
            this.loggerService.error(errors);
            rule._errors = errors;
        }
    }

    patchConditionGroup(rule: RuleModel, group: ConditionGroupModel, disable = true): void {
        this.ruleUpdating(rule, false);
        if (disable && rule.enabled) {
            rule.enabled = false;
        }

        this._conditionGroupService
            .updateConditionGroup(rule.key, group)
            .subscribe((_result) => {});
    }

    patchRule(rule: RuleModel, disable = true): void {
        this.ruleUpdating(rule, false);
        if (disable && rule.enabled) {
            rule.enabled = false;
        }

        if (rule.isValid()) {
            if (rule.isPersisted()) {
                this._ruleService.updateRule(rule.key, rule).subscribe(
                    () => {
                        this.ruleUpdated(rule);
                    },
                    (e: CwError) => {
                        const ruleError = this._handle403Error(e) ? null : { invalid: e.message };
                        this.ruleUpdated(rule, ruleError);
                        this.initRules();
                    }
                );
            } else {
                this._ruleService.createRule(rule).subscribe(
                    () => {
                        this.ruleUpdated(rule);
                    },
                    (e: CwError) => {
                        const ruleError = this._handle403Error(e) ? null : { invalid: e.message };
                        this.ruleUpdated(rule, ruleError);
                        this.initRules();
                    }
                );
            }
        } else {
            this.ruleUpdated(rule, {
                invalid: 'Cannot save, rule is not valid.'
            });
        }
    }

    patchAction(rule: RuleModel, ruleAction: ActionModel): void {
        if (ruleAction.isValid()) {
            this.ruleUpdating(rule, false);
            if (!ruleAction.isPersisted()) {
                this._ruleActionService.createRuleAction(rule.key, ruleAction).subscribe(
                    (_) => {
                        this.ruleUpdated(rule);
                    },
                    (e: CwError) => {
                        const ruleError = this._handle403Error(e) ? null : { invalid: e.message };
                        this.ruleUpdated(rule, ruleError);
                    }
                );
            } else {
                this._ruleActionService.updateRuleAction(rule.key, ruleAction).subscribe(
                    (_result) => {
                        this.ruleUpdated(rule);
                    },
                    (e: any) => {
                        const ruleError = this._handle403Error(e) ? null : { invalid: e.message };
                        this.ruleUpdated(rule, ruleError);
                    }
                );
            }
        } else {
            this.ruleUpdating(rule);
            this.ruleUpdated(rule, {
                invalid: 'Cannot save, action is not valid.'
            });
        }
    }

    patchCondition(rule: RuleModel, group: ConditionGroupModel, condition: ConditionModel): void {
        try {
            if (condition.isValid()) {
                this.ruleUpdating(rule, false);
                if (condition.isPersisted()) {
                    this._conditionService.save(group.key, condition).subscribe(
                        (_result) => {
                            this.ruleUpdated(rule);
                        },
                        (e: any) => {
                            const ruleError = this._handle403Error(e)
                                ? null
                                : { invalid: e.message };
                            this.ruleUpdated(rule, ruleError);
                        }
                    );
                } else {
                    if (!group.isPersisted()) {
                        this._conditionGroupService
                            .createConditionGroup(rule.key, group)
                            .subscribe((_foo) => {
                                this._conditionService.add(group.key, condition).subscribe(
                                    () => {
                                        group.conditions[condition.key] = true;
                                        this.ruleUpdated(rule);
                                    },
                                    (e: CwError) => {
                                        const ruleError = this._handle403Error(e)
                                            ? null
                                            : { invalid: e.message };
                                        this.ruleUpdated(rule, ruleError);
                                    }
                                );
                            });
                    } else {
                        this._conditionService.add(group.key, condition).subscribe(
                            () => {
                                group.conditions[condition.key] = true;
                                this.ruleUpdated(rule);
                            },
                            (e: CwError) => {
                                const ruleError = this._handle403Error(e)
                                    ? null
                                    : { invalid: e.message };
                                this.ruleUpdated(rule, ruleError);
                            }
                        );
                    }
                }
            } else {
                this.ruleUpdating(rule);
                this.loggerService.info('RuleEngineContainer', 'patchCondition', 'Not valid');
                rule._saving = false;
                rule._errors = { invalid: 'Condition not valid.' };
            }
        } catch (e) {
            this.loggerService.error(e);
            this.ruleUpdated(rule, { invalid: e.message });
        }
    }

    prioritySortFn(a: any, b: any): number {
        return a.priority - b.priority;
    }

    private initRules(): void {
        this.state.loading = true;

        this.pageId = '';

        const pageIdParams = this.route.params.pipe(map((params: Params) => params.pageId));
        const queryParams = this.route.queryParams.pipe(map((params: Params) => params.realmId));

        merge(pageIdParams, queryParams)
            .pipe(
                filter((res) => !!res),
                take(1)
            )
            .subscribe((id: string) => {
                this.pageId = id;
            });

        this._ruleService.requestRules(this.pageId);
        this._ruleService
            .loadRules()
            .pipe(takeUntil(this.destroy$))
            .subscribe((rules: RuleModel[]) => {
                this.loadRules(rules);
            });
        this.route.queryParams
            .pipe(take(1))
            .subscribe(
                (params: Params) => (this.isContentletHost = params.isContentletHost === 'true')
            );
    }

    private loadRules(rules: RuleModel[]): void {
        rules.sort((a, b) => {
            return b.priority - a.priority;
        });
        this.rules$.emit(rules);
        this.state.loading = false;
    }

    private _handle403Error(e: CwError): boolean {
        let handled = false;
        try {
            if (e && e.response.status === HttpCode.FORBIDDEN) {
                const errorJson = e.response;
                if (errorJson && errorJson.error) {
                    this.ruleViewService.showErrorMessage(
                        errorJson.error.message.replace('dotcms.api.error.forbidden: ', '')
                    );
                    handled = true;
                }
            }
        } catch (e) {
            this.loggerService.error('Error while processing invalid response: ', e);
        }

        this.initRules();

        return handled;
    }
}

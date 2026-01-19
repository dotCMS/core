import { Observable, from, of } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    Component,
    EventEmitter,
    ElementRef,
    Input,
    Output,
    ChangeDetectionStrategy,
    inject,
    OnChanges
} from '@angular/core';
import {
    UntypedFormControl,
    Validators,
    UntypedFormGroup,
    UntypedFormBuilder,
    ReactiveFormsModule,
    FormsModule
} from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime, map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { UserModel, ApiRoot, LoggerService } from '@dotcms/dotcms-js';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotConditionGroupComponent } from '../dot-condition-group/dot-condition-group.component';
import { DotRuleActionComponent } from '../dot-rule-action/dot-rule-action.component';
import {
    ConditionActionEvent,
    RuleActionActionEvent,
    RuleActionEvent,
    ConditionGroupActionEvent
} from '../dot-rule-engine-container/dot-rule-engine-container.component';
import { IPublishEnvironment } from '../services/bundle-service';
import {
    RuleModel,
    RULE_UPDATE_ENABLED_STATE,
    RULE_UPDATE_NAME,
    RULE_DELETE,
    RULE_RULE_ACTION_UPDATE_TYPE,
    RULE_RULE_ACTION_UPDATE_PARAMETER,
    V_RULE_UPDATE_EXPANDED_STATE,
    RULE_CONDITION_UPDATE_PARAMETER,
    RULE_CONDITION_UPDATE_OPERATOR,
    RULE_CONDITION_UPDATE_TYPE,
    ConditionGroupModel,
    ActionModel,
    RULE_RULE_ACTION_DELETE,
    RULE_RULE_ACTION_CREATE,
    RULE_CONDITION_GROUP_CREATE,
    RuleService
} from '../services/Rule';
import { ServerSideTypeModel } from '../services/ServerSideFieldModel';
import { I18nService } from '../services/system/locale/I18n';

const I8N_BASE = 'api.sites.ruleengine';

@Component({
    selector: 'dot-rule',
    templateUrl: './dot-rule.component.html',
    imports: [
        AsyncPipe,
        ReactiveFormsModule,
        FormsModule,
        AutoFocusModule,
        ButtonModule,
        InputTextModule,
        MenuModule,
        SelectModule,
        ToggleSwitchModule,
        TooltipModule,
        DotAddToBundleComponent,
        DotConditionGroupComponent,
        DotRuleActionComponent
    ],
    changeDetection: ChangeDetectionStrategy.Default
})
export class DotRuleComponent implements OnChanges {
    private _user = inject(UserModel);
    elementRef = inject(ElementRef);
    resources = inject(I18nService);
    ruleService = inject(RuleService);
    apiRoot = inject(ApiRoot);
    private loggerService = inject(LoggerService);

    @Input() rule: RuleModel;
    @Input() saved = false;
    @Input() saving = false;
    @Input() errors: { [key: string]: any } = null;
    @Input() ruleActions: ActionModel[] = [];
    @Input() ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() conditionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() environmentStores: IPublishEnvironment[] = [];

    @Input() hidden = false;

    @Output() deleteRule: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateExpandedState: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateName: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateEnabledState: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateFireOn: EventEmitter<RuleActionEvent> = new EventEmitter(false);

    @Output() createRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output() updateRuleActionType: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output()
    updateRuleActionParameter: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output() deleteRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);

    @Output()
    updateConditionGroupOperator: EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false);
    @Output()
    createConditionGroup: EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false);

    @Output() createCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() deleteCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionType: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output()
    updateConditionParameter: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionOperator: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() openPushPublishDialog: EventEmitter<string> = new EventEmitter(false);

    formModel: UntypedFormGroup;
    fireOnValue = 'EVERY_PAGE';
    fireOnOptions$: Observable<{ label: string; value: string }[]>;
    fireOnPlaceholder$: Observable<string>;
    showAddToBundleDialog = false;
    hideFireOn: boolean;
    actionTypePlaceholder = '';
    conditionTypePlaceholder = '';
    ruleActionOptions: MenuItem[];
    tooltipRuleOnText: string;
    tooltipRuleOffText: string;

    private _updateEnabledStateDelay: EventEmitter<{
        type: string;
        payload: { rule: RuleModel; value: boolean };
    }> = new EventEmitter(false);

    private _rsrcCache: { [key: string]: Observable<string> };

    constructor() {
        const apiRoot = this.apiRoot;
        const fb = inject(UntypedFormBuilder);

        this._rsrcCache = {};
        this.hideFireOn = document.location.hash.includes('edit-page') || apiRoot.hideFireOn;

        /* Need to delay the firing of the state change toggle, to give any blur events time to fire. */
        this._updateEnabledStateDelay.pipe(debounceTime(20)).subscribe((event: RuleActionEvent) => {
            this.updateEnabledState.emit(event);
        });

        // Build fireOn options with resolved labels
        const fireOnRawOptions = [
            { label: this.rsrc('inputs.fireOn.options.EveryPage'), value: 'EVERY_PAGE' },
            { label: this.rsrc('inputs.fireOn.options.OncePerVisit'), value: 'ONCE_PER_VISIT' },
            { label: this.rsrc('inputs.fireOn.options.OncePerVisitor'), value: 'ONCE_PER_VISITOR' },
            { label: this.rsrc('inputs.fireOn.options.EveryRequest'), value: 'EVERY_REQUEST' }
        ];

        this.fireOnOptions$ = from(fireOnRawOptions).pipe(
            mergeMap((item: { label: Observable<string>; value: string }) => {
                return item.label.pipe(
                    map((text: string) => ({
                        label: text,
                        value: item.value
                    }))
                );
            }),
            toArray(),
            startWith([]),
            shareReplay(1)
        );

        this.fireOnPlaceholder$ = this.rsrc('inputs.fireOn.placeholder', 'Select One');

        this.initFormModel(fb);

        this.resources
            .get('api.sites.ruleengine.rules.inputs.action.type.placeholder')
            .subscribe((label) => {
                this.actionTypePlaceholder = label;
            });

        this.resources
            .get('api.sites.ruleengine.rules.inputs.condition.type.placeholder')
            .subscribe((label) => {
                this.conditionTypePlaceholder = label;
            });

        this.resources
            .get('api.sites.ruleengine.rules.inputs.add_to_bundle.label')
            .subscribe((addToBundleLabel) => {
                this.resources
                    .get('api.sites.ruleengine.rules.inputs.deleteRule.label')
                    .subscribe((deleteRuleLabel) => {
                        this.ruleActionOptions = [
                            {
                                label: addToBundleLabel,
                                visible: !this.apiRoot.hideRulePushOptions,
                                command: () => {
                                    this.showAddToBundleDialog = true;
                                }
                            },
                            {
                                label: deleteRuleLabel,
                                visible: !this.apiRoot.hideRulePushOptions,
                                command: (event) => {
                                    this.deleteRuleClicked(event.originalEvent);
                                }
                            }
                        ];
                    });
            });

        this.resources
            .get('api.sites.ruleengine.rules.inputs.onOff.tip')
            .subscribe((tooltipLabel) => {
                this.resources
                    .get('api.sites.ruleengine.rules.inputs.onOff.on.label')
                    .subscribe((ruleOnLabel) => {
                        this.resources
                            .get('api.sites.ruleengine.rules.inputs.onOff.off.label')
                            .subscribe((ruleOffLabel) => {
                                this.tooltipRuleOnText = `${tooltipLabel} (${ruleOnLabel})`;
                                this.tooltipRuleOffText = `${tooltipLabel} (${ruleOffLabel})`;
                            });
                    });
            });
    }

    initFormModel(fb: UntypedFormBuilder): void {
        const vFns = [];
        vFns.push(Validators.required);
        vFns.push(Validators.minLength(3));
        this.formModel = fb.group({
            name: new UntypedFormControl(this.rule ? this.rule.name : '', Validators.compose(vFns))
        });
    }

    rsrc(subkey: string, defVal = '-missing-'): any {
        let msgObserver = this._rsrcCache[subkey];
        if (!msgObserver) {
            msgObserver = this.resources.get(I8N_BASE + '.rules.' + subkey, defVal);
            this._rsrcCache[subkey] = msgObserver;
        }

        return msgObserver;
    }

    ngOnChanges(change): void {
        if (change.rule) {
            const rule = this.rule;
            const ctrl: UntypedFormControl = <UntypedFormControl>this.formModel.controls['name'];
            ctrl.patchValue(this.rule.name, {});

            ctrl.valueChanges.pipe(debounceTime(250)).subscribe((name: string) => {
                if (ctrl.valid) {
                    this.updateName.emit({
                        payload: { rule: this.rule, value: name },
                        type: RULE_UPDATE_NAME
                    });
                }
            });
            if (rule.isPersisted()) {
                this.fireOnValue = rule.fireOn;
            }
        }
    }

    onFireOnChange(value: string): void {
        this.updateFireOn.emit({
            type: 'RULE_UPDATE_FIRE_ON',
            payload: { rule: this.rule, value }
        });
    }

    statusText(length = 0): string {
        let t = '';
        if (this.saved) {
            t = 'All changes saved';
        } else if (this.saving) {
            t = 'Saving...';
        } else if (this.errors) {
            t = this.errors['invalid'] || this.errors['serverError'] || 'Unsaved changes...';
        }

        if (length) {
            t = t.substring(0, length) + '...';
        }

        return t;
    }

    setRuleExpandedState(expanded: boolean): void {
        if (this.rule.name) {
            this.updateExpandedState.emit({
                payload: { rule: this.rule, value: expanded },
                type: V_RULE_UPDATE_EXPANDED_STATE
            });
        }
    }

    setRuleEnabledState(event: any): void {
        this._updateEnabledStateDelay.emit({
            payload: { rule: this.rule, value: event.checked },
            type: RULE_UPDATE_ENABLED_STATE
        });
        event.originalEvent.stopPropagation();
    }

    onCreateRuleAction(): void {
        this.loggerService.info('DotRuleComponent', 'onCreateRuleAction');
        this.createRuleAction.emit({ payload: { rule: this.rule }, type: RULE_RULE_ACTION_CREATE });
    }

    onDeleteCondition(event: ConditionActionEvent, conditionGroup: ConditionGroupModel): void {
        Object.assign(event.payload, { conditionGroup: conditionGroup, rule: this.rule });
        this.deleteCondition.emit(event);
    }

    onCreateConditionGroupClicked(): void {
        const len = this.rule._conditionGroups.length;
        const priority: number = len ? this.rule._conditionGroups[len - 1].priority : 1;
        this.createConditionGroup.emit({
            payload: { rule: this.rule, priority },
            type: RULE_CONDITION_GROUP_CREATE
        });
    }

    onCreateCondition(event: ConditionActionEvent): void {
        this.loggerService.info('DotRuleComponent', 'onCreateCondition');
        Object.assign(event.payload, { rule: this.rule });
        this.createCondition.emit(event);
    }

    onUpdateRuleActionType(event: {
        type: string;
        payload: { value: string; index: number };
    }): void {
        this.loggerService.info('DotRuleComponent', 'onUpdateRuleActionType');
        this.updateRuleActionType.emit({
            payload: Object.assign({ rule: this.rule }, event.payload),
            type: RULE_RULE_ACTION_UPDATE_TYPE
        });
    }

    onUpdateRuleActionParameter(event): void {
        this.loggerService.info('DotRuleComponent', 'onUpdateRuleActionParameter');
        this.updateRuleActionParameter.emit({
            payload: Object.assign({ rule: this.rule }, event.payload),
            type: RULE_RULE_ACTION_UPDATE_PARAMETER
        });
    }

    onDeleteRuleAction(event: { type: string; payload: { value: string; index: number } }): void {
        this.loggerService.info('DotRuleComponent', 'onDeleteRuleAction');
        this.deleteRuleAction.emit({
            payload: Object.assign({ rule: this.rule }, event.payload),
            type: RULE_RULE_ACTION_DELETE
        });
    }

    onUpdateConditionGroupOperator(
        event: { type: string; payload: { value: string; index: number } },
        conditionGroup: ConditionGroupModel
    ): void {
        this.updateConditionGroupOperator.emit({
            payload: Object.assign(
                { conditionGroup: conditionGroup, rule: this.rule },
                event.payload
            ),
            type: RULE_CONDITION_UPDATE_TYPE
        });
    }

    onUpdateConditionType(
        event: { type: string; payload: { value: string; index: number } },
        conditionGroup: ConditionGroupModel
    ): void {
        this.loggerService.info('DotRuleComponent', 'onUpdateConditionType');
        this.updateConditionType.emit({
            payload: Object.assign(
                { conditionGroup: conditionGroup, rule: this.rule },
                event.payload
            ),
            type: RULE_CONDITION_UPDATE_TYPE
        });
    }

    onUpdateConditionParameter(event, conditionGroup: ConditionGroupModel): void {
        this.loggerService.info('DotRuleComponent', 'onUpdateConditionParameter');
        this.updateConditionParameter.emit({
            payload: Object.assign(
                { conditionGroup: conditionGroup, rule: this.rule },
                event.payload
            ),
            type: RULE_CONDITION_UPDATE_PARAMETER
        });
    }

    onUpdateConditionOperator(event, conditionGroup: ConditionGroupModel): void {
        this.loggerService.info('DotRuleComponent', 'onUpdateConditionOperator');
        this.updateConditionOperator.emit({
            payload: Object.assign(
                { conditionGroup: conditionGroup, rule: this.rule },
                event.payload
            ),
            type: RULE_CONDITION_UPDATE_OPERATOR
        });
    }

    deleteRuleClicked(event: any): void {
        let noWarn = this._user.suppressAlerts || (event.altKey && event.shiftKey);
        if (!noWarn) {
            noWarn = this.ruleActions.length === 1 && !this.ruleActions[0].isPersisted();
            noWarn = noWarn && this.rule._conditionGroups.length === 1;
            if (noWarn) {
                const conditions = this.rule._conditionGroups[0].conditions;
                const keys = Object.keys(conditions);
                noWarn = noWarn && keys.length === 0;
            }
        }

        if (noWarn || confirm('Are you sure you want delete this rule?')) {
            this.deleteRule.emit({ payload: { rule: this.rule }, type: RULE_DELETE });
        }
    }
}

import { Observable, from } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    Component,
    ElementRef,
    ChangeDetectionStrategy,
    inject,
    input,
    output,
    signal,
    effect
} from '@angular/core';
import {
    UntypedFormControl,
    Validators,
    UntypedFormGroup,
    UntypedFormBuilder,
    ReactiveFormsModule,
    FormsModule
} from '@angular/forms';

import { MenuItem, MenuItemCommandEvent } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchChangeEvent, ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime, map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { UserModel, ApiRoot, LoggerService } from '@dotcms/dotcms-js';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { IPublishEnvironment } from '../../services/api/bundle/bundle-service';
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
} from '../../services/api/rule/Rule';
import { ServerSideTypeModel } from '../../services/api/serverside-field/ServerSideFieldModel';
import { I18nService } from '../../services/i18n/i18n.service';
import { DotRuleActionComponent } from '../actions/dot-rule-action.component';
import { DotConditionGroupComponent } from '../conditions/condition-group/dot-condition-group.component';
import {
    ConditionActionEvent,
    RuleActionActionEvent,
    RuleActionEvent,
    ConditionGroupActionEvent
} from '../rule-engine/dot-rule-engine-container.component';

const I18N_BASE = 'api.sites.ruleengine';

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
export class DotRuleComponent {
    private readonly user = inject(UserModel);
    private readonly formBuilder = inject(UntypedFormBuilder);
    private readonly logger = inject(LoggerService);
    readonly elementRef = inject(ElementRef);
    readonly i18nService = inject(I18nService);
    readonly ruleService = inject(RuleService);
    readonly apiRoot = inject(ApiRoot);

    // Inputs
    readonly $rule = input.required<RuleModel>({ alias: 'rule' });
    readonly $saved = input<boolean>(false, { alias: 'saved' });
    readonly $saving = input<boolean>(false, { alias: 'saving' });
    readonly $errors = input<Record<string, string | Error> | null>(null, { alias: 'errors' });
    readonly $ruleActions = input<ActionModel[]>([], { alias: 'ruleActions' });
    readonly $ruleActionTypes = input<Record<string, ServerSideTypeModel>>(
        {},
        { alias: 'ruleActionTypes' }
    );
    readonly $conditionTypes = input<Record<string, ServerSideTypeModel>>(
        {},
        { alias: 'conditionTypes' }
    );
    readonly $environmentStores = input<IPublishEnvironment[]>([], { alias: 'environmentStores' });
    readonly $hidden = input<boolean>(false, { alias: 'hidden' });

    // Outputs - Rule Events
    readonly deleteRule = output<RuleActionEvent>();
    readonly updateExpandedState = output<RuleActionEvent>();
    readonly updateName = output<RuleActionEvent>();
    readonly updateEnabledState = output<RuleActionEvent>();
    readonly updateFireOn = output<RuleActionEvent>();

    // Outputs - Rule Action Events
    readonly createRuleAction = output<RuleActionActionEvent>();
    readonly updateRuleActionType = output<RuleActionActionEvent>();
    readonly updateRuleActionParameter = output<RuleActionActionEvent>();
    readonly deleteRuleAction = output<RuleActionActionEvent>();

    // Outputs - Condition Group Events
    readonly updateConditionGroupOperator = output<ConditionGroupActionEvent>();
    readonly createConditionGroup = output<ConditionGroupActionEvent>();

    // Outputs - Condition Events
    readonly createCondition = output<ConditionActionEvent>();
    readonly deleteCondition = output<ConditionActionEvent>();
    readonly updateConditionType = output<ConditionActionEvent>();
    readonly updateConditionParameter = output<ConditionActionEvent>();
    readonly updateConditionOperator = output<ConditionActionEvent>();
    readonly openPushPublishDialog = output<string>();

    // State
    formModel: UntypedFormGroup;
    readonly fireOnValue = signal('EVERY_PAGE');
    fireOnOptions$: Observable<{ label: string; value: string }[]>;
    fireOnPlaceholder$: Observable<string>;
    readonly showAddToBundleDialog = signal(false);
    hideFireOn: boolean;
    actionTypePlaceholder = '';
    conditionTypePlaceholder = '';
    ruleActionOptions: MenuItem[];
    tooltipRuleOnText: string;
    tooltipRuleOffText: string;

    private readonly enabledStateDelayEmitter = signal<RuleActionEvent | null>(null);
    private readonly i18nCache: Record<string, Observable<string>> = {};

    constructor() {
        this.hideFireOn = document.location.hash.includes('edit-page') || this.apiRoot.hideFireOn;

        this.initializeFormModel();
        this.initializeFireOnOptions();
        this.loadI18nLabels();

        // Handle rule changes
        effect(() => {
            const rule = this.$rule();
            if (rule) {
                this.onRuleChange(rule);
            }
        });

        // Debounce enabled state changes
        effect(() => {
            const event = this.enabledStateDelayEmitter();
            if (event) {
                // Using setTimeout to simulate debounce behavior
                setTimeout(() => {
                    this.updateEnabledState.emit(event);
                }, 20);
            }
        });
    }

    private initializeFormModel(): void {
        const validators = [Validators.required, Validators.minLength(3)];
        this.formModel = this.formBuilder.group({
            name: new UntypedFormControl('', Validators.compose(validators))
        });
    }

    private initializeFireOnOptions(): void {
        const fireOnRawOptions = [
            { label: this.getI18nLabel('inputs.fireOn.options.EveryPage'), value: 'EVERY_PAGE' },
            {
                label: this.getI18nLabel('inputs.fireOn.options.OncePerVisit'),
                value: 'ONCE_PER_VISIT'
            },
            {
                label: this.getI18nLabel('inputs.fireOn.options.OncePerVisitor'),
                value: 'ONCE_PER_VISITOR'
            },
            {
                label: this.getI18nLabel('inputs.fireOn.options.EveryRequest'),
                value: 'EVERY_REQUEST'
            }
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

        this.fireOnPlaceholder$ = this.getI18nLabel('inputs.fireOn.placeholder', 'Select One');
    }

    private loadI18nLabels(): void {
        this.i18nService
            .get('api.sites.ruleengine.rules.inputs.action.type.placeholder')
            .subscribe((label) => {
                this.actionTypePlaceholder = label;
            });

        this.i18nService
            .get('api.sites.ruleengine.rules.inputs.condition.type.placeholder')
            .subscribe((label) => {
                this.conditionTypePlaceholder = label;
            });

        // Load menu options
        this.i18nService
            .get('api.sites.ruleengine.rules.inputs.add_to_bundle.label')
            .subscribe((addToBundleLabel) => {
                this.i18nService
                    .get('api.sites.ruleengine.rules.inputs.deleteRule.label')
                    .subscribe((deleteRuleLabel) => {
                        this.ruleActionOptions = [
                            {
                                label: addToBundleLabel,
                                visible: !this.apiRoot.hideRulePushOptions,
                                command: () => {
                                    this.showAddToBundleDialog.set(true);
                                }
                            },
                            {
                                label: deleteRuleLabel,
                                visible: !this.apiRoot.hideRulePushOptions,
                                command: (event: MenuItemCommandEvent) => {
                                    this.deleteRuleClicked(event.originalEvent);
                                }
                            }
                        ];
                    });
            });

        // Load tooltip texts
        this.i18nService
            .get('api.sites.ruleengine.rules.inputs.onOff.tip')
            .subscribe((tooltipLabel) => {
                this.i18nService
                    .get('api.sites.ruleengine.rules.inputs.onOff.on.label')
                    .subscribe((ruleOnLabel) => {
                        this.i18nService
                            .get('api.sites.ruleengine.rules.inputs.onOff.off.label')
                            .subscribe((ruleOffLabel) => {
                                this.tooltipRuleOnText = `${tooltipLabel} (${ruleOnLabel})`;
                                this.tooltipRuleOffText = `${tooltipLabel} (${ruleOffLabel})`;
                            });
                    });
            });
    }

    private onRuleChange(rule: RuleModel): void {
        const nameControl = this.formModel.controls['name'] as UntypedFormControl;
        nameControl.patchValue(rule.name, {});

        nameControl.valueChanges.pipe(debounceTime(250)).subscribe((name: string) => {
            if (nameControl.valid) {
                this.updateName.emit({
                    payload: { rule: this.$rule(), value: name },
                    type: RULE_UPDATE_NAME
                });
            }
        });

        if (rule.isPersisted()) {
            this.fireOnValue.set(rule.fireOn);
        }
    }

    getI18nLabel(subkey: string, defaultValue = '-missing-'): Observable<string> {
        let cached = this.i18nCache[subkey];
        if (!cached) {
            cached = this.i18nService.get(`${I18N_BASE}.rules.${subkey}`, defaultValue);
            this.i18nCache[subkey] = cached;
        }
        return cached;
    }

    onFireOnChange(value: string): void {
        this.updateFireOn.emit({
            type: 'RULE_UPDATE_FIRE_ON',
            payload: { rule: this.$rule(), value }
        });
    }

    getStatusText(maxLength = 0): string {
        const saved = this.$saved();
        const saving = this.$saving();
        const errors = this.$errors();

        let text = '';
        if (saved) {
            text = 'All changes saved';
        } else if (saving) {
            text = 'Saving...';
        } else if (errors) {
            text =
                (errors['invalid'] as string) ||
                (errors['serverError'] as string) ||
                'Unsaved changes...';
        }

        if (maxLength && text.length > maxLength) {
            text = text.substring(0, maxLength) + '...';
        }

        return text;
    }

    setRuleExpandedState(expanded: boolean): void {
        const rule = this.$rule();
        if (rule.name) {
            this.updateExpandedState.emit({
                payload: { rule, value: expanded },
                type: V_RULE_UPDATE_EXPANDED_STATE
            });
        }
    }

    setRuleEnabledState(event: ToggleSwitchChangeEvent): void {
        this.enabledStateDelayEmitter.set({
            payload: { rule: this.$rule(), value: event.checked },
            type: RULE_UPDATE_ENABLED_STATE
        });
        event.originalEvent.stopPropagation();
    }

    onCreateRuleAction(): void {
        this.logger.info('DotRuleComponent', 'onCreateRuleAction');
        this.createRuleAction.emit({
            payload: { rule: this.$rule() },
            type: RULE_RULE_ACTION_CREATE
        });
    }

    onDeleteCondition(event: ConditionActionEvent, conditionGroup: ConditionGroupModel): void {
        Object.assign(event.payload, { conditionGroup, rule: this.$rule() });
        this.deleteCondition.emit(event);
    }

    onCreateConditionGroupClicked(): void {
        const rule = this.$rule();
        const groupCount = rule._conditionGroups.length;
        const priority: number = groupCount ? rule._conditionGroups[groupCount - 1].priority : 1;
        this.createConditionGroup.emit({
            payload: { rule, priority },
            type: RULE_CONDITION_GROUP_CREATE
        });
    }

    onCreateCondition(event: ConditionActionEvent): void {
        this.logger.info('DotRuleComponent', 'onCreateCondition');
        Object.assign(event.payload, { rule: this.$rule() });
        this.createCondition.emit(event);
    }

    onUpdateRuleActionType(event: {
        type: string;
        payload: { value: string; index: number };
    }): void {
        this.logger.info('DotRuleComponent', 'onUpdateRuleActionType');
        this.updateRuleActionType.emit({
            payload: Object.assign({ rule: this.$rule() }, event.payload),
            type: RULE_RULE_ACTION_UPDATE_TYPE
        });
    }

    onUpdateRuleActionParameter(event): void {
        this.logger.info('DotRuleComponent', 'onUpdateRuleActionParameter');
        this.updateRuleActionParameter.emit({
            payload: Object.assign({ rule: this.$rule() }, event.payload),
            type: RULE_RULE_ACTION_UPDATE_PARAMETER
        });
    }

    onDeleteRuleAction(event: { type: string; payload: { value: string; index: number } }): void {
        this.logger.info('DotRuleComponent', 'onDeleteRuleAction');
        this.deleteRuleAction.emit({
            payload: Object.assign({ rule: this.$rule() }, event.payload),
            type: RULE_RULE_ACTION_DELETE
        });
    }

    onUpdateConditionGroupOperator(
        event: { type: string; payload: { value: string; index: number } },
        conditionGroup: ConditionGroupModel
    ): void {
        this.updateConditionGroupOperator.emit({
            payload: Object.assign({ conditionGroup, rule: this.$rule() }, event.payload),
            type: RULE_CONDITION_UPDATE_TYPE
        });
    }

    onUpdateConditionType(
        event: { type: string; payload: { value: string; index: number } },
        conditionGroup: ConditionGroupModel
    ): void {
        this.logger.info('DotRuleComponent', 'onUpdateConditionType');
        this.updateConditionType.emit({
            payload: Object.assign({ conditionGroup, rule: this.$rule() }, event.payload),
            type: RULE_CONDITION_UPDATE_TYPE
        });
    }

    onUpdateConditionParameter(event, conditionGroup: ConditionGroupModel): void {
        this.logger.info('DotRuleComponent', 'onUpdateConditionParameter');
        this.updateConditionParameter.emit({
            payload: Object.assign({ conditionGroup, rule: this.$rule() }, event.payload),
            type: RULE_CONDITION_UPDATE_PARAMETER
        });
    }

    onUpdateConditionOperator(event, conditionGroup: ConditionGroupModel): void {
        this.logger.info('DotRuleComponent', 'onUpdateConditionOperator');
        this.updateConditionOperator.emit({
            payload: Object.assign({ conditionGroup, rule: this.$rule() }, event.payload),
            type: RULE_CONDITION_UPDATE_OPERATOR
        });
    }

    deleteRuleClicked(event: Event): void {
        const ruleActions = this.$ruleActions();
        const rule = this.$rule();

        let skipWarning =
            this.user.suppressAlerts ||
            ((event as KeyboardEvent)?.altKey && (event as KeyboardEvent)?.shiftKey);

        if (!skipWarning) {
            skipWarning = ruleActions.length === 1 && !ruleActions[0].isPersisted();
            skipWarning = skipWarning && rule._conditionGroups.length === 1;
            if (skipWarning) {
                const conditions = rule._conditionGroups[0].conditions;
                const keys = Object.keys(conditions);
                skipWarning = skipWarning && keys.length === 0;
            }
        }

        if (skipWarning || confirm('Are you sure you want delete this rule?')) {
            this.deleteRule.emit({ payload: { rule }, type: RULE_DELETE });
        }
    }
}

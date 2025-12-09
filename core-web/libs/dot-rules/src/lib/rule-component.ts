import { Observable } from 'rxjs';

import {
    Component,
    EventEmitter,
    ElementRef,
    Input,
    Output,
    ChangeDetectionStrategy,
    inject
} from '@angular/core';
import {
    UntypedFormControl,
    Validators,
    UntypedFormGroup,
    UntypedFormBuilder
} from '@angular/forms';

import { MenuItem } from 'primeng/api';

import { debounceTime } from 'rxjs/operators';

import { UserModel } from '@dotcms/dotcms-js';
import { ApiRoot } from '@dotcms/dotcms-js';
import { LoggerService } from '@dotcms/dotcms-js';

import {
    ConditionActionEvent,
    RuleActionActionEvent,
    RuleActionEvent,
    ConditionGroupActionEvent
} from './rule-engine.container';
import { IPublishEnvironment } from './services/bundle-service';
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
} from './services/Rule';
import { ServerSideTypeModel } from './services/ServerSideFieldModel';
import { I18nService } from './services/system/locale/I18n';

const I8N_BASE = 'api.sites.ruleengine';

@Component({
    changeDetection: ChangeDetectionStrategy.Default,
    selector: 'rule',
    template: `
        <form [formGroup]="formModel" let rf="ngForm">
            <cw-add-to-bundle-dialog-container (close)="showAddToBundleDialog = false"
                [assetId]="rule.key"
                [hidden]="!showAddToBundleDialog" />
            <div
                [class.cw-hidden]="hidden"
                [class.cw-disabled]="!rule.enabled"
                [class.cw-saving]="saving"
                [class.cw-saved]="saved"
                [class.cw-out-of-sync]="!saved && !saving"
                class="cw-rule">
                @if (!hidden) {
                    <div
                        (click)="setRuleExpandedState(!rule._expanded)"
                        class="cw-header"
                        flex
                        layout="row">
                        <div
                            class="cw-header-info"
                            flex="70"
                            layout="row"
                            layout-align="start center">
                            <i
                                [class.pi-angle-right]="!rule._expanded"
                                [class.pi-angle-down]="rule._expanded"
                                class="cw-header-info-arrow pi"
                                aria-hidden="true"></i>
                            <div flex="70" layout="column">
                                <input
                                    (click)="$event.stopPropagation()"
                                    class="cw-rule-name-input"
                                    pInputText
                                    placeholder="{{ rsrc('inputs.name.placeholder') | async }}"
                                    formControlName="name"
                                    dotAutofocus />
                                <div
                                    [hidden]="
                                        !formModel.controls['name'].touched ||
                                        formModel.controls['name'].valid
                                    "
                                    class="name cw-warn basic label"
                                    flex="50">
                                    Name is required
                                </div>
                            </div>
                            @if (!hideFireOn) {
                                <span class="cw-fire-on-label">
                                    {{ rsrc('inputs.fireOn.label') | async }}
                                </span>
                            }
                            @if (!hideFireOn) {
                                <cw-input-dropdown (onDropDownChange)="
                                        updateFireOn.emit({
                                            type: 'RULE_UPDATE_FIRE_ON',
                                            payload: { rule: rule, value: $event }
                                        })
                                    "
                                    (click)="$event.stopPropagation()"
                                    [value]="fireOn.value"
                                    [options]="fireOn.options"
                                    class="cw-fire-on-dropdown"
                                    flex="none"
                                    placeholder="{{
                                        fireOn.placeholder | async
                                    }}" />
                            }
                        </div>
                        <div
                            class="cw-header-actions"
                            flex="30"
                            layout="row"
                            layout-align="end center">
                            <span class="cw-rule-status-text" title="{{ statusText() }}">
                                {{ statusText(30) }}
                            </span>
                            <p-inputSwitch (onChange)="setRuleEnabledState($event)"
                                [(ngModel)]="rule.enabled"
                                [ngModelOptions]="{ standalone: true }"
                                [pTooltip]="rule.enabled ? tooltipRuleOnText : tooltipRuleOffText"
                                tooltipPosition="bottom" />
                            <div class="cw-btn-group">
                                <button
                                    (click)="ruleOptions.toggle($event); $event.stopPropagation()"
                                    class="p-button-secondary"
                                    pButton
                                    icon="pi pi-ellipsis-v"></button>
                                <button
                                    (click)="
                                        onCreateConditionGroupClicked();
                                        setRuleExpandedState(true);
                                        $event.stopPropagation()
                                    "
                                    [disabled]="!rule.isPersisted()"
                                    class="p-button-secondary"
                                    style="margin-left:0.5rem"
                                    pButton
                                    icon="pi pi-plus"
                                    arial-label="Add Group"></button>
                                <p-menu [model]="ruleActionOptions"
                                    #ruleOptions
                                    appendTo="body"
                                    popup="true" />
                            </div>
                        </div>
                    </div>
                }
                @if (rule._expanded) {
                    <div class="cw-accordion-body">
                        @for (group of rule._conditionGroups; track group; let i = $index) {
                            <condition-group (createCondition)="onCreateCondition($event)"
                                (deleteCondition)="onDeleteCondition($event, group)"
                                (updateConditionGroupOperator)="
                                    onUpdateConditionGroupOperator($event, group)
                                "
                                (updateConditionType)="onUpdateConditionType($event, group)"
                                (updateConditionParameter)="
                                    onUpdateConditionParameter($event, group)
                                "
                                (updateConditionOperator)="onUpdateConditionOperator($event, group)"
                                [group]="group"
                                [conditionTypes]="conditionTypes"
                                [groupIndex]="i"
                                [conditionTypePlaceholder]="
                                    conditionTypePlaceholder
                                " />
                        }
                        <div class="cw-action-group">
                            <div class="cw-action-separator">
                                {{ rsrc('inputs.action.firesActions') | async }}
                            </div>
                            <div class="cw-rule-actions" flex layout="column">
                                @for (ruleAction of ruleActions; track ruleAction; let i = $index) {
                                    <div class="cw-action-row" layout="row">
                                        <rule-action (updateRuleActionType)="onUpdateRuleActionType($event)"
                                            (updateRuleActionParameter)="
                                                onUpdateRuleActionParameter($event)
                                            "
                                            (deleteRuleAction)="onDeleteRuleAction($event)"
                                            [action]="ruleAction"
                                            [index]="i"
                                            [actionTypePlaceholder]="actionTypePlaceholder"
                                            [ruleActionTypes]="ruleActionTypes"
                                            flex
                                            layout="row" />
                                        <div class="cw-btn-group cw-add-btn">
                                            @if (i === ruleActions.length - 1) {
                                                <div class="ui basic icon buttons">
                                                    <button
                                                        (click)="onCreateRuleAction()"
                                                        [disabled]="!ruleAction.isPersisted()"
                                                        class="p-button-rounded p-button-success p-button-text"
                                                        pButton
                                                        type="button"
                                                        icon="pi pi-plus"
                                                        arial-label="Add Action"></button>
                                                </div>
                                            }
                                        </div>
                                    </div>
                                }
                            </div>
                        </div>
                    </div>
                }
            </div>
        </form>
    `,
    standalone: false
})
class RuleComponent {
    private _user = inject(UserModel);
    elementRef = inject(ElementRef);
    resources = inject(I18nService);
    ruleService = inject(RuleService);
    apiRoot = inject(ApiRoot);
    private loggerService = inject(LoggerService);

    @Input() rule: RuleModel;
    @Input() saved: boolean;
    @Input() saving: boolean;
    @Input() errors: { [key: string]: any };
    @Input() ruleActions: ActionModel[];
    @Input() ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() conditionTypes: { [key: string]: ServerSideTypeModel };
    @Input() environmentStores: IPublishEnvironment[];

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
    fireOn: any;
    // tslint:disable-next-line:no-unused-variable
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

        this.fireOn = {
            options: [
                { label: this.rsrc('inputs.fireOn.options.EveryPage'), value: 'EVERY_PAGE' },
                { label: this.rsrc('inputs.fireOn.options.OncePerVisit'), value: 'ONCE_PER_VISIT' },
                {
                    label: this.rsrc('inputs.fireOn.options.OncePerVisitor'),
                    value: 'ONCE_PER_VISITOR'
                },
                { label: this.rsrc('inputs.fireOn.options.EveryRequest'), value: 'EVERY_REQUEST' }
            ],
            placeholder: this.rsrc('inputs.fireOn.placeholder', 'Select One'),
            value: 'EVERY_PAGE'
        };
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
                this.fireOn.value = rule.fireOn;
            }
        }
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
        this.loggerService.info('RuleComponent', 'onCreateRuleAction');
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
        this.loggerService.info('RuleComponent', 'onCreateCondition');
        Object.assign(event.payload, { rule: this.rule });
        this.createCondition.emit(event);
    }

    onUpdateRuleActionType(event: {
        type: string;
        payload: { value: string; index: number };
    }): void {
        this.loggerService.info('RuleComponent', 'onUpdateRuleActionType');
        this.updateRuleActionType.emit({
            payload: Object.assign({ rule: this.rule }, event.payload),
            type: RULE_RULE_ACTION_UPDATE_TYPE
        });
    }

    onUpdateRuleActionParameter(event): void {
        this.loggerService.info('RuleComponent', 'onUpdateRuleActionParameter');
        this.updateRuleActionParameter.emit({
            payload: Object.assign({ rule: this.rule }, event.payload),
            type: RULE_RULE_ACTION_UPDATE_PARAMETER
        });
    }

    onDeleteRuleAction(event: { type: string; payload: { value: string; index: number } }): void {
        this.loggerService.info('RuleComponent', 'onDeleteRuleAction');
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
        this.loggerService.info('RuleComponent', 'onUpdateConditionType');
        this.updateConditionType.emit({
            payload: Object.assign(
                { conditionGroup: conditionGroup, rule: this.rule },
                event.payload
            ),
            type: RULE_CONDITION_UPDATE_TYPE
        });
    }

    onUpdateConditionParameter(event, conditionGroup: ConditionGroupModel): void {
        this.loggerService.info('RuleComponent', 'onUpdateConditionParameter');
        this.updateConditionParameter.emit({
            payload: Object.assign(
                { conditionGroup: conditionGroup, rule: this.rule },
                event.payload
            ),
            type: RULE_CONDITION_UPDATE_PARAMETER
        });
    }

    onUpdateConditionOperator(event, conditionGroup: ConditionGroupModel): void {
        this.loggerService.info('RuleComponent', 'onUpdateConditionOperator');
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

export { RuleComponent };

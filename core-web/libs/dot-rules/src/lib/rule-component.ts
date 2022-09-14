import { debounceTime } from 'rxjs/operators';
import {
    Component,
    EventEmitter,
    ElementRef,
    Input,
    Output,
    ChangeDetectionStrategy
} from '@angular/core';
import { UntypedFormControl, Validators, UntypedFormGroup, UntypedFormBuilder } from '@angular/forms';
import { Observable } from 'rxjs';

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

import { I18nService } from './services/system/locale/I18n';
import { UserModel } from '@dotcms/dotcms-js';
import { ApiRoot } from '@dotcms/dotcms-js';
import {
    ConditionActionEvent,
    RuleActionActionEvent,
    RuleActionEvent,
    ConditionGroupActionEvent
} from './rule-engine.container';
import { ServerSideTypeModel } from './services/ServerSideFieldModel';
import { IPublishEnvironment } from './services/bundle-service';
import { LoggerService } from '@dotcms/dotcms-js';
import { MenuItem } from 'primeng/api';

const I8N_BASE = 'api.sites.ruleengine';

@Component({
    changeDetection: ChangeDetectionStrategy.Default,
    selector: 'rule',
    template: `<form [formGroup]="formModel" let rf="ngForm">
        <cw-add-to-bundle-dialog-container
            [assetId]="rule.key"
            [hidden]="!showAddToBundleDialog"
            (close)="showAddToBundleDialog = false"
        ></cw-add-to-bundle-dialog-container>
        <div
            class="cw-rule"
            [class.cw-hidden]="hidden"
            [class.cw-disabled]="!rule.enabled"
            [class.cw-saving]="saving"
            [class.cw-saved]="saved"
            [class.cw-out-of-sync]="!saved && !saving"
        >
            <div
                flex
                layout="row"
                class="cw-header"
                *ngIf="!hidden"
                (click)="setRuleExpandedState(!rule._expanded)"
            >
                <div flex="70" layout="row" layout-align="start center" class="cw-header-info">
                    <i
                        class="cw-header-info-arrow pi"
                        [class.pi-angle-right]="!rule._expanded"
                        [class.pi-angle-down]="rule._expanded"
                        aria-hidden="true"
                    ></i>
                    <div flex="70" layout="column">
                        <input
                            pInputText
                            class="cw-rule-name-input"
                            placeholder="{{ rsrc('inputs.name.placeholder') | async }}"
                            formControlName="name"
                            (click)="$event.stopPropagation()"
                            dotAutofocus
                        />
                        <div
                            flex="50"
                            [hidden]="
                                !formModel.controls['name'].touched ||
                                formModel.controls['name'].valid
                            "
                            class="name cw-warn basic label"
                        >
                            Name is required
                        </div>
                    </div>
                    <span class="cw-fire-on-label" *ngIf="!hideFireOn">{{
                        rsrc('inputs.fireOn.label') | async
                    }}</span>
                    <cw-input-dropdown
                        flex="none"
                        *ngIf="!hideFireOn"
                        class="cw-fire-on-dropdown"
                        [value]="fireOn.value"
                        [options]="fireOn.options"
                        placeholder="{{ fireOn.placeholder | async }}"
                        (onDropDownChange)="
                            updateFireOn.emit({
                                type: 'RULE_UPDATE_FIRE_ON',
                                payload: { rule: rule, value: $event }
                            })
                        "
                        (click)="$event.stopPropagation()"
                    >
                    </cw-input-dropdown>
                </div>
                <div flex="30" layout="row" layout-align="end center" class="cw-header-actions">
                    <span class="cw-rule-status-text" title="{{ statusText() }}">{{
                        statusText(30)
                    }}</span>
                    <p-inputSwitch
                        [(ngModel)]="rule.enabled"
                        (onChange)="setRuleEnabledState($event)"
                        [ngModelOptions]="{ standalone: true }"
                    ></p-inputSwitch>
                    <div class="cw-btn-group">
                        <button
                            pButton
                            class="p-button-secondary"
                            icon="pi pi-ellipsis-v"
                            (click)="ruleOptions.toggle($event); $event.stopPropagation()"
                        ></button>
                        <button
                            style="margin-left:0.5rem"
                            pButton
                            class="p-button-secondary"
                            icon="pi pi-plus"
                            arial-label="Add Group"
                            (click)="
                                onCreateConditionGroupClicked();
                                setRuleExpandedState(true);
                                $event.stopPropagation()
                            "
                            [disabled]="!rule.isPersisted()"
                        ></button>
                        <p-menu
                            #ruleOptions
                            appendTo="body"
                            popup="true"
                            [model]="ruleActionOptions"
                        ></p-menu>
                    </div>
                </div>
            </div>
            <div class="cw-accordion-body" *ngIf="rule._expanded">
                <condition-group
                    *ngFor="let group of rule._conditionGroups; let i = index"
                    [group]="group"
                    [conditionTypes]="conditionTypes"
                    [groupIndex]="i"
                    [conditionTypePlaceholder]="conditionTypePlaceholder"
                    (createCondition)="onCreateCondition($event)"
                    (deleteCondition)="onDeleteCondition($event, group)"
                    (updateConditionGroupOperator)="onUpdateConditionGroupOperator($event, group)"
                    (updateConditionType)="onUpdateConditionType($event, group)"
                    (updateConditionParameter)="onUpdateConditionParameter($event, group)"
                    (updateConditionOperator)="onUpdateConditionOperator($event, group)"
                ></condition-group>
                <div class="cw-action-group">
                    <div class="cw-action-separator">
                        {{ rsrc('inputs.action.firesActions') | async }}
                    </div>
                    <div flex layout="column" class="cw-rule-actions">
                        <div
                            layout="row"
                            class="cw-action-row"
                            *ngFor="let ruleAction of ruleActions; let i = index"
                        >
                            <rule-action
                                flex
                                layout="row"
                                [action]="ruleAction"
                                [index]="i"
                                [actionTypePlaceholder]="actionTypePlaceholder"
                                [ruleActionTypes]="ruleActionTypes"
                                (updateRuleActionType)="onUpdateRuleActionType($event)"
                                (updateRuleActionParameter)="onUpdateRuleActionParameter($event)"
                                (deleteRuleAction)="onDeleteRuleAction($event)"
                            ></rule-action>
                            <div class="cw-btn-group cw-add-btn">
                                <div
                                    class="ui basic icon buttons"
                                    *ngIf="i === ruleActions.length - 1"
                                >
                                    <button
                                        pButton
                                        type="button"
                                        icon="pi pi-plus"
                                        class="p-button-rounded p-button-success p-button-text"
                                        arial-label="Add Action"
                                        (click)="onCreateRuleAction()"
                                        [disabled]="!ruleAction.isPersisted()"
                                    ></button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </form> `
})
class RuleComponent {
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

    private _updateEnabledStateDelay: EventEmitter<{
        type: string;
        payload: { rule: RuleModel; value: boolean };
    }> = new EventEmitter(false);

    private _rsrcCache: { [key: string]: Observable<string> };

    constructor(
        private _user: UserModel,
        public elementRef: ElementRef,
        public resources: I18nService,
        public ruleService: RuleService,
        public apiRoot: ApiRoot,
        fb: UntypedFormBuilder,
        private loggerService: LoggerService
    ) {
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

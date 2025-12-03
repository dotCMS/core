import { EMPTY, Observable, Subject } from 'rxjs';

import { Component, EventEmitter, Input, Output, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { map, switchMap, take, takeUntil } from 'rxjs/operators';

import { DotPushPublishDialogService } from '@dotcms/dotcms-js';

import {
    ConditionActionEvent,
    RuleActionActionEvent,
    RuleActionEvent,
    ConditionGroupActionEvent
} from './rule-engine.container';
import { IPublishEnvironment } from './services/bundle-service';
import { RuleViewService, DotRuleMessage } from './services/dot-view-rule-service';
import { RuleModel, RULE_CREATE } from './services/Rule';
import { ServerSideTypeModel } from './services/ServerSideFieldModel';
import { I18nService } from './services/system/locale/I18n';
import { CwFilter } from './services/util/CwFilter';

const I8N_BASE = 'api.sites.ruleengine';

/**
 *
 */
@Component({
    selector: 'cw-rule-engine',
    template: `
        @if (loading) {
            <div [class.cw-loading]="loading" class="cw-modal-glasspane"></div>
        }
        @if (
            !loading && globalError && globalError.errorKey !== 'dotcms.api.error.license.required'
        ) {
            <div class="ui negative message cw-message">
                <div class="header">{{ globalError.message }}</div>
                <p>{{ rsrc('contact.admin.error') | async }}</p>
                @if (showCloseButton) {
                    <i
                        (click)="globalError.message = ''"
                        class="material-icons"
                        class="close-button">
                        X
                    </i>
                }
            </div>
        }
        @if (
            !loading && globalError && globalError.errorKey === 'dotcms.api.error.license.required'
        ) {
            <dot-not-license />
        }
        @if (!loading && showRules) {
            <div class="cw-rule-engine">
                <div class="cw-header">
                    <div flex layout="row" style="align-items:center">
                        <input
                            (keyup)="filterText = $event.target.value"
                            [value]="filterText"
                            pInputText
                            placeholder="{{ rsrc('inputs.filter.placeholder') | async }}" />
                        <div flex="2"></div>
                        <button (click)="addRule()" class="dot-icon-button">
                            <i class="material-icons">add</i>
                        </button>
                    </div>
                    <div class="cw-filter-links">
                        <span>{{ rsrc('inputs.filter.status.show.label') | async }}:</span>
                        <a
                            (click)="setFieldFilter('enabled', null)"
                            [class.active]="!isFilteringField('enabled')"
                            class="cw-filter-link"
                            href="javascript:void(0)">
                            {{ rsrc('inputs.filter.status.all.label') | async }}
                        </a>
                        <span>&#124;</span>
                        <a
                            (click)="setFieldFilter('enabled', true)"
                            [class.active]="isFilteringField('enabled', true)"
                            class="cw-filter-link"
                            href="javascript:void(0)">
                            {{ rsrc('inputs.filter.status.active.label') | async }}
                        </a>
                        <span>&#124;</span>
                        <a
                            (click)="setFieldFilter('enabled', false)"
                            [class.active]="isFilteringField('enabled', false)"
                            class="cw-filter-link"
                            href="javascript:void(0)">
                            {{ rsrc('inputs.filter.status.inactive.label') | async }}
                        </a>
                    </div>
                </div>
                @if (!rules.length) {
                    <div class="cw-rule-engine__empty">
                        <i class="material-icons">tune</i>
                        <h2>
                            {{ rsrc('inputs.no.rules') | async }}
                            {{
                                rsrc(
                                    pageId && !isContentletHost
                                        ? 'inputs.on.page'
                                        : 'inputs.on.site'
                                ) | async
                            }}{{ rsrc('inputs.add.one.now') | async }}
                        </h2>
                        @if (pageId && !isContentletHost) {
                            <span>
                                {{ rsrc('inputs.page.rules.fired.every.time') | async }}
                            </span>
                        }
                        <button
                            (click)="addRule()"
                            pButton
                            label="{{ rsrc('inputs.addRule.label') | async }}"
                            icon="fa fa-plus"></button>
                    </div>
                }
                @for (rule of rules; track rule) {
                    <rule
                        (updateName)="updateName.emit($event)"
                        (updateFireOn)="updateFireOn.emit($event)"
                        (updateEnabledState)="updateEnabledState.emit($event)"
                        (updateExpandedState)="updateExpandedState.emit($event)"
                        (createRuleAction)="createRuleAction.emit($event)"
                        (updateRuleActionType)="updateRuleActionType.emit($event)"
                        (updateRuleActionParameter)="updateRuleActionParameter.emit($event)"
                        (deleteRuleAction)="deleteRuleAction.emit($event)"
                        (openPushPublishDialog)="showPushPublishDialog($event)"
                        (createCondition)="createCondition.emit($event)"
                        (createConditionGroup)="createConditionGroup.emit($event)"
                        (updateConditionGroupOperator)="updateConditionGroupOperator.emit($event)"
                        (updateConditionType)="updateConditionType.emit($event)"
                        (updateConditionParameter)="updateConditionParameter.emit($event)"
                        (updateConditionOperator)="updateConditionOperator.emit($event)"
                        (deleteCondition)="deleteCondition.emit($event)"
                        (deleteRule)="deleteRule.emit($event)"
                        [rule]="rule"
                        [hidden]="isFiltered(rule) === true"
                        [environmentStores]="environmentStores"
                        [ruleActions]="rule._ruleActions"
                        [ruleActionTypes]="ruleActionTypes"
                        [conditionTypes]="conditionTypes"
                        [saved]="rule._saved"
                        [saving]="rule._saving"
                        [errors]="rule._errors"></rule>
                }
            </div>
        }
    `,
    standalone: false
})
export class RuleEngineComponent implements OnDestroy {
    private ruleViewService = inject(RuleViewService);
    private dotPushPublishDialogService = inject(DotPushPublishDialogService);

    @Input() rules: RuleModel[];
    @Input() ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() loading: boolean;
    @Input() showRules: boolean;
    @Input() pageId: string;
    @Input() isContentletHost: boolean;
    @Input() conditionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() environmentStores: IPublishEnvironment[];

    @Output() createRule: EventEmitter<{ type: string }> = new EventEmitter(false);
    @Output() deleteRule: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateName: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateExpandedState: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateEnabledState: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateFireOn: EventEmitter<RuleActionEvent> = new EventEmitter(false);

    @Output() createRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output() deleteRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output() updateRuleActionType: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output()
    updateRuleActionParameter: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);

    @Output()
    createConditionGroup: EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false);
    @Output()
    updateConditionGroupOperator: EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false);

    @Output() createCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() deleteCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionType: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output()
    updateConditionParameter: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionOperator: EventEmitter<ConditionActionEvent> = new EventEmitter(false);

    globalError: DotRuleMessage;
    showCloseButton: boolean;

    filterText: string;
    status: string;
    activeRules: number;

    private resources: I18nService;
    private _rsrcCache: { [key: string]: Observable<string> };
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private pushPublishTitleLabel = '';

    private readonly route = inject(ActivatedRoute);

    constructor() {
        const resources = inject(I18nService);

        this.resources = resources;
        resources.get(I8N_BASE).subscribe((_rsrc) => {});
        this.filterText = '';
        this.rules = [];
        this._rsrcCache = {};
        this.status = null;

        this.route.data
            .pipe(
                takeUntil(this.destroy$),
                map((x) => x?.haveLicense)
            )
            .subscribe((haveLicense) => {
                if (!haveLicense) {
                    this.globalError = {
                        message: 'push_publish.end_point.license_required_message',
                        allowClose: false,
                        errorKey: 'dotcms.api.error.license.required'
                    };
                }
            });

        this.ruleViewService.message
            .pipe(takeUntil(this.destroy$))
            .subscribe((dotRuleMessage: DotRuleMessage) => {
                this.globalError = dotRuleMessage;
                this.showCloseButton = dotRuleMessage.allowClose;
            });

        this.rsrc('pushPublish.title')
            .pipe(take(1))
            .subscribe((label) => {
                this.pushPublishTitleLabel = label;
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    rsrc(subkey: string): Observable<any> {
        let x = this._rsrcCache[subkey];
        if (!x) {
            x = this.resources.get(I8N_BASE + '.rules.' + subkey);
            this._rsrcCache[subkey] = x;
        }

        return x;
    }

    ngOnChange(change): void {
        if (change.rules) {
            this.updateActiveRuleCount();
        }
    }

    addRule(): void {
        this.createRule.emit({ type: RULE_CREATE });
    }

    updateActiveRuleCount(): void {
        this.activeRules = 0;
        for (let i = 0; i < this.rules.length; i++) {
            if (this.rules[i].enabled) {
                this.activeRules++;
            }
        }
    }

    setFieldFilter(field: string, value: boolean = null): void {
        // remove old status
        const re = new RegExp(field + ':[\\w]*');
        this.filterText = this.filterText.replace(re, ''); // whitespace issues: "blah:foo enabled:false mahRule"
        if (value !== null) {
            this.filterText = field + ':' + value + ' ' + this.filterText;
        }
    }

    isFilteringField(field: string, value: any = null): boolean {
        let isFiltering;
        if (value === null) {
            const re = new RegExp(field + ':[\\w]*');
            isFiltering = this.filterText.match(re) !== null;
        } else {
            isFiltering = this.filterText.indexOf(field + ':' + value) >= 0;
        }

        return isFiltering;
    }

    isFiltered(rule: RuleModel): boolean {
        return CwFilter.isFiltered(rule, this.filterText);
    }

    showPushPublishDialog(ruleKey: any): void {
        this.dotPushPublishDialogService.open({
            assetIdentifier: ruleKey,
            title: this.pushPublishTitleLabel
        });
    }
}

import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, Output, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { take } from 'rxjs/operators';

import { DotPushPublishDialogService } from '@dotcms/dotcms-js';

import { DotRuleComponent } from '../dot-rule/dot-rule.component';
import {
    ConditionActionEvent,
    RuleActionActionEvent,
    RuleActionEvent,
    ConditionGroupActionEvent
} from '../dot-rule-engine-container/dot-rule-engine-container.component';
import { IPublishEnvironment } from '../services/bundle-service';
import { RuleViewService, DotRuleMessage } from '../services/dot-view-rule-service';
import { RuleModel, RULE_CREATE } from '../services/Rule';
import { ServerSideTypeModel } from '../services/ServerSideFieldModel';
import { I18nService } from '../services/system/locale/I18n';
import { CwFilter } from '../services/util/CwFilter';

const I8N_BASE = 'api.sites.ruleengine';

/**
 *
 */
@Component({
    selector: 'dot-rule-engine',
    templateUrl: './dot-rule-engine.component.html',
    imports: [AsyncPipe, ButtonModule, InputTextModule, DotRuleComponent]
})
export class DotRuleEngineComponent {
    private ruleViewService = inject(RuleViewService);
    private dotPushPublishDialogService = inject(DotPushPublishDialogService);

    @Input() rules: RuleModel[] = [];
    @Input() ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() loading = false;
    @Input() showRules = false;
    @Input() pageId = '';
    @Input() isContentletHost = false;
    @Input() conditionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() environmentStores: IPublishEnvironment[] = [];

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

    filterText = '';
    status: string = null;
    activeRules = 0;

    private _rsrcCache: { [key: string]: Observable<string> } = {};

    private pushPublishTitleLabel = '';

    private resources = inject(I18nService);
    private destroy$ = inject(DestroyRef);

    constructor() {
        this.resources.get(I8N_BASE).subscribe(() => {
            // Pre-loading i18n resources
        });

        this.ruleViewService.message
            .pipe(takeUntilDestroyed(this.destroy$))
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

    rsrc(subkey: string): Observable<string> {
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

    isFilteringField(field: string, value: boolean | null = null): boolean {
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

    showPushPublishDialog(ruleKey: string): void {
        this.dotPushPublishDialogService.open({
            assetIdentifier: ruleKey,
            title: this.pushPublishTitleLabel
        });
    }
}

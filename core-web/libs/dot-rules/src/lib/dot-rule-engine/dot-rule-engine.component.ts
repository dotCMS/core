import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
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

const I18N_BASE = 'api.sites.ruleengine';

@Component({
    selector: 'dot-rule-engine',
    templateUrl: './dot-rule-engine.component.html',
    imports: [AsyncPipe, ButtonModule, InputTextModule, DotRuleComponent]
})
export class DotRuleEngineComponent {
    private readonly ruleViewService = inject(RuleViewService);
    private readonly pushPublishService = inject(DotPushPublishDialogService);
    private readonly i18nService = inject(I18nService);
    private readonly destroyRef = inject(DestroyRef);

    // Inputs
    readonly $rules = input<RuleModel[]>([], { alias: 'rules' });
    readonly $ruleActionTypes = input<Record<string, ServerSideTypeModel>>(
        {},
        { alias: 'ruleActionTypes' }
    );
    readonly $loading = input<boolean>(false, { alias: 'loading' });
    readonly $showRules = input<boolean>(false, { alias: 'showRules' });
    readonly $pageId = input<string>('', { alias: 'pageId' });
    readonly $isContentletHost = input<boolean>(false, { alias: 'isContentletHost' });
    readonly $conditionTypes = input<Record<string, ServerSideTypeModel>>(
        {},
        { alias: 'conditionTypes' }
    );
    readonly $environmentStores = input<IPublishEnvironment[]>([], { alias: 'environmentStores' });

    // Outputs - Rule Events
    readonly createRule = output<{ type: string }>();
    readonly deleteRule = output<RuleActionEvent>();
    readonly updateName = output<RuleActionEvent>();
    readonly updateExpandedState = output<RuleActionEvent>();
    readonly updateEnabledState = output<RuleActionEvent>();
    readonly updateFireOn = output<RuleActionEvent>();

    // Outputs - Rule Action Events
    readonly createRuleAction = output<RuleActionActionEvent>();
    readonly deleteRuleAction = output<RuleActionActionEvent>();
    readonly updateRuleActionType = output<RuleActionActionEvent>();
    readonly updateRuleActionParameter = output<RuleActionActionEvent>();

    // Outputs - Condition Group Events
    readonly createConditionGroup = output<ConditionGroupActionEvent>();
    readonly updateConditionGroupOperator = output<ConditionGroupActionEvent>();

    // Outputs - Condition Events
    readonly createCondition = output<ConditionActionEvent>();
    readonly deleteCondition = output<ConditionActionEvent>();
    readonly updateConditionType = output<ConditionActionEvent>();
    readonly updateConditionParameter = output<ConditionActionEvent>();
    readonly updateConditionOperator = output<ConditionActionEvent>();

    // State
    readonly globalError = signal<DotRuleMessage | null>(null);
    readonly showCloseButton = signal(false);
    readonly filterText = signal('');
    readonly status = signal<string | null>(null);
    readonly activeRuleCount = signal(0);

    // i18n cache
    private readonly i18nCache: Record<string, Observable<string>> = {};
    private pushPublishTitleLabel = '';

    constructor() {
        // Pre-load i18n resources
        this.i18nService.get(I18N_BASE).subscribe();

        // Listen for error messages
        this.ruleViewService.message
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((message: DotRuleMessage) => {
                this.globalError.set(message);
                this.showCloseButton.set(message.allowClose);
            });

        // Load push publish title
        this.getI18nLabel('pushPublish.title')
            .pipe(take(1))
            .subscribe((label) => {
                this.pushPublishTitleLabel = label;
            });
    }

    /**
     * Get i18n resource for a given subkey
     */
    getI18nLabel(subkey: string): Observable<string> {
        let cached = this.i18nCache[subkey];
        if (!cached) {
            cached = this.i18nService.get(`${I18N_BASE}.rules.${subkey}`);
            this.i18nCache[subkey] = cached;
        }
        return cached;
    }

    addRule(): void {
        this.createRule.emit({ type: RULE_CREATE });
    }

    updateActiveRuleCount(): void {
        const rules = this.$rules();
        const activeCount = rules.filter((rule) => rule.enabled).length;
        this.activeRuleCount.set(activeCount);
    }

    setFieldFilter(field: string, value: boolean | null = null): void {
        const currentFilter = this.filterText();
        // Remove old status
        const regex = new RegExp(`${field}:[\\w]*`);
        let newFilter = currentFilter.replace(regex, '');

        if (value !== null) {
            newFilter = `${field}:${value} ${newFilter}`;
        }

        this.filterText.set(newFilter);
    }

    isFilteringField(field: string, value: boolean | null = null): boolean {
        const filter = this.filterText();
        if (value === null) {
            const regex = new RegExp(`${field}:[\\w]*`);
            return regex.test(filter);
        }
        return filter.includes(`${field}:${value}`);
    }

    isFiltered(rule: RuleModel): boolean {
        return CwFilter.isFiltered(rule, this.filterText());
    }

    showPushPublishDialog(ruleKey: string): void {
        this.pushPublishService.open({
            assetIdentifier: ruleKey,
            title: this.pushPublishTitleLabel
        });
    }

    clearGlobalError(): void {
        const error = this.globalError();
        if (error) {
            this.globalError.set({ ...error, message: '' });
        }
    }
}

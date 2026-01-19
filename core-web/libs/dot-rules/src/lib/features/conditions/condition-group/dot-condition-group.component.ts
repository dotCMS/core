import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { LoggerService } from '@dotcms/dotcms-js';

import {
    RULE_CONDITION_GROUP_UPDATE_OPERATOR,
    RULE_CONDITION_CREATE,
    ConditionGroupModel,
    ConditionModel
} from '../../../services/api/rule/Rule';
import { ServerSideTypeModel } from '../../../services/api/serverside-field/ServerSideFieldModel';
import { I18nService } from '../../../services/i18n/i18n.service';
import {
    ConditionActionEvent,
    ConditionGroupActionEvent
} from '../../rule-engine/dot-rule-engine-container.component';
import { DotRuleConditionComponent } from '../rule-condition/dot-rule-condition.component';

const I18N_BASE = 'api.sites.ruleengine.rules';

@Component({
    selector: 'dot-condition-group',
    templateUrl: './dot-condition-group.component.html',
    imports: [AsyncPipe, ButtonModule, DotRuleConditionComponent]
})
export class DotConditionGroupComponent {
    private readonly logger = inject(LoggerService);
    private readonly i18nService = inject(I18nService);

    // Inputs
    readonly $group = input.required<ConditionGroupModel, ConditionGroupModel>({
        alias: 'group',
        transform: (value: ConditionGroupModel) => {
            if (value && value._conditions.length === 0) {
                value._conditions.push(new ConditionModel({ _type: new ServerSideTypeModel() }));
            }
            return value;
        }
    });
    readonly $groupIndex = input<number>(0, { alias: 'groupIndex' });
    readonly $conditionTypes = input<Record<string, ServerSideTypeModel>>(
        {},
        { alias: 'conditionTypes' }
    );
    readonly $conditionTypePlaceholder = input<string>('', { alias: 'conditionTypePlaceholder' });

    // Outputs
    readonly deleteConditionGroup = output<ConditionGroupModel>();
    readonly updateConditionGroupOperator = output<ConditionGroupActionEvent>();
    readonly createCondition = output<ConditionActionEvent>();
    readonly deleteCondition = output<ConditionActionEvent>();
    readonly updateConditionType = output<ConditionActionEvent>();
    readonly updateConditionParameter = output<ConditionActionEvent>();
    readonly updateConditionOperator = output<ConditionActionEvent>();

    // i18n cache
    private readonly i18nCache: Record<string, Observable<string>> = {};

    /**
     * Get i18n resource for a given subkey
     */
    getI18nLabel(subkey: string): Observable<string> {
        let cached = this.i18nCache[subkey];
        if (!cached) {
            cached = this.i18nService.get(`${I18N_BASE}.${subkey}`);
            this.i18nCache[subkey] = cached;
        }
        return cached;
    }

    onCreateCondition(): void {
        this.logger.info('DotConditionGroupComponent', 'onCreateCondition');
        this.createCondition.emit({
            payload: {
                conditionGroup: this.$group(),
                index: this.$groupIndex(),
                type: RULE_CONDITION_CREATE
            }
        } as ConditionActionEvent);
    }

    toggleGroupOperator(): void {
        const group = this.$group();
        const newValue = group.operator === 'AND' ? 'OR' : 'AND';
        this.updateConditionGroupOperator.emit({
            payload: {
                conditionGroup: group,
                index: this.$groupIndex(),
                type: RULE_CONDITION_GROUP_UPDATE_OPERATOR,
                value: newValue
            }
        } as ConditionActionEvent);
    }

    trackByCondition(index: number, condition: ConditionModel): string {
        // Include type key to ensure re-render when type changes
        return `${index}-${condition.key || 'new'}-${condition.type?.key || 'NoSelection'}`;
    }
}

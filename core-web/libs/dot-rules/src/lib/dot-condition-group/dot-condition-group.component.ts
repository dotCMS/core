import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotRuleConditionComponent } from '../dot-rule-condition/dot-rule-condition.component';
import {
    ConditionActionEvent,
    ConditionGroupActionEvent
} from '../dot-rule-engine-container/dot-rule-engine-container.component';
import {
    RULE_CONDITION_GROUP_UPDATE_OPERATOR,
    RULE_CONDITION_CREATE,
    ConditionGroupModel,
    ConditionModel
} from '../services/Rule';
import { ServerSideTypeModel } from '../services/ServerSideFieldModel';
import { I18nService } from '../services/system/locale/I18n';

@Component({
    selector: 'dot-condition-group',
    templateUrl: './dot-condition-group.component.html',
    imports: [AsyncPipe, ButtonModule, DotRuleConditionComponent]
})
export class DotConditionGroupComponent {
    private loggerService = inject(LoggerService);

    private static I8N_BASE = 'api.sites.ruleengine.rules';

    $group = input<ConditionGroupModel, ConditionGroupModel>(undefined, {
        alias: 'group',
        transform: (value: ConditionGroupModel) => {
            if (value && value._conditions.length === 0) {
                value._conditions.push(new ConditionModel({ _type: new ServerSideTypeModel() }));
            }

            return value;
        }
    });

    @Input() conditionTypePlaceholder = '';

    @Input() groupIndex = 0;
    @Input() conditionTypes: { [key: string]: ServerSideTypeModel } = {};

    @Output() deleteConditionGroup: EventEmitter<ConditionGroupModel> = new EventEmitter(false);
    @Output()
    updateConditionGroupOperator: EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false);

    @Output() createCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() deleteCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionType: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output()
    updateConditionParameter: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionOperator: EventEmitter<ConditionActionEvent> = new EventEmitter(false);

    private resources: I18nService = inject(I18nService);
    private _rsrcCache: { [key: string]: Observable<string> } = {};

    rsrc(subkey: string): Observable<string> {
        let x = this._rsrcCache[subkey];
        if (!x) {
            x = this.resources.get(DotConditionGroupComponent.I8N_BASE + '.' + subkey);
            this._rsrcCache[subkey] = x;
        }

        return x;
    }

    onCreateCondition(): void {
        this.loggerService.info('DotConditionGroupComponent', 'onCreateCondition');
        this.createCondition.emit(<ConditionActionEvent>{
            payload: {
                conditionGroup: this.$group(),
                index: this.groupIndex,
                type: RULE_CONDITION_CREATE
            }
        });
    }

    toggleGroupOperator(): void {
        // tslint:disable-next-line:prefer-const
        const value = this.$group().operator === 'AND' ? 'OR' : 'AND';
        this.updateConditionGroupOperator.emit(<ConditionActionEvent>{
            payload: {
                conditionGroup: this.$group(),
                index: this.groupIndex,
                type: RULE_CONDITION_GROUP_UPDATE_OPERATOR,
                value: value
            }
        });
    }

    trackByFn(index: number, condition: ConditionModel): string {
        // Include type key to ensure re-render when type changes
        return `${index}-${condition.key || 'new'}-${condition.type?.key || 'NoSelection'}`;
    }
}

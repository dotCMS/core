import { Observable, from, of } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, effect, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';

import { map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotServersideConditionComponent } from '../components/dot-serverside-condition/dot-serverside-condition.component';
import { RuleActionActionEvent } from '../dot-rule-engine-container/dot-rule-engine-container.component';
import {
    RULE_RULE_ACTION_UPDATE_TYPE,
    RULE_RULE_ACTION_UPDATE_PARAMETER,
    RULE_RULE_ACTION_DELETE,
    ActionModel
} from '../services/Rule';
import { ServerSideTypeModel } from '../services/ServerSideFieldModel';

@Component({
    selector: 'dot-rule-action',
    templateUrl: './dot-rule-action.component.html',
    imports: [AsyncPipe, FormsModule, ButtonModule, SelectModule, DotServersideConditionComponent]
})
export class DotRuleActionComponent {
    private readonly logger = inject(LoggerService);

    // Inputs
    readonly $action = input.required<ActionModel>({ alias: 'action' });
    readonly $index = input<number>(0, { alias: 'index' });
    readonly $actionTypePlaceholder = input<string>('', { alias: 'actionTypePlaceholder' });
    readonly $ruleActionTypes = input<Record<string, ServerSideTypeModel>>(
        {},
        { alias: 'ruleActionTypes' }
    );

    // Outputs
    readonly updateRuleActionType = output<RuleActionActionEvent>();
    readonly updateRuleActionParameter = output<RuleActionActionEvent>();
    readonly deleteRuleAction = output<RuleActionActionEvent>();

    // State
    typeDropdownOptions$: Observable<{ label: string; value: string }[]> = of([]);

    constructor() {
        // React to ruleActionTypes changes
        effect(() => {
            const types = this.$ruleActionTypes();
            if (types && Object.keys(types).length > 0) {
                this.buildDropdownOptions(types);
            }
        });
    }

    private buildDropdownOptions(actionTypes: Record<string, ServerSideTypeModel>): void {
        const rawOptions = Object.keys(actionTypes).map((key) => {
            const type = actionTypes[key];
            return {
                label: type._opt.label as Observable<string>,
                value: type._opt.value as string
            };
        });

        this.typeDropdownOptions$ = from(rawOptions).pipe(
            mergeMap((item) => {
                if (item.label && (item.label as Observable<string>).pipe) {
                    return (item.label as Observable<string>).pipe(
                        map((text: string) => ({
                            label: text,
                            value: item.value
                        }))
                    );
                }

                return of({
                    label: item.label as unknown as string,
                    value: item.value
                });
            }),
            toArray(),
            startWith([]),
            shareReplay(1)
        );
    }

    onTypeChange(type: string): void {
        this.logger.info('DotRuleActionComponent', 'onTypeChange', type);
        this.updateRuleActionType.emit({
            type: RULE_RULE_ACTION_UPDATE_TYPE,
            payload: {
                ruleAction: this.$action(),
                value: type,
                index: this.$index()
            }
        });
    }

    onParameterValueChange(change: { name: string; value: string }): void {
        this.logger.info('DotRuleActionComponent', 'onParameterValueChange', change);
        this.updateRuleActionParameter.emit({
            payload: {
                ruleAction: this.$action(),
                name: change.name,
                value: change.value,
                index: this.$index()
            },
            type: RULE_RULE_ACTION_UPDATE_PARAMETER
        });
    }

    onDeleteRuleActionClicked(): void {
        this.deleteRuleAction.emit({
            type: RULE_RULE_ACTION_DELETE,
            payload: {
                ruleAction: this.$action(),
                index: this.$index()
            }
        });
    }
}

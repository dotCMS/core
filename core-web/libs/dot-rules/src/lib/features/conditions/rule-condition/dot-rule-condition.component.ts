import { Observable, from, of } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';

import { map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { LoggerService } from '@dotcms/dotcms-js';

import {
    RULE_CONDITION_UPDATE_PARAMETER,
    RULE_CONDITION_UPDATE_TYPE,
    RULE_CONDITION_DELETE,
    RULE_CONDITION_UPDATE_OPERATOR,
    ConditionModel
} from '../../../services/api/rule/Rule';
import { ServerSideTypeModel } from '../../../services/api/serverside-field/ServerSideFieldModel';
import { DotVisitorsLocationContainerComponent } from '../geolocation/dot-visitors-location-container.component';
import { DotServersideConditionComponent } from '../serverside-condition/dot-serverside-condition.component';

export interface ConditionPayload {
    condition: ConditionModel;
    index?: number;
    name?: string;
    value: string;
}

export interface ConditionEvent {
    type: string;
    payload: ConditionPayload;
}

export interface DeleteConditionEvent {
    type: string;
    payload: { condition: ConditionModel };
}

@Component({
    selector: 'dot-rule-condition',
    templateUrl: './dot-rule-condition.component.html',
    imports: [
        AsyncPipe,
        FormsModule,
        ButtonModule,
        SelectModule,
        DotServersideConditionComponent,
        DotVisitorsLocationContainerComponent
    ]
})
export class DotRuleConditionComponent {
    private readonly logger = inject(LoggerService);

    // Inputs
    readonly $condition = input.required<ConditionModel>({ alias: 'condition' });
    readonly $index = input<number>(0, { alias: 'index' });
    readonly $conditionTypes = input<Record<string, ServerSideTypeModel>>(
        {},
        { alias: 'conditionTypes' }
    );
    readonly $conditionTypePlaceholder = input<string>('', { alias: 'conditionTypePlaceholder' });

    // Outputs
    readonly updateConditionType = output<ConditionEvent>();
    readonly updateConditionParameter = output<ConditionEvent>();
    readonly updateConditionOperator = output<ConditionEvent>();
    readonly deleteCondition = output<DeleteConditionEvent>();

    // State
    readonly typeOptions = signal<{ label: string; value: string }[]>([]);
    typeDropdownOptions$: Observable<{ label: string; value: string }[]> = of([]);

    constructor() {
        // React to conditionTypes changes
        effect(() => {
            const types = this.$conditionTypes();
            if (types && Object.keys(types).length > 0) {
                this.buildDropdownOptions(types);
            }
        });
    }

    private buildDropdownOptions(conditionTypes: Record<string, ServerSideTypeModel>): void {
        const rawOptions = Object.keys(conditionTypes).map((key) => {
            const type = conditionTypes[key];
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
        this.logger.info('DotRuleConditionComponent', 'onTypeChange', type);
        this.updateConditionType.emit({
            payload: {
                condition: this.$condition(),
                value: type,
                index: this.$index()
            },
            type: RULE_CONDITION_UPDATE_TYPE
        });
    }

    onParameterValuesChange(changes: { name: string; value: string }[]): void {
        changes.forEach((change) => this.onParameterValueChange(change));
    }

    onParameterValueChange(change: { name: string; value: string }): void {
        this.logger.info('DotRuleConditionComponent', 'onParameterValueChange');
        this.updateConditionParameter.emit({
            payload: {
                condition: this.$condition(),
                name: change.name,
                value: change.value,
                index: this.$index()
            },
            type: RULE_CONDITION_UPDATE_PARAMETER
        });
    }

    toggleOperator(): void {
        const condition = this.$condition();
        const newOperator = condition.operator === 'AND' ? 'OR' : 'AND';
        this.updateConditionOperator.emit({
            type: RULE_CONDITION_UPDATE_OPERATOR,
            payload: {
                condition: condition,
                value: newOperator,
                index: this.$index()
            }
        });
    }

    onDeleteConditionClicked(): void {
        this.deleteCondition.emit({
            type: RULE_CONDITION_DELETE,
            payload: { condition: this.$condition() }
        });
    }
}

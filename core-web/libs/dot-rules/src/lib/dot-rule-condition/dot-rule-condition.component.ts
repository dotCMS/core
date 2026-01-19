import { Observable, from, of } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    Component,
    EventEmitter,
    Input,
    Output,
    OnChanges,
    SimpleChanges,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';

import { map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotServersideConditionComponent } from '../components/dot-serverside-condition/dot-serverside-condition.component';
import { DotVisitorsLocationContainerComponent } from '../custom-types/visitors-location/dot-visitors-location-container.component';
import {
    RULE_CONDITION_UPDATE_PARAMETER,
    RULE_CONDITION_UPDATE_TYPE,
    RULE_CONDITION_DELETE,
    RULE_CONDITION_UPDATE_OPERATOR,
    ConditionModel
} from '../services/Rule';
import { ServerSideTypeModel } from '../services/ServerSideFieldModel';

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
export class DotRuleConditionComponent implements OnChanges {
    private loggerService = inject(LoggerService);

    @Input() condition: ConditionModel;
    @Input() index = 0;
    @Input() conditionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() conditionTypePlaceholder = '';

    @Output()
    updateConditionType: EventEmitter<{ type: string; payload: Payload }> = new EventEmitter(false);
    @Output()
    updateConditionParameter: EventEmitter<{ type: string; payload: Payload }> = new EventEmitter(
        false
    );
    @Output()
    updateConditionOperator: EventEmitter<{ type: string; payload: Payload }> = new EventEmitter(
        false
    );

    @Output()
    deleteCondition: EventEmitter<{
        type: string;
        payload: { condition: ConditionModel };
    }> = new EventEmitter(false);

    typeDropdownOptions$: Observable<{ label: string; value: string }[]> = of([]);

    ngOnChanges(changes: SimpleChanges): void {
        try {
            if (
                changes.conditionTypes &&
                this.conditionTypes &&
                Object.keys(this.conditionTypes).length > 0
            ) {
                this.buildDropdownOptions();
            }
        } catch (e) {
            this.loggerService.error('DotRuleConditionComponent', 'ngOnChanges', e);
        }
    }

    private buildDropdownOptions(): void {
        const rawOptions = Object.keys(this.conditionTypes).map((key) => {
            const type = this.conditionTypes[key];

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
        this.loggerService.info('DotRuleConditionComponent', 'onTypeChange', type);
        this.updateConditionType.emit({
            payload: { condition: this.condition, value: type, index: this.index },
            type: RULE_CONDITION_UPDATE_TYPE
        });
    }

    onParameterValuesChange(event: { name: string; value: string }[]): void {
        event.forEach((change) => this.onParameterValueChange(change));
    }

    onParameterValueChange(event: { name: string; value: string }): void {
        this.loggerService.info('DotRuleConditionComponent', 'onParameterValueChange');
        this.updateConditionParameter.emit({
            payload: {
                condition: this.condition,
                name: event.name,
                value: event.value,
                index: this.index
            },
            type: RULE_CONDITION_UPDATE_PARAMETER
        });
    }

    toggleOperator(): void {
        const op = this.condition.operator === 'AND' ? 'OR' : 'AND';
        this.updateConditionOperator.emit({
            type: RULE_CONDITION_UPDATE_OPERATOR,
            payload: { condition: this.condition, value: op, index: this.index }
        });
    }

    onDeleteConditionClicked(): void {
        this.deleteCondition.emit({
            type: RULE_CONDITION_DELETE,
            payload: { condition: this.condition }
        });
    }
}

export interface Payload {
    condition: ConditionModel;
    index?: number;
    name?: string;
    value: string;
}

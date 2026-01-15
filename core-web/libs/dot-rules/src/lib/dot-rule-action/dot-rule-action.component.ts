import { Observable, from, of } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';

import { map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotServersideConditionComponent } from '../condition-types/serverside-condition/dot-serverside-condition.component';
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
export class DotRuleActionComponent implements OnChanges {
    private loggerService = inject(LoggerService);

    @Input() action: ActionModel;
    @Input() index = 0;
    @Input() actionTypePlaceholder = '';
    @Input() ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};

    @Output() updateRuleActionType: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output()
    updateRuleActionParameter: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output() deleteRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);

    typeDropdownOptions$: Observable<{ label: string; value: string }[]> = of([]);

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.ruleActionTypes && this.ruleActionTypes && Object.keys(this.ruleActionTypes).length > 0) {
            this.buildDropdownOptions();
        }
    }

    private buildDropdownOptions(): void {
        const rawOptions = Object.keys(this.ruleActionTypes).map((key) => {
            const type = this.ruleActionTypes[key];

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
        this.loggerService.info('DotRuleActionComponent', 'onTypeChange', type);
        this.updateRuleActionType.emit({
            type: RULE_RULE_ACTION_UPDATE_TYPE,
            payload: { ruleAction: this.action, value: type, index: this.index }
        });
    }

    onParameterValueChange(event: { name: string; value: string }): void {
        this.loggerService.info('DotRuleActionComponent', 'onParameterValueChange', event);
        this.updateRuleActionParameter.emit({
            payload: {
                ruleAction: this.action,
                name: event.name,
                value: event.value,
                index: this.index
            },
            type: RULE_RULE_ACTION_UPDATE_PARAMETER
        });
    }

    onDeleteRuleActionClicked(): void {
        this.deleteRuleAction.emit({
            type: RULE_RULE_ACTION_DELETE,
            payload: { ruleAction: this.action, index: this.index }
        });
    }
}

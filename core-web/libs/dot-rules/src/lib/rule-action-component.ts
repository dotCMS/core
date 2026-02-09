import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';

import { LoggerService } from '@dotcms/dotcms-js';

import { RuleActionActionEvent } from './rule-engine.container';
import {
    RULE_RULE_ACTION_UPDATE_TYPE,
    RULE_RULE_ACTION_UPDATE_PARAMETER,
    RULE_RULE_ACTION_DELETE,
    ActionModel
} from './services/Rule';
import { ServerSideTypeModel } from './services/ServerSideFieldModel';

@Component({
    selector: 'rule-action',
    template: `
        @if (typeDropdown !== null) {
            <div flex layout="row" class="cw-rule-action cw-entry">
                <div flex="25" layout="row" class="cw-row-start-area">
                    <cw-input-dropdown
                        (onDropDownChange)="onTypeChange($event)"
                        [value]="action.type?.key"
                        [options]="typeDropdown?.options"
                        flex
                        class="cw-type-dropdown"
                        placeholder="{{ actionTypePlaceholder }}" />
                </div>
                <cw-serverside-condition
                    (parameterValueChange)="onParameterValueChange($event)"
                    [componentInstance]="action"
                    flex="75"
                    class="cw-condition-component" />
                <div class="cw-btn-group cw-delete-btn">
                    <div class="ui basic icon buttons">
                        <button
                            (click)="onDeleteRuleActionClicked()"
                            [disabled]="!action.isPersisted()"
                            pButton
                            type="button"
                            icon="pi pi-trash"
                            class="p-button-rounded p-button-danger p-button-text"></button>
                    </div>
                </div>
            </div>
        }
    `,
    standalone: false
})
export class RuleActionComponent implements OnInit {
    private loggerService = inject(LoggerService);

    @Input() action: ActionModel;
    @Input() index = 0;
    @Input() actionTypePlaceholder: string;
    @Input() ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};

    @Output() updateRuleActionType: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output()
    updateRuleActionParameter: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output() deleteRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);

    typeDropdown: any = null;

    ngOnChanges(change): void {
        if (change.action) {
            if (this.typeDropdown && this.action.type) {
                if (this.action.type.key !== 'NoSelection') {
                    this.typeDropdown.value = this.action.type.key;
                }
            }
        }
    }

    ngOnInit(): void {
        setTimeout(() => {
            this.typeDropdown = {
                options: Object.keys(this.ruleActionTypes).map((key) => {
                    const type = this.ruleActionTypes[key];

                    return {
                        label: type._opt.label,
                        value: type._opt.value
                    };
                })
            };
        }, 0);
    }

    onTypeChange(type: string): void {
        this.loggerService.info('RuleActionComponent', 'onTypeChange', type);
        this.updateRuleActionType.emit({
            type: RULE_RULE_ACTION_UPDATE_TYPE,
            payload: { ruleAction: this.action, value: type, index: this.index }
        });
    }

    onParameterValueChange(event: { name: string; value: string }): void {
        this.loggerService.info('RuleActionComponent', 'onParameterValueChange', event);
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

import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { ServerSideTypeModel } from './services/ServerSideFieldModel';
import { I18nService } from './services/system/locale/I18n';
import {
    RULE_CONDITION_UPDATE_PARAMETER,
    RULE_CONDITION_UPDATE_TYPE,
    RULE_CONDITION_DELETE,
    RULE_CONDITION_UPDATE_OPERATOR,
    ConditionModel
} from './services/Rule';
import { LoggerService } from 'dotcms-js';

@Component({
    selector: 'rule-condition',
    template: `<div *ngIf="typeDropdown != null" flex layout="row" class="cw-condition cw-entry">
  <div class="cw-btn-group cw-condition-toggle">
    <button pButton class="p-button-secondary" aria-label="Swap And/Or" (click)="toggleOperator()" [label]="condition.operator" *ngIf="index !== 0"></button>
  </div>

  <cw-input-dropdown
      [options]="typeDropdown.options"
      flex="25"
      class="cw-type-dropdown"
      [value]="condition.type?.key"
      placeholder="{{conditionTypePlaceholder}}"
      (onDropDownChange)="onTypeChange($event)">

  </cw-input-dropdown>
  <div flex="75" class="cw-condition-row-main" [ngSwitch]="condition.type?.key">
    <ng-template [ngSwitchCase]="'NoSelection'">
      <div class="cw-condition-component"></div>
    </ng-template>
    <ng-template [ngSwitchCase]="'VisitorsGeolocationConditionlet'">
      <cw-visitors-location-container
          [componentInstance]="condition"
          (parameterValuesChange)="onParameterValuesChange($event)"></cw-visitors-location-container>
    </ng-template>
    <ng-template ngSwitchDefault>
      <cw-serverside-condition class="cw-condition-component"
                               [componentInstance]="condition"
                               (parameterValueChange)="onParameterValueChange($event)">
      </cw-serverside-condition>
    </ng-template>
  </div>
</div>
<div class="cw-btn-group cw-delete-btn">
  <div class="ui basic icon buttons">
    <button pButton type="button" icon="pi pi-trash" class="p-button-rounded p-button-danger p-button-text"  aria-label="Delete Condition" (click)="onDeleteConditionClicked()" [disabled]="!condition.isPersisted()">
    </button>
  </div>
</div>
`
})
export class ConditionComponent implements OnInit {
    @Input() condition: ConditionModel;
    @Input() index: number;
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

    typeDropdown: any;

    constructor(private _resources: I18nService, private loggerService: LoggerService) { }

    ngOnInit(): void {
        setTimeout(() => {
            this.typeDropdown = {
                options: Object.keys(this.conditionTypes).map(key => {
                    const type = this.conditionTypes[key];
                        return {
                            label: type._opt.label,
                            value: type._opt.value
                        };
                }),
                placeholder: this._resources.get(
                    'api.sites.ruleengine.rules.inputs.condition.type.placeholder'
                )
            };
        }, 0);
    }

    ngOnChanges(change): void {
        try {
            if (change.condition) {
                if (this.typeDropdown && this.condition.type) {
                    if (this.condition.type.key !== 'NoSelection') {
                        this.typeDropdown.value = this.condition.type.key;
                    }
                }
            }
        } catch (e) {
            this.loggerService.error('ConditionComponent', 'ngOnChanges', e);
        }
    }

    onTypeChange(type: string): void {
        this.loggerService.info('ConditionComponent', 'onTypeChange', type);
        this.updateConditionType.emit({
            payload: { condition: this.condition, value: type, index: this.index },
            type: RULE_CONDITION_UPDATE_TYPE
        });
    }

    onParameterValuesChange(event: { name: string; value: string }[]): void {
        event.forEach(change => this.onParameterValueChange(change));
    }

    onParameterValueChange(event: { name: string; value: string }): void {
        this.loggerService.info('ConditionComponent', 'onParameterValueChange');
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

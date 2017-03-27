import { Component, EventEmitter, Input, Output} from '@angular/core';

import {I18nService} from '../../../api/system/locale/I18n';
import {ServerSideTypeModel} from '../../../api/rule-engine/ServerSideFieldModel';
import {
    RULE_RULE_ACTION_UPDATE_TYPE, RULE_RULE_ACTION_UPDATE_PARAMETER,
    RULE_RULE_ACTION_DELETE, ActionModel
} from '../../../api/rule-engine/Rule';
import {RuleActionActionEvent} from './rule-engine.container';

@Component({
  selector: 'rule-action',
  template: `<div *ngIf="typeDropdown != null" flex layout="row" class="cw-rule-action cw-entry">
  <div flex="25" layout="row" class="cw-row-start-area">
    <cw-input-dropdown
      flex
      class="cw-type-dropdown"
      [value]="action.type?.key"
      placeholder="{{actionTypePlaceholder}}"
      (change)="onTypeChange($event)">
        <cw-input-option
        *ngFor="let opt of typeDropdown.options"
        [value]="opt.value"
        [label]="opt.label"
        icon="{{opt.icon}}"></cw-input-option>
    </cw-input-dropdown>
  </div>
  <cw-serverside-condition flex="75"
                           class="cw-condition-component"
                           [componentInstance]="action"
                           (parameterValueChange)="onParameterValueChange($event)">
  </cw-serverside-condition>
  <div class="cw-btn-group cw-delete-btn">
    <div class="ui basic icon buttons">
      <button class="ui button" aria-label="Delete Action" (click)="onDeleteRuleActionClicked()" [disabled]="!action.isPersisted()">
        <i class="trash icon"></i>
      </button>
    </div>
  </div>
</div>`
})
export class RuleActionComponent {

  @Input() action: ActionModel;
  @Input() index = 0;
  @Input() actionTypePlaceholder: string;
  @Input() ruleActionTypes: {[key: string]: ServerSideTypeModel} = {};

  @Output() updateRuleActionType: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
  @Output() updateRuleActionParameter: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
  @Output() deleteRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);

  @Input() typeDropdown: any;

  constructor(private _resources: I18nService) {}

  ngOnChanges(change): void {
    if (change.ruleActionTypes && !this.typeDropdown) {
      this.typeDropdown = {
        options: []
      };
      Object.keys(this.ruleActionTypes).forEach(key => {
        let type = this.ruleActionTypes[key];
        this.typeDropdown.options.push(type._opt);
      });
    }
    if (change.action) {
      if (this.typeDropdown && this.action.type) {
        if (this.action.type.key !== 'NoSelection') {
          this.typeDropdown.value = this.action.type.key;
        }
      }
    }
  }

  onTypeChange(type: string): void {
    console.log('RuleActionComponent', 'onTypeChange', type);
    this.updateRuleActionType.emit({type: RULE_RULE_ACTION_UPDATE_TYPE, payload: {ruleAction: this.action, value: type, index: this.index}});
  }

  onParameterValueChange(event: {name: string, value: string}): void {
    console.log('RuleActionComponent', 'onParameterValueChange', event);
    this.updateRuleActionParameter.emit({type: RULE_RULE_ACTION_UPDATE_PARAMETER, payload: {ruleAction: this.action, name: event.name, value: event.value, index: this.index}});
  }

  onDeleteRuleActionClicked(): void {
    this.deleteRuleAction.emit({type: RULE_RULE_ACTION_DELETE, payload: {ruleAction: this.action, index: this.index}});
  }
}

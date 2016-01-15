import { Component, Directive, View, Inject, EventEmitter, Input, Output} from 'angular2/core';
import {CORE_DIRECTIVES} from 'angular2/common';
import * as Rx from 'rxjs/Rx'


import {ConditionComponent} from './rule-condition-component';

import {ApiRoot} from '../../../api/persistence/ApiRoot'
import {ConditionGroupService, ConditionGroupModel} from "../../../api/rule-engine/ConditionGroup";
import {ConditionService, ConditionModel} from "../../../api/rule-engine/Condition";
import {RuleModel} from "../../../api/rule-engine/Rule";
import {CwChangeEvent} from "../../../api/util/CwEvent";
import {RuleService} from "../../../api/rule-engine/Rule";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";

@Component({
  selector: 'condition-group'
})
@View({
  template: `<div class="cw-rule-group">
  <div flex class="cw-condition-group-separator" *ngIf="groupIndex === 0">
    {{rsrc.inputs.group.whenConditions.label}}
  </div>
  <div flex class="cw-condition-group-separator" *ngIf="groupIndex !== 0">
    <div class="ui basic icon buttons">
      <button class="ui small button cw-group-operator" (click)="toggleGroupOperator()">
        <div>{{group.operator}}</div>
      </button>
    </div>
    <span flex class="cw-header-text">{{rsrc.inputs.group.whenFurtherConditions.label}}</span>
  </div>
  <div flex layout="column" class="cw-conditions">
    <div layout="row"
         class="cw-condition-row"
         *ngFor="var condition of conditions; var i=index">
      <rule-condition flex layout="row"
                      [condition]="condition"
                      [index]="i"
                      (remove)="onConditionRemove($event)"
                      (change)="onConditionChange($event)"></rule-condition>
      <div class="cw-btn-group cw-add-btn">
        <div class="ui basic icon buttons" *ngIf="i === (conditions.length - 1)">
          <button class="cw-button-add-item ui button" arial-label="Add Condition" (click)="addCondition();" [disabled]="!condition.isPersisted()">
            <i class="plus icon" aria-hidden="true"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</div>

`,
  directives: [CORE_DIRECTIVES, ConditionComponent]
})
export class ConditionGroupComponent {

  @Input() group:ConditionGroupModel
  @Input() groupIndex:number
  @Output() change:EventEmitter<ConditionGroupModel>
  @Output() remove:EventEmitter<ConditionGroupModel>

  conditions:Array<ConditionModel>;
  groupCollapsed:boolean
  rsrc:any

  private apiRoot:ApiRoot
  private _groupService:ConditionGroupService;
  private _conditionService:ConditionService;

  constructor(apiRoot:ApiRoot,
              ruleService:RuleService,
              groupService:ConditionGroupService,
              conditionService:ConditionService) {
    this.change = new EventEmitter()
    this.remove = new EventEmitter()
    this.apiRoot = apiRoot
    this._groupService = groupService;
    this._conditionService = conditionService;
    this.groupCollapsed = false
    this.conditions = []
    this.groupIndex = 0

    this.rsrc = ruleService.rsrc

  }

  ngOnChanges(change) {
    if (change.group) {
      let group:ConditionGroupModel = change.group.currentValue
      let groupKeys = Object.keys(group.conditions)
      if (groupKeys.length === 0) {
        this.addCondition()
      } else {
        this._conditionService.listForGroup(group).subscribe(conditions => {
          console.log("ConditionGroupComponent", "list for group", conditions.length, groupKeys.length, conditions)
          this.conditions = conditions
        })
      }
    }
  }



  addCondition() {
    console.log('Adding condition to ConditionsGroup')
    let condition = new ConditionModel(null, new ServerSideTypeModel())
    condition.priority = this.conditions.length ? this.conditions[this.conditions.length - 1].priority + 1 : 1
    condition.owningGroup = this.group
    condition.operator = 'AND'
    this.conditions.push(condition)
  }

  sort() {
    this.conditions.sort(function (a, b) {
      return a.priority - b.priority;
    });
  }

  _addCondition(condition:ConditionModel) {
    if (condition.isValid()) {
      this._conditionService.add(condition)
    }
  }

  toggleGroupOperator() {
    this.group.operator = this.group.operator === "AND" ? "OR" : "AND"
  }

  onConditionChange(condition:ConditionModel) {
    if (condition.isValid()) {
      if (condition.isPersisted()) {
        this._conditionService.save(condition)
      } else {
        if (!this.group.isPersisted()) {
          this._groupService.add(this.group, (foo) => {
            this._conditionService.add(condition)
          })
        } else {
          this._conditionService.add(condition)
        }
      }
    }
  }

  onConditionRemove(conditionModel:ConditionModel) {

    this._conditionService.remove(conditionModel, () =>{
      this.conditions = this.conditions.filter((aryModel:ConditionModel)=> {
        return aryModel.key != conditionModel.key
      })
      if (this.conditions.length === 0) {
        this.remove.emit(this.group)
      }
    })
  }
}
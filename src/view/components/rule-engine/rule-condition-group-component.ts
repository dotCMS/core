import { Component, EventEmitter, Input, Output} from 'angular2/core';
import {CORE_DIRECTIVES} from 'angular2/common';


import {ConditionComponent} from './rule-condition-component';

import {ApiRoot} from '../../../api/persistence/ApiRoot'
import {ConditionGroupService, ConditionGroupModel} from "../../../api/rule-engine/ConditionGroup";
import {ConditionService, ConditionModel} from "../../../api/rule-engine/Condition";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";
import {I18nService} from "../../../api/system/locale/I18n";
import {Observable} from "rxjs/Observable";

@Component({
  selector: 'condition-group',
  template: `<div class="cw-rule-group">
  <div class="cw-condition-group-separator" *ngIf="groupIndex === 0">
    {{rsrc('inputs.group.whenConditions.label') | async}}
  </div>
  <div class="cw-condition-group-separator" *ngIf="groupIndex !== 0">
    <div class="ui basic icon buttons">
      <button class="ui small button cw-group-operator" (click)="toggleGroupOperator()">
        <div>{{group.operator}}</div>
      </button>
    </div>
    <span flex class="cw-header-text">
    {{rsrc('inputs.group.whenFurtherConditions.label') | async}}</span>
  </div>
  <div flex layout="column" class="cw-conditions">
    <div layout="row"
         class="cw-condition-row"
         *ngFor="var condition of conditions; var i=index">
      <rule-condition flex layout="row"
                      [condition]="condition"
                      [index]="i"
                      (remove)="onConditionRemove($event)"
                      (change)="onConditionChange($event)"
                      (parameterValueChange)="onParamValueChange($event)"
                      ></rule-condition>
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

  private static I8N_BASE:string = 'api.sites.ruleengine.rules'

  @Input() group:ConditionGroupModel
  @Input() groupIndex:number
  @Output() change:EventEmitter<ConditionGroupModel>
  @Output() remove:EventEmitter<ConditionGroupModel>

  conditions:Array<ConditionModel>;
  groupCollapsed:boolean

  private apiRoot:ApiRoot
  private _groupService:ConditionGroupService;
  private _conditionService:ConditionService;
  private resources:I18nService
  private _rsrcCache:{[key:string]:Observable<string>}

  constructor(apiRoot:ApiRoot,
              groupService:ConditionGroupService,
              conditionService:ConditionService,
              resources:I18nService) {
    this.resources = resources
    this._rsrcCache = {}

    this.change = new EventEmitter()
    this.remove = new EventEmitter()
    this.apiRoot = apiRoot
    this._groupService = groupService;
    this._conditionService = conditionService;
    this.groupCollapsed = false
    this.conditions = []
    this.groupIndex = 0

  }
  rsrc(subkey:string) {
    let x = this._rsrcCache[subkey]
    if(!x){
      x = this.resources.get(ConditionGroupComponent.I8N_BASE + '.' + subkey)
      this._rsrcCache[subkey] = x
    }
    return x
  }
  ngOnChanges(change) {
    if (change.group) {
      let group:ConditionGroupModel = change.group.currentValue
      let groupKeys = Object.keys(group.conditions)
      if (groupKeys.length === 0) {
        this.addCondition()
      } else {
        this._conditionService.listForGroup(group).subscribe(conditions => {
          this.conditions = conditions
          this.sort()
        })
      }
    }
  }

  addCondition() {
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
    if (this.group.isPersisted()) {
      this._groupService.save(this.group)
    }
  }

  onParamValueChange(event:{condition:ConditionModel, name:string, value:string, valid:boolean}){
    if(event.valid){
      event.condition.setParameter(event.name, event.value)
      this.onConditionChange(event.condition)
    }
  }

  onConditionChange(condition:ConditionModel) {
    if (condition.isValid()) {
      if (condition.isPersisted()) {
        this._conditionService.save(condition)
      } else {
        if (!this.group.isPersisted()) {
          this._groupService.add(this.group, (foo) => {
            this._conditionService.add(condition, () => {
              this.group.conditions[condition.key] = true
            })
          })
        } else {
          this._conditionService.add(condition, () => {
            this.group.conditions[condition.key] = true
          })
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
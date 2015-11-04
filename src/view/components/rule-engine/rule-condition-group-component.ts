import {NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';
//import * as Rx from '../../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'


import {ConditionComponent} from './rule-condition-component';

import {ApiRoot} from '../../../api/persistence/ApiRoot'
import {ConditionGroupService, ConditionGroupModel} from "../../../api/rule-engine/ConditionGroup";
import {ConditionService, ConditionModel} from "../../../api/rule-engine/Condition";
import {RuleModel} from "../../../api/rule-engine/Rule";
import {CwChangeEvent} from "../../../api/util/CwEvent";

@Component({
  selector: 'condition-group',
  properties: [
    "rule",
    "group",
    "groupIndex"
  ]
})
@View({
  template: `<div flex="grow" layout="column" layout-align="center-start" class="cw-rule-group">
  <div flex="grow" layout="row" layout-align="center-center">
    <div flex layout="row" layout-align="start-center" class="cw-header" *ng-if="groupIndex === 0">
      This rule fires when the following conditions are met:
    </div>
    <div flex layout="row" layout-align="center-center" class="cw-header" *ng-if="groupIndex !== 0">
      <div class="ui basic icon buttons">
        <button class="ui small button cw-group-operator" (click)="toggleGroupOperator()">
          <div (click)="toggleGroupOperator()">{{group.operator}}</div>
        </button>
      </div>
      <span flex class="cw-header-text">when the following condition(s) are met:</span>
    </div>
  </div>
  <div flex layout="column" layout-align="center-center" class="cw-conditions">
    <div flex layout="row" layout-align="center-center" class="cw-conditions" *ng-for="var condition of conditions; var i=index">
      <rule-condition flex layout="row" [condition]="condition" [index]="i"></rule-condition>
      <div class="cw-spacer cw-add-condition" *ng-if="i !== (conditions.length - 1)"></div>
      <div class="cw-btn-group" *ng-if="i === (conditions.length - 1)">
        <div class="ui basic icon buttons">
          <button class="cw-button-add-item ui small basic button" arial-label="Add Condition" (click)="addCondition();" [disabled]="!condition.isPersisted()">
            <i class="plus icon" aria-hidden="true"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</div>

`,
  directives: [ConditionComponent, NgIf, NgFor]
})
export class ConditionGroupComponent {
  groupIndex:number
  _group:ConditionGroupModel
  rule:RuleModel
  conditions:Array<ConditionModel>;
  groupCollapsed:boolean

  private conditionStub:ConditionModel
  private conditionStubWatch:Rx.Subscription<CwChangeEvent<any>>
  private apiRoot:ApiRoot
  private groupService:ConditionGroupService;
  private conditionService:ConditionService;

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot,
              @Inject(ConditionGroupService) groupService:ConditionGroupService,
              @Inject(ConditionService) conditionService:ConditionService) {
    this.apiRoot = apiRoot
    this.groupService = groupService;
    this.conditionService = conditionService;
    this.groupCollapsed = false
    this.conditions = []
    this.groupIndex = 0
    this.conditionService.onAdd.subscribe((conditionModel) => {
      if (conditionModel.owningGroup.key == this._group.key) {
        this.handleAddCondition(conditionModel)

      }
    })
    this.conditionService.onRemove.subscribe((conditionModel) => {
      if (conditionModel.owningGroup.key == this._group.key) {
        this.handleRemoveCondition(conditionModel)
      }
    })
  }

  set group(group:ConditionGroupModel) {
    this._group = group
    let groupKeys = Object.keys(group.conditions)
    if (groupKeys.length == 0) {
      this.addCondition()
    } else {
      this.conditionService.listForGroup(group)
    }
  }

  get group() {
    return this._group;
  }

  addCondition() {
    console.log('Adding condition to ConditionsGroup')
    let condition = new ConditionModel()
    condition.priority = 10
    condition.name = "Condition. " + new Date().toISOString()
    condition.owningGroup = this._group
    condition.comparison = 'is'
    condition.operator = 'AND'
    condition.setParameter('headerKeyValue', '')
    condition.setParameter('compareTo', '')
    condition.setParameter('isoCode', '')

    this.conditionStub = condition
    //noinspection TypeScriptUnresolvedVariable
    this.conditionStubWatch = condition.onChange.subscribe((self)=> {
      if (condition.isValid()) {
        this.conditionService.add(condition)
      }
    })
    this.conditions.push(this.conditionStub)

  }

  toggleGroupOperator() {
    this.group.operator = this.group.operator === "AND" ? "OR" : "AND"
  }

  handleRemoveCondition(conditionModel:ConditionModel) {
    this.conditions = this.conditions.filter((aryModel:ConditionModel)=> {
      return aryModel.key != conditionModel.key
    })
    if (this.conditions.length === 0) {
      this.groupService.remove(this.group)
    }
  }

  handleAddCondition(conditionModel:ConditionModel) {
    if(this.conditionStub && this.conditionStub.key === conditionModel.key){
      this.conditionStub = null
      //noinspection TypeScriptUnresolvedFunction
      this.conditionStubWatch.unsubscribe()
    } else{
      this.conditions.push(conditionModel)
    }
  }
}
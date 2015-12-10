import {NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';
//import * as Rx from '../../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'


import {ConditionComponent} from './rule-condition-component';

import {ApiRoot} from '../../../api/persistence/ApiRoot'
import {ConditionGroupService, ConditionGroupModel} from "../../../api/rule-engine/ConditionGroup";
import {ConditionService, ConditionModel} from "../../../api/rule-engine/Condition";
import {RuleModel} from "../../../api/rule-engine/Rule";
import {CwChangeEvent} from "../../../api/util/CwEvent";
import {RuleService} from "../../../api/rule-engine/Rule";

@Component({
  selector: 'condition-group',
  properties: [
    "rule",
    "group",
    "groupIndex"
  ]
})
@View({
  template: `<div flex layout="column" layout-align="center-start" class="cw-rule-group">
  <div flex="0" layout-fill layout="row" layout-align="start-center">
    <div flex layout="row" layout-align="start-center" class="cw-condition-group-separator" *ng-if="groupIndex === 0">
      {{rsrc.inputs.group.whenConditions.label}}
    </div>
    <div flex layout="row" layout-align="start-center" class="cw-condition-group-separator" *ng-if="groupIndex !== 0">
      <div class="ui basic icon buttons">
        <button class="ui small button cw-group-operator" (click)="toggleGroupOperator()">
          <div >{{group.operator}}</div>
        </button>
      </div>
      <span flex class="cw-header-text">{{rsrc.inputs.group.whenFurtherConditions.label}}</span>
    </div>
  </div>
  <div flex layout-fill layout="column" layout-align="start-start" class="cw-conditions">
    <div layout-fill layout="row" layout-align="space-between-center" class="cw-condition-row" *ng-for="var condition of conditions; var i=index">
      <div flex layout-fill layout="row" layout-align="start-center">
        <rule-condition flex layout-fill layout="row" [condition]="condition" [index]="i"></rule-condition>
      </div>
      <div flex="0" layout="row" layout-align="end-center">
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
  rsrc:any

  private conditionStub:ConditionModel
  private conditionStubWatch:Rx.Subscription<CwChangeEvent<any>>
  private apiRoot:ApiRoot
  private _groupService:ConditionGroupService;
  private _conditionService:ConditionService;

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot,
              @Inject(RuleService) ruleService:RuleService,
              @Inject(ConditionGroupService) groupService:ConditionGroupService,
              @Inject(ConditionService) conditionService:ConditionService) {
    this.apiRoot = apiRoot
    this._groupService = groupService;
    this._conditionService = conditionService;
    this.groupCollapsed = false
    this.conditions = []
    this.groupIndex = 0

    this.rsrc = ruleService.rsrc
    ruleService.onResourceUpdate.subscribe((messages)=>{
      this.rsrc = messages
    })


    this._conditionService.onAdd.subscribe((conditionModel) => {
      if (conditionModel.owningGroup.key == this._group.key) {
        this.handleAddCondition(conditionModel)

      }
    })
    this._conditionService.onRemove.subscribe((conditionModel) => {
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
      this._conditionService.listForGroup(group)
    }

    this._group.onChange.subscribe((event:CwChangeEvent<ConditionGroupModel>)=> {
      if (event.target.isValid() && event.target.isPersisted()) {
        this._groupService.save(event.target)
      }
    })
  }

  get group() {
    return this._group;
  }

  addCondition() {
    console.log('Adding condition to ConditionsGroup')
    let condition = new ConditionModel(null, null)
    condition.priority = this.conditions.length ? this.conditions[this.conditions.length - 1].priority + 1 : 1
    condition.name = "Condition. " + new Date().toISOString()
    condition.owningGroup = this._group
    condition.operator = 'AND'
    condition.setParameter('headerKeyValue', '')
    condition.setParameter('compareTo', '')
    condition.setParameter('isoCode', '')

    this.conditionStub = condition
    //noinspection TypeScriptUnresolvedVariable
    this.conditionStubWatch = condition.onChange.subscribe((self)=> {
      if(this.group.isValid()){
        if (this._group.isPersisted()) {
          this._addCondition(condition)
        } else {
          this._groupService.add(this._group, ()=> {
            this._addCondition(condition)
          })
        }
      }
    })
    this.conditions.push(this.conditionStub)
  }

  _addCondition(condition:ConditionModel){
    if (condition.isValid()) {
      this._conditionService.add(condition)
    }
  }

  toggleGroupOperator() {
    this.group.operator = this.group.operator === "AND" ? "OR" : "AND"
  }

  handleRemoveCondition(conditionModel:ConditionModel) {
    this.conditions = this.conditions.filter((aryModel:ConditionModel)=> {
      return aryModel.key != conditionModel.key
    })
    if (this.conditions.length === 0) {
      this._groupService.remove(this.group)
    }
  }

  handleAddCondition(conditionModel:ConditionModel) {
    if(this.conditionStub && this.conditionStub.key === conditionModel.key){
      this.conditionStub = null
      //noinspection TypeScriptUnresolvedFunction
      this.conditionStubWatch.unsubscribe()
    } else{
      this.conditions.push(conditionModel)
      this.conditions.sort(function (a, b) {
        return a.priority - b.priority;
      });
    }
  }
}
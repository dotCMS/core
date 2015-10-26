/// <reference path="../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />


import {NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';
import {ConditionComponent} from './rule-condition-component';

import {ApiRoot} from 'api/persistence/ApiRoot'

@Component({
  selector: 'condition-group',
  properties: [
    "rule",
    "groupSnap",
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
    <div flex layout="row" layout-align="center-center" class="cw-conditions" *ng-for="var meta of conditions; var i=index">
      <rule-condition flex layout="row" [condition-meta]="meta" [index]="i"></rule-condition>
      <div class="cw-spacer cw-add-condition" *ng-if="i !== (conditions.length - 1)"></div>
      <div class="cw-btn-group" *ng-if="i === (conditions.length - 1)">
        <div class="ui basic icon buttons">
          <button class="cw-button-add-item ui small basic button" arial-label="Add Condition" (click)="addCondition();" >
            <i class="plus icon" aria-hidden="true" (click)="addCondition();"></i>
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
  apiRoot:ApiRoot
  groupIndex:number
  _groupSnap:any
  group:any;
  rule:any;
  groupCollapsed:boolean;
  conditions:Array<any>;
  conditionsRef:any;

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot) {
    this.apiRoot = apiRoot
    this.groupCollapsed = false
    this.conditions = []
    this.groupIndex = 0
    this.conditionsRef = this.apiRoot.defaultSite.child('ruleengine/conditions')
    this.conditionsRef.on('child_added', (conditionSnap) => {
      if (conditionSnap.val().owningGroup == this.groupSnap.key()) {
        this.conditions.push(conditionSnap.ref())
      }
    })
    this.conditionsRef.on('child_removed', (conditionSnap) => {
      if (conditionSnap.val().owningGroup == this.groupSnap.key()) {
        this.conditions = this.conditions.filter((existingSnap) => {
          return existingSnap.key() != conditionSnap.key()
        })
        if (this.conditions.length === 0) {
          this._groupSnap.ref().remove()
        }
      }
    })
  }

  set groupSnap(groupSnap) {
    console.log('Setting ConditionGroup snapshot: ', groupSnap.key())
    this._groupSnap = groupSnap
    this.group = groupSnap.val()
    this.getConditions()
  }

  get groupSnap() {
    return this._groupSnap;
  }

  getConditions() {
    let conditionMetas = []
    let conditionSnap = this.groupSnap.child('conditions')
    conditionSnap.forEach((childSnap) => {
      let key = childSnap.key()
      var ref = this.conditionsRef.child(key);
      conditionMetas.push(ref)
      ref.once('value', (snap)=> {
      });
    })
    return conditionMetas
  }

  addCondition() {
    console.log('Adding condition to ConditionsGroup')
    let condition = {
      priority: 10,
      name: "Condition. " + new Date().toISOString(),
      owningGroup: this._groupSnap.key(),
      conditionlet: 'UsersBrowserHeaderConditionlet',
      comparison: 'is',
      operator: 'AND',
      values: {
        headerKeyValue: {
          id: 'fakeId',
          key: 'headerKeyValue',
          value: '',
          priority: 10
        },
        compareTo: {
          id: 'fakeId2',
          key: 'compareTo',
          value: '',
          priority: 1
        },
        isoCode: {
          id: 'fakeId3',
          key: 'isoCode',
          value: '',
          priority: 1
        }
      }
    }

    this.conditionsRef.push(condition, (result) => {
      this.group.conditions = this.group.conditions || {}
      this.group.conditions[result.key()] = true
      this.updateGroup()
    })
  }

  toggleGroupOperator() {
    this.group.operator = this.group.operator === "AND" ? "OR" : "AND"
    this.updateGroup()
  }

  updateGroup() {
    console.log('Updating ConditionsGroup')
    this.groupSnap.ref().set(this.group)
  }

}
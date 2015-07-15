/// <reference path="../../../typings/dotcms/dotcms-core-web.d.ts" />
/// <reference path="../../../typings/entity-forge/entity-forge.d.ts" />
import XDebug from 'debug';
let log = XDebug('RuleEngineView.ConditionGroupComponent');

import {For, If} from 'angular2/angular2';

import {Component, Directive} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';

import {ConditionComponent} from './condition-component.ts';

@Component({
  selector: 'condition-group',
  properties: {
    "rule": "rule",
    "groupSnap": "group-snap"
  }
})
@View({
  template: RuleEngine.templates.conditionGroupTemplate,
  directives: [ConditionComponent, If, For]
})
export class ConditionGroupComponent {
  _groupSnap:any;
  group:any;
  rule:any;
  groupCollapsed:boolean;
  conditions:Array<any>;

  constructor() {
    log('Creating ConditionGroupComponent')
    this.groupCollapsed = false
    this.conditions = []
  }

  set groupSnap(groupSnap) {
    log('Setting ConditionGroup snapshot: ', groupSnap.key())
    this._groupSnap = groupSnap
    this.group = groupSnap.val()
    this.conditions = this.getConditions()
  }

  get groupSnap() {
    return this._groupSnap;
  }

  getConditions() {
    let referenceSnaps = []
    let conditionMetas = []
    this.groupSnap.child('conditions').forEach((childSnap) => {
      let key = childSnap.key()
      let childMeta = childSnap['entityMeta']
      referenceSnaps.push(key) // the snap value is 'true', as this is a reference.
      conditionMetas.push(new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/ruleengine/conditions/' + key))
    })
    return conditionMetas
  }

  addCondition() {
    log('Adding condition to ConditionsGroup')
    let condition = {
      priority: 10,
      name: "Condition. " + new Date().toISOString(),
      owningGroup: this._groupSnap.key(),
      conditionlet: 'UsersCountryConditionlet',
      comparison: 'Is',
      operator: 'AND',
      values: {
        a: {
          id: 'a',
          value: 'US',
          priority: 10
        }
      }
    }
    let condRoot:EntityMeta = new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/ruleengine/conditions')

    condRoot.push(condition).then((result) => {
      this.group.conditions = this.group.conditions || {}
      this.group.conditions[result.key()] = true
      this.updateGroup()
    }).catch((e) => {
      log(e)
    })
  }

  toggleOperator() {
    this.group.operator = this.group.operator === "AND" ? "OR" : "AND"
    this.updateGroup()
  }

  updateGroup() {
    log('Updating ConditionsGroup')
    this.groupSnap.ref().set(this.group)
    this.conditions = this.getConditions()
  }

}
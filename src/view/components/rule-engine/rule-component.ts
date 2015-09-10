/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />

import {NgFor, NgIf, Component, Directive, View} from 'angular2/angular2';

import {RuleActionComponent} from './rule-action-component';
import {ConditionGroupComponent} from './rule-condition-group-component';

import {ruleTemplate} from './templates/index'

@Component({
  selector: 'rule',
  properties: ["ruleSnap"]
})
@View({
  template: ruleTemplate,
  directives: [RuleActionComponent, ConditionGroupComponent, NgIf, NgFor]
})
class RuleComponent {
  rule:any;
  _ruleSnap:any;
  collapsed:boolean;
  fireOnDropDownExpanded:boolean;
  ruleGroups:Array<any>;
  ruleActions:Array<any>;
  groupsSnap:EntitySnapshot;

  constructor() {
    console.log('Creating RuleComponent')
    this.collapsed = true
    this.fireOnDropDownExpanded = false
    this.ruleGroups = []
    this.ruleActions = []
  }

  set ruleSnap(ruleSnap:any) {
    console.log('Setting Rule snapshot')
    this._ruleSnap = ruleSnap
    this.rule = ruleSnap.val()
    this.ruleGroups = []
    this.groupsSnap = this.ruleSnap.child('conditionGroups')
    this.groupsSnap.forEach((childSnap) => {
      this.ruleGroups.push(childSnap)
    })
    this.groupsSnap.ref().on('child_added', (childSnap)=>{
      this.ruleGroups.push(childSnap)
    })
    this.groupsSnap.ref().on('child_removed', (childGroupSnap)=>{
      this.ruleGroups = this.ruleGroups.filter((group) => {
        return group.key() != childGroupSnap.key()
      })
    })

    this.ruleActions = this.getRuleActions()
  }

  getRuleActions() {
    let actionMetas = []
    let actionsSnap = this.ruleSnap.child('ruleActions')
    if (actionsSnap.exists()) {
      actionsSnap.forEach((childSnap:EntitySnapshot) => {
        let key = childSnap.key()
        actionMetas.push(new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/ruleengine/ruleActions/' + key))
      })
    }
    return actionMetas
  }

  get ruleSnap():any {
    return this._ruleSnap
  }

  setFireOn(value:string) {
    this.rule.fireOn = value
    this.fireOnDropDownExpanded = false
    this.updateRule()
  }

  toggleEnabled() {
    this.rule.enabled = !this.rule.enabled
    this.updateRule()
  }

  setRuleName(name:string) {
    this.rule.name = name
    this.updateRule()
  }

  addGroup() {
    let group = {
      priority: 10,
      operator: 'AND'
    }

    this.ruleSnap.ref().child('conditionGroups').push(group).then((snapshot) => {
      let group = snapshot['val']()
      this.rule.conditionGroups[snapshot.key()] = group
      group.conditions = group.conditions || {}
      this.updateRule().then(()=> this.addCondition(snapshot) )
    }).catch((e) => {
      console.log(e)
    })
  }

  addCondition(groupSnap) {
    console.log('Adding condition to new condition group')
    let group = groupSnap.val()
    let condition = {
      priority: 10,
      name: "Condition. " + new Date().toISOString(),
      owningGroup: groupSnap.key(),
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
      group.conditions = group.conditions || {}
      group.conditions[result.key()] = true
      groupSnap.ref().set(group)
    }).catch((e) => {
      console.log(e)
    })
  }

  addRuleAction() {
    let action = {
      name: "CoreWeb created this action: " + new Date().toISOString(),
      priority: 10,
      owningRule: this.ruleSnap.key(),
      actionlet: 'CountRequestsActionlet'
    }
    let actionRoot:EntityMeta = new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/ruleengine/ruleActions')

    actionRoot.push(action).then((snapshot)=> {
      this.rule.actions = this.rule.ruleActions || {}
      this.rule.actions[snapshot.key()] = true
      this.updateRule()
    })
  }


  removeRule() {
    this.ruleSnap.ref().remove().catch((e) => {
      console.log("Error removing rule", e)
      throw e
    })
  }

  updateRule() {
    console.log('Updating Rule')
    return this.ruleSnap.ref().set(this.rule)
  }

}

export {RuleComponent}
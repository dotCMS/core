/// <reference path="../../../typings/es6/lib.es6.d.ts" />

/// <reference path="../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../typings/dotcms/dotcms-core-web.d.ts" />
/// <reference path="../../../typings/entity-forge/entity-forge.d.ts" />

import {NgFor, NgIf, Component, Directive, View} from 'angular2/angular2';

import {RuleActionComponent} from './rule-action-component';
import {ConditionGroupComponent} from './rule-condition-group-component';

import ruleTemplate from './templates/rule-component.tpl.html!text'

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

  setRuleName(name:string) {
    this.rule.name = name
    this.updateRule()
  }

  addGroup() {
    let group = {
      priority: 10,
      operator: 'OR'
    }

    this.ruleSnap.ref().child('conditionGroups').push(group).then((snapshot) => {
      let group = snapshot['val']()
      this.rule.conditionGroups[snapshot.key()] = group
      group.conditions = group.conditions || {}
      this.updateRule()
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
    this.ruleSnap.ref().set(this.rule)
    this.ruleActions = this.getRuleActions()
  }

}

export {RuleComponent}
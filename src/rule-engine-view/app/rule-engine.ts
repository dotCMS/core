import XDebug from 'debug';
let log = XDebug('RuleEngineView.RuleEngineComponent');


import {bootstrap, For, If} from 'angular2/angular2';

import {Component, Directive} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';

import {FormBuilder, Validators, ControlGroup} from 'angular2/forms';

import jsonp from 'jsonp'

import {Core, ServerManager, EntityMeta} from '../../coreweb/index.js'
import {RuleEngineAPI, ruleRepo, ruleGroupRepo, Rule, RuleGroup, RuleStore, actions} from '../../rule-engine/index.js';

import "bootstrap/css/bootstrap.css!";
import "./styles/rule-engine.css!";
import "./styles/theme-dark.css!";

import ruleEngineTemplate from './rule-engine.tpl.html!text'
import ruleTemplate from './rule.tpl.html!text'
import ruleActionTemplate from './rule-action.tpl.html!text'
import conditionGroupTemplate from './condition-group.tpl.html!text'
import conditionTemplate from './condition.tpl.html!text'


@Component({
  selector: 'rule-condition',
  properties: {
    "conditionSnap": "condition-snap"
  }
})
@View({
  template: conditionTemplate,
  directives: [If, For]
})
class ConditionComponent {
  _conditionMeta:any;
  conditionSnap:any;
  condition:any;
  clauseTypes:Array;
  comparisons:Array;

  constructor() {
    this.clauseTypes = [{id: 'hello', text: 'foo'}, {id: 'goodbye', text: 'bar'}]
    this.comparisons = [{id: 'compHello', text: 'cFoo'}, {id: 'compGoodbye', text: 'cBar'}];
    this.condition = {}
  }

  set conditionMeta(conditionMeta) {
    this._conditionMeta = conditionMeta
    conditionMeta.once('value', (snap)=>{
      this.condition = snap.val()
      this.conditionSnap = snap
    })
  }

  get conditionMeta() {
    return this._conditionMeta;
  }


  removeCondition() {
    //ClauseActionCreators.removeClause(this._clause)
  }

  toggleOperator() {
    //ClauseActionCreators.toggleOperator(this._clause)
  }

  updateCondition() {
    this.conditionSnap.ref().set(this.condition)
  }

}


@Component({
  selector: 'condition-group',
  properties: {
    "rule": "rule",
    "groupSnap": "group-snap"
  }
})
@View({
  template: conditionGroupTemplate,
  directives: [ConditionComponent, If, For]
})
class ConditionGroupComponent {
  _groupSnap:any;
  group:any;
  rule:any;
  groupCollapsed:boolean;

  constructor() {
    this.groupCollapsed = false;
  }

  set groupSnap(groupSnap) {
    this._groupSnap = groupSnap
    this.group = groupSnap.val()
  }

  get groupSnap() {
    return this._groupSnap;
  }

  getConditions(){
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
    let condition = {
      priority: 10,

      name: "Condition. " + new Date().toISOString(),
      rule: this.rule.key,
      conditionGroup: this.groupSnap.key(),
      conditionlet: 'bob',
      comparison: 'Is',
      operator: 'AND',
      values: {
        a: {
          id: 'a',
          value: 'something',
          priority: 10
        }
      }
    }
    debugger;
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
    this.groupSnap.ref().set(this.group)
  }

}


@Component({
  selector: 'rule-action',
  properties: {
    "actionSnap": "action-snap",
    "ruleAction": "rule-action",
    "index": "index"
  }
})
@View({
  template: ruleActionTemplate,
  directives: [If, For],
})
class RuleActionComponent {
  _actionSnap:any;
  ruleAction:any;
  index:number;

  constructor() {
  }

  get actionSnap():any {
    return this._actionSnap;
  }

  set actionSnap(actionSnap:any) {
    this._actionSnap = actionSnap
    this.ruleAction = actionSnap.val();
  }


  saveChanges() {
    //RuleActionActionCreators.updateRuleAction(this._ruleAction)
  }

  onChange(action) {
  }

  removeRuleAction() {
    //RuleActionActionCreators.removeRuleAction(this._ruleAction.$key)
  }
}


@Component({
  selector: 'rule',
  properties: {
    "ruleSnap": "rule-snap"
  },
  injectables: [
    FormBuilder
  ]
})
@View({
  template: ruleTemplate,
  directives: [ConditionGroupComponent, If, For]
})
class RuleComponent {
  rule:any;
  _ruleSnap:any;
  collapsed:boolean;
  fireOnDropDownExpanded:boolean;

  constructor() {
    log('init RuleComponent')
    this.collapsed = true
    this.fireOnDropDownExpanded = false
  }

  set ruleSnap(ruleSnap:any) {
    this._ruleSnap = ruleSnap
    this.rule = ruleSnap.val()
  }

  get ruleSnap():any {
    return this._ruleSnap
  }

  getRuleGroups() {
    let groupSnaps = []
    this.ruleSnap.child('conditionGroups').forEach((childSnap) => {
      groupSnaps.push(childSnap)
    })
    return groupSnaps
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

    this.ruleSnap.ref().child('conditiongroups').push(group).then((snapshot) => {
      let group = snapshot['val']()
      this.rule.conditionGroups[snapshot.key()] = group
      group.conditions = group.conditions || {}
      this.updateRule()
    }).catch((e) => {
      log(e)
    })
  }

  addRuleAction() {
    let action = {
      name: "CoreWeb created this action: " + new Date().toISOString(),
      priority: 10,
      actionlet: 'Set user variable'
    }
  }


  removeRule() {
    this.ruleSnap.ref().remove().then((x) => {
      actions.update(x)
    }).catch((e) => {
      log("Not yay :~(: ", e)
      throw e
    })
  }

  updateRule() {
    this.ruleSnap.ref().set(this.rule)
  }

}


@Component({
  selector: 'rule-engine'
})
@View({
  template: ruleEngineTemplate,
  directives: [For, RuleComponent, If]
})
class RuleEngine {
  rules:Array;
  baseUrl:string;
  rulesEntityMeta:any;

  constructor() {
    this.rules = []
    this.baseUrl = ServerManager.baseUrl;
    this.rulesEntityMeta = new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/rules')
    log("creating rules engine");
    this.onChange()
    RuleStore.addChangeListener(this.onChange.bind(this))
  }

  updateBaseUrl(value) {
    let oldUrl = ServerManager.baseUrl
    ServerManager.baseUrl = value;
    this.baseUrl = value;
    this.testBaseUrl(value).catch((e => {
      alert("Error using provided Base Url. Check the development console.");
      log("Error using provided Base Url: ", e)
      this.baseUrl = oldUrl;
      ServerManager.baseUrl = oldUrl
      throw e
    }))

  }

  onChange(event = null) {
    log("Handling change event: ", event)
    this.rulesEntityMeta.once('value', (result) => {
      this.rules = []
      if (result && result.forEach) {
        result.forEach((ruleSnap) => {
          this.rules.push(ruleSnap)
        })
      }
      else {
        throw result
      }
    })
  }

  addRule() {
    let testRule = new Rule();
    testRule.name = "CoreWeb created this rule. " + new Date().toISOString()
    testRule.enabled = true
    testRule.priority = 10
    testRule.fireOn = "EVERY_PAGE"
    testRule.shortCircuit = false
    testRule.conditionGroups = {}
    testRule.actions = {}
    ruleRepo.push(testRule).then(()=> this.onChange())

  }

  testBaseUrl(baseUrl) {
    return new Promise((resolve, reject) => {
      RuleStore.init();
    })
  }
}

export function main() {
  log("Bootstrapping rules engine")
  return bootstrap(RuleEngine);
}

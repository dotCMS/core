/// <reference path="../../../typings/dotcms/dotcms-core-web.d.ts" />
/// <reference path="../../../typings/entity-forge/entity-forge.d.ts" />


import XDebug from 'debug';
let log = XDebug('RuleEngineView.RuleEngineComponent');


import {bootstrap, For, If} from 'angular2/angular2';

import {Component, Directive} from 'angular2/src/core/annotations_impl/annotations';
import {View} from 'angular2/src/core/annotations_impl/view';
import jsonp from 'jsonp'

import {RuleActionComponent, initActionlets} from './rule-action-component.ts';

var templates:any;


let count = 0;
@Component({
  selector: 'rule-condition',
  properties: {
    "conditionMeta": "condition-meta"
  }
})
@View({
  template: templates.conditionTemplate,
  directives: [If, For]
})
class ConditionComponent {
  idCount:number;
  _conditionMeta:any;
  condition:any;
  conditionValue:string;
  conditionlet:any;
  conditionlets:Array<any>;

  constructor() {
    this.idCount = count;
    log('Creating ConditionComponent: ', count++)
    this.conditionlets = []
    conditionletsPromise.then(()=>{
      this.conditionlets = conditionletsAry
    })
    this.condition = {}
    this.conditionValue = ''
    this.conditionlet = {}

  }

  onChange(snapshot){
    log(this.idCount, " Condition's type is ", this.condition);
    this.condition = snapshot.val()
    this.conditionlet = conditionletsMap.get(this.condition.conditionlet)
    this.conditionValue = this.getComparisonValue()
  }


  set conditionMeta(conditionMeta) {
    log(this.idCount, " Setting conditionMeta: ", conditionMeta.key())
    this._conditionMeta = conditionMeta
    conditionMeta.once('value', this.onChange.bind(this))
  }

  get conditionMeta() {
    return this._conditionMeta;
  }

  setConditionlet(condtitionletId){
    log('Setting conditionlet id to: ', condtitionletId)
    this.condition.conditionlet = condtitionletId
    this.conditionlet =  conditionletsMap.get(this.condition.conditionlet)
    this.updateCondition()
  }

  setComparison(comparisonId){
    log('Setting conditionlet comparison id to: ', comparisonId)
    this.condition.comparison = comparisonId
    this.updateCondition()

  }

  getComparisonValueKey(){
    let key = null
    let keys = Object.keys(this.condition.values)
    if(keys.length){
      key = keys[0]
    }
    return key
  }

  getComparisonValue(){
    let value = ''
    let key = this.getComparisonValueKey()
    if(key) {
      value = this.condition.values[key].value
    }
    return value
  }

  setComparisonValue(newValue){
    let key = this.getComparisonValueKey() || 'aFakeId'
    this.condition.values[key] = {id:key, priority: 10, value: newValue}
    this.updateCondition()
  }


  toggleOperator() {
    //ClauseActionCreators.toggleOperator(this._clause)
  }

  updateCondition() {
    log('Updating Condition: ', this.condition)
    this.conditionMeta.set(this.condition)
  }

  removeCondition() {
    log('Removing Condition: ', this.condition)
    this.conditionMeta.remove()
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
  template: templates.conditionGroupTemplate,
  directives: [ConditionComponent, If, For]
})
class ConditionGroupComponent {
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

@Component({
  selector: 'rule',
  properties: {
    "ruleSnap": "rule-snap"
  }
})
@View({
  template: templates.ruleTemplate,
  directives: [RuleActionComponent, ConditionGroupComponent, If, For]
})
class RuleComponent {
  rule:any;
  _ruleSnap:any;
  collapsed:boolean;
  fireOnDropDownExpanded:boolean;
  ruleGroups:Array<any>;
  ruleActions:Array<any>;


  constructor() {
    log('Creating RuleComponent')
    this.collapsed = true
    this.fireOnDropDownExpanded = false
    this.ruleGroups = []
    this.ruleActions = []

  }

  set ruleSnap(ruleSnap:any) {
    log('Setting Rule snapshot')
    this._ruleSnap = ruleSnap
    this.rule = ruleSnap.val()
    this.ruleGroups = []
    this.ruleSnap.child('conditionGroups').forEach((childSnap) => {
      this.ruleGroups.push(childSnap)
    })
    this.ruleActions = this.getRuleActions()

  }

  getRuleActions(){
    let actionMetas = []
    let actionsSnap = this.ruleSnap.child('ruleActions')
    if (actionsSnap.exists()) {
      actionsSnap.forEach((childSnap) => {
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
      owningRule: this.ruleSnap.key(),
      actionlet: 'CountRequestsActionlet'
    }
    let actionRoot:EntityMeta = new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/ruleengine/ruleActions')

    actionRoot.push(action).then((snapshot)=>{
      this.rule.actions = this.rule.ruleActions || {}
      this.rule.actions[snapshot.key()] = true
      this.updateRule()
    })
  }


  removeRule() {
    this.ruleSnap.ref().remove().then((x) => {
      // update
    }).catch((e) => {
      log("Not yay :~(: ", e)
      throw e
    })
  }

  updateRule() {
    log('Updating Rule')
    this.ruleSnap.ref().set(this.rule)
    this.ruleActions = this.getRuleActions()
  }

}


@Component({
  selector: 'rule-engine'
})
@View({
  template: templates.ruleEngineTemplate,
  directives: [For, RuleComponent, If]
})
class RuleEngineComponent {
  rules:Array<any>;
  baseUrl:string;
  rulesRef:EntityMeta;

  constructor() {
    log('Creating RuleEngine component.')
    this.rules = []
    this.baseUrl = ConnectionManager.baseUrl;
    this.rulesRef = new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/rules')
    this.onChange()
  }

  updateBaseUrl(value) {
    let oldUrl = ConnectionManager.baseUrl
    ConnectionManager.baseUrl = value;
    this.baseUrl = value;
    this.testBaseUrl(value).catch((e => {
      alert("Error using provided Base Url. Check the development console.");
      log("Error using provided Base Url: ", e)
      this.baseUrl = oldUrl;
      ConnectionManager.baseUrl = oldUrl
      throw e
    }))

  }

  onChange(event = null) {
    log("RuleEngine change event: ", event)
    this.rulesRef.once('value', (rulesSnap) => {
      this.rules = []
      if (rulesSnap && rulesSnap.forEach) {
        rulesSnap.forEach((ruleSnap) => {
          this.rules.push(ruleSnap)
        })
      }
      else {
        throw rulesSnap
      }
    })
  }

  addRule() {
    log("Adding Rule")
    let testRule = new RuleEngine.Rule();
    testRule.name = "CoreWeb created this rule. " + new Date().toISOString()
    testRule.enabled = true
    testRule.priority = 10
    testRule.fireOn = "EVERY_PAGE"
    testRule.shortCircuit = false
    testRule.conditionGroups = {}
    testRule.actions = {}
    this.rulesRef.push(testRule).then((ruleRef) => this.onChange())
  }

  testBaseUrl(baseUrl) {
    return new Promise((resolve, reject) => {
      // get rules.
    })
  }
}

var conditionletsAry = []
var conditionletsMap = new Map()
var conditionletsPromise;


let initConditionlets = function () {
  let conditionletsRef:EntityMeta = new EntityMeta('/api/v1/system/conditionlets')
  conditionletsPromise = new Promise((resolve, reject) => {
    conditionletsRef.once('value').then((snap) => {
      let conditionlets = snap['val']()
      let results = (Object.keys(conditionlets).map((key) => {
        conditionletsMap.set(key, conditionlets[key])
        return conditionlets[key]
      }))

      Array.prototype.push.apply(conditionletsAry,results);
      resolve(snap);
    })
  });
}


export function main(providedTemplates:any) {
  templates = providedTemplates;
  log("Bootstrapping rules engine")
  ConnectionManager.persistenceHandler = RestDataStore
  initConditionlets()
  initActionlets()
  return bootstrap(RuleEngineComponent);
}

/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />

import {NgFor, NgIf, Component, Directive, View, ElementRef, Inject} from 'angular2/angular2';

import {RuleActionComponent} from './rule-action-component';
import {ConditionGroupComponent} from './rule-condition-group-component';

import {ruleTemplate} from './templates/index'
import {ApiRoot} from 'api/persistence/ApiRoot';

@Component({
  selector: 'rule',
  properties: ["ruleSnap"]
})
@View({
  template: ruleTemplate,
  directives: [RuleActionComponent, ConditionGroupComponent, NgIf, NgFor]
})
class RuleComponent{
  apiRoot:ApiRoot
  rule:any
  _ruleSnap:any
  collapsed:boolean
  fireOnDropDownExpanded:boolean
  ruleGroups:Array<any>
  ruleActions:Array<any>
  groupsSnap:EntitySnapshot
  elementRef:ElementRef
  actionsRef:EntityMeta

  constructor(elementRef:ElementRef, @Inject(ApiRoot) apiRoot:ApiRoot) {
    this.apiRoot = apiRoot
    this.elementRef = elementRef //DOM element
    this.collapsed = true
    this.fireOnDropDownExpanded = false
    this.ruleGroups = []
    this.ruleActions = []
    this.actionsRef = apiRoot.defaultSite.child('ruleengine/ruleActions')
    this.actionsRef.on('child_removed', (childActionSnap) => {
      this.ruleActions = this.ruleActions.filter((action) => {
        return action.key() != childActionSnap.key()
      })
    })
  }

  onInit(){
    if(this.rule.name === 'CoreWeb created this rule.'){
      this.rule.name = 'CoreWeb created this rule.' +  new Date().toISOString();//to avoid duplicate name error for now
      this.updateRule()
      var el = this.elementRef.nativeElement.children[0].children[0].children[0].children[0].children[0].childNodes[1]
      window.setTimeout(function() {el['focus']();}, 10) //avoid tick recursively error
    }
  }

  set ruleSnap(ruleSnap:any) {
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
        actionMetas.push(this.actionsRef.child(key))
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

    this.groupsSnap.ref().push(group, (snapshot) => {
      let group = snapshot['val']()
      this.rule.conditionGroups[snapshot.key()] = group
      group.conditions = group.conditions || {}
      this.updateRule().then(()=> this.addCondition(snapshot) )
    })
  }

  addCondition(groupSnap) {
    console.log('Adding condition to new condition group')
    let group = groupSnap.val()
    let condition = {
      priority: 10,
      name: "Condition. " + new Date().toISOString(),
      owningGroup: groupSnap.key(),
      conditionlet: 'UsersBrowserHeaderConditionlet',
      comparison: 'Is',
      operator: 'AND',
      values: {
        fakeId: {
          id: 'fakeId',
          key: 'headerValue',
          value: 'US',
          priority: 10
        }
      }
    }
    let condRoot:EntityMeta = this.apiRoot.defaultSite.child('ruleengine/conditions')

    condRoot.push(condition, (result) => {
      group.conditions = group.conditions || {}
      group.conditions[result.key()] = true
      groupSnap.ref().set(group)
    })
  }

  addRuleAction() {
    let action = {
      name: "CoreWeb created this action: " + new Date().toISOString(),
      priority: 10,
      owningRule: this.ruleSnap.key(),
      actionlet: 'CountRequestsActionlet'
    }
    let actionRoot:EntityMeta = this.apiRoot.defaultSite.child('ruleengine/ruleActions')

    actionRoot.push(action, (actionSnap:EntitySnapshot)=> {
      this.rule.actions = this.rule.ruleActions || {}
      this.rule.actions[actionSnap.key()] = true
      let key = actionSnap.key()
      this.ruleActions.push(actionRoot.child(key))
      this.updateRule()
    })
  }


  removeRule() {
    if (confirm('Are you sure you want delete this rule?')) {
      this.ruleSnap.ref().remove().catch((e) => {
        console.log("Error removing rule", e)
        throw e
      })
    }
  }

  updateRule() {
    console.log('Updating Rule')
    return this.ruleSnap.ref().set(this.rule)
  }

}

export {RuleComponent}
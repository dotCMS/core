/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />

import {NgFor, NgIf, Component, Directive, View, ElementRef, Inject} from 'angular2/angular2';

import {RuleActionComponent} from './rule-action-component';
import {ConditionGroupComponent} from './rule-condition-group-component';

//noinspection TypeScriptCheckImport
import {InputToggle} from 'view/components/input/toggle/InputToggle'
import {ApiRoot} from 'api/persistence/ApiRoot';


var rsrc = {
  fireOn: {
    EVERY_PAGE: 'Every Page',
    ONCE_PER_VISIT: 'Once per visit',
    ONCE_PER_VISITOR: 'Once per visitor',
    EVERY_REQUEST: 'Every Request'
  }
}

@Component({
  selector: 'rule',
  properties: ["ruleSnap", "hidden"]
})
@View({
  template: `<div flex layout="column" class="cw-rule" *ng-if="!hidden">
  <div flex="grow" layout="row" layout-align="space-between-center" class="cw-header" *ng-if="!hidden">
    <i class="caret icon" [class.right]="collapsed" [class.down]="!collapsed" aria-hidden="true" (click)="toggleCollapsed()"></i>
    <input flex type="text" class="cw-name cw-input" placeholder="Describe the rule" [value]="rule.name" (change)="setRuleName($event.target.value)" (focus)="collapsed = false">
    <select [value]="rule.fireOn" (change)="setFireOn($event.target.value)">
      <option value="EVERY_PAGE" [selected]="rule.fireOn === 'EVERY_PAGE'">{{fireOnLabel('EVERY_PAGE')}}</option>
      <option value="ONCE_PER_VISIT" [selected]="rule.fireOn === 'ONCE_PER_VISIT'">{{fireOnLabel('ONCE_PER_VISIT')}}</option>
      <option value="ONCE_PER_VISITOR" [selected]="rule.fireOn === 'ONCE_PER_VISITOR'">{{fireOnLabel('ONCE_PER_VISITOR')}}</option>
      <option value="EVERY_REQUEST" [selected]="rule.fireOn === 'EVERY_REQUEST'">{{fireOnLabel('EVERY_REQUEST')}}</option>
    </select>
    <cw-toggle-input class="cw-input" [value]="rule.enabled" (change)="rule.enabled = $event"></cw-toggle-input>
    <div class="cw-btn-group">
      <div class="ui basic icon buttons">
        <button class="ui button" aria-label="Delete Rule" (click)="removeRule()">
          <i class="trash icon" (click)="removeRule()"></i>
        </button>
        <button class="ui button" arial-label="Add Group" (click)="addGroup(); collapsed=false;">
          <i class="plus icon" aria-hidden="true" (click)="addGroup(); collapsed=false;"></i>
        </button>
      </div>
    </div>
  </div>
  <div flex layout="column" class="cw-accordion-body" *ng-if="!collapsed">
    <condition-group flex="grow" layout="row" *ng-for="var groupSnap of ruleGroups; var i=index"
                     [rule]="rule"
                     [group-snap]="groupSnap"
                     [group-index]="i"></condition-group>
    <div flex layout="column" layout-align="center-start" class="cw-action-separator">
      This rule sets the following action(s)
    </div>
    <div flex="100" layout="column" class="cw-rule-actions">
      <button class="cw-add-action-button ui button" arial-label="Add Action" (click)="addRuleAction(); collapsed=false;" *ng-if="ruleActions.length === 0">
        <i class="plus icon" aria-hidden="true" (click)="addRuleAction(); collapsed=false;"></i>
      </button>
      <rule-action flex *ng-for="var actionMeta of ruleActions; var i=index" [action-meta]="actionMeta"></rule-action>
    </div>
  </div>
</div>

`,
  directives: [InputToggle, RuleActionComponent, ConditionGroupComponent, NgIf, NgFor]
})
class RuleComponent {
  apiRoot:ApiRoot
  hidden:boolean
  rule:any
  _ruleSnap:any
  collapsed:boolean
  ruleGroups:Array<any>
  ruleActions:Array<any>
  groupsSnap:EntitySnapshot
  elementRef:ElementRef
  actionsRef:EntityMeta

  constructor(elementRef:ElementRef, @Inject(ApiRoot) apiRoot:ApiRoot) {
    this.apiRoot = apiRoot
    this.hidden = false
    this.elementRef = elementRef //DOM element
    this.collapsed = true
    this.ruleGroups = []
    this.ruleActions = []
    this.actionsRef = apiRoot.defaultSite.child('ruleengine/actions')
    this.actionsRef.on('child_removed', (childActionSnap) => {
      delete this.rule.actions[childActionSnap.key()]
      this.ruleActions = this.ruleActions.filter((action) => {
        return action.key() != childActionSnap.key()
      })
    })
  }

  onInit() {
    if (this.rule.name === 'CoreWeb created this rule.') {
      this.rule.name = 'CoreWeb created this rule.' + new Date().toISOString();//to avoid duplicate name error for now
      this.updateRule()
      var el = this.elementRef.nativeElement.children[0].children[0].children[0].children[0].children[1].childNodes[1]
      window.setTimeout(function () {
        el['focus']();
      }, 10) //avoid tick recursively error
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
    this.groupsSnap.ref().on('child_added', (childSnap)=> {
      this.ruleGroups.push(childSnap)
    })
    this.groupsSnap.ref().on('child_removed', (childGroupSnap)=> {
      delete this.rule.conditionGroups[childGroupSnap.key()]
      this.ruleGroups = this.ruleGroups.filter((group) => {
        return group.key() != childGroupSnap.key()
      })
    })

    this.ruleActions = this.getRuleActions()
  }

  toggleCollapsed() {
    console.log('collapsed: ', this.collapsed)
    this.collapsed = !this.collapsed
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

  fireOnLabel(fireOnId:string) {
    return rsrc.fireOn[fireOnId];
  }

  setFireOn(value:string) {
    this.rule.fireOn = value
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
      this.updateRule().then(()=> this.addCondition(snapshot))
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
    let actionRoot:EntityMeta = this.apiRoot.defaultSite.child('ruleengine/actions')

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
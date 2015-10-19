/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />

import {NgFor, NgIf, Component, Directive, View, ElementRef, Inject} from 'angular2/angular2';

import {RuleActionComponent} from './rule-action-component';
import {ConditionGroupComponent} from './rule-condition-group-component';

//noinspection TypeScriptCheckImport
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
  properties: ["ruleSnap"]
})
@View({
  template: `<div class="panel panel-default rule">
  <div class="panel-heading" (click)="collapsed = !collapsed">
    <div class="container">
      <div class="row" (click)="collapsed = !collapsed">
        <div class="col-xs-1 collapse-icon">
          <span class="glyphicon glyphicon-triangle-right collapse-icon" [class.glyphicon-triangle-bottom]="!collapsed" aria-hidden="true" (click)="collapsed = !collapsed"></span>
        </div>
        <div class="col-xs-5">
          <input type="text" class="form-control  rule-title"
          placeholder="Describe the rule"
          [value]="rule.name"
          (change)="setRuleName($event.target.value)"
          (focus)="collapsed = false">
        </div>

        <div class="col-xs-3">
          <div class="operations rule-operations">
            <label>Fire on:</label>

            <div class="btn-group">
              <button type="button" class="btn btn-default" (click)="fireOnDropDownExpanded = !fireOnDropDownExpanded">{{fireOnLabel(rule.fireOn)}}</button>
              <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="false" (click)="fireOnDropDownExpanded = !fireOnDropDownExpanded">
                <span class="caret" (click)="fireOnDropDownExpanded = !fireOnDropDownExpanded"></span>
                <span class="sr-only" (click)="fireOnDropDownExpanded = !fireOnDropDownExpanded">Toggle Drop Down</span>
              </button>
              <ul class="dropdown-menu collapse" role="menu" [class.in]="fireOnDropDownExpanded">
                <li (click)="setFireOn('EVERY_PAGE')"><a href="#" (click)="setFireOn('EVERY_PAGE')">{{fireOnLabel('EVERY_PAGE')}}</a></li>
                <li (click)="setFireOn('ONCE_PER_VISIT')"><a href="#" (click)="setFireOn('ONCE_PER_VISIT')">{{fireOnLabel('ONCE_PER_VISIT')}}</a></li>
                <li (click)="setFireOn('ONCE_PER_VISITOR')"><a href="#" (click)="setFireOn('ONCE_PER_VISITOR')">{{fireOnLabel('ONCE_PER_VISITOR')}}</a></li>
                <li (click)="setFireOn('EVERY_REQUEST')"><a href="#" (click)="setFireOn('EVERY_REQUEST')">{{fireOnLabel('EVERY_REQUEST')}}</a></li>
              </ul>
            </div>
          </div>
        </div>
        <div class="col-xs-3">
          <div class="operations">
          <button type="button" class="btn btn-default btn-md" [class.btn-danger]="rule.enabled" aria-label="Enable/Disable Rule"
          (click)="toggleEnabled()">
          <span class="glyphicon glyphicon-off" (click)="toggleEnabled()"></span>
        </button>
        <button type="button" class="btn btn-default btn-md btn-danger" aria-label="Delete Rule"
        (click)="removeRule()">
        <span class="glyphicon glyphicon-trash" (click)="removeRule()"></span>
      </button>
      <button type="button" class="btn btn-default btn-md" arial-label="Add Group" (click)="addGroup()">
        <span class="glyphicon glyphicon-plus" aria-hidden="true" (click)="addGroup()"></span>
      </button>
    </div>
    </div>
  </div>
</div>
</div>
<div class="panel-body collapse" [class.in]="!collapsed">
  <div class="section-separator alert alert-info">
    This rule fires when the following condition(s) are met?
  </div>
  <condition-group *ng-for="var groupSnap of ruleGroups; var i=index"
  [rule]="rule"
  [group-snap]="groupSnap"
  [group-index]="i"></condition-group>

  <div class="alert alert-success">
    This rule sets the following action(s)
  </div>
  <rule-action *ng-for="var actionMeta of ruleActions; var i=index" [action-meta]="actionMeta"></rule-action>
  <div class="col-md-2">
      <button type="button" class="btn btn-default btn-md" aria-label="Add Action" (click)="addRuleAction()">
      <span class="glyphicon glyphicon-plus" aria-hidden="true" (click)="addRuleAction()"></span>
    </button>
  </div>
</div>
</div>
`,
  directives: [RuleActionComponent, ConditionGroupComponent, NgIf, NgFor]
})
class RuleComponent {
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
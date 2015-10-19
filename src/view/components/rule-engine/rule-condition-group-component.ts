/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />


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
  template: `<div class="row">
  <div class="col-sm-12">
    <div class="alert alert-info" *ng-if="groupIndex !== 0">
      <p class="pull-left">
        <a class="btn btn-default add-button-alert" (click)="toggleGroupOperator()">
          {{group.operator}}
        </a>
      </p>
      when the following condition(s) are met?
    </div>
  </div>
</div>
<div class="panel-body">
  <rule-condition *ng-for="var meta of conditions; var i=index" [condition-meta]="meta" [index]="i"></rule-condition>
  <button type="button" class="btn btn-default btn-md" aria-label="Add Condition" (click)="addCondition()">
    <span class="glyphicon glyphicon-plus" aria-hidden="true" (click)="addCondition()"></span>
  </button>
</div>`,
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
      if(conditionSnap.val().owningGroup == this.groupSnap.key()) {
        this.conditions.push(conditionSnap.ref())
      }
    })
    this.conditionsRef.on('child_removed', (conditionSnap) => {
      if(conditionSnap.val().owningGroup == this.groupSnap.key()){
        this.conditions = this.conditions.filter((existingSnap) =>{
          return existingSnap.key() != conditionSnap.key()
        })
        if(this.conditions.length === 0){
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
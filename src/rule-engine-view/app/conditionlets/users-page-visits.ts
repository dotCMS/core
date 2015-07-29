/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Ancestor, ObservableWrapper, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';
import {ConditionletDirective} from './conditionlet-base';

@Component({
  selector: 'conditionlet users-page-visits'
})
@View({
  directives: [NgFor],
  template: `
    <div class="col-sm-5">
      <select class="form-control clause-selector" [value]="conditionletDir.condition.comparison" (change)="setComparison($event)">
        <option value="{{x.id}}" *ng-for="var x of conditionletDir.conditionlet.comparisons">{{x.label}}</option>
      </select>
    </div>
    <div class="col-sm-2">
      <h4 class="separator"></h4>
    </div>
    <div class="col-sm-5">
      <input type="number" class="form-control condition-value" [value]="conditionletDir.value" (input)="setValue($event)"/>
    </div>
  `
})
export class UsersPageVisitsConditionlet {
  conditionletDir:ConditionletDirective;

  constructor(@Ancestor() conditionletDir:ConditionletDirective, @Attribute('id') id:string) {
    this.conditionletDir = conditionletDir
  }

  setComparison(value){
    this.conditionletDir.onComparisonChange(event)
  }

  setValue(event) {
    this.conditionletDir.onValueChange(event)
  }


}
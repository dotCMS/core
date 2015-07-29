/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Ancestor, ObservableWrapper, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';
import { ObservableWrapper } from 'angular2/src/facade/async';

import {ConditionletDirective} from './conditionlet-component';

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
      <input type="number" class="form-control condition-value" [value]="conditionletDir.value" (input)="updateValue($event)" (focus)="setHasFocus(true)" (blur)="setHasFocus(false)"/>
    </div>
  `
})
export class UsersPageVisitsConditionlet {
  conditionletDir:ConditionletDirective;
  inputHasValue:boolean;
  inputHasFocus:boolean;
  valueChange: EventEmitter;
  comparisonChange:EventEmitter;


  constructor(@Ancestor() conditionletDir:ConditionletDirective, @Attribute('id') id:string) {
    this.conditionletDir = conditionletDir
    this.inputHasValue = false;
    this.inputHasFocus = false;
    this.valueChange = new EventEmitter();
    this.comparisonChange = new EventEmitter();

    this.conditionletDir.register(this)
  }

  setComparison(value:string){
    ObservableWrapper.callNext(this.comparisonChange, {was: this.conditionletDir.condition.comparison,  isNow: event.target.value})
  }

  updateValue(event) {
    ObservableWrapper.callNext(this.valueChange, {was: this.conditionletDir.value,  isNow: event.target.value})
  }


  setHasFocus(hasFocus:boolean) {
    this.inputHasFocus = hasFocus
  }


}
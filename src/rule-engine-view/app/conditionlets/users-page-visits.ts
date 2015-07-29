/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Ancestor, ObservableWrapper, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';

@Component({
  selector: 'conditionlet users-page-visits',
  properties: ["value", "condition", "conditionlet"],
  events: ['change']
})
@View({
  directives: [NgFor],
  template: `
    <div class="col-sm-5">
      <select class="form-control clause-selector" [value]="condition.comparison" (change)="setComparison($event.target.value)">
        <option value="{{x.id}}" *ng-for="var x of conditionlet.comparisons">{{x.label}}</option>
      </select>
    </div>
    <div class="col-sm-2">
      <h4 class="separator"></h4>
    </div>
    <div class="col-sm-5">
      <input type="text" class="form-control condition-value" [value]="value" (input)="updateValue($event)" (focus)="setHasFocus(true)" (blur)="setHasFocus(false)"/>
    </div>
  `
})
export class UsersPageVisitsConditionlet {
  value:string;
  condition:any;
  conditionlet:any;
  inputHasValue:boolean;
  inputHasFocus:boolean;
  change = new EventEmitter();

  constructor(@Attribute('value') value:string, @Attribute('condition') condition:any, @Attribute('conditionlet') conditionlet:any, @Attribute('id') id:string) {
    this.value = value ? value : ''
    this.condition = condition ? condition : null
    this.conditionlet = conditionlet ? conditionlet : null
    this.inputHasValue = false;
    this.inputHasFocus = false;
  }

  setComparison(value:string){
    debugger;
  }

  updateValue(event) {
    this.value = event.target.value;
    this.onValueChange()
  }
  onValueChange(): void {
    this.change.next(this.value);
  }

  setHasFocus(hasFocus:boolean) {
    this.inputHasFocus = hasFocus
  }


}
/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Ancestor, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';

@Directive({
  selector: 'conditionlet',
  properties: ["value", "condition", "conditionlet"],
  events: ['change']
})
export class ConditionletDirective {
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


}
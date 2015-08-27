/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Host, SkipSelf, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';

@Directive({
  selector: 'cw-conditionlet',
  properties: ["value", "condition", "conditionlet"],
  events: ['valueChange', 'comparisonChange']
})
export class ConditionletDirective {
  value:string;
  condition:any;
  conditionlet:any;
  child:any;
  valueChange:EventEmitter;
  comparisonChange:EventEmitter;


  constructor(@Attribute('value') value:string, @Attribute('condition') condition:any, @Attribute('conditionlet') conditionlet:any, @Attribute('id') id:string) {
    this.value = value ? value : ''
    this.condition = condition ? condition : null
    this.conditionlet = conditionlet ? conditionlet : null
    this.valueChange = new EventEmitter();
    this.comparisonChange = new EventEmitter();
  }

  onValueChange(event) {
    console.log('conditionlet-component:onValueChanged', event)
    this.valueChange.next(Object.assign({was: this.value,  isNow: event.target.value}, event))
  }

  onComparisonChange(event) {
    console.log('conditionlet-component:onComparisonChange', event)
    this.comparisonChange.next(Object.assign({was: this.condition.comparison,  isNow: event.target.value}, event))
  }
}


export class BaseConditionletComponent {
  conditionletDir:ConditionletDirective;

  constructor(@SkipSelf() @Host() conditionletDir:ConditionletDirective, @Attribute('id') id:string) {
    this.conditionletDir = conditionletDir
  }


  setComparison(value){
    if(this.conditionletDir) {
      this.conditionletDir.onComparisonChange(event)
    }
  }

  setValue(event) {
    if(this.conditionletDir) {
      this.conditionletDir.onValueChange(event)
    }
  }

}
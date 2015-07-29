/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Ancestor, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';
import { ObservableWrapper } from 'angular2/src/facade/async';

@Directive({
  selector: 'conditionlet',
  properties: ["value", "condition", "conditionlet"],
  events: ['valueChange', 'comparisonChange']
})
export class ConditionletDirective {
  value:string;
  condition:any;
  conditionlet:any;
  child:any;
  valueChange: EventEmitter;
  comparisonChange:EventEmitter;


  constructor(@Attribute('value') value:string, @Attribute('condition') condition:any, @Attribute('conditionlet') conditionlet:any, @Attribute('id') id:string) {
    this.value = value ? value : ''
    this.condition = condition ? condition : null
    this.conditionlet = conditionlet ? conditionlet : null
    this.valueChange = new EventEmitter();
    this.comparisonChange = new EventEmitter();
  }

  /** Forward events from the child conditionlet. */
  register(child) {
    this.child = child;
    ObservableWrapper.subscribe(child.valueChange, (event) => {
          debugger;
          this.valueChange.next(event)
        }
    );

    ObservableWrapper.subscribe(child.comparisonChange, (event) => {
      this.comparisonChange.next(event)
    });
  }


}
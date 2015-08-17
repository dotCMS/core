/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Host, SkipSelf, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';

@Component({
  selector: 'single-value-input',
  properties: ["value", "type"],
  events: ['change']
})
@View({
  template: `
    <div class="col-sm-1">
      <h4 class="separator"></h4>
    </div>
    <div class="col-sm-3">
      <input [type]="type" class="form-control condition-value" [value]="value" (input)="updateValue($event)" (focus)="setHasFocus(true)" (blur)="setHasFocus(false)"/>
    </div>
  `
})
export class SingleValueInput {
  value:string;
  type:string;
  inputHasValue:boolean;
  inputHasFocus:boolean;
  change = new EventEmitter();


  constructor(@Attribute('value') value:string, @Attribute('type') type:string, @Attribute('id') id:string) {
    this.value = value ? value : ''
    this.type = type ? type : 'text'
  }

  updateValue(event) {
    this.value = event.target.value;
    this.onValueChange()
  }

  onValueChange():void {
    this.change.next(this.value);
  }

  setHasFocus(hasFocus:boolean) {
    this.inputHasFocus = hasFocus
  }
}



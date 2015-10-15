/// <reference path="../../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../../typings/angular2/angular2.d.ts" />

import {Component, View, Attribute, ElementRef} from 'angular2/angular2';
var idCounter = 1

@Component({
  selector: 'cw-toggle-input',

  properties: [
    'value',
    'onText',
    'offText'
  ]
})
@View({
  template: `<style>
  .ui.toggle.checkbox label {
    float: left
  }

  .on-label, .off-label {
    position: absolute;
    top: 0;
    padding-top: .2em;
    font-weight: bolder;
    font-size: 75%;
    z-index: 2;
  }

  .on-label {
    left: .75em;
  }

  .off-label {
    right: .75em;
  }

  .off .on-label, .on .off-label {
    display: none;
  }

</style>
  <div class="ui toggle fitted checkbox" [class.on]="value === true" [class.off]="value === false">
    <input type="checkbox" [value]="value" [checked]="value" (change)="updateValue($event.target.checked)">
    <label></label>
    <span class="on-label">{{onText}}</span>
    <span class="off-label">{{offText}}</span>
  </div>
  `
})
export class InputToggle {
  el:ElementRef
  value:boolean
  onText:string
  offText:string

  constructor(el:ElementRef, @Attribute('value') value:string, @Attribute('onText') onText:string, @Attribute('offText') offText:string) {
    this.el = el
    var nativeEl = el.nativeElement
    this.value = (value !== false && value !== 'false')
    this.onText = onText || 'On'
    this.offText = offText || 'Off'
  }

  updateValue(value) {
    console.log('input value changed: [from / to]', this.value, value)
    this.value = value;
  }
}


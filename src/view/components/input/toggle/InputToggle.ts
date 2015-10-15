/// <reference path="../../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../../typings/angular2/angular2.d.ts" />

import {Component, View, EventEmitter, Attribute} from 'angular2/angular2';

@Component({
  selector: 'cw-toggle-input',

  properties: [
    'value',
    'onText',
    'offText'
  ],events: [
    "change"
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
    padding-top: .3em;
    font-weight: 900;
    font-size: 75%;
    z-index: 2;
  }

  .on-label {
    left: .75em;
    color: white;
  }

  .off-label {
    right: .75em;
    color:#555;
  }

  .off .on-label, .on .off-label {
    display: none;
  }

</style>
  <span class="ui toggle fitted checkbox" [class.on]="value === true" [class.off]="value === false">
    <input type="checkbox" [value]="value" [checked]="value" (change)="updateValue($event.target.checked)">
    <label></label>
    <span class="on-label">{{onText}}</span>
    <span class="off-label">{{offText}}</span>
  </span>
  `
})
export class InputToggle {
  value:boolean
  onText:string
  offText:string
  change:EventEmitter

  constructor(@Attribute('value') value:string, @Attribute('onText') onText:string, @Attribute('offText') offText:string) {
    this.value = (value !== false && value !== 'false')
    this.onText = onText || 'On'
    this.offText = offText || 'Off'
    this.change = new EventEmitter()
  }

  updateValue(value) {
    console.log('input value changed: [from / to]', this.value, value)
    this.value = value;
    this.change.next(value)
  }
}


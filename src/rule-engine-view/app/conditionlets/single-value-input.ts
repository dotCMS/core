/// <reference path="../../../../typings/es6/lib.es6.d.ts" />
/// <reference path="../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Ancestor, ObservableWrapper, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';

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
  _input:ComparisonInput;
  inputHasValue:boolean;
  inputHasFocus:boolean;
  change = new EventEmitter();



  constructor(@Attribute('value') value:string, @Attribute('type') type:string, @Attribute('id') id:string) {
    this.value = value ? value : ''
    this.type = type ? type : 'text'
    this._input = null;
    this.inputHasValue = false;
    this.inputHasFocus = false;
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

  registerInput(input) {
    this._input = input;
    this.inputHasValue = input.value != '';

    // Listen to input changes and focus events so that we can apply the appropriate CSS
    // classes based on the input state.
    ObservableWrapper.subscribe(input.dotChange, value => {
      debugger
      this.inputHasValue = value != '';

    });

    ObservableWrapper.subscribe(input.dotFocusChange, hasFocus => {
      debugger

      this.inputHasFocus = hasFocus
    });
  }

}


@Directive({
  selector: 'single-value-input bob',
  events: ['dotChange', 'dotFocusChange'],
  host: {
    '(input)': 'updateValue($event)',
    '(focus)': 'setHasFocus(true)',
    '(blur)': 'setHasFocus(false)'
  }
})
export class ComparisonInput {
  value:string;

  dotChange:EventEmitter;
  dotFocusChange:EventEmitter;

  constructor(@Attribute('value') value:string, @Ancestor() container:SingleValueInput,
              @Attribute('id') id:string) {
    debugger;
    this.value = value == null ? '' : value;
    this.dotChange = new EventEmitter();
    this.dotFocusChange = new EventEmitter();

    container.registerInput(this);
  }

  updateValue(event) {
    debugger

    this.value = event.target.value;
    ObservableWrapper.callNext(this.dotChange, this.value);
  }

  setHasFocus(hasFocus:boolean) {
    debugger
    ObservableWrapper.callNext(this.dotFocusChange, hasFocus);
  }
}



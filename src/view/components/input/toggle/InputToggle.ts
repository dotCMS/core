import {Component, View, EventEmitter, Attribute} from 'angular2/angular2';

@Component({
  selector: 'cw-toggle-input',

  properties: [
    'value',
    'onText',
    'offText'
  ],events: [
    "toggle" // 'change' is fired by the input component.
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
  get offText():string {
    return this._offText;
  }

  set offText(value:string) {
    this._offText = value;
  }
  _value:boolean
  onText:string
  private _offText:string
  toggle:EventEmitter

  constructor(@Attribute('value') value:string, @Attribute('onText') onText:string, @Attribute('_offText') offText:string) {
    this.value = (value !== 'false')
    this.onText = onText || 'On'
    this.offText = offText || 'Off'
    this.toggle = new EventEmitter()
  }

  set value(value:boolean){
    this._value = value === true
  }

  get value():boolean {
    return this._value
  }

  updateValue(value) {
    console.log('input value changed: [from / to]', this.value, value)
    this.value = value
    this.toggle.next({type: 'toggle', target: this, value: value })
  }
}


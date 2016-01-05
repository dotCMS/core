import {Component, View, EventEmitter, Attribute, Input, Output} from 'angular2/core';

@Component({
  selector: 'cw-toggle-input'
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
    <input type="checkbox" [value]="value" [checked]="value" (change)="updateValue($event)">
    <label></label>
    <span class="on-label">{{onText}}</span>
    <span class="off-label">{{offText}}</span>
  </span>
  `
})
export class InputToggle {
  @Input() value:boolean
  onText:string
  offText:string

  @Output() change:EventEmitter<boolean>

  constructor(@Attribute('value') value:string, @Attribute('onText') onText:string, @Attribute('offText') offText:string) {
    this.value = (value !== 'false')
    this.onText = onText || 'On'
    this.offText = offText || 'Off'
    this.change = new EventEmitter()
  }

  ngOnChanges(change){
    if(change.value){
      this.value = change.value.currentValue === true
    }
  }

  updateValue($event) {
    $event.stopPropagation() // grrr.
    let value = $event.target.checked
    console.log("InputToggle", "updateValue", 'input value changed: [from / to]', this.value, value)
    this.value = value
    this.change.emit(value)
  }
}


import { Component, View, TemplateRef, EventEmitter, ElementRef} from 'angular2/core';
import { CORE_DIRECTIVES } from 'angular2/common';

/**
 * Angular 2 wrapper around Semantic UI Input Element.
 * @see http://semantic-ui.com/elements/input.html
 */

export class InputDateModel {
  name:string
  placeholder:string
  value:string
  disabled:string
  icon:string
  type:string

  constructor(name:string = null,
              placeholder:string = '',
              type:string = 'date',
              value:string = null,
              disabled:string = null,
              icon:string = '') {

    this.name = !!name ? name : 'field-' + new Date().getTime() + Math.floor(Math.random() * 1000)
    this.placeholder = placeholder
    this.type = type
    this.value = value
    this.disabled = disabled
    this.icon = icon || ''
    if(this.icon.indexOf(' ') == -1 && this.icon.length > 0){
      this.icon = (this.icon + ' icon').trim()
    }
  }

  validateDate(date:string) {
    var date_regex = /^(?:(?:31(\/|-|\.)(?:0?[13578]|1[02]))\1|(?:(?:29|30)(\/|-|\.)(?:0?[1,3-9]|1[0-2])\2))(?:(?:1[6-9]|[2-9]\d)?\d{2})$|^(?:29(\/|-|\.)0?2\3(?:(?:(?:1[6-9]|[2-9]\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\d|2[0-8])(\/|-|\.)(?:(?:0?[1-9])|(?:1[0-2]))\4(?:(?:1[6-9]|[2-9]\d)?\d{2})$/;
    if (!date_regex.test(date)) {
      throw new Error("Insert a valid date dd/mm/yyyy,dd-mm-yyyy or dd.mm.yyyy");
    }
  }

  validateTime(time:string) {
    var time_regex = /^(10|11|12|[1-9]):[0-5][0-9]$/;
    if (!time_regex.test(time)) {
      throw new Error("Insert a valid time HH:MM");
    }
  }

  validateDateTime(dateTime:string) {
    // TODO: better match this regex for MM/DD/YYYY HH:MM
    var date_time_regex = /^(((\d\d)(([02468][048])|([13579][26]))-02-29)|(((\d\d)(\d\d)))-((((0\d)|(1[0-2]))-((0\d)|(1\d)|(2[0-8])))|((((0[13578])|(1[02]))-31)|(((0[1,3-9])|(1[0-2]))-(29|30)))))\s(([01]\d|2[0-3]):([0-5]\d):([0-5]\d))$/;
    if (!date_time_regex.test(dateTime)) {
      throw new Error("Insert a valid date time");
    }
  }

  validate(value:string){
    console.log(this.type);
    if (this.type === 'date') {
      this.validateDate(value)
    } else if (this.type === 'time') {
      this.validateTime(value)
    } else if (this.type === 'datetime-local') {
      this.validateDateTime(value)
    }
  };
}

@Component({
  selector: 'cw-input-date',

  properties: [
    'model',
  ], events: [
    "change"
  ]
})
@View({
  template: `
<div class="ui fluid input" [ng-class]="{disabled: model.disabled, error: errorMessage, icon: model.icon}">
  <input [type]="model.type" [name]="model.name" [value]="model.value" [placeholder]="model.placeholder" (change)="inputChange($event)">
  <i [ng-class]="model.icon" *ng-if="model.icon"></i>
  <br />
  <div class="ui small red message" *ng-if="errorMessage">{{errorMessage}}</div>
</div>

  `,
  directives: [CORE_DIRECTIVES]
})
export class InputDate {

  private _model:InputDateModel
  private errorMessage:String

  change:EventEmitter<InputDateModel>
  private elementRef:ElementRef

  constructor(@ElementRef elementRef:ElementRef) {
    this.elementRef = elementRef
    this.change = new EventEmitter()
    this._model = new InputDateModel()
    this.errorMessage = null
  }

  get model():InputDateModel {
    return this._model;
  }

  set model(model:InputDateModel) {
    this._model = model;
  }

  inputChange(event){
    try {
      this.errorMessage = null

      this._model.value = event.target.value

      //Check if the validate function exists in this model.
      if (typeof this._model.validate == 'function') {
        this._model.validate(event.target.value)
      }
    }
    catch(err) {
      this.errorMessage = err.toString();
    }
  }

}


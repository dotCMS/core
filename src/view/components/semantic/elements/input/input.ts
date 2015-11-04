/// <reference path="../../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />

import { NgClass, NgIf, Component, View, TemplateRef, EventEmitter, ElementRef} from 'angular2/angular2';

/**
 * Angular 2 wrapper around Semantic UI Input Element.
 * @see http://semantic-ui.com/elements/input.html
 * Comments for event handlers, etc, copied directly from semantic-ui documentation.
 */

export class InputModel {
  name:string
  placeholder:string
  value:string
  disabled:string
  icon:string

  constructor(name:string = null,
              placeholder:string = '',
              value:string = null,
              disabled:string = null,
              icon:string = '') {

    this.name = !!name ? name : "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    this.placeholder = placeholder
    this.value = value
    this.disabled = disabled
    this.icon = icon
  }

  validate():{
  };

}

@Component({
  selector: 'cw-input',

  properties: [
    'model',
  ], events: [
    "change"
  ]
})
@View({
  template: `
<div class="ui fluid input" [ng-class]="{disabled: model.disabled, error: errorMessage, icon: model.icon}">
  <input type="text" [name]="model.name" [value]="model.value" [placeholder]="model.placeholder" (change)="inputChange($event)">
  <i [ng-class]="model.icon + ' icon'"></i>
  <div class="ui small red message" *ng-if="errorMessage">{{errorMessage}}</div>
</div>
  `,
  directives: [NgClass, NgIf]
})
export class Input {

  private _model:InputModel
  private errorMessage:String

  change:EventEmitter
  private elementRef:ElementRef

  constructor(@ElementRef elementRef:ElementRef) {
    this.elementRef = elementRef
    this.change = new EventEmitter()
    this._model = new InputModel()
    this.errorMessage = null
  }

  get model():InputModel {
    return this._model;
  }

  set model(model:InputModel) {
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


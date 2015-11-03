/// <reference path="../../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />

import { NgClass, NgIf, ElementRef, Component, View, Directive, ViewContainerRef, TemplateRef, EventEmitter, Attribute, NgFor} from 'angular2/angular2';

/**
 * Angular 2 wrapper around Semantic UI Input Element.
 * @see http://semantic-ui.com/elements/input.html
 * Comments for event handlers, etc, copied directly from semantic-ui documentation.
 */

export class InputModel {
  name:string
  placeholder:string
  value:string
  error:string
  disabled:string
  icon: string

  constructor(name:string = null,
              placeholder:string = '',
              value:string = null,
              error:string = null,
              disabled:string = null,
              icon:string = '') {

    this.name = !!name ? name : "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    this.placeholder = placeholder
    this.value = value
    this.error = error
    this.disabled = disabled
    this.icon = icon
  }

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
<div class="ui fluid input" [ng-class]="{disabled: model.disabled, error: model.error, icon: model.icon}">
  <input type="text" [name]="model.name" [value]="model.value" [placeholder]="model.placeholder">
  <i [ng-class]="model.icon + ' icon'"></i>
  <div class="ui small red message" *ng-if="model.error">{{model.error}}</div>
</div>
  `,
  directives: [NgClass, NgIf]
})
export class Input {

  private _model:InputModel

  change:EventEmitter
  private elementRef:ElementRef

  constructor(@ElementRef elementRef:ElementRef) {
    this.elementRef = elementRef
    this.change = new EventEmitter()
    this._model = new InputModel()
  }

  get model():InputModel {
    return this._model;
  }

  set model(model:InputModel) {
    this._model = model;
  }
}


import { NgClass, NgIf, Component, View, TemplateRef, EventEmitter, ElementRef} from 'angular2/angular2';
import {CwTextInputModel} from "api/util/CwInputModel";
import {CwComponent} from "api/util/CwComponent";
import {ParameterDefinition} from "api/util/CwInputModel";
import {ParameterModel} from "api/rule-engine/Condition";

/**
 * Angular 2 wrapper around Semantic UI Input Element.
 * @see http://semantic-ui.com/elements/input.html
 */

export class InputTextModel extends CwComponent {
  name:string
  placeholder:string
  value:string
  disabled:boolean
  icon:string


  constructor(name:string = null,
              placeholder:string = '',
              value:string = null,
              disabled:boolean = null,
              icon:string = '') {
    super()
    this.name = !!name ? name : "field-" + new Date().getTime() + Math.floor(Math.random() * 1000)
    this.placeholder = placeholder
    this.value = value
    this.disabled = disabled
    this.icon = icon || ''
    if(this.icon.indexOf(' ') == -1 && this.icon.length > 0){
      this.icon = (this.icon + ' icon').trim()
    }
  }

  validate(value:string){ };

  static fromParameter(param:ParameterModel, paramDef:ParameterDefinition):InputTextModel {
    let itm = new InputTextModel(param.key, paramDef.inputType.placeholder, param.value)
    console.log("PARAM:", param.key, param.value, param )
    return itm
  }
}

@Component({
  selector: 'cw-input-text',

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
  <i [ng-class]="model.icon" *ng-if="model.icon"></i>
  <div class="ui small red message" *ng-if="errorMessage">{{errorMessage}}</div>
</div>
  `,
  directives: [NgClass, NgIf]
})
export class InputText {

  private _model:InputTextModel
  private errorMessage:String

  change:EventEmitter
  private elementRef:ElementRef

  constructor(@ElementRef elementRef:ElementRef) {
    this.elementRef = elementRef
    this.change = new EventEmitter()
    this._model = new InputTextModel()
    this.errorMessage = null
  }

  get model():InputTextModel {
    return this._model;
  }

  set model(model:InputTextModel) {
    this._model = model;
  }

  inputChange(event){
    try {
      this.errorMessage = null
      event.value = event.target.value
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


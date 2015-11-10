import {Component, View, Attribute, EventEmitter, NgFor, NgIf} from 'angular2/angular2';

import {InputText, InputTextModel} from "../../../semantic/elements/input-text/input-text";
import {ActionTypeModel} from "../../../../../api/rule-engine/ActionType";
import {ActionModel} from "../../../../../api/rule-engine/Action";


@Component({
  selector: 'cw-serverside-action',
  properties: [
    "model"
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [NgFor, InputText],
  template: `<div flex layout="row" layout-align="start-center" class="cw-action-component-body">
  <cw-input-text *ng-for="var input of _inputs"
                 flex
                 class="cw-input"
                 (change)="handleParamValueChange(input.name, $event)"
                 [model]="input">
  </cw-input-text>
</div>`
})
export class ServersideAction {
  change:EventEmitter;
  private _model:ActionModel;
  private _inputs:Array<InputTextModel>

  constructor() {
    this.change = new EventEmitter();
    this._inputs = []
  }

  _updateInputs(){
    this._inputs = []
    let paramDefs = this._model.actionType.parameters
    Object.keys(paramDefs).forEach((paramKey)=> {
      let paramDef = paramDefs[paramKey]
      this._inputs.push(new InputTextModel(paramKey, paramDef.i18nKey, this._model.getParameter(paramKey)))
    })
  }

  set model(model:ActionModel) {
    this._model = model;
    this._model.onChange.subscribe((event) => {
      if(event.type === 'key' || event.type === 'actionType'){
         this._updateInputs()
      }
    })
    this._updateInputs()
  }



  get model():ActionModel {
    return this._model
  }


  handleParamValueChange(paramKey:string, event:any) {
    this.model.setParameter(paramKey, event.target.value)
  }

}

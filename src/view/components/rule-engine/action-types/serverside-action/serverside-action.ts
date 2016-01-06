import { Component, Directive, View, Inject, EventEmitter, Input, Output} from 'angular2/core';
import {CORE_DIRECTIVES} from 'angular2/common';

import {InputText} from "../../../semantic/elements/input-text/input-text";
import {ActionModel} from "../../../../../api/rule-engine/Action";


@Component({
  selector: 'cw-serverside-action',

})
@View({
  template: `<div flex layout="row" layout-align="start center" class="cw-action-component-body">
  <cw-input-text *ngFor="var input of _inputs"
                 flex
                 class="cw-input"
                 (change)="handleParamValueChange(input.name, $event)"
                 [name]="input.name"
                 [placeholder]="input.i18nKey"
                 [value]="input.value">
  </cw-input-text>
</div>`,
  directives: [CORE_DIRECTIVES, InputText]
})
export class ServersideAction {

  @Input() model:ActionModel
  @Output() change:EventEmitter<any>;
  private _inputs:Array<any>

  constructor() {
    this.change = new EventEmitter();
    this._inputs = []
  }

  ngOnChanges(change){
  }

  _updateInputs(){
    this._inputs = []
    let paramDefs = this.model.type.parameters
    Object.keys(paramDefs).forEach((paramKey)=> {

      let paramDef = paramDefs[paramKey]
      this._inputs.push({name:paramKey, i18nKey: paramDef.key, value: this.model.getParameter(paramKey )})
    })
  }

  //set model(model:ActionModel) {
  //  this._model = model;

    // @todo ggranum
    //this._model.onChange.subscribe((event) => {
    //  if(event.type === 'key' || event.type === 'actionType'){
    //     this._updateInputs()
    //  }
    //})

    //this._updateInputs()
  //}
  //
  //get model():ActionModel {
  //  return this._model
  //}

  handleParamValueChange(paramKey:string, event:any) {
    this.model.setParameter(paramKey, event.target.value)
  }

}

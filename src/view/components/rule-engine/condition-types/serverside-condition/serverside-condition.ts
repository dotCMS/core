import {Component, View, Attribute, EventEmitter,CORE_DIRECTIVES} from 'angular2/angular2';
import {Dropdown, DropdownModel, DropdownOption} from '../../../../../view/components/semantic/modules/dropdown/dropdown'
import {InputText, InputTextModel} from "../../../semantic/elements/input-text/input-text";
import {ConditionTypeModel} from "../../../../../api/rule-engine/ConditionType";
import {ConditionModel} from "../../../../../api/rule-engine/Condition";
import {ParameterDefinition} from "../../../../../api/rule-engine/ConditionType";
import {CwDropdownInputModel} from "../../../../../api/util/CwInputModel";
import {CwInputDefinition} from "../../../../../api/util/CwInputModel";
import {CwTextInputModel} from "../../../../../api/util/CwInputModel";
import {CwComponent} from "../../../../../api/util/CwComponent";
import {ParameterModel} from "../../../../../api/rule-engine/Condition";
import {CwSpacerInputDefinition} from "../../../../../api/util/CwInputModel";


@Component({
  selector: 'cw-serverside-condition',
  properties: [
    "model"
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [CORE_DIRECTIVES, Dropdown, InputText],
  template: `<div flex layout="row" layout-align="start-center" class="cw-condition-component-body">
  <template ng-for #input [ng-for-of]="inputs" #islast="last">
    <div *ng-if="input.inputDef.type == 'spacer'" flex layout-fill class="cw-input cw-input-placeholder">&nbsp;</div>
    <cw-input-dropdown *ng-if="input.inputDef.type == 'dropdown'"
                       flex
                       layout-fill
                       class="cw-input"
                       [class.cw-comparator-selector]="input.inputDef.name == 'comparison'"
                       [class.cw-last]="islast"
                       [model]="input.field"
                       (change)="handleParamValueChange($event, input)"></cw-input-dropdown>

    <cw-input-text *ng-if="input.inputDef.type == 'text'"
                   flex
                   layout-fill
                   class="cw-input"
                   [class.cw-last]="islast"
                   (change)="handleParamValueChange($event, input)"
                   [model]="input.field">
    </cw-input-text>
  </template>

</div>`
})
export class ServersideCondition {

  _model:ConditionModel;
  change:EventEmitter;

  private inputs:Array<{ inputDef:CwInputDefinition, field:any}>


  constructor() {
    this.change = new EventEmitter();
    this.inputs = [];
  }

  set model(model:ConditionModel) {
    this._model = model;
    let prevPriority = 0
    Object.keys(model.parameterDefs).forEach(key => {
      let paramDef = model.getParameterDef(key)
      let param = model.getParameter(key);
      if(paramDef.priority > (prevPriority + 1)){
        this.inputs.push({inputDef: new CwSpacerInputDefinition(40), field:null})
      }
      prevPriority = paramDef.priority
      let input = {inputDef: paramDef.inputType, field: this.inputModelFromCondition(param, paramDef)}
      this.inputs.push(input)
    })
  }

  inputModelFromCondition(param:ParameterModel, paramDef:ParameterDefinition):CwComponent {
    let field
    if (paramDef.inputType.type === 'text') {
      field = InputTextModel.fromParameter(param, paramDef)
    } else if (paramDef.inputType.type === 'dropdown') {
      field = DropdownModel.fromParameter(param, paramDef)
    }
    return field
  }

  get model():ConditionModel {
    return this._model
  }


  handleParamValueChange(event:any, input:any) {
    this._model.setParameter(input.field.name, event.value)
  }

}

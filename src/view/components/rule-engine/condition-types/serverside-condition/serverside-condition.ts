
import {Component, View, Attribute, EventEmitter, NgFor, NgIf} from 'angular2/angular2';
import {Dropdown, DropdownModel, DropdownOption} from '../../../../../view/components/semantic/modules/dropdown/dropdown'
import {InputText, InputTextModel} from "../../../semantic/elements/input-text/input-text";
import {ConditionTypeModel} from "../../../../../api/rule-engine/ConditionType";
import {ConditionModel} from "../../../../../api/rule-engine/Condition";


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
  directives: [NgFor, Dropdown, InputText],
  template: `<div flex layout="row" layout-align="start-center" class="cw-condition-component-body">
  <!-- Spacer-->
  <div flex="40" class="cw-input-placeholder">&nbsp;</div>
  <cw-input-dropdown flex="initial"
                     class="cw-input cw-comparator-selector"
                     [model]="comparisonDropdown"
                     (change)="handleComparisonChange($event)"></cw-input-dropdown>
  <cw-input-text flex
                 layout-fill
                 class="cw-input"
                 (change)="handleParamValueChange($event)"
                 [model]="inputText">
  </cw-input-text>
</div>`
})
export class ServersideCondition {
  comparisonOptions:Array<DropdownOption> = [];

  _model:ConditionModel;

  change:EventEmitter;
  private comparisonDropdown:DropdownModel

  private inputText: InputTextModel

  constructor() {
    this.change = new EventEmitter();
    this.comparisonDropdown = new DropdownModel("comparison", "Comparison", ["is"], this.comparisonOptions)

    this.inputText = new InputTextModel()
    this.inputText.placeholder = "Enter a value"
  }

  set model(model:ConditionModel){
    this._model = model;
    let opts = []
    model.conditionType.comparisons.forEach((comparison:any)=>{
      opts.push(new DropdownOption(comparison.id))
    })
    this.comparisonDropdown.options = opts
    this.comparisonDropdown.selected = [model.comparison]
    this.inputText.value = this._model.getParameter("isoCode")
  }

  get model():ConditionModel{
    return this._model
  }

  handleComparisonChange(event:any) {
    this.model.comparison = event.value
  }


  handleParamValueChange(event:any) {
    this.model.setParameter("isoCode", event.target.value)
  }

}

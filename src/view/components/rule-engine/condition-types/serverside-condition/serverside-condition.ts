import {Component,Input, Output, View, Attribute, EventEmitter,CORE_DIRECTIVES} from 'angular2/angular2';
import {Dropdown, DropdownModel, DropdownOption} from '../../../../../view/components/semantic/modules/dropdown/dropdown'
import {InputText} from "../../../semantic/elements/input-text/input-text";
import {ParameterDefinition} from "../../../../../api/util/CwInputModel";
import {CwDropdownInputModel} from "../../../../../api/util/CwInputModel";
import {CwInputDefinition} from "../../../../../api/util/CwInputModel";
import {CwTextInputModel} from "../../../../../api/util/CwInputModel";
import {CwComponent} from "../../../../../api/util/CwComponent";
import {ParameterModel} from "../../../../../api/rule-engine/Condition";
import {CwSpacerInputDefinition} from "../../../../../api/util/CwInputModel";
import {ServerSideFieldModel} from "../../../../../api/rule-engine/ServerSideFieldModel";

@Component({
  selector: 'cw-serverside-condition'
})
@View({
  directives: [CORE_DIRECTIVES, Dropdown, InputText],
  template: `<div flex layout-fill layout="row" layout-align="start-center" class="cw-condition-component-body">
  <template ngFor #input [ngForOf]="inputs" #islast="last">
    <div *ngIf="input.inputDef.type == 'spacer'" flex layout-fill class="cw-input cw-input-placeholder">&nbsp;</div>
    <cw-input-dropdown *ngIf="input.inputDef.type == 'dropdown'"
                       flex
                       layout-fill
                       class="cw-input"
                       [required]="input.field.required"
                       [class.cw-comparator-selector]="input.inputDef.name == 'comparison'"
                       [class.cw-last]="islast"
                       [model]="input.field"
                       (change)="handleParamValueChange($event, input)"></cw-input-dropdown>

    <cw-input-text *ngIf="input.inputDef.type == 'text'"
                   flex
                   layout-fill
                   class="cw-input"
                   [class.cw-last]="islast"
                   [required]="input.field.required"
                   [name]="input.field.name"
                   [placeholder]="input.field.placeholder"
                   [value]="input.field.value"
                   (blur)="handleParamValueChange($event, input)"></cw-input-text>
  </template>

</div>`
})
export class ServersideCondition {

  @Input() model:ServerSideFieldModel
  @Input() paramDefs:{ [key:string]:ParameterDefinition}
  @Output() change:EventEmitter<ServerSideFieldModel>

  private inputs:Array<{ inputDef:CwInputDefinition, field:any}>

  constructor() {
    this.change = new EventEmitter();
    this.inputs = [];
  }

  ngOnChanges(change){
    console.log("ServersideCondition", "ngOnChanges", change)
    if(change.model){
      console.log("ServersideCondition", "ngOnChanges-value", change.model.currentValue)
    }
    if(change.paramDefs){
      let prevPriority = 0
      this.inputs = []
      Object.keys(this.paramDefs).forEach(key => {
        let paramDef = this.model.getParameterDef(key)
        let param = this.model.getParameter(key);
        if(paramDef.priority > (prevPriority + 1)){
          this.inputs.push({inputDef: new CwSpacerInputDefinition(40), field:null})
        }
        prevPriority = paramDef.priority
        let input = {inputDef: paramDef.inputType, field: this.inputModelFromCondition(param, paramDef)}
        this.inputs.push(input)
      })
    }
  }

  inputModelFromCondition(param:ParameterModel, paramDef:ParameterDefinition):CwComponent {
    let field:any = {}

    if (paramDef.inputType.type === 'text') {
      field.name = param.key
      field.placeholder = paramDef.key
      field.value = this.model.getParameterValue(param.key)
      field.required = paramDef.inputType.dataType['minLength'] > 0
    } else if (paramDef.inputType.type === 'dropdown') {
      field = DropdownModel.fromParameter(param, paramDef)

    }
    return field
  }

  handleParamValueChange(value:any, input:any) {
    this.model.setParameter(input.field.name, value)
    this.change.emit(this.model)
  }

}

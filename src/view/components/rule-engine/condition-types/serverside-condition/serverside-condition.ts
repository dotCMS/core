import {Component,Input, Output, View, Attribute, EventEmitter,CORE_DIRECTIVES} from 'angular2/angular2';
import {Dropdown, InputOption} from '../../../../../view/components/semantic/modules/dropdown/dropdown'
import {Observable} from 'rxjs/Rx.KitchenSink'

import {InputText} from "../../../semantic/elements/input-text/input-text";
import {ParameterDefinition} from "../../../../../api/util/CwInputModel";
import {CwDropdownInputModel} from "../../../../../api/util/CwInputModel";
import {CwInputDefinition} from "../../../../../api/util/CwInputModel";
import {CwTextInputModel} from "../../../../../api/util/CwInputModel";
import {CwComponent} from "../../../../../api/util/CwComponent";
import {ParameterModel} from "../../../../../api/rule-engine/Condition";
import {CwSpacerInputDefinition} from "../../../../../api/util/CwInputModel";
import {ServerSideFieldModel} from "../../../../../api/rule-engine/ServerSideFieldModel";
import {I18nService} from "../../../../../api/system/locale/I18n";

@Component({
  selector: 'cw-serverside-condition'
})
@View({
  directives: [CORE_DIRECTIVES, Dropdown, InputOption, InputText],
  template: `<div flex layout-fill layout="row" layout-align="start-center" class="cw-condition-component-body">
  <template ngFor #input [ngForOf]="_inputs" #islast="last">
    <div *ngIf="input.type == 'spacer'" flex layout-fill class="cw-input cw-input-placeholder">&nbsp;</div>
    <cw-input-dropdown *ngIf="input.type == 'dropdown'"
                       flex
                       layout-fill
                       class="cw-input"
                       [value]="input.value"
                       placeholder="{{input.placeholder}}"
                       [required]="input.required"
                       [class.cw-comparator-selector]="input.name == 'comparison'"
                       [class.cw-last]="islast"
                       (change)="handleParamValueChange($event, input)">
                       <cw-input-option
            *ngFor="#opt of input.options"
            [value]="opt.value"
            [label]="opt.label | async"
            icon="{{opt.icon}}"></cw-input-option>
    </cw-input-dropdown>

    <cw-input-text *ngIf="input.type == 'text'"
                   flex
                   layout-fill
                   class="cw-input"
                   [class.cw-last]="islast"
                   [required]="input.required"
                   [name]="input.name"
                   [placeholder]="input.placeholder"
                   [value]="input.value"
                   (blur)="handleParamValueChange($event, input)"></cw-input-text>
  </template>

</div>`
})
export class ServersideCondition {

  @Input() model:ServerSideFieldModel
  @Input() paramDefs:{ [key:string]:ParameterDefinition}
  @Output() change:EventEmitter<ServerSideFieldModel>


  private _inputs:Array<any>
  private _resources:I18nService

  constructor(resources:I18nService) {
    this._resources = resources;
    this.change = new EventEmitter();
    this._inputs = [];
  }

  ngOnChanges(change) {
    console.log("ServersideCondition", "ngOnChanges", change)
    if (change.model) {
      console.log("ServersideCondition", "ngOnChanges-value", change.model.currentValue)
    }
    if (change.paramDefs) {
      let prevPriority = 0
      this._inputs = []
      Object.keys(this.paramDefs).forEach(key => {
        let paramDef = this.model.getParameterDef(key)
        let param = this.model.getParameter(key);
        if (paramDef.priority > (prevPriority + 1)) {
          this._inputs.push({type: 'spacer', flex: 40})
        }
        prevPriority = paramDef.priority
        this._inputs.push(this.getInputFor(paramDef.inputType.type, param, paramDef))
      })
    }
  }

  getInputFor(type:string, param, paramDef):any {
    let input
    if (type === 'text') {
      input = this.getTextInput(param, paramDef)
    } else if (type === 'dropdown') {
      input = this.getDropdownInput(param, paramDef)
    }
    input.type = type
    return input
  }

  private getTextInput(param, paramDef) {
    return {
      name: param.key,
      placeholder: paramDef.key,
      value: this.model.getParameterValue(param.key),
      required: paramDef.inputType.dataType['minLength'] > 0
    }
  };

  private getDropdownInput(param:ParameterModel, paramDef:ParameterDefinition):CwComponent {
    let inputType:CwDropdownInputModel = <CwDropdownInputModel>paramDef.inputType;
    let opts = []
    let options = inputType.options;
    let i18nBaseKey
    if (param.key == 'comparison') {
      i18nBaseKey = 'api.sites.ruleengine.rules.inputs.comparison'
    }
    else if(paramDef.i18nBaseKey){
      i18nBaseKey = paramDef.i18nBaseKey
    }
    else{
      i18nBaseKey = this.model.type.i18nKey + '.inputs.' + paramDef.key + '.options'
    }

    Object.keys(options).forEach((key:any)=> {
      let option = options[key]
      let labelKey = i18nBaseKey + '.' + option.i18nKey
      // hack for country - @todo ggranum: kill 'name' on locale?
      if(param.key === 'country'){
        labelKey = labelKey + '.name'
      }
      opts.push({
        value: key,
        label: this._resources.get(labelKey),
        icon: option.icon
      })
    })
    let input:any = {
      value: this.model.getParameterValue(param.key),
      name: param.key,
      placeholder: inputType.placeholder,
      options: opts,
      minSelections: inputType.minSelections,
      maxSelections: inputType.maxSelections,
      required: inputType.minSelections > 0
    }
    if (!input.value) {
      input.value = inputType.selected != null ? inputType.selected : ''
    }
    return input
  }


  handleParamValueChange(value:any, input:any) {
    this.model.setParameter(input.name, value)
    this.change.emit(this.model)
  }

}



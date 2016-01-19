import {Component, Input, Output, View, Attribute, EventEmitter} from 'angular2/core';
import {Control, Validators, ControlGroup, CORE_DIRECTIVES, FormBuilder, FORM_DIRECTIVES} from 'angular2/common';
import {Dropdown, InputOption} from '../../../../../view/components/semantic/modules/dropdown/dropdown'
import {Observable} from 'rxjs/Rx'

import {InputText} from "../../../semantic/elements/input-text/input-text";
import {InputDate} from "../../../semantic/elements/input-date/input-date";
import {ParameterDefinition} from "../../../../../api/util/CwInputModel";
import {CwDropdownInputModel} from "../../../../../api/util/CwInputModel";
import {CwInputDefinition} from "../../../../../api/util/CwInputModel";
import {CwTextInputModel} from "../../../../../api/util/CwInputModel";
import {CwComponent} from "../../../../../api/util/CwComponent";
import {ParameterModel} from "../../../../../api/rule-engine/Condition";
import {CwSpacerInputDefinition} from "../../../../../api/util/CwInputModel";
import {ServerSideFieldModel} from "../../../../../api/rule-engine/ServerSideFieldModel";
import {I18nService} from "../../../../../api/system/locale/I18n";
import {ObservableHack} from "../../../../../api/util/ObservableHack";
import {CwRestDropdownInputModel} from "../../../../../api/util/CwInputModel";
import {RestDropdown} from "../../../semantic/modules/restdropdown/RestDropdown";

@Component({
  selector: 'cw-serverside-condition'
})
@View({

  directives: [FORM_DIRECTIVES, CORE_DIRECTIVES, RestDropdown, Dropdown, InputOption, InputText, InputDate],
  template: `<form>
  <div flex layout="row" class="cw-condition-component-body">

    <template ngFor #input [ngForOf]="_inputs" #islast="last">
      <div *ngIf="input.type == 'spacer'" flex class="cw-input cw-input-placeholder">&nbsp;</div>
      <cw-input-dropdown *ngIf="input.type == 'dropdown'"
                         flex
                         class="cw-input"
                         [value]="input.value"
                         placeholder="{{input.placeholder | async}}"
                         [required]="input.required"
                         [allowAdditions]="input.allowAdditions"
                         [class.cw-comparator-selector]="input.name == 'comparison'"
                         [class.cw-last]="islast"
                         [hidden]="!input.visible"
                         (change)="setVisible($event, input); handleParamValueChange($event, input)">
        <cw-input-option
            *ngFor="#opt of input.options"
            [value]="opt.value"
            [label]="opt.label | async"
            icon="{{opt.icon}}"></cw-input-option>
      </cw-input-dropdown>

      <cw-input-rest-dropdown *ngIf="input.type == 'restDropdown'"
                              flex
                              class="cw-input"
                              [value]="input.value"
                              placeholder="{{input.placeholder | async}}"
                              optionUrl="{{input.optionUrl}}"
                              optionValueField="{{input.optionValueField}}"
                              optionLabelField="{{input.optionLabelField}}"
                              [required]="input.required"
                              [allowAdditions]="input.allowAdditions"
                              [class.cw-comparator-selector]="input.name == 'comparison'"
                              [class.cw-last]="islast"
                              (change)="handleParamValueChange($event, input)">
      </cw-input-rest-dropdown>

      <div flex layout-fill layout="column" *ngIf="input.type == 'text' || input.type == 'number'">
        <cw-input-text
            flex
            class="cw-input"
            [class.cw-last]="islast"
            [placeholder]="input.placeholder | async"
            [ngFormControl]="input.control"
            [type]="input.type"
            [hidden]="!input.visible"
            #fInput="ngForm"
        ></cw-input-text>
        <div flex="50" [hidden]="fInput.valid" class="name red basic label">[Required]</div>
      </div>

      <cw-input-date *ngIf="input.type == 'datetime' "
                     flex
                     class="cw-input"
                     [class.cw-last]="islast"
                     [placeholder]="input.placeholder | async"
                     type="datetime-local"
                     [hidden]="!input.visible"
                     [value]="input.value"
                     (blur)="handleParamValueChange($event, input)"></cw-input-date>


    </template>

  </div>
</form>`
})
export class ServersideCondition {

  @Input() model:ServerSideFieldModel
  @Input() paramDefs:{ [key:string]:ParameterDefinition}
  @Output() change:EventEmitter<ServerSideFieldModel>

  private _inputs:Array<any>
  private _resources:I18nService



  constructor(fb:FormBuilder, resources:I18nService) {
    this._resources = resources;
    this.change = new EventEmitter();
    this._inputs = [];
  }

  setVisible(value, input):void {
    if (value && input && input.name === 'comparison') {
      let idx = this._inputs.indexOf(input)
      let comparisonObj = input.options.filter((e)=> { return e.value == value })[0]
      if (comparisonObj && comparisonObj.value) {
        for (var i = idx + 1; i < this._inputs.length; i++) {
          this._inputs[i].visible = (i <= idx + comparisonObj.rightHandArgCount)
        }
      }
    }
  }

  ngOnChanges(change) {
    if (change.paramDefs) {
      let prevPriority = 0
      this._inputs = []
      let comparison
      Object.keys(this.paramDefs).forEach(key => {
        let paramDef = this.model.getParameterDef(key)
        let param = this.model.getParameter(key);
        if (paramDef.priority > (prevPriority + 1)) {
          this._inputs.push({type: 'spacer', flex: 40})
        }
        prevPriority = paramDef.priority
        let input = this.getInputFor(paramDef.inputType.type, param, paramDef)

        if (input.name === 'comparison') {
          comparison = input
        }

        this._inputs.push(input)
      })

      this._inputs.forEach(input => {
        this.setVisible(input.value, comparison)
      })
    }
  }

  getInputFor(type:string, param, paramDef:ParameterDefinition):any {

    let i18nBaseKey = paramDef.i18nBaseKey || this.model.type.i18nKey
    /* Save a potentially large number of requests by loading parent key: */
    this._resources.get(i18nBaseKey).subscribe(()=> {})

    let input
    if (type === 'text' || type === 'number') {
      input = this.getTextInput(param, paramDef, i18nBaseKey)
    } else if (type === 'datetime') {
      input = this.getDateTimeInput(param, paramDef, i18nBaseKey)
    } else if (type === 'restDropdown') {
      input = this.getRestDropdownInput(param, paramDef, i18nBaseKey)
    } else if (type === 'dropdown') {
      input = this.getDropdownInput(param, paramDef, i18nBaseKey)
    }
    input.type = type;
    return input
  }

  private getTextInput(param, paramDef, i18nBaseKey:string) {
    let rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key
    let placeholderKey = rsrcKey + '.placeholder'
    let vFns:Function[] = []
    let minLen = paramDef.inputType.dataType['minLength']
    if (minLen > 0) {
      vFns.push(Validators.required)
      vFns.push(Validators.minLength(minLen))
    }
    let control = new Control(this.model.getParameterValue(param.key), Validators.compose(vFns))
    control.valueChanges.debounceTime(250).subscribe((value) => {
      this.model.setParameter(param.key , value)
      this.change.emit(this.model)
    })
    return {
      name: param.key,
      placeholder: this._resources.get(placeholderKey, paramDef.key),
      control: control,
      required: paramDef.inputType.dataType['minLength'] > 0,
      visible: true
    }
  }

  private getDateTimeInput(param, paramDef, i18nBaseKey:string) {
    let rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key
    return {
      name: param.key,
      value: this.model.getParameterValue(param.key),
      required: paramDef.inputType.dataType['minLength'] > 0,
      visible: true
    }
  }

  private getRestDropdownInput(param, paramDef, i18nBaseKey:string) {
    let inputType:CwRestDropdownInputModel = <CwRestDropdownInputModel>paramDef.inputType;
    let rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key
    let placeholderKey = rsrcKey + '.placeholder'

    let currentValue = this.model.getParameterValue(param.key)
    let input:any = {
      value: currentValue,
      name: param.key,
      placeholder: this._resources.get(placeholderKey, paramDef.key),
      optionUrl: inputType.optionUrl,
      optionValueField: inputType.optionValueField,
      optionLabelField: inputType.optionLabelField,
      minSelections: inputType.minSelections,
      maxSelections: inputType.maxSelections,
      required: inputType.minSelections > 0,
      allowAdditions: inputType.allowAdditions,
      visible: true,
    }
    if (!input.value) {
      input.value = inputType.selected != null ? inputType.selected : ''
    }
    return input
  }

  private getDropdownInput(param:ParameterModel, paramDef:ParameterDefinition, i18nBaseKey:string):CwComponent {
    let inputType:CwDropdownInputModel = <CwDropdownInputModel>paramDef.inputType;
    let opts = []
    let options = inputType.options;
    let rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key
    let placeholderKey = rsrcKey + '.placeholder'
    if (param.key == 'comparison') {
      rsrcKey = 'api.sites.ruleengine.rules.inputs.comparison'
    }
    else {
      rsrcKey = rsrcKey + '.options'
    }

    let currentValue = this.model.getParameterValue(param.key)
    let needsCustomAttribute = currentValue != null

    Object.keys(options).forEach((key:any)=> {
      let option = options[key]
      if (needsCustomAttribute && key == currentValue) {
        needsCustomAttribute = false
      }
      let labelKey = rsrcKey + '.' + option.i18nKey
      // hack for country - @todo ggranum: kill 'name' on locale?
      if (param.key === 'country') {
        labelKey = i18nBaseKey + '.' + option.i18nKey + '.name'
      }

      opts.push({
        value: key,
        label: this._resources.get(labelKey, option.i18nKey),
        icon: option.icon,
        rightHandArgCount: option.rightHandArgCount
      })
    })

    if (needsCustomAttribute) {
      opts.push({
        value: currentValue,
        label: ObservableHack.of(currentValue)
      })
    }


    let input:any = {
      value: currentValue,
      name: param.key,
      placeholder: this._resources.get(placeholderKey, paramDef.key),
      options: opts,
      minSelections: inputType.minSelections,
      maxSelections: inputType.maxSelections,
      required: inputType.minSelections > 0,
      allowAdditions: inputType.allowAdditions,
      visible: true,
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



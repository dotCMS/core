import {Component, Input, Output, EventEmitter, ChangeDetectionStrategy} from '@angular/core';
import {FormBuilder} from '@angular/forms';
import {ParameterDefinition} from "../../../../../api/util/CwInputModel";
import {CwDropdownInputModel} from "../../../../../api/util/CwInputModel";
import {CwComponent} from "../../../../../api/util/CwComponent";
import {ServerSideFieldModel} from "../../../../../api/rule-engine/ServerSideFieldModel";
import {I18nService} from "../../../../../api/system/locale/I18n";
import {ObservableHack} from "../../../../../api/util/ObservableHack";
import {CwRestDropdownInputModel} from "../../../../../api/util/CwInputModel";
import {Verify} from "../../../../../api/validation/Verify";
import {ParameterModel} from "../../../../../api/rule-engine/Rule";

@Component({
  selector: 'cw-serverside-condition',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<form>
  <div flex layout="row" class="cw-condition-component-body">
    <template ngFor let-input [ngForOf]="_inputs">
      <div *ngIf="input.type == 'spacer'" flex class="cw-input cw-input-placeholder">&nbsp;</div>
      <cw-input-dropdown *ngIf="input.type == 'dropdown'"
                         flex
                         class="cw-input"
                         [hidden]="input.argIndex !== null && input.argIndex >= _rhArgCount"
                         [formControl]="input.control"
                         [required]="input.required"
                         [allowAdditions]="input.allowAdditions"
                         [class.cw-comparator-selector]="input.name == 'comparison'"
                         [class.cw-last]="islast"
                         (touch)="onBlur(input)"
                         placeholder="{{input.placeholder | async}}">
        <cw-input-option
            *ngFor="let opt of input.options"
            [value]="opt.value"
            [label]="opt.label | async"
            icon="{{opt.icon}}"></cw-input-option>
      </cw-input-dropdown>

      <div flex layout-fill layout="column" class="cw-input" [class.cw-last]="islast" *ngIf="input.type == 'restDropdown'">
        <cw-input-rest-dropdown flex
                                class="cw-input"
                                [value]="input.value"
                                [formControl]="input.control"
                                [hidden]="input.argIndex !== null && input.argIndex >= _rhArgCount"
                                placeholder="{{input.placeholder | async}}"
                                [minSelections]="input.minSelections"
                                [maxSelections]="input.maxSelections"
                                optionUrl="{{input.optionUrl}}"
                                optionValueField="{{input.optionValueField}}"
                                optionLabelField="{{input.optionLabelField}}"
                                [required]="input.required"
                                [allowAdditions]="input.allowAdditions"
                                [class.cw-comparator-selector]="input.name == 'comparison'"
                                [class.cw-last]="islast"
                                (touch)="onBlur(input)"
                                #rdInput="ngForm"
                                >
        </cw-input-rest-dropdown>
        <div flex="50" *ngIf="rdInput.touched && !rdInput.valid && (input.argIndex == null || input.argIndex < _rhArgCount)"
            class="name cw-warn basic label">{{getErrorMessage(input)}}</div>
      </div>

      <div flex layout-fill layout="column" class="cw-input" [class.cw-last]="islast" *ngIf="input.type == 'text' || input.type == 'number'">
        <cw-input-text
            flex
            [placeholder]="input.placeholder | async"
            [formControl]="input.control"
            [type]="input.type"
            [hidden]="input.argIndex !== null && input.argIndex >= _rhArgCount"
            (blur)="onBlur(input)"
            #fInput="ngForm"
        ></cw-input-text>
        <div flex="50" *ngIf="fInput.touched && !fInput.valid && (input.argIndex == null || input.argIndex < _rhArgCount)"
            class="name cw-warn basic label">{{getErrorMessage(input)}}</div>
      </div>

      <cw-input-date *ngIf="input.type == 'datetime'"
                     flex
                    layout-fill
                     class="cw-input"
                     [formControl]="input.control"
                     [class.cw-last]="islast"
                     [placeholder]="input.placeholder | async"
                     [hidden]="input.argIndex !== null && input.argIndex >= _rhArgCount"
                     [value]="input.value"
                     (blur)="onBlur(input)"
                     #gInput="ngForm"
      ></cw-input-date>
    </template>
  </div>
</form>`
})
export class ServersideCondition {

  @Input() componentInstance:ServerSideFieldModel
  @Output() parameterValueChange:EventEmitter<{name:string, value:string}> = new EventEmitter(false)

  private _inputs:Array<any>
  private _resources:I18nService
  private _rhArgCount:number

  private _errorMessageFormatters = {
    required: "Required",
    minLength: "Input must be at least ${len} characters long.",
    noQuotes: "Input cannot contain quote [\" or '] characters."
  }

  constructor(fb:FormBuilder, resources:I18nService) {
    this._resources = resources;
    this._inputs = [];
  }

  ngOnChanges(change) {
    let paramDefs = null
    if( change.componentInstance ) {
      this._rhArgCount = null
      paramDefs = this.componentInstance.type.parameters
    }
    if (paramDefs) {
      let prevPriority = 0
      this._inputs = []
      Object.keys(paramDefs).forEach(key => {

        let paramDef = this.componentInstance.getParameterDef(key)
        let param = this.componentInstance.getParameter(key);
        if (paramDef.priority > (prevPriority + 1)) {
          this._inputs.push({type: 'spacer', flex: 40})
        }
        prevPriority = paramDef.priority
        console.log("ServersideCondition", "onChange", "params", key, param)
        let input = this.getInputFor(paramDef.inputType.type, param, paramDef)
        this._inputs.push(input)
      })

      let comparison
      let comparisonIdx = null
      this._inputs.forEach((input:any, idx) => {
        if(ServersideCondition.isComparisonParameter(input)) {
          comparison = input
          this.applyRhsCount(comparison.value)
          comparisonIdx = idx
        } else if(comparisonIdx !== null){
          if(this._rhArgCount !== null ){
            input.argIndex = idx - comparisonIdx - 1
          }
        }
      })
      if(comparison){
        this.applyRhsCount(comparison.value)
      }
    }
  }

  /**
   * Brute force error messages from lookup table for now.
   * @todo look up the known error formatters by key ('required', 'minLength', etc) from the I18NResource endpoint
   * and pre-cache them, so that we can retrieve them synchronously.
   */
  getErrorMessage(input):string{
    let control = input.control
    let message = ""
    Object.keys(control.errors || {}).forEach((key) => {
      let err = control.errors[key]
       message +=  this._errorMessageFormatters[key]
      if(Object.keys(err).length){
        debugger
      }
    })
    return message
  }

  onBlur(input){
    if(input.control.dirty) {
      this.setParameterValue(input.name, input.control.value, input.control.valid, true)
    }
  }

  setParameterValue(name:string, value:any, valid:boolean, isBlur:boolean=false) {
    this.parameterValueChange.emit({name, value})
    if(name == 'comparison'){
      this.applyRhsCount(value)
    }
  }

  getInputFor(type:string, param, paramDef:ParameterDefinition):any {

    let i18nBaseKey = paramDef.i18nBaseKey || this.componentInstance.type.i18nKey
    /* Save a potentially large number of requests by loading parent key: */
    this._resources.get(i18nBaseKey).subscribe(()=> {})

    let input
    if (type === 'text' || type === 'number') {
      input = this.getTextInput(param, paramDef, i18nBaseKey)
      console.log("ServersideCondition", "getInputFor", type, paramDef)
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
    let control = ServerSideFieldModel.createNgControl(this.componentInstance, param.key)
    return {
      name: param.key,
      placeholder: this._resources.get(placeholderKey, paramDef.key),
      control: control,
      required: paramDef.inputType.dataType['minLength'] > 0
    }
  }

  private getDateTimeInput(param, paramDef, i18nBaseKey:string) {
    let rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key
    return {
      name: param.key,
      value: this.componentInstance.getParameterValue(param.key),
      control: ServerSideFieldModel.createNgControl(this.componentInstance, param.key),
      required: paramDef.inputType.dataType['minLength'] > 0,
      visible: true
    }
  }

  private getRestDropdownInput(param, paramDef, i18nBaseKey:string) {
    let inputType:CwRestDropdownInputModel = <CwRestDropdownInputModel>paramDef.inputType;
    let rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key
    let placeholderKey = rsrcKey + '.placeholder'

    let currentValue = this.componentInstance.getParameterValue(param.key)
    if(currentValue && ( currentValue.indexOf('"') != -1 || currentValue.indexOf("'") != -1) ){
      currentValue = currentValue.replace(/["']/g, '')
      this.componentInstance.setParameter(param.key, currentValue)
    }
    const control = ServerSideFieldModel.createNgControl(this.componentInstance, param.key)
    let input:any = {
      value: currentValue,
      name: param.key,
      control: control,
      placeholder: this._resources.get(placeholderKey, paramDef.key),
      optionUrl: inputType.optionUrl,
      optionValueField: inputType.optionValueField,
      optionLabelField: inputType.optionLabelField,
      minSelections: inputType.minSelections,
      maxSelections: inputType.maxSelections,
      required: inputType.minSelections > 0,
      allowAdditions: inputType.allowAdditions
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

    let currentValue = this.componentInstance.getParameterValue(param.key)
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
      control: ServerSideFieldModel.createNgControl(this.componentInstance, param.key),
      placeholder: this._resources.get(placeholderKey, paramDef.key),
      options: opts,
      minSelections: inputType.minSelections,
      maxSelections: inputType.maxSelections,
      required: inputType.minSelections > 0,
      allowAdditions: inputType.allowAdditions,
    }
    if (!input.value) {
      input.value = inputType.selected != null ? inputType.selected : ''
    }
    return input
  }



  private applyRhsCount(selectedComparison:string) {
    let comparisonDef = this.componentInstance.getParameterDef('comparison')
    let comparisonType:CwDropdownInputModel = <CwDropdownInputModel>comparisonDef.inputType
    let selectedComparisonDef = comparisonType.options[selectedComparison]
    this._rhArgCount = ServersideCondition.getRightHandArgCount(selectedComparisonDef)
  }

  private static getRightHandArgCount(selectedComparison) {
    let argCount = null
    if (selectedComparison) {
      argCount = Verify.isNumber(selectedComparison.rightHandArgCount)
          ? selectedComparison.rightHandArgCount
          : 1
    }
    return argCount
  }

  private static isComparisonParameter(input) {
    return input && input.name === 'comparison'
  }

  private static getSelectedOption(input, value) {
    let opt = null
    let optAry = input.options.filter((e)=> { return e.value == value })
    if(optAry && optAry.length === 1){
      opt = optAry[0]
    }
    return opt
  }

}



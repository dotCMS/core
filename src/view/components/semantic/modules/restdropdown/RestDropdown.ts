import { ElementRef, Component, View, Directive, ViewContainerRef, TemplateRef, EventEmitter, Attribute} from 'angular2/core';
import { Host, AfterViewInit, AfterViewChecked, OnDestroy, Output, Input, ChangeDetectionStrategy } from 'angular2/core';
import { CORE_DIRECTIVES,  } from 'angular2/common';
import {Http, HTTP_PROVIDERS} from 'angular2/http';

import {Observable} from 'rxjs/Rx'

import {Dropdown, InputOption} from '../dropdown/dropdown'
import {Verify} from "../../../../../api/validation/Verify";

@Component({
  selector: 'cw-input-rest-dropdown',
  viewProviders: [HTTP_PROVIDERS]
  //changeDetection: ChangeDetectionStrategy.OnPush
})
@View({
  template: `
  <cw-input-dropdown [value]="value" name="{{name}}" placeholder="{{placeholder}}" (change)="handleParamValueChange($event, input)">
        <cw-input-option *ngFor="#opt of _options" [value]="opt.value" [label]="opt.label" [icon]="opt.icon"></cw-input-option>
      </cw-input-dropdown>`,
  directives: [CORE_DIRECTIVES, Dropdown, InputOption]
})
export class RestDropdown {

  @Input() value:string
  @Input() name:string
  @Input() placeholder:string
  @Input() allowAdditions:boolean
  @Input() minSelections:number
  @Input() maxSelections:number
  @Input() optionUrl:string
  @Input() optionNameField:string
  @Input() optionValueField:string

  @Output() change:EventEmitter<any>

  private _http
  private _options:any[]

  constructor(http:Http) {
    this._http = http
    this.placeholder = ""
    this.optionNameField = "key"
    this.optionValueField = "value"
    this.allowAdditions = false
    this.minSelections = 0
    this.maxSelections = 1
    this.change = new EventEmitter()
    this.name = "dd-" + new Date().getTime() + Math.random()
  }

  handleParamValueChange(event) {
    this.change.emit(event)
  }

  ngOnChanges(change) {
    if (change.optionUrl) {
      this._http.get(change.optionUrl.currentValue)
          // Call map on the response observable to get the parsed people object
          .map((res:any)=> this.jsonEntriesToOptions(res))
          .subscribe(options => {
                this._options = options
              }
          );
    }
  }

  private jsonEntriesToOptions(res:any) {
    let valuesJson = res.json()
    let ary = []
    if (Verify.isArray(valuesJson)) {
      ary = valuesJson.map(valueJson => this.jsonEntryToOption(valueJson))
    } else {
      ary = Object.keys(valuesJson).map((key) => {
        return this.jsonEntryToOption(valuesJson[key], key)
      })
    }
    return ary
  };

  private jsonEntryToOption(json:any, key:string = null):{value:string, label:string} {
    let opt = {value: null, label: null}
    if (!json[this.optionNameField] && this.optionNameField === 'key' && key != null) {
      opt.value = key
    } else {
      opt.value = json[this.optionNameField]
    }
    opt.label = json[this.optionValueField]

    if(this.value === opt.value){
      console.log("AAAand stuff: ", opt)
    }
    return opt
  }

}



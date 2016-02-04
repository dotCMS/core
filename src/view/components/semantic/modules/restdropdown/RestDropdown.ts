import { ElementRef, Component, View, Directive, ViewContainerRef, TemplateRef, EventEmitter, Attribute} from 'angular2/core';
import { Host, AfterViewInit, AfterViewChecked, OnDestroy, Output, Input, ChangeDetectionStrategy } from 'angular2/core';
import { CORE_DIRECTIVES,  } from 'angular2/common';
import {Http, HTTP_PROVIDERS} from 'angular2/http';

import {Observable} from 'rxjs/Rx'

import {Dropdown, InputOption} from '../dropdown/dropdown'
import {Verify} from "../../../../../api/validation/Verify";
import {ApiRoot} from "../../../../../api/persistence/ApiRoot";
import {Observer} from "rxjs/Observer";

@Component({
  selector: 'cw-input-rest-dropdown',
  viewProviders: [HTTP_PROVIDERS],
  changeDetection: ChangeDetectionStrategy.OnPush
})
@View({
  template: `
  <cw-input-dropdown [value]="value"  placeholder="{{placeholder}}" (change)="handleParamValueChange($event, input)">
        <cw-input-option *ngFor="#opt of _options | async" [value]="opt.value" [label]="opt.label" [icon]="opt.icon"></cw-input-option>
      </cw-input-dropdown>`,
  directives: [CORE_DIRECTIVES, Dropdown, InputOption]
})
export class RestDropdown {

  @Input() value:string
  @Input() placeholder:string
  @Input() allowAdditions:boolean
  @Input() minSelections:number
  @Input() maxSelections:number
  @Input() optionUrl:string
  @Input() optionValueField:string
  @Input() optionLabelField:string

  @Output() change:EventEmitter<any>

  private _http
  private _options:Observer<any>
  private _apiRoot:ApiRoot

  constructor(http:Http, apiRoot: ApiRoot) {
    this._http = http
    this._apiRoot = apiRoot
    this.placeholder = ""
    this.optionValueField = "key"
    this.optionLabelField = "value"
    this.allowAdditions = false
    this.minSelections = 0
    this.maxSelections = 1
    this.change = new EventEmitter()
  }

  handleParamValueChange(event) {
    this.change.emit(event)
  }

  ngOnChanges(change) {
    if (change.optionUrl) {
      let requestOptionArgs = this._apiRoot.getDefaultRequestOptions()
      this._options = this._http.get(change.optionUrl.currentValue, requestOptionArgs)
          .map((res:any)=> this.jsonEntriesToOptions(res))
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
    if (!json[this.optionValueField] && this.optionValueField === 'key' && key != null) {
      opt.value = key
    } else {
      opt.value = json[this.optionValueField]
    }
    opt.label = json[this.optionLabelField]
    return opt
  }

}



import { Component, EventEmitter, Input, Output} from 'angular2/core';
import {CORE_DIRECTIVES} from 'angular2/common';

import {ActionTypeService} from "../../../api/rule-engine/ActionType";
import {ActionService, ActionModel} from "../../../api/rule-engine/Action";
import {Dropdown, InputOption} from "../semantic/modules/dropdown/dropdown";
import {I18nService} from "../../../api/system/locale/I18n";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";
import {ServersideCondition} from "./condition-types/serverside-condition/serverside-condition";
import {ParameterChangeEvent, TypeChangeEvent} from "./rule-engine";

@Component({
  selector: 'rule-action',
  template: `<div *ngIf="typeDropdown != null" flex layout="row" class="cw-rule-action cw-entry">
  <div flex="25" layout="row" class="cw-row-start-area">
    <cw-input-dropdown
      flex
      class="cw-type-dropdown"
      [value]="typeDropdown.value"
      placeholder="{{typeDropdown.placeholder | async}}"
      (change)="onTypeChange($event)">
        <cw-input-option
        *ngFor="#opt of typeDropdown.options"
        [value]="opt.value"
        [label]="opt.label | async"
        icon="{{opt.icon}}"></cw-input-option>
    </cw-input-dropdown>
  </div>
  <cw-serverside-condition flex="75"
                           class="cw-condition-component"
                           [componentInstance]="action"
                           (parameterValueChange)="onParameterValueChange($event)">
  </cw-serverside-condition>
  <div class="cw-btn-group cw-delete-btn">
    <div class="ui basic icon buttons">
      <button class="ui button" aria-label="Delete Action" (click)="removeAction()" [disabled]="!action.isPersisted()">
        <i class="trash icon"></i>
      </button>
    </div>
  </div>
</div>`, directives: [CORE_DIRECTIVES,
    ServersideCondition,
    Dropdown,
    InputOption
  ]
})
export class RuleActionComponent {

  @Input()  action:ActionModel
  @Input()  index:number = 0
  @Output() change:EventEmitter<any> = new EventEmitter(false)
  @Output() parameterValueChange:EventEmitter<ParameterChangeEvent> = new EventEmitter(false)
  @Output() typeChange:EventEmitter<TypeChangeEvent> = new EventEmitter(false)
  @Output() remove:EventEmitter<any> = new EventEmitter(false)

  typeDropdown:any

  private _types:{[key:string]: any} = {}


  constructor(private _actionService:ActionService, private _typeService:ActionTypeService, resources:I18nService) {

    _typeService.list().subscribe((types:ServerSideTypeModel[])=> {
      this.typeDropdown = {
        value: "",
        placeholder: resources.get("api.sites.ruleengine.rules.inputs.action.type.placeholder"),
        options: []
      }
      types.forEach(type => {
        this._types[type.key] = type
        let opt = { value: type.key, label: resources.get(type.i18nKey + '.name', type.i18nKey)}
        this.typeDropdown.options.push(opt)
      })
    })
  }

  ngOnChanges(change){
    if (change.action){
      console.log("RuleActionComponent", "ngOnChanges")
      if (this.typeDropdown && this.action.type) {
        if(this.action.type.key != 'NoSelection') {
          this.typeDropdown.value = this.action.type.key
        }
      }
    }
  }

  onTypeChange(value) {
    console.log("RuleActionComponent", "onTypeChange", value)
    const type = this._types[value]
    this.typeChange.emit({valid:true, isBlur:true, value:type, index:this.index, source:this.action})
  }


  onParameterValueChange(event:ParameterChangeEvent) {
    console.log("RuleActionComponent", "onParameterValueChange", event)
    this.parameterValueChange.emit(Object.assign({ source: this.action}, event))
  }

  removeAction() {
    this.remove.emit(this.action)
  }
}

import { Component, EventEmitter, Input, Output} from 'angular2/core';
import {CORE_DIRECTIVES} from 'angular2/common';

import {ActionTypeService} from "../../../api/rule-engine/ActionType";
import {ActionService, ActionModel} from "../../../api/rule-engine/Action";
import {Dropdown, InputOption} from "../semantic/modules/dropdown/dropdown";
import {I18nService} from "../../../api/system/locale/I18n";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";
import {ServersideCondition} from "./condition-types/serverside-condition/serverside-condition";

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
  @Input()  index:number
  @Output() change:EventEmitter<any> = new EventEmitter(false)
  @Output() parameterValueChange:EventEmitter<{action:ActionModel, name:string, value:string}> = new EventEmitter(false)
  @Output() remove:EventEmitter<any> = new EventEmitter(false)

  typeDropdown:any

  private _typeService:ActionTypeService
  private _actionService:ActionService;
  private _types:{[key:string]: any}


  constructor(actionService:ActionService, typeService:ActionTypeService, resources:I18nService) {
    this._types = {}

    this._actionService = actionService;
    this._typeService = typeService

    this.index = 0

    typeService.list().subscribe((types:ServerSideTypeModel[])=> {
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
      this.action = change.action.currentValue
      if (this.typeDropdown && this.action.type) {
        if(this.action.type.key != 'NoSelection') {
          this.typeDropdown.value = this.action.type.key
        }
      }
    }
  }

  onTypeChange(value) {
    this.action.type = this._types[value]
    // required to force change detection on child that doesn't reference type.
    this.action = new ActionModel(this.action.key, this.action.type, this.action.owningRule, this.action.priority)
    this.change.emit(this.action)
  }

  onParameterValueChange(event:{name:string, value:string}) {
    this.parameterValueChange.emit(Object.assign({action: this.action}, event))
  }

  removeAction() {
    this.remove.emit(this.action)
  }
}

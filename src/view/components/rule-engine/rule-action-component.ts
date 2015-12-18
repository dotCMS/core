import {Component, Directive, View, Inject} from 'angular2/angular2';
import {Input, Output, EventEmitter} from 'angular2/angular2';
import {CORE_DIRECTIVES} from 'angular2/angular2';

import {ActionModel} from "../../../api/rule-engine/Action";
import {ActionTypeService} from "../../../api/rule-engine/ActionType";
import {ActionService} from "../../../api/rule-engine/Action";
import {Dropdown, DropdownModel, DropdownOption} from "../semantic/modules/dropdown/dropdown";
import {I18nService} from "../../../api/system/locale/I18n";
import {RuleService} from "../../../api/rule-engine/Rule";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";
import {ServersideCondition} from "./condition-types/serverside-condition/serverside-condition";
import {CountryCondition} from "./condition-types/country/country-condition";
import {ConditionModel} from "../../../api/rule-engine/Condition";
import {ServerSideFieldModel} from "../../../api/rule-engine/ServerSideFieldModel";

@Component({
  selector: 'rule-action'})
@View({
  template: `<div *ngIf="typeDropdown != null" flex layout="row" layout-align="space-between-center" class="cw-rule-action cw-entry">
  <div flex="35" layout="row" layout-align="end-center" class="cw-row-start-area">
    <cw-input-dropdown
        class="cw-type-dropdown"
        [model]="typeDropdown"
        [value]="[action.type.key]"
        (change)="onActionTypeChange($event)">
    </cw-input-dropdown>
  </div>
  <div flex layout-fill class="cw-condition-row-main">
  <cw-serverside-condition class="cw-condition-component"
                           [model]="action"
                           [paramDefs]="action.type?.parameters"

                           (change)="onActionChange($event)">
  </cw-serverside-condition>
  </div>
  <div flex="5" layout="row" layout-align="end-center" class="cw-btn-group">
    <div class="ui basic icon buttons">
      <button class="ui button" aria-label="Delete Action" (click)="removeAction()" [disabled]="!action.isPersisted()">
        <i class="trash icon"></i>
      </button>
    </div>
  </div>
</div>`, directives: [CORE_DIRECTIVES,
    ServersideCondition,
    CountryCondition,
    Dropdown
  ]
})
export class RuleActionComponent {

  @Input()  action:ServerSideFieldModel
  @Input()  index:number
  @Output() change:EventEmitter<ServerSideFieldModel>
  @Output() remove:EventEmitter<ServerSideFieldModel>

  typeDropdown:DropdownModel

  private _typeService:ActionTypeService
  private _actionService:ActionService;

  workPls:string

  constructor(actionService:ActionService, typeService:ActionTypeService) {
    this.change = new EventEmitter()
    this.remove = new EventEmitter()

    this._actionService = actionService;
    this._typeService = typeService

    this.action = new ConditionModel(null, new ServerSideTypeModel())
    this.index = 0

    this.workPls = ""

    typeService.list().subscribe((types:ServerSideTypeModel[])=> {
      let opts = []
      types.forEach(type => {
        opts.push(new DropdownOption(type.key, type, type.rsrc.name))
      })
      this.typeDropdown = new DropdownModel('actionType', "Select an Action", [], opts)
    })
  }

  ngOnChanges(change){
    console.log("RuleActionComponent", "ngOnChanges", change)
    if (change.action){
      console.log("RuleActionComponent", "ngOnChanges-value", change.action.currentValue)
      this.action = change.action.currentValue
      if (this.typeDropdown && this.action.type) {
        this.typeDropdown.selected = [this.action.type.key]
      }
    }
  }

  onActionTypeChange(value) {
    console.log("RuleActionComponent", "onActionTypeChange", value)
    this.action.type = value
    this.workPls = "nrrrg-" + new Date().getTime()
    this.change.emit(this.action)
  }

  onActionChange(event) {
    console.log("RuleActionComponent", "onActionChange")
    this.change.emit(this.action)
  }

  removeAction() {
    this.remove.emit(this.action)
  }
}

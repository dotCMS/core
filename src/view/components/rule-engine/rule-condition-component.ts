import {Attribute, Component, Directive, View, EventEmitter} from 'angular2/angular2';
import {Input, Output} from 'angular2/angular2';
import {CORE_DIRECTIVES} from 'angular2/angular2';
import {Observable} from 'rxjs/Rx.KitchenSink'

import {ServersideCondition} from './condition-types/serverside-condition/serverside-condition'
import {CountryCondition} from './condition-types/country/country-condition'
import {ConditionService, ConditionModel} from "../../../api/rule-engine/Condition";
import {CwChangeEvent} from "../../../api/util/CwEvent";

import {Dropdown, InputOption} from '../../../view/components/semantic/modules/dropdown/dropdown'
import {ConditionTypeService} from "../../../api/rule-engine/ConditionType";
import {RuleService} from "../../../api/rule-engine/Rule";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";
import {I18nService} from "../../../api/system/locale/I18n";


@Component({
  selector: 'rule-condition',
  properties: ["condition", "index"]
})
@View({
  template: `
<div *ngIf="typeDropdown != null" flex layout-fill layout="row" layout-align="space-between-center" class="cw-condition cw-entry">
  <div flex="35" layout="row" layout-align="end-center" class="cw-row-start-area">
    <div flex class="cw-btn-group cw-condition-toggle">
      <button flex class="ui basic button cw-button-toggle-operator" aria-label="Swap And/Or" (click)="toggleOperator()" *ngIf="index !== 0">
        {{condition.operator}}
      </button>
    </div>
     <cw-input-dropdown
        class="cw-type-dropdown"
        [value]="condition.type.key"
        placeholder="{{typeDropdown.placeholder | async}}"
        (change)="onTypeChange($event)">
         <cw-input-option
            *ngFor="#opt of typeDropdown.options"
            [value]="opt.value"
            [label]="opt.label | async"
            icon="{{opt.icon}}"></cw-input-option>
    </cw-input-dropdown>
  </div>
  <div flex layout-fill class="cw-condition-row-main" [ngSwitch]="condition.type?.key">
    <template [ngSwitchWhen]="'XUsersCountryConditionlet'">
      <cw-country-condition
          class="cw-condition-component"
          [comparator-value]="condition.comparison"
          [parameter-values]="condition.parameters"
          (change)="onConditionChange($event)">
      </cw-country-condition>

    </template>
    <template [ngSwitchWhen]="'NoSelection'">
      <div class="cw-condition-component"></div>
    </template>
    <template ngSwitchDefault>
      <cw-serverside-condition class="cw-condition-component"
                               [model]="condition"
                               [paramDefs]="condition.type?.parameters"
                               (change)="onConditionChange($event)">
      </cw-serverside-condition>
    </template>
  </div>
  <div flex="5" layout="row" layout-align="end-center" class="cw-btn-group">
    <div class="ui basic icon buttons">
      <button class="ui button" aria-label="Delete Condition" (click)="removeCondition()">
        <i class="trash icon"></i>
      </button>
    </div>
  </div>
</div>
`,
  directives: [CORE_DIRECTIVES,
    ServersideCondition,
    CountryCondition,
    Dropdown,
    InputOption
  ]
})
export class ConditionComponent {

  @Input() condition:ConditionModel
  @Input() index:number
  @Output() change:EventEmitter<ConditionModel>
  @Output() remove:EventEmitter<ConditionModel>
  typeDropdown:any

  private _typeService:ConditionTypeService
  private _conditionService:ConditionService;
  private _types:{[key:string]: any}

  constructor(conditionService:ConditionService, typeService:ConditionTypeService, resources:I18nService) {
    this.change = new EventEmitter()
    this.remove = new EventEmitter()

    this._conditionService = conditionService;
    this._typeService = typeService
    this._types = {}

    this.condition = new ConditionModel(null, new ServerSideTypeModel())
    this.index = 0

    typeService.list().subscribe((types:ServerSideTypeModel[])=> {
      this.typeDropdown = {
        placeholder: resources.get("api.sites.ruleengine.rules.inputs.condition.type.placeholder"),
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
    console.log("ConditionComponent", "ngOnChanges", change)
    if(change.condition){
      this.condition = change.condition.currentValue
      if(this.typeDropdown && this.condition.type){
        this.typeDropdown.value = this.condition.type.key
      }
    }
  }

  onTypeChange(value) {
    this.condition.type = this._types[value]
    this.change.emit(this.condition)
  }

  toggleOperator() {
    this.condition.operator = this.condition.operator === 'AND' ? 'OR' : 'AND'
    this.change.emit(this.condition)
  }

  onConditionChange(condition) {
    this.change.emit(condition)
  }

  removeCondition() {
    this.remove.emit(this.condition)
  }
}


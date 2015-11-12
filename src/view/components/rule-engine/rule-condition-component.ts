import {Attribute, Component, Directive, View, NgFor, NgIf, NgSwitch, NgSwitchWhen, NgSwitchDefault, EventEmitter, Inject} from 'angular2/angular2';

import {ServersideCondition} from './condition-types/serverside-condition/serverside-condition'
import {RequestHeaderCondition} from './condition-types/request-header/request-header-condition'
import {CountryCondition} from './condition-types/country/country-condition'
import {ConditionService, ConditionModel} from "../../../api/rule-engine/Condition";
import {CwChangeEvent} from "../../../api/util/CwEvent";

import {Dropdown, DropdownModel, DropdownOption} from '../../../view/components/semantic/modules/dropdown/dropdown'
import {ConditionTypeService, ConditionTypeModel} from "../../../api/rule-engine/ConditionType";


@Component({
  selector: 'rule-condition',
  properties: ["condition", "index"]
})
@View({
  template: `<div flex layout-fill layout="row" layout-align="space-between-center" class="cw-condition cw-entry">
  <div flex="30" layout="row" layout-align="end-center" class="cw-row-start-area">
    <div flex class="cw-btn-group cw-condition-toggle">
      <button flex class="ui basic button cw-button-toggle-operator" aria-label="Swap And/Or" (click)="toggleOperator()" *ng-if="index !== 0">
        {{condition.operator}}
      </button>
    </div>
    <cw-input-dropdown class="cw-condition-type-dropdown" [model]="conditionTypesDropdown" (change)="handleConditionTypeChange($event)"></cw-input-dropdown>
  </div>
  <div flex="65" layout-fill class="cw-condition-row-main" [ng-switch]="condition.conditionType?.key">
    <template [ng-switch-when]="'UsersBrowserHeaderConditionlet'">
      <cw-request-header-condition
          class="cw-condition-component"
          [comparator-value]="condition.comparison"
          [parameter-values]="parameterValues"
          (change)="conditionChanged($event)">
      </cw-request-header-condition>
    </template>
    <template [ng-switch-when]="'UsersCountryConditionlet'">
      <cw-country-condition
          class="cw-condition-component"
          [comparator-value]="condition.comparison"
          [parameter-values]="parameterValues"
          (change)="conditionChanged($event)">
      </cw-country-condition>

    </template>
    <template [ng-switch-when]="'NoSelection'">
      <div class="cw-condition-component"></div>
    </template>
    <template ng-switch-default>
      <cw-serverside-condition class="cw-condition-component"
                               [model]="condition"
                               (change)="conditionChanged($event)">
      </cw-serverside-condition>
    </template>
  </div>
  <div flex="5" layout="row" layout-align="end-center">
    <div flex class="cw-btn-group cw-condition-buttons">
      <div class="ui basic icon buttons">
        <button class="ui button" aria-label="Delete Condition" (click)="removeCondition()" [disabled]="!condition.isPersisted()">
          <i class="trash icon"></i>
        </button>
      </div>
    </div>
  </div>
</div>
`,
  directives: [NgIf, NgFor, NgSwitch, NgSwitchWhen, NgSwitchDefault,
    ServersideCondition,
    RequestHeaderCondition,
    CountryCondition,
    Dropdown,

  ]
})
export class ConditionComponent {
  index:number
  _condition:ConditionModel
  parameterValues:any
  conditionTypesDropdown:DropdownModel
  typeService:ConditionTypeService
  private _conditionService:ConditionService;

  constructor(@Inject(ConditionTypeService) typeService:ConditionTypeService, @Inject(ConditionService) conditionService:ConditionService) {
    this._conditionService = conditionService;
    this.typeService = typeService

    this.conditionTypesDropdown = new DropdownModel('conditionType', "Select a Condition")
    let condition = new ConditionModel()
    condition.conditionType = new ConditionTypeModel()
    this.condition = condition
    this.parameterValues = {}
    this.index = 0

    /* Note that 'typeService.list()' was called earlier, and the following observer relies on that fact. */
    typeService.onAdd.subscribe((conditionType:ConditionTypeModel)=> {
      this.conditionTypesDropdown.addOptions([new DropdownOption(conditionType.key, conditionType)])
    })
  }

  set condition(condition:ConditionModel) {
    this._condition = condition
    if (this._condition.conditionType) {
      this.conditionTypesDropdown.selected = [this._condition.conditionType.key]
    }

    this._condition.onChange.subscribe((event:CwChangeEvent<ConditionModel>)=> {
      if (event.target.isValid() && event.target.isPersisted()) {
        this._conditionService.save(event.target)
      }
      if (this._condition.conditionType) {
        this.conditionTypesDropdown.selected = [this._condition.conditionType.key]
      }

    })
    this.parameterValues = this.condition.parameters
  }

  get condition() {
    return this._condition;
  }

  handleConditionTypeChange(event) {
    this.condition.conditionType = event.target.model.selectedValues()[0]
    this.condition.clearParameters()
  }

  toggleOperator() {
    this.condition.operator = this.condition.operator === 'AND' ? 'OR' : 'AND'
  }

  removeCondition() {
    this._conditionService.remove(this._condition)
  }


  conditionChanged(event) {
    if (event.type == 'comparisonChange') {
      this.condition.comparison = event.value
    } else if (event.type == 'parameterValueChange') {
      event.value.forEach((param)=> {
        this.condition.setParameter(param.key, param.value)
      })
    }
  }
}


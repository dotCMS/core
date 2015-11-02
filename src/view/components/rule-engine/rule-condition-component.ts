/// <reference path="../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />


import {Attribute, Component, Directive, View, NgFor, NgIf, EventEmitter, Inject} from 'angular2/angular2';

import {ApiRoot} from 'api/persistence/ApiRoot'
import {ConditionTypesProvider, ConditionTypeModel} from 'api/rule-engine/ConditionTypes';


import {BrowserConditionlet} from './conditionlets/browser-conditionlet/browser-conditionlet'
import {RequestHeaderConditionlet} from './conditionlets/request-header-conditionlet/request-header-conditionlet'
import {CountryCondition} from './conditionlets/country/country-condition'
import {ConditionService, ConditionModel} from "api/rule-engine/Condition";
import {CwChangeEvent} from "api/util/CwEvent";
import {ConditionModel} from "api/rule-engine/Condition";

import {Dropdown, DropdownModel, DropdownOption} from 'view/components/semantic/modules/dropdown/dropdown'
import {DropdownModel} from "../semantic/modules/dropdown/dropdown";
import {DropdownOption} from "../semantic/modules/dropdown/dropdown";


@Component({
  selector: 'rule-condition',
  properties: ["condition", "index"]
})
@View({
  template: `<div flex="grow" layout="row" layout-align="space-between-center" class="cw-condition cw-entry">
  <div flex="none" layout="row" layout-align="start-center" class="cw-row-start-area">
    <div flex="none" class="cw-btn-group cw-condition-toggle">
      <div class="ui basic icon buttons" (click)="toggleOperator()" *ng-if="index !== 0">
        <button flex="none" class="ui button cw-button-toggle-operator" aria-label="Swap And/Or" (click)="toggleOperator()">
          {{condition.operator}}
        </button>
      </div>
    </div>
    <cw-input-dropdown [model]="conditionTypesDropdown" (change)="handleConditionTypeChange($event)"></cw-input-dropdown>
  </div>
    <cw-request-header-conditionlet
        class="cw-condition-component"
        *ng-if="condition.conditionType?.id == 'UsersBrowserHeaderConditionlet'"
        [comparator-value]="condition.comparison"
        [comparison-values]="conditionValue"
        (change)="conditionChanged($event)">
    </cw-request-header-conditionlet>
    <cw-country-condition
        class="cw-condition-component"
        *ng-if="condition.conditionType?.id == 'UsersCountryConditionlet'"
        [comparator-value]="condition.comparison"
        [comparison-values]="conditionValue"
        (change)="conditionChanged($event)">
    </cw-country-condition>
    <div class="cw-condition-component"
         *ng-if="condition.conditionType.id == 'NoSelection'">
    </div>
    <div flex="none" class="cw-btn-group cw-condition-buttons">
      <div class="ui basic icon buttons">
        <button class="ui button" aria-label="Delete Condition" (click)="removeCondition()" [disabled]="!condition.isPersisted()">
          <i class="trash icon" ></i>
        </button>
      </div>
  </div>
</div>
`,
  directives: [NgIf, NgFor,
    RequestHeaderConditionlet,
    CountryCondition,
    BrowserConditionlet,
    Dropdown
  ]
})
export class ConditionComponent {
  index:number
  _condition:ConditionModel
  conditionValue:any
  conditionTypesDropdown:DropdownModel
  typesProvider:ConditionTypesProvider
  private conditionServce:ConditionService;

  constructor(@Inject(ConditionTypesProvider) typesProvider:ConditionTypesProvider, @Inject(ConditionService) conditionServce:ConditionService) {
    this.conditionServce = conditionServce;
    this.typesProvider = typesProvider

    this.conditionTypesDropdown = new DropdownModel('conditionType', "Select a Condition", [], [])
    let condition = new ConditionModel()
    condition.conditionType = new ConditionTypeModel('NoSelection', {})
    this.condition = condition
    this.conditionValue = ''
    this.index = 0

    typesProvider.promise.then(()=> {
      let opts = []
      typesProvider.ary.forEach((type)=>{
        opts.push(new DropdownOption(type.id))
      })
      this.conditionTypesDropdown.addOptions(opts)
    })
  }

  set condition(condition:ConditionModel) {
    this._condition = condition
    if(this._condition.conditionType){
      this.conditionTypesDropdown.selected = [this._condition.conditionType.id]
    }

    this._condition.onChange.subscribe((event:CwChangeEvent<ConditionModel>)=> {
      if (event.target.isValid() && event.target.isPersisted()) {
        this.conditionServce.save(event.target)
      }
      if(this._condition.conditionType){
        this.conditionTypesDropdown.selected = [this._condition.conditionType.id]
      }

    })
    this.conditionValue = this.condition.parameters
  }

  get condition() {
    return this._condition;
  }

  handleConditionTypeChange(event){
    this.condition.conditionType = this.typesProvider.getType(event.target.model.selected[0])
    this.condition.clearParameters()
  }

  toggleOperator() {
    this.condition.operator = this.condition.operator === 'AND' ? 'OR' : 'AND'
  }

  removeCondition() {
    this.conditionServce.remove(this._condition)
  }


  conditionChanged(event) {
    let target = event.ngTarget
    let val = target.value
    this.condition.comparison = val.comparatorValue
    let parameterKeys = val['parameterKeys']

    parameterKeys.forEach((key)=> {
      this.condition.setParameter(key, val[key])
    })

  }
}


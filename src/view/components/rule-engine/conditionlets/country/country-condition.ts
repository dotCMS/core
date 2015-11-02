/// <reference path="../../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />

import {Component, View, Attribute, EventEmitter, NgFor, NgIf, Inject} from 'angular2/angular2';
import {I18NCountryProvider} from 'api/system/locale/I18NCountryProvider'

import {Dropdown, DropdownModel, DropdownOption} from 'view/components/semantic/modules/dropdown/dropdown'


export class CountryConditionModel {
  parameterKeys:Array<string> = ['isoCode']

  isoCode:string;
  comparatorValue:string;

  constructor(comparatorValue:string = null, isoCode:string = null) {
    this.comparatorValue = comparatorValue
    this.isoCode = isoCode
  }

  clone():CountryConditionModel {
    return new CountryConditionModel(this.comparatorValue, this.isoCode)
  }
}

@Component({
  selector: 'cw-country-condition',
  properties: [
    "comparatorValue", "comparisonValues",
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [NgFor, Dropdown],
  template: `<div flex="grow" layout="row" layout-align="start-center" class="cw-condition-component">
  <!-- Spacer-->
  <div flex="20" class="cw-input">&nbsp;</div>
  <select flex="20" class="cw-input" [value]="value.comparatorValue" (change)="updateComparator($event)">
    <option [selected]="cOpt === value.comparatorValue" value="{{cOpt}}" *ng-for="var cOpt of comparisonOptions">
      {{cOpt}}
    </option>
  </select>
  <cw-input-dropdown flex class="cw-clause-selector" [model]="countryDropdown" (change)="handleCountryChange($event)"></cw-input-dropdown>

</div>
  `
})
export class CountryCondition {
  change:EventEmitter;

  private countryDropdown:DropdownModel

  comparisonOptions:Array<string> = ["is", "is not"
    //"startsWith", "endsWith", "contains", "regex"
  ];

  countries:Array<any>

  value:CountryConditionModel

  constructor(@Inject(I18NCountryProvider) countryProvider:I18NCountryProvider) {
    this.countries = []
    this.change = new EventEmitter();
    this.value = new CountryConditionModel()
    this.countryDropdown = new DropdownModel("country", "Country")

    countryProvider.promise.then(()=> {
      var byNames = countryProvider.byName
      var names = countryProvider.names
      var tempCountries = []
      let opts = []
      names.forEach((name)=> {
        tempCountries.push({id: byNames[name], label: name})
        let id = byNames[name]
        opts.push(new DropdownOption(id, id, name, id.toLowerCase() + " flag"))
      })
      this.countries = tempCountries
      this.countryDropdown.addOptions(opts)
      if (this.value.isoCode === 'NoSelection') {
        this.value.isoCode = this.countries[0].id
      }
    })

  }

  _modifyEventForForwarding(event:Event, field, oldState:any):Event {
    Object.assign(event, {ngTarget: this, was: oldState, value: this.value, valueField: field})
    return event
  }

  set comparatorValue(value:string) {
    this.value.comparatorValue = value
  }

  set comparisonValues(value:any) {
    let isoCode = value[this.value.parameterKeys[0]]
    isoCode = isoCode ? isoCode.value : null
    if (!isoCode) {
      this.countryDropdown.selected = []
    } else {
      this.countryDropdown.selected = [isoCode]
    }
    this.value.isoCode = isoCode
  }

  updateComparator(event:Event) {
    let value = event.target['value']
    this.value.comparatorValue = value ? value.toLowerCase() : ''
    this.value.parameterKeys = ["isoCode"]
    this.change.next({type: 'comparisonChange', target: this, value: value})
  }

  handleCountryChange(event) {
    let isoCode = event.value
    this.value.parameterKeys = ["isoCode"]
    this.value.isoCode = isoCode
    this.change.next({type: 'countryChange', target: this, value: isoCode})
  }
}


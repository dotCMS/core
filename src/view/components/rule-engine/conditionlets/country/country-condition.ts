/// <reference path="../../../../../../typings/angular2/angular2.d.ts" />

import {Component, View, Attribute, EventEmitter, NgFor, NgIf, Inject} from 'angular2/angular2';
import {I18NCountryProvider} from 'api/system/locale/I18NCountryProvider'

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
    "comparatorValue", "comparisonValues"
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [NgFor],
  template: `
  <div class="col-sm-2">
  <select class="form-control comparator" [value]="value.comparatorValue" (change)="updateComparator($event)">
          <option [selected]="cOpt === value.comparatorValue" value="{{cOpt}}" *ng-for="var cOpt of comparisonOptions">{{cOpt}}</option>
        </select>
</div>
<div class="col-sm-4">
  <select class="form-control clause-selector" [value]="value.isoCode" (change)="updateComparisonValues($event)">
    <option value="{{country.id}}" *ng-for="var country of countries" [selected]="country.id == value.isoCode">{{country.label}}
    </option>
  </select>
</div>
  `
})
export class CountryCondition {
  change:EventEmitter;

  comparisonOptions:Array<string> = ["is",
    //"startsWith", "endsWith", "contains", "regex"
  ];

  countries:Array<any>

  value:CountryConditionModel

  constructor(@Inject(I18NCountryProvider) countryProvider:I18NCountryProvider) {
    this.countries = []
    this.change = new EventEmitter();
    this.value = new CountryConditionModel()

    countryProvider.promise.then(()=> {
      var byNames = countryProvider.byName
      var names = countryProvider.names
      var tempCountries = []
      names.forEach((name)=>{
        tempCountries.push({id: byNames[name], label: name})
      })
      this.countries = tempCountries
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
    isoCode = isoCode ? isoCode.value : ''
    this.value.isoCode = isoCode
  }

  updateComparator(event:Event) {
    let value = event.target['value']
    let e = this._modifyEventForForwarding(event, 'comparatorValue', this.value.clone())
    this.value.comparatorValue = value ? value.toLowerCase() : ''
    this.change.next(e)
  }

  updateComparisonValues(event:Event) {
    let value = event.target['value']
    let e = this._modifyEventForForwarding(event, 'comparisonValues', this.value.clone())
    this.value.isoCode = value
    this.change.next(e)
  }
}


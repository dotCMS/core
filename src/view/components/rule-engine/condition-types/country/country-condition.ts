import {Component, View, Attribute, EventEmitter, NgFor, NgIf, Inject} from 'angular2/angular2';


import {Dropdown} from "../../../semantic/modules/dropdown/dropdown";
import {DropdownOption} from "../../../semantic/modules/dropdown/dropdown";
import {I18NCountryProvider} from "../../../../../api/system/locale/I18NCountryProvider";
import {DropdownModel} from "../../../semantic/modules/dropdown/dropdown";

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
    "comparatorValue", "parameterValues",
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [NgFor, Dropdown],
  template: `<div flex layout="row" layout-align="start-center" class="cw-condition-component-body">
  <!-- Spacer-->
  <div flex="40" class="cw-input-placeholder">&nbsp;</div>
  <cw-input-dropdown flex="initial"
                     class="cw-input cw-comparator-selector"
                     [model]="comparatorDropdown"
                     (change)="handleComparatorChange($event)"></cw-input-dropdown>
  <cw-input-dropdown flex
                     layout-fill
                     class="cw-input"
                     [model]="countryDropdown"
                     (change)="handleCountryChange($event)"></cw-input-dropdown>

</div>
  `
})
export class CountryCondition {
  change:EventEmitter;


  comparisonOptions:Array<DropdownOption> = [
      new DropdownOption("is", "is", "Is"),
      new DropdownOption("isNot", "isNot", "Is Not")  ];

  countries:Array<any>

  value:CountryConditionModel
  private countryDropdown:DropdownModel
  private comparatorDropdown:DropdownModel

  constructor(@Inject(I18NCountryProvider) countryProvider:I18NCountryProvider) {
    this.countries = []
    this.change = new EventEmitter();
    this.value = new CountryConditionModel()
    this.comparatorDropdown = new DropdownModel("comparator", "Comparison", ["is"], this.comparisonOptions)
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

  set isoCode(isoCode:string){
    let selected = []
    this.value.isoCode = isoCode
    if (this.value.isoCode) {
      selected.push(this.value.isoCode)
    }
    this.countryDropdown.selected = selected
  }

  set comparatorValue(value:string) {
    this.value.comparatorValue = value
    this.comparatorDropdown.selected = [value]
  }

  set parameterValues(value:any) {
    this.value.parameterKeys.forEach((paramKey)=> {
      let v = value[paramKey]
      v = v ? v.value : ''
      this[paramKey] = v
    })
  }

  private getEventValue():Array<any>{
    let eventValue = []
    this.value.parameterKeys.forEach((key)=>{
      eventValue.push({key: key, value:this.value[key]})
    })
    return eventValue
  }

  handleComparatorChange(event) {
    let value = event.value
    this.value.comparatorValue = value
    this.change.next({type: 'comparisonChange', target: this, value: value})
  }

  handleCountryChange(event) {
    this.value.isoCode = event.value
    this.change.next({type:'parameterValueChange', target:this, value:this.getEventValue()})

  }
}


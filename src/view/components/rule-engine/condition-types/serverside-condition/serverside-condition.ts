
import {Component, View, Attribute, EventEmitter, NgFor, NgIf} from 'angular2/angular2';
import {Dropdown, DropdownModel, DropdownOption} from '../../../../../view/components/semantic/modules/dropdown/dropdown'
import {InputText, InputTextModel} from "../../../semantic/elements/input-text/input-text";


export class ServersideConditionModel {
  parameterKeys:Array<string> = ['headerKeyValue', 'compareTo']
  headerKeyValue:string
  comparatorValue:string
  compareTo:string

  constructor(comparatorValue:string = null) {
    this.comparatorValue = comparatorValue
  }

  clone():ServersideConditionModel {
    return new ServersideConditionModel(this.comparatorValue)
  }
}

@Component({
  selector: 'cw-serverside-condition',
  properties: [
    "comparatorValue", "parameterValues"
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [NgFor, Dropdown, InputText],
  template: `<div flex layout="row" layout-align="start-center" class="cw-condition-component-body">
  <!-- Spacer-->
  <div flex="40" class="cw-input">&nbsp;</div>
  <cw-input-dropdown flex="initial"
                     class="cw-input cw-comparator-selector"
                     [model]="comparatorDropdown"
                     (change)="handleComparatorChange($event)"></cw-input-dropdown>
  <cw-input-text flex
                 layout-fill
                 class="cw-input"
                 (change)="handleCompareToChange($event)"
                 [model]="requestHeaderInputTextModel">
  </cw-input-text>
</div>`
})
export class ServersideCondition {
  // @todo populate the comparisons options from the server.
  comparisonOptions:Array<DropdownOption> = [
    new DropdownOption("exists", "exists", "Exists"),
    new DropdownOption("is", "is", "Is"),
    new DropdownOption("is not", "is not", "Is Not"),
    new DropdownOption("startsWith", "startsWith", "Starts With"),
    new DropdownOption("endsWith", "endsWith", "Ends With"),
    new DropdownOption("contains", "contains", "Contains"),
    new DropdownOption("regex", "regex", "Regex")];

  value:ServersideConditionModel;

  change:EventEmitter;
  private comparatorDropdown:DropdownModel

  private requestHeaderInputTextModel: InputTextModel

  constructor(@Attribute('comparatorValue') comparatorValue:string,
              @Attribute('parameterValues') parameterValues:Array<string>) {
    this.value = new ServersideConditionModel(comparatorValue)
    this.change = new EventEmitter();
    this.comparatorDropdown = new DropdownModel("comparator", "Comparison", ["is"], this.comparisonOptions)


    this.requestHeaderInputTextModel = new InputTextModel()
    this.requestHeaderInputTextModel.placeholder = "Enter a value"
  }


  set compareTo(value:string) {
    this.value.compareTo = value
    this.requestHeaderInputTextModel.value = value
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

  handleComparatorChange(event:any) {
    let value = event.value
    this.value.comparatorValue = value
    this.change.next({type:'comparisonChange', target:this, value:value})
  }


  handleCompareToChange(event:any) {
    this.value.compareTo = event.target.value
    this.requestHeaderInputTextModel.value = event.target.value
    this.change.next({type:'parameterValueChange', target:this, value:this.getEventValue()})
  }

}
